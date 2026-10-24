# ⛩ TN-TuTienPlugin — Hệ Thống Tu Tiên Minecraft (v2.2 Skyblock Edition)

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.6+-brightgreen?style=flat-square&logo=minecraft)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spigot](https://img.shields.io/badge/API-Spigot%2FPaper-yellow?style=flat-square)
![Version](https://img.shields.io/badge/Version-2.2_Skyblock-blue?style=flat-square)

> Plugin Minecraft RPG mô phỏng thế giới tu tiên chuyên sâu (tối ưu hóa hoàn hảo cho máy chủ **Skyblock** & **Survival**) — 17 cảnh giới, 7 đại đạo, 50+ công thức luyện đan phân trang, đột phá Cửu Đạo Thiên Kiếp an toàn cho đảo, chế tác trang bị 8 phẩm cấp cộng chỉ số thật, phụ bản Linh Thú Viên tích hợp máy đào đá (Cobblestone Generator), Gacha Linh Thú trợ chiến, Bảng Xếp Hạng Top 10 & Vạn Giới Các.

---

## 📋 Mục Lục

- [Tổng Quan](#-tổng-quan)
- [Điểm Mới Bản v2.2 Skyblock Edition](#-điểm-mới-bản-v22-skyblock-edition)
- [Yêu Cầu Hệ Thống](#-yêu-cầu-hệ-thống)
- [Hệ Thống Cảnh Giới & Đột Phá](#-hệ-thống-cảnh-giới--đột-phá)
- [Linh Căn & 7 Đại Đạo](#-linh-căn--7-đại-đạo)
- [Luyện Đan (50+ Đan Dược) & Chế Tác](#-luyện-đan--chế-tác-trang-bị)
- [Tích Hợp Skyblock Generator & Phụ Bản](#-tích-hợp-skyblock-generator--phụ-bản)
- [Gacha Linh Thú Trợ Chiến](#-gacha-linh-thú-trợ-chiến)
- [Bảng Xếp Hạng & PlaceholderAPI](#-bảng-xếp-hạng--placeholderapi)
- [Danh Sách Lệnh](#-danh-sách-lệnh)
- [Tác Giả](#-tác-giả)

---

## 🌟 Tổng Quan

**TN-TuTienPlugin** biến máy chủ Minecraft của bạn thành một thế giới tu tiên hoàn chỉnh. Người chơi bắt đầu từ **Phàm Nhân**, tọa thiền hấp thu linh khí ngay trên đảo, thu hoạch nông sản luyện đan, đào khoáng thạch tại máy tạo đá rèn trang bị 8 phẩm cấp, triệu hồi Linh Thú, vượt qua Cửu Đạo Thiên Kiếp và đăng lâm đỉnh cao **Tiên Đế**.

---

## 🚀 Điểm Mới Bản v2.2 Skyblock Edition

- **An Toàn Tuyệt Đối Cho Đảo Skyblock**:
  - Sấm sét Độ Kiếp và Thần Thông dùng `strikeLightningEffect` — **không gây cháy đảo gỗ**, **không biến Dân Làng (Villager) thành Phù Thủy (Witch)**, không phá `ArmorStand` / `ItemFrame`.
  - Lập Tông Môn (`/tutien laptong`) đặt Đại Điện ngay trên đảo hiện tại (kèm `/tutien tongmon sethome`), bỏ hoàn toàn cơ chế dịch chuyển ra tọa độ `(10000, 10000)` hư không.
  - **Ngự Kiếm Hộ Phong**: Tự động ban hiệu ứng *Rơi Chậm (Slow Falling) 10s* khi cạn Linh Lực giữa không trung để tránh rơi xuống vực Void, đồng thời tôn trọng quyền `/is fly` của các plugin Skyblock.
  - **Thuấn Di (Blink) An Toàn**: Chỉ kích hoạt khi cầm Kiếm và nhìn chếch lên trên (`pitch < -15°`), không bao giờ làm người chơi lao xuống vực khi đang bấm Shift bắc cầu (Speed-bridging).
- **Vòng Lặp Tài Nguyên Skyblock**:
  - Đào đá/quặng tại máy **Cobblestone / Ore Generator** nhận trực tiếp **Khoáng Thạch**, **Tu Vi** và tiến độ nhiệm vụ hàng ngày (tăng gấp 2.5 lần tỷ lệ + rớt **Đạo Niệm** khi bật chế độ *Thí Luyện Đạo Tràng*).
  - Bổ sung quầy **Đổi Khoáng Thạch** (`/tutien doikhoang` hoặc ô số 23 trong Vạn Giới Các).
- **50+ Loại Đan Dược & Phân Trang Lò Luyện Đan**:
  - Đầy đủ 23 đan Nhất–Tam Phẩm, 10 đan Tứ–Lục Phẩm, 5 đan Thất Phẩm+ và 16 Đan Đột Phá cho toàn bộ cảnh giới.
  - Hỗ trợ **Tự Động Luyện Đan** mỗi 5 giây và **Chuột Phải** ăn mọi loại đan để tăng vĩnh viễn Công Kích, Sinh Lực, Tu Vi.
- **Bảo Mật Vật Phẩm (`PersistentDataContainer`) & Đa Luồng (`ConcurrentHashMap`)**:
  - Chống dùng Đe (Anvil) đổi tên Đất/Đá thành Đan Đột Phá hoặc cất vào Túi Đồ Hư Không để farm tiền.
  - Trao vật phẩm an toàn (`giveItemSafe`): Tự động thả vật phẩm dưới chân khi đầy túi đồ, không bao giờ mất vật phẩm.

---

## 📦 Yêu Cầu Hệ Thống

| Thành phần | Phiên bản | Bắt buộc |
|-----------|----------|---------|
| Minecraft Server | 1.20.6+ | ✅ |
| Java | 21+ | ✅ |
| Spigot / Paper / Purpur | 1.20.6+ | ✅ |
| Vault + Economy Plugin | 1.7+ | ⚠️ Khuyến nghị |
| PlaceholderAPI | 2.11+ | ⚠️ Khuyến nghị |

---

## 🏔️ Hệ Thống Cảnh Giới & Đột Phá

- **17 Đại Cảnh Giới** (mỗi cảnh giới chia làm 10 trọng từ *Nhất Trọng* → *Thập Trọng* → *Viên Mãn*):
  `Phàm Nhân` → `Luyện Khí` → `Trúc Cơ` → `Kim Đan` → `Nguyên Anh` → `Hóa Thần` → `Luyện Hư` → `Hợp Thể` → `Đại Thừa` → `Độ Kiếp` → `Chân Tiên` → `Địa Tiên` → `Thiên Tiên` → `Huyền Tiên` → `Tiên Quân` → `Tiên Vương` → `Tiên Đế`.
- **Điều Kiện Độ Kiếp (`/tutien dokiep`)**:
  1. Đạt đủ mốc Tu Vi yêu cầu.
  2. Sở hữu viên **Đan Đột Phá** tương ứng (luyện tại Đan Lò, mua ở Vạn Giới Các hoặc đổi tại Shop Bế Quan).
  3. Đạt ngưỡng **Sinh Lực (HP) tối thiểu** (bằng cách ăn đan dược tăng máu vĩnh viễn và mặc Giáp/Phụ Kiện Linh Khí).
  4. Sống sót qua **9 đạo Thiên Lôi** giáng xuống liên tiếp.

---

## 🌿 Linh Căn & 7 Đại Đạo

- **6 Bậc Linh Căn**: Từ *Phế Linh Căn* (`x1.0`) đến *Hỗn Độn Linh Căn* (`x20.0` tốc độ tu luyện).
- **7 Đại Đạo (`/tutien menu` → Chọn Hệ)**:
  - 🗡️ **Kiếm Tu**: `+20%` sát thương khi cầm Kiếm, giảm `50%` Linh Lực tiêu hao khi Ngự Kiếm.
  - 🛡️ **Thể Tu**: `+40%` Sinh lực tối đa, giảm `10%` sát thương nhận vào.
  - 🔮 **Pháp Tu**: `+15%` sát thương, `+30%` giới hạn Linh Lực tối đa.
  - 👹 **Ma Tu**: `+30%` sát thương, nhận thêm `20%` sát thương.
  - 🐉 **Yêu Tu**: `+15%` tốc độ di chuyển, `x2` tốc độ hồi máu tự nhiên.
  - 🧪 **Đan Tu**: Tăng tỷ lệ luyện đan và `+25%` chỉ số vĩnh viễn khi phục dụng đan dược.
  - 💑 **Song Tu**: `+5%` HP/DMG cơ bản, `+15%` tốc độ Tu Vi khi tọa thiền gần đạo lữ.

---

## 🧪 Luyện Đan & ⚒️ Chế Tác Trang Bị

### Bát Quái Đan Lò (`/tutien luyendan`)
- Giao diện nhiều trang hiển thị toàn bộ **50+ công thức đan dược** trong `danduoc.yml`.
- Hỗ trợ nâng cấp Đan Lò (tăng tỷ lệ thành công) và chế độ **Tự Động Luyện Đan** khi treo máy.

### Lò Rèn Linh Khí (`/tutien chetac`)
- Sử dụng **Khoáng Thạch** để rèn 8 loại trang bị (Kiếm Thần Linh, Kiếm Huyền Thiết, Giáp Thần Lực, Giáp Thiên Phong, Mũ, Quần, Giày, Phụ Kiện Hộ Mệnh).
- **8 Phẩm Cấp**: `Phàm Phẩm` → `Hạ Phẩm` → `Trung Phẩm` → `Thượng Phẩm` → `Chí Bảo` → `Linh Bảo` → `Chân Bảo` → `Linh Khí`.
- Gắn trực tiếp `AttributeModifier` cộng **Sinh Lực tối đa**, **Giáp** và **Sát Thương** thật lên nhân vật.

---

## 🐉 Tích Hợp Skyblock Generator & Phụ Bản

Mở bằng lệnh `/tutien linhthuvien`:
1. **Huyền Ảnh Bí Cảnh** (*Luyện Khí+*): Triệu hồi đợt quái linh an toàn trên mặt đất đảo, tiêu diệt nhận Tu Vi và Đạo Niệm.
2. **Thí Luyện Đạo Tràng** (*Trúc Cơ+*): Cường hóa hoạt động đào mỏ tại máy Generator trên đảo — nhận x2.5 tỷ lệ Khoáng Thạch, Tu Vi và cơ hội rơi Đạo Niệm.
3. **Thiên Nguyên Cấm Địa** (*Nguyên Anh+*): Khiêu chiến **Cổ Thần Boss** nhận lượng lớn Tu Vi, Đạo Niệm, Khoáng Thạch và *Tinh Hoa Cổ Thần*.

---

## 🎴 Gacha Linh Thú Trợ Chiến

Mở bằng lệnh `/tutien gacha` (sử dụng **Đạo Niệm**, có bảo hiểm Pity 90 lần và hướng dẫn chi tiết ở ô số 0):
- **Hoa Linh Mao** (`60%`): `+5` Sinh lực tối đa.
- **Thanh Lang Vũ** (`20%`): `+10%` tốc độ Tu Vi AFK.
- **Huyết Phù Quy** (`10%`): `+15%` Phòng thủ (giảm sát thương nhận vào).
- **Thiên Mãu Hổ** (`6%`): `+20%` Sát thương cận chiến.
- **Long Mã Kim Cương** (`3%`): `+30%` Tốc độ di chuyển & bay.
- **Thiên Phượng** (`1%`): `+50%` tốc độ Tu Vi AFK & `+25` Sinh lực tối đa.

---

## 🏆 Bảng Xếp Hạng & PlaceholderAPI

Giao diện `/tutien xephang` hiển thị Top dẫn đầu theo 5 hạng mục. Các biến PlaceholderAPI hỗ trợ:

| Placeholder | Ý nghĩa |
|------------|---------|
| `%tutien_canhgioi%` | Cảnh giới hiện tại |
| `%tutien_tuvi%` / `%tutien_tuvi_max%` | Tu vi hiện tại / Tu vi yêu cầu đột phá |
| `%tutien_linhcan%` / `%tutien_he%` | Linh căn / Hệ tu luyện |
| `%tutien_linhluc%` / `%tutien_linhluc_max%` | Linh lực hiện tại / Tối đa |
| `%tutien_khoangthach%` | Số Khoáng Thạch sở hữu |
| `%tutien_daoniem%` | Số Đạo Niệm sở hữu |
| `%tutien_bequan_diem%` | Điểm Bế Quan tích lũy |
| `%tutien_bonus_damage%` / `%tutien_bonus_health%` | Sát thương / Sinh lực vĩnh viễn từ đan dược |
| `%tutien_canhgioi_#%` | Thứ hạng Cảnh Giới của người chơi (`#1`..`#10`) |
| `%tutien_tuvi_#%`, `%tutien_khoangthach_#%`, `%tutien_daoniem_#%` | Thứ hạng Tu Vi / Khoáng Thạch / Đạo Niệm cá nhân |
| `%tutien_canhgioi_1%` $\rightarrow$ `%tutien_canhgioi_10%` | Top 1 $\rightarrow$ 10 Cảnh Giới |
| `%tutien_tuvi_top_1%` $\rightarrow$ `%tutien_tuvi_top_10%` | Top 1 $\rightarrow$ 10 Tu Vi |
| `%tutien_khoangthach_top_1%` $\rightarrow$ `%tutien_khoangthach_top_10%` | Top 1 $\rightarrow$ 10 Khoáng Thạch |
| `%tutien_daoniem_top_1%` $\rightarrow$ `%tutien_daoniem_top_10%` | Top 1 $\rightarrow$ 10 Đạo Niệm |
| `%tutien_bequandiem_top_1%` $\rightarrow$ `%tutien_bequandiem_top_10%` | Top 1 $\rightarrow$ 10 Điểm Bế Quan |

---

## 📝 Danh Sách Lệnh

| Lệnh | Mô tả |
|-------|--------|
| `/tutien menu` | Mở giao diện trung tâm 54 ô |
| `/tutien tuluyen` | Bật/tắt tọa thiền tại chỗ (AFK) |
| `/tutien dokiep` | Mở giao diện Đột Phá & Độ Kiếp |
| `/tutien luyendan` | Mở Bát Quái Đan Lò (50+ loại đan) |
| `/tutien chetac` | Mở Lò Rèn Linh Khí |
| `/tutien doikhoang` | Mở quầy Quy Đổi Khoáng Thạch |
| `/tutien vangioi` | Mở cửa hàng Vạn Giới Các |
| `/tutien bequan` | Mở cửa hàng đổi Điểm Bế Quan |
| `/tutien gacha` | Mở giao diện Triệu Hồi Linh Thú |
| `/tutien linhthuvien` | Mở phụ bản Linh Thú Viên |
| `/tutien nhiemvu` | Mở bảng 6 Nhiệm Vụ Hàng Ngày |
| `/tutien tuido` | Mở Túi Đồ Hư Không |
| `/tutien xephang` | Mở Bảng Xếp Hạng Top 10 |
| `/tutien ngukiem` | Bật/tắt Ngự Kiếm Phi Hành |
| `/tutien laptong <tên>` | Khai tông lập phái trên đảo (Hóa Thần+) |
| `/tutien tongmon <moi/duoi/roi/ve/sethome>` | Quản lý Tông Môn |
| `/tiende <thiendiet/hophong/dialiet/tatdau/huyetkhi>` | Thi triển Thần Thông Tiên Đế |
| `/tutien admin <tuvi/linhluc/canhgioi/linhcan/he> ...` | Lệnh Quản Trị Viên (Thiên Đạo) |

---

## 👨‍💻 Tác Giả

- **Tài Dev Nguyễn**
- Phiên bản: `TN-TuTienPlugin v2.2 (Skyblock Edition)`
- Tương thích API: Spigot / Paper `1.20.6+`

> *"Vạn pháp quy nhất, nhất kiếm phá vạn pháp."* ⚔️
