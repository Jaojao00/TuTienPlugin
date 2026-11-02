# 🔒 Chính Sách Bảo Mật & An Toàn Hệ Thống (Security Policy)

Tài liệu này quy định các tiêu chuẩn bảo mật, cơ chế chống gian lận (Anti-Exploit / Anti-Dupe), quy trình ứng phó lỗ hổng và hướng dẫn bảo vệ máy chủ cho **TN-TuTienPlugin**.

---

## 1. 🛡️ Các Phiên Bản Được Hỗ Trợ (Supported Versions)

Chúng tôi cam kết phát hành bản vá bảo mật và hỗ trợ kỹ thuật chủ động cho các phiên bản sau:

| Phiên Bản Plugin | Nền Tảng Máy Chủ | Môi Trường Java | Trạng Thái Hỗ Trợ |
| :--- | :--- | :---: | :---: |
| **v2.3 (Enterprise)** | **Paper / Purpur / Spigot (1.20.6 - 1.21.x)** | **Java 21 LTS** | 🟢 **Hỗ trợ chủ động (Active)** |
| v2.0 - v2.2 | Paper / Spigot 1.20.4+ | Java 17 - 21 | 🟡 Chỉ vá lỗi bảo mật nghiêm trọng |
| v1.x (Legacy) | Spigot / Paper 1.19+ | Java 17 | 🔴 Hết thời hạn hỗ trợ (End of Life) |

---

## 2. 🔐 Kiến Trúc Bảo Mật & Cơ Chế Chống Gian Lận Cốt Lõi

Plugin được xây dựng theo tiêu chuẩn **Zero-Trust** với nhiều lớp bảo vệ độc lập, đảm bảo an toàn tuyệt đối cho nền kinh tế và dữ liệu máy chủ:

```
                  [Yêu Cầu Từ Client / Người Chơi]
                                 │
                                 ▼
              [Lớp 1: Anti-Glitch GUI & Input Sanitizer]
           (Hủy bỏ Drag, Hotbar Swap, Auto-Purge Nút GUI)
                                 │
                                 ▼
            [Lớp 2: Kiểm Tra Quyền Hạn & Rate-Limit / Cooldown]
            (Ngăn chặn Spam Packet / Privilege Escalation)
                                 │
                                 ▼
           [Lớp 3: PersistentDataContainer (PDC) Validation]
              (Chống dùng Đe đổi tên Anvil giả mạo đan dược)
                                 │
                                 ▼
           [Lớp 4: Giao Dịch Nguyên Tử (Atomic Vault & Escrow)]
           (State Machine: PREPARED ➔ ACTIVE ➔ COMMITTED/REFUND)
                                 │
                                 ▼
            [Lớp 5: Ghi Dữ Liệu An Toàn & Tự Phục Hồi Crash]
              (Atomic File Write, Crash Recovery on Startup)
```

### Chi Tiết Các Cơ Chế Bảo Vệ:

### A. Cơ Chế Ký Quỹ Không Thất Thoát (Zero-Loss Escrow Engine)
* **Vấn Đề:** Trong các máy chủ Skyblock và Survival, tình trạng mất mạng (disconnect), khởi động lại máy chủ hoặc crash giữa chừng khi đang luyện đan có thể làm người chơi mất trắng dược liệu quý hiếm hoặc sinh ra lỗi nhân bản (duplication bug).
* **Giải Pháp Bảo Mật:**
  1. Mọi phiên luyện đan đều trải qua cỗ máy trạng thái tuần tự: `PREPARED` $\to$ `ACTIVE` $\to$ `COMMITTED` $\to$ `CLAIMED`.
  2. Snapshot túi đồ và mã băm toàn vẹn SHA-256 được lưu trữ tức thời.
  3. Nếu phiên bị ngắt bất thường, trạng thái tự động chuyển thành `REFUND_PENDING`. Khi máy chủ khởi động lại hoặc người chơi kết nối lại, hệ thống đối soát và hoàn trả 100% nguyên liệu vào hòm đồ an toàn.
  4. Quá trình phát thưởng đan dược được đánh dấu `CLAIMED` ngay lập tức để ngăn ngừa nhận đan nhiều lần.

### B. Chống Rút Đồ & Khóa Chặt Giao Diện (Anti-Glitch GUI Protection)
* **Vấn Đề:** Người chơi sử dụng thủ thuật bấm phím tắt số (Number Keys 1-9), kéo chuột nhanh (Drag), dùng cheat client để rút các item biểu tượng trong GUI (Dược đỉnh, Băng khối, Đuốc, Sao Nether) vào túi đồ cá nhân.
* **Giải Pháp Bảo Mật:**
  1. Hủy bỏ triệt để (`event.setCancelled(true)`) trên mọi sự kiện `InventoryClickEvent` và `InventoryDragEvent` với độ ưu tiên cao nhất (`EventPriority.HIGHEST`).
  2. Bổ sung cơ chế **Tự Động Thanh Tẩy (Glitch Item Auto-Purge)**: Mỗi khi người chơi mở/đóng GUI, thả đồ (`PlayerDropItemEvent`), đặt block (`BlockPlaceEvent`) hoặc tương tác (`PlayerInteractEvent`), nếu phát hiện bất kỳ item nào mang tên biểu tượng hệ thống thì lập tức tịch thu và xóa sổ.

### C. Chống Giả Mạo Bằng Đe Đổi Tên (Anvil Anti-Forgery via PDC)
* Không dựa vào tên hiển thị (`DisplayName`) hoặc Lore thông thường để xác thực đan dược hay bí tịch.
* Mọi vật phẩm phát ra từ hệ thống đều được ký số bằng khóa Bukkit **PersistentDataContainer (PDC)** với canonical key (ví dụ: `tutien:dan_duoc_id`, `tutien:bitich_id`). Kẻ gian dùng đe đổi tên item thường thành đan dược sẽ hoàn toàn vô hiệu.

### D. Bảo Vệ Công Trình Skyblock (Skyblock Safe Environment)
* **Sét Lôi Kiếp An Toàn:** Lôi kiếp đột phá và đan lôi sử dụng hiệu ứng lôi đình giả lập (`World#strikeLightningEffect`) hoặc kiểm soát không lan truyền lửa (`block fire ticks`).
* **Không Cháy Nổ:** Thần thông diện rộng tuyệt đối không phá hủy block, không biến đổi Dân Làng (Villagers) thành Phù Thủy, không làm cháy đồ rớt trên đất.

---

## 3. 🚨 Báo Cáo Lỗ Hổng Bảo Mật (Reporting a Vulnerability)

Nếu bạn phát hiện bất kỳ lỗ hổng bảo mật nào (nhân bản item, bypass cảnh giới, crash máy chủ, lỗi kinh tế Vault):

1. **TUYỆT ĐỐI KHÔNG** đăng công khai lên diễn đàn, Discord chung hoặc tạo GitHub Issue công khai.
2. Gửi báo cáo chi tiết trực tiếp qua kênh bảo mật riêng:
   * **Email:** `tainguyenhr.dev@gmail.com`
   * **Tiêu đề email:** `[SECURITY] Báo cáo lỗ hổng TuTienPlugin - <Mô tả ngắn>`
   * **Nội dung bao gồm:**
     * Phiên bản plugin và phiên bản server Paper đang chạy (`/version`).
     * Các bước tái hiện chi tiết (kèm video hoặc ảnh chụp nếu có).
     * Mức độ ảnh hưởng ước tính (sập server, dupe đồ, leo quyền Admin).
     * Đề xuất hướng khắc phục (nếu bạn đã nghiên cứu được nguyên nhân).

### Cam Kết Thời Gian Phản Hồi:

| Giai Đoạn | Thời Gian Xử Lý |
| :--- | :--- |
| **Xác nhận tiếp nhận báo cáo** | Trong vòng **24 - 48 giờ** |
| **Phân tích nguyên nhân & Đánh giá rủi ro** | Trong vòng **3 - 5 ngày** |
| **Phát hành bản vá khẩn cấp (Hotfix)** | Trong vòng **7 ngày** (đối với lỗi nghiêm trọng/Critical) |

---

## 4. 📋 Khuyến Nghị Bảo Mật Dành Cho Quản Trị Viên Máy Chủ (Admin Checklist)

Để đảm bảo máy chủ vận hành an toàn và ổn định nhất, quản trị viên nên thực hiện các khuyến nghị sau:

* [ ] **Chỉ cấp quyền `tutien.admin` cho nhân sự cấp cao:** Không phân quyền này cho Helper hoặc Mod thông thường để tránh lạm dụng lệnh buff tu vi hoặc cấp vật phẩm.
* [ ] **Sao lưu định kỳ thư mục dữ liệu:** Đảm bảo lên lịch tự động sao lưu thư mục `plugins/TuTienPlugin/playerdata/` hàng ngày.
* [ ] **Không sử dụng lệnh `/reload`:** Luôn sử dụng `/tutien reload` để nạp lại file cấu hình, hoặc khởi động lại máy chủ (`/restart`) khi cập nhật file jar plugin.
* [ ] **Cập nhật Paper Build mới nhất:** Sử dụng các bản build ổn định của Paper 1.20.6 hoặc 1.21+ để có các bản vá chống exploit packet từ client.
* [ ] **Kiểm soát khu vực Luyện Đan bằng WorldGuard (Tùy chọn):** Nếu muốn hạn chế người chơi chỉ được luyện đan tại Đan Phòng của Tông Môn hoặc Spawn, bật tùy chọn `KhuVucLuyenDan.BatBuoc: true` trong `danduoc.yml`.

---

<div align="center">
  <sub>Bảo mật thông tin & an toàn tài sản là ưu tiên hàng đầu của chúng tôi • © 2026 TN-TuTienPlugin</sub>
</div>
