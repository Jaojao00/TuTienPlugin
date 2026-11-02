# 🔑 HỆ THỐNG KHÓA BẢN QUYỀN (LICENSE KEY / LINEKEY)
### TN-TuTienPlugin Enterprise Edition — Quản Lý Giấy Phép Sử Dụng

Tài liệu này hướng dẫn chi tiết về cấu trúc, cơ chế xác thực và cách thức kích hoạt **Khóa Bản Quyền (License Key / LineKey)** cho máy chủ Minecraft sử dụng **TN-TuTienPlugin v2.3**.

---

## 1. 🏷️ Cấu Trúc Khóa Bản Quyền (LineKey Structure)

Mỗi khóa bản quyền (License Key) được tạo ra với cấu trúc mã hóa độc nhất, định danh theo định dạng chuẩn:

```
TT-KEY-[TIER]-[CLIENT_ID]-[CHECKSUM]-[EXPIRY]
```

### Chi Tiết Các Trường Dữ Liệu:
| Thành Phần | Ý Nghĩa & Quy Định | Ví Dụ |
| :--- | :--- | :--- |
| `TT-KEY` | Tiền tố nhận diện chuẩn của hệ thống Tu Tiên Plugin | `TT-KEY` |
| `[TIER]` | Phân hạng bản quyền: `ENT` (Enterprise), `COM` (Commercial), `STD` (Standard) | `ENT` |
| `[CLIENT_ID]` | Mã định danh khách hàng hoặc tên máy chủ đăng ký | `JAOMC` |
| `[CHECKSUM]` | Chuỗi băm mã hóa bảo mật SHA-256 rút gọn 8 ký tự | `9A8F-4B1C` |
| `[EXPIRY]` | Thời hạn giấy phép: `LIFETIME` (Vĩnh viễn) hoặc `YYYYMMDD` | `LIFETIME` |

👉 **Ví dụ Khóa Bản Quyền Hoàn Chỉnh:**
```yaml
LicenseKey: "TT-KEY-ENT-JAOMC-9A8F-4B1C-LIFETIME"
```

---

## 2. ⚙️ Hướng Dẫn Kích Hoạt Khóa Bản Quyền Trên Server

Để kích hoạt bản quyền cho máy chủ của bạn, thực hiện theo 3 bước sau:

### Bước 1: Mở tệp cấu hình chính
Truy cập vào thư mục máy chủ:
```
plugins/TuTienPlugin/config.yml
```

### Bước 2: Nhập thông tin Khóa Bản Quyền
Tìm đến mục `License` và điền thông tin bản quyền đã được cấp:
```yaml
# ============================================================
#               BẢN QUYỀN & XÁC THỰC MÁY CHỦ
# ============================================================
License:
  Enabled: true
  Owner: "Team Jao MC"
  Email: "tainguyenhr.dev@gmail.com"
  LicenseKey: "TT-KEY-ENT-JAOMC-9A8F-4B1C-LIFETIME"
  ServerIP: "AUTO" # Hoặc nhập dải IP cụ thể nếu dùng IP cố định
```

### Bước 3: Tải lại hoặc khởi động lại máy chủ
Chạy lệnh trong console hoặc trong game:
```
/tutien reload
```
Console sẽ xuất hiện thông báo xác nhận:
```
[TuTienPlugin] ========================================
[TuTienPlugin]  XÁC THỰC BẢN QUYỀN THÀNH CÔNG!
[TuTienPlugin]  Chủ sở hữu: Team Jao MC
[TuTienPlugin]  Phân hạng: Enterprise (Vĩnh viễn)
[TuTienPlugin] ========================================
```

---

## 3. 🛡️ Các Phân Hạng Bản Quyền (License Tiers)

| Quyền Lợi & Tính Năng | Standard (STD) | Commercial (COM) | Enterprise (ENT) |
| :--- | :---: | :---: | :---: |
| **Hệ Thống 17 Cảnh Giới Tiên Nghịch** | ✅ | ✅ | ✅ |
| **22 Bộ Công Pháp Tứ Đại Kỳ Thư** | ✅ | ✅ | ✅ |
| **Luyện Đan BTTH (4 Chỉ Số Hồn Lực)** | ❌ Cơ Bản | ✅ Đầy Đủ | ✅ Đầy Đủ + Tùy Biến Sâu |
| **Cơ Chế Ký Quỹ Zero-Loss Escrow** | ❌ | ✅ | ✅ |
| **Tích Hợp MythicMobs Boss Drops** | ❌ | ✅ | ✅ |
| **Số Lượng Cụm Server (Bungee/Velocity)** | 1 Server | Tối đa 3 Server | Không Giới Hạn Cụm |
| **Hỗ Trợ Kỹ Thuật Trực Tiếp** | Ticket 72h | Ưu tiên 24h | Kỹ sư hỗ trợ 24/7 |

---

## 4. 🔄 Quy Trình Cấp Lại & Đổi Máy Chủ (Transfer Policy)

1. **Đổi IP hoặc Nâng Cấp Phần Cứng Máy Chủ:**
   * Chủ sở hữu bản quyền chỉ cần gửi email từ địa chỉ đã đăng ký (`tainguyenhr.dev@gmail.com`) thông báo IP mới.
   * Hệ thống sẽ tự động cập nhật và duy trì hiệu lực của mã khóa hiện tại trong vòng 24 giờ mà không phát sinh chi phí.
2. **Nghiêm Cấm:**
   * Việc chuyển nhượng, cho thuê lại hoặc mua bán chung mã khóa bản quyền (Share License) sẽ dẫn đến việc khóa vĩnh viễn key trên toàn bộ hệ thống.

---

## 5. 📞 Kênh Hỗ Trợ Kích Hoạt & Cấp Khóa Bản Quyền

* **Đơn vị phát triển:** Team Jao MC
* **Lập trình viên phụ trách:** Tai Nguyen
* **Email:** `tainguyenhr.dev@gmail.com`
* **Vị trí file lưu trữ:** `C:\Users\ASUS\IdeaProjects\TuTienPlugin\LICENSE_KEY.md`
