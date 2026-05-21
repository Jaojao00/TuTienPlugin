# ⛩ TuTienPlugin — Hệ Thống Tu Tiên Minecraft (Bản Hoàn Thiện SPRINT 1→4)

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.6-brightgreen?style=flat-square&logo=minecraft)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spigot](https://img.shields.io/badge/API-Spigot-yellow?style=flat-square)
![Version](https://img.shields.io/badge/Version-2.0-blue?style=flat-square) 

> Plugin Minecraft RPG mô phỏng thế giới tu tiên — 17 cảnh giới, 7 đại đạo, luyện đan, đột phá lôi kiếp, tông môn bang hội, chế tác trang bị, phụ bản linh thú, gacha triệu hồi, và cửa hàng Vạn Giới Các.

---

## 📋 Mục Lục

- [Tổng Quan](#-tổng-quan)
- [Cập Nhật (Sprint 1-4)](#-cập-nhật-sprint-1-4)
- [Yêu Cầu](#-yêu-cầu)
- [Hệ Thống Cảnh Giới](#-hệ-thống-cảnh-giới)
- [Linh Căn & 7 Đại Đạo](#-linh-căn--7-đại-đạo)
- [Luyện Đan & Chế Tác](#-luyện-đan--chế-tác-trang-bị)
- [Nhiệm Vụ Hàng Ngày](#-nhiệm-vụ-hàng-ngày)
- [Phụ Bản & Gacha Linh Thú](#-phụ-bản--gacha-linh-thú)
- [Đột Phá & Lôi Kiếp](#-đột-phá--lôi-kiếp)
- [Tông Môn](#-tông-môn)
- [Ngự Kiếm Phi Hành](#-ngự-kiếm-phi-hành)
- [Cấm Thuật Tiên Đế](#-cấm-thuật-tiên-đế)
- [Lệnh & Placeholder](#-lệnh--placeholderapi)
- [Cấu Hình](#%EF%B8%8F-cấu-hình)
- [Tác Giả](#-tác-giả)

---

## 🌟 Tổng Quan

**TuTienPlugin** biến server Minecraft của bạn thành một thế giới tu tiên hoàn chỉnh. Người chơi sẽ bắt đầu từ **Phàm Nhân**, tu luyện qua 17 cảnh giới, chọn con đường tu luyện riêng, luyện đan dược, rèn linh khí, săn boss phụ bản, quay gacha linh thú, vượt qua lôi kiếp, và cuối cùng đạt đến đỉnh cao — **Tiên Đế**.

---

## 🚀 Cập Nhật (Sprint 1-4)

- **Sprint 1 (Bug Fix & System)**: Hoàn thiện data-driven (StatsManager), Async Save/Load, mở rộng đủ 17 loại đan dược cho Đột Phá, thêm cooldown Đột Phá 30s.
- **Sprint 2 (Mechanics)**: Ngự kiếm phi hành tiêu hao Linh Lực, Tọa thiền Song Tu (gần đạo lữ +15% tu vi), Shop đổi điểm Bế Quan lấy đan dược và vật phẩm quý.
- **Sprint 3 (Crafting & Quests)**: Lò Rèn Linh Khí 8 phẩm cấp (Phàm → Linh Khí) với tỷ lệ chế tác, Hệ thống 5 Nhiệm Vụ Hàng Ngày (reset mỗi 00:00).
- **Sprint 4 (Dungeons & Gacha)**: Linh Thú Viên (3 phân khu: Thí Luyện, Huyền Ảnh, Thiên Nguyên Boss), Gacha triệu hồi Linh Thú bằng Đạo Niệm (6 độ hiếm, Pity 90, Quay x10).

---

## 📦 Yêu Cầu

| Thành phần | Phiên bản | Bắt buộc |
|-----------|----------|---------|
| Minecraft Server | 1.20.6 | ✅ |
| Java | 21+ | ✅ |
| Spigot / Paper | 1.20.6 | ✅ |
| Vault | 1.7+ | ⚠️ Khuyến nghị |
| PlaceholderAPI | 2.11+ | ⚠️ Khuyến nghị |

---

## 🏔️ Hệ Thống Cảnh Giới

Mỗi đại cảnh giới chia thành **10 tiểu cảnh giới** (Nhất Trọng → Thập Trọng → Viên Mãn). 
Có tổng cộng **17 Cảnh Giới** từ Phàm Nhân (0) đến Tiên Đế (16). 
Chỉ số tu vi cần thiết, HP, DMG sẽ Scale theo từng cảnh giới (cấu hình trong `canhgioi.yml`).

---

## 🌿 Linh Căn & 7 Đại Đạo

- **6 Linh Căn**: Random khi vào game, từ Phế Linh Căn (x1) đến Hỗn Độn Linh Căn (x20 tốc độ tu luyện).
- **7 Đại Đạo**:
  - 🗡️ **Kiếm Tu**: +20% DMG Kiếm, -50% Linh lực bay
  - 🛡️ **Thể Tu**: +40% HP, +10% giảm sát thương
  - 🔮 **Pháp Tu**: +30% Linh lực (thay đổi trần linh lực thực tế)
  - 👹 **Ma Tu**: +30% DMG, -20% phòng thủ
  - 🐉 **Yêu Tu**: +15% tốc độ, x2 hồi phục tự nhiên
  - 🧪 **Đan Đạo**: +25% tỷ lệ luyện đan
  - 💑 **Song Tu**: +15% tu vi khi gần đạo lữ

---

## 🧪 Luyện Đan & ⚒️ Chế Tác Trang Bị

### Bát Quái Đan Lò (Luyện Đan)
- Luyện đan dược từ nguyên liệu thật (Lúa mì, Táo vàng, v.v.).
- Nâng cấp lò luyện bằng tiền Vault (lên đến cấp 6, tăng tỷ lệ thành công).
- Kết nối tự động làm nhiệm vụ hàng ngày.

### Lò Rèn Linh Khí (Chế Tác)
- Chế tác 6 loại trang bị (Kiếm, Giáp, Mũ, Quần, Giày, Phụ Kiện).
- 8 phẩm cấp: Phàm → Hạ → Trung → Thượng → Chí Bảo → Linh Bảo → Chân Bảo → Linh Khí.
- Nâng cấp lò rèn bằng Khoáng Thạch, tăng % ra phẩm cấp xịn.

---

## 📜 Nhiệm Vụ Hàng Ngày

5 Nhiệm vụ reset vào lúc 00:00 (UTC) mỗi ngày:
1. **Tu Luyện Cơ Bản**: Tọa thiền 30 phút.
2. **Thợ Luyện Đan**: Luyện 20 viên đan.
3. **Rèn Trang Bị**: Chế tác 5 món trang bị.
4. **Tu Bổ Tông Môn**: Đặt 30 khối trong tông môn.
5. **Hàng Ngày Giết Quái**: Giết 50 quái vật.

*Phần thưởng: Điểm Bế Quan & Tu Vi.*

---

## 🐉 Phụ Bản & Gacha Linh Thú

### Linh Thú Viên (Phụ Bản)
3 Phân khu cày cuốc chuyên biệt:
- **Thí Luyện Chi Địa** (Luyện Khí+): Farm tài nguyên nguyên liệu.
- **Huyền Ảnh Mê Cung** (Kim Đan+): Farm Đạo Niệm & Tu Vi.
- **Thiên Nguyên Đỉnh** (Hóa Thần+): Khiêu chiến Boss Khỉ Khổng Lồ (Scale HP/DMG theo người chơi).

### Vạn Thú Các (Gacha)
- Dùng **Đạo Niệm** (từ phụ bản) để triệu hồi thú cưỡi / pet.
- 6 Độ hiếm: N (60%) → R (25%) → SR (10%) → SSR (4%) → UR (0.9%) → EX (0.1%).
- **Pity System**: Đảm bảo ra SSR+ ở lần roll thứ 90. Roll x10 đảm bảo ít nhất 1 thẻ R+. Broadcast server khi ra thú xịn.

---

## ⚡ Đột Phá & Lôi Kiếp
- **Cửu Đạo Thiên Kiếp** cho người chơi từ Kim Đan trở lên.
- Phải có đủ Tu Vi + Đan Dược Đột Phá tương ứng (từ Tụ Khí Đan đến Tiên Đế Đan).
- Cửu Thiên Lôi Kiếp giáng xuống 9 lần sét, gây sát thương theo % Max HP. Thành công sẽ thăng cấp, thất bại mất 10% Tu Vi hiện tại.

---

## 🏯 Tông Môn
- Lập Tông (Hóa Thần+), tự động tạo kết giới an toàn. Thu nạp, đuổi đệ tử. Dịch chuyển về tông môn miễn phí.

---

## 🗡️ Ngự Kiếm Phi Hành
- Kích hoạt bằng `/tutien ngukiem` (Cần Kim Đan+).
- Tiêu tốn 50 Linh Lực mỗi 2 giây bay. Hết linh lực tự rơi xuống.

---

## 👑 Cấm Thuật Tiên Đế
5 kỹ năng tối thượng cho Tiên Đế:
- 🌩️ **Chân Thiên Diệt**: Gọi sét, sát thương AoE khủng.
- 🌪️ **Hô Phong Hoán Vũ**: Tạo bão sét xung quanh.
- 🏔️ **Địa Liệt Sơn Băng**: Động đất hất tung kẻ thù.
- ⚔️ **Tát Đậu Thành Binh**: Triệu hồi Golem đệ tử.
- ❤️ **Huyết Khí Vô Tận**: Hồi phục 100% HP và thêm lượng lớn máu ảo.

---

## 📝 Lệnh & PlaceholderAPI

### Lệnh Chính
| Lệnh | Mô tả |
|-------|--------|
| `/tutien menu` | Giao diện tổng |
| `/tutien tuluyen` | Tọa thiền (AFK) |
| `/tutien ngukiem` | Ngự kiếm phi hành |
| `/tutien bequan` | Cửa hàng điểm Bế Quan |
| `/tutien chetac` | Lò rèn trang bị |
| `/tutien nhiemvu` | Bảng nhiệm vụ ngày |
| `/tutien linhthuvien` | Vào phụ bản |
| `/tutien gacha` | Quay Linh Thú |
| `/tutien vangioi` | Cửa hàng Vạn Giới Các |
| `/tutien dokiep` | Đột phá cảnh giới |
| `/tutien tongmon` | Quản lý tông môn |
| `/tiende` | Thi triển Cấm Thuật (Tiên Đế) |
| `/tutien admin ...` | Quản trị viên (Set Tu Vi, Linh Lực, Cảnh Giới...) |

### PlaceholderAPI Thường Dùng
- `%tutien_canhgioi%`, `%tutien_tuvi%`, `%tutien_linhluc%`, `%tutien_linhcan%`
- `%tutien_bequan_diem%` (Điểm Bế Quan)
- `%tutien_daoniem%` (Đạo Niệm - Gacha)

---

## ⚙️ Cấu Hình
Plugin sẽ tự generate các file sau:
- `canhgioi.yml` (Chỉ số cảnh giới)
- `danduoc.yml` (Công thức đan dược)
- `VanGioiCacGUI.yml` (Cửa hàng tiền tệ Vault)

---

## 📂 Cấu Trúc Mã Nguồn (Tham Khảo)
```
src/main/java/tutien/
├── core/               # Main, Manager, Enums, Expansions
├── command/            # Lệnh người chơi & Admin
├── event/              # Xử lý Combat, Movement, Block, Quit, v.v.
├── gui/                # Đan Lò, Chế Tác, Nhiệm Vụ, Phụ Bản, Gacha, Bế Quan
├── combat/             # Chỉ số Stats (HP, DMG theo Cảnh Giới)
├── craft/              # Hệ thống Chế Tác Lò Rèn
├── dungeon/            # Linh Thú Viên & Boss
├── inventory/          # Túi đồ ảo
├── quest/              # Quản lý Nhiệm Vụ Ngày
├── tongmon/            # Bang hội
└── task/               # TuLuyenTask, NguKiemTask, AutoSaveTask, BossTask
```

---

## 👨‍💻 Tác Giả

- **Tài Dev Nguyễn**
- Cập nhật Sprint 4 (Phiên bản: `2.0`)
- Tương thích API: Spigot 1.20.6

> *"Vạn pháp quy nhất, nhất kiếm phá vạn pháp."* ⚔️
