# ⛩ TuTienPlugin — Hệ Thống Tu Tiên Minecraft

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.6-brightgreen?style=flat-square&logo=minecraft)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spigot](https://img.shields.io/badge/API-Spigot-yellow?style=flat-square)
![License](https://img.shields.io/badge/License-Private-red?style=flat-square)

> Plugin Minecraft RPG mô phỏng thế giới tu tiên — 17 cảnh giới, 7 đại đạo, luyện đan, đột phá lôi kiếp, tông môn bang hội, và cửa hàng Vạn Giới Các.

---

## 📋 Mục Lục

- [Tổng Quan](#-tổng-quan)
- [Yêu Cầu](#-yêu-cầu)
- [Cài Đặt](#-cài-đặt)
- [Hệ Thống Cảnh Giới](#-hệ-thống-cảnh-giới)
- [Linh Căn](#-linh-căn-talent-system)
- [7 Đại Đạo](#%EF%B8%8F-7-đại-đạo-class-system)
- [Luyện Đan](#-luyện-đan)
- [Vạn Giới Các](#-vạn-giới-các-cửa-hàng)
- [Đột Phá & Lôi Kiếp](#-đột-phá--lôi-kiếp)
- [Tông Môn](#-tông-môn-bang-hội)
- [Ngự Kiếm Phi Hành](#%EF%B8%8F-ngự-kiếm-phi-hành)
- [Cấm Thuật Tiên Đế](#-cấm-thuật-tiên-đế)
- [Lệnh](#-danh-sách-lệnh)
- [PlaceholderAPI](#-placeholderapi)
- [Cấu Hình](#%EF%B8%8F-cấu-hình)
- [Tác Giả](#-tác-giả)

---

## 🌟 Tổng Quan

**TuTienPlugin** biến server Minecraft của bạn thành một thế giới tu tiên hoàn chỉnh. Người chơi sẽ bắt đầu từ **Phàm Nhân**, tu luyện qua 17 cảnh giới, chọn con đường tu luyện riêng, luyện đan dược, vượt qua lôi kiếp, và cuối cùng đạt đến đỉnh cao — **Tiên Đế**.

### Tính năng chính:
- 🏔️ **17 Cảnh Giới** — Từ Phàm Nhân đến Tiên Đế
- 🌿 **6 Linh Căn** — Thiên phú random ảnh hưởng tốc độ tu luyện
- ⚔️ **7 Đại Đạo** — Hệ thống class chuyên biệt (Kiếm Tu, Thể Tu, Ma Tu...)
- 🧪 **18 Đan Dược** — Luyện từ nguyên liệu Minecraft thật, cấu hình hoàn toàn bằng YML
- 🏪 **Vạn Giới Các** — Cửa hàng 4 phân khu với giá Vault Economy
- ⚡ **Cửu Đạo Thiên Kiếp** — Vượt 9 cú sét để đột phá cảnh giới
- 🏯 **Tông Môn** — Hệ thống bang hội với kết giới tự động
- 📦 **Túi Đồ Hư Không** — Kho đồ ảo riêng biệt
- 🗡️ **Ngự Kiếm Phi Hành** — Bay bằng kiếm từ Kim Đan trở lên
- 👑 **Cấm Thuật Tiên Đế** — 5 kỹ năng ultimate cho cảnh giới tối cao

---

## 📦 Yêu Cầu

| Thành phần | Phiên bản | Bắt buộc |
|-----------|----------|---------|
| Minecraft Server | 1.20.6 | ✅ |
| Java | 21+ | ✅ |
| Spigot / Paper | 1.20.6 | ✅ |
| Vault | 1.7+ | ⚠️ Khuyến nghị |
| PlaceholderAPI | 2.11+ | ⚠️ Khuyến nghị |
| WorldGuard | 7.0.9+ | ⚠️ Tuỳ chọn |
| WorldEdit | 7.3+ | ⚠️ Tuỳ chọn |

---

## 🚀 Cài Đặt

1. **Build plugin:**
   ```bash
   mvn clean package
   ```
2. **Copy file JAR** từ `target/TuTienPlugin-1.0.jar` vào thư mục `plugins/` của server.
3. **Khởi động server** — Plugin sẽ tự tạo các file cấu hình.
4. **(Tuỳ chọn)** Cài Vault + plugin kinh tế (EssentialsX, CMI) để sử dụng hệ thống tiền tệ.
5. **(Tuỳ chọn)** Cài WorldGuard để giới hạn khu vực luyện đan.

---

## 🏔️ Hệ Thống Cảnh Giới

Mỗi đại cảnh giới chia thành **10 tiểu cảnh giới** (Nhất Trọng → Thập Trọng → Viên Mãn).

| # | Cảnh Giới | Tu Vi Cần | Max HP | Base DMG |
|---|-----------|----------|--------|----------|
| 0 | Phàm Nhân | 0 | 20 | 1.0 |
| 1 | Luyện Khí | 100 | 24 | 2.0 |
| 2 | Trúc Cơ | 500 | 30 | 4.0 |
| 3 | Kim Đan | 2,000 | 40 | 7.0 |
| 4 | Nguyên Anh | 5,000 | 60 | 12.0 |
| 5 | Hóa Thần | 12,000 | 100 | 20.0 |
| 6 | Luyện Hư | 30,000 | 150 | 35.0 |
| 7 | Hợp Thể | 70,000 | 250 | 60.0 |
| 8 | Đại Thừa | 150,000 | 400 | 100.0 |
| 9 | Độ Kiếp | 300,000 | 600 | 150.0 |
| 10 | Chân Tiên | 600,000 | 1,000 | 250.0 |
| 11 | Địa Tiên | 1,200,000 | 1,500 | 400.0 |
| 12 | Thiên Tiên | 2,500,000 | 2,500 | 700.0 |
| 13 | Huyền Tiên | 5,000,000 | 4,000 | 1,200.0 |
| 14 | Tiên Quân | 10,000,000 | 7,000 | 2,000.0 |
| 15 | Tiên Vương | 25,000,000 | 12,000 | 4,000.0 |
| 16 | Tiên Đế | 50,000,000 | 20,000 | 8,000.0 |

> Chỉ số có thể tuỳ chỉnh trong `canhgioi.yml`.

---

## 🌿 Linh Căn (Talent System)

Mỗi người chơi vào server lần đầu sẽ được **random Linh Căn**, quyết định tốc độ tu luyện AFK:

| Linh Căn | Hệ số | Tỷ lệ |
|----------|-------|-------|
| 🔴 Phế Linh Căn | x1 | 30% |
| ⚪ Tạp Linh Căn | x2 | 30% |
| 🟢 Chân Linh Căn | x3 | 25% |
| 🟣 Địa Linh Căn | x5 | 12% |
| 🟡 Thiên Linh Căn | x10 | 2.5% |
| 🔥 Hỗn Độn Linh Căn | x20 | 0.5% |

---

## ⚔️ 7 Đại Đạo (Class System)

Chọn 1 trong 7 con đường tu luyện chuyên biệt:

| Đại Đạo | Buff |
|---------|------|
| 🗡️ **Kiếm Tu** | +20% DMG Kiếm, -50% Linh lực bay |
| 🛡️ **Thể Tu** | +40% HP tối đa, +10% giảm sát thương |
| 🔮 **Pháp Tu** | +30% Linh lực, +15% DMG phép |
| 👹 **Ma Tu** | +30% DMG nhưng -20% phòng thủ |
| 🐉 **Yêu Tu** | +15% tốc độ, hồi phục tự nhiên |
| 🧪 **Đan Đạo** | +25% tỷ lệ luyện đan thành công |
| 💑 **Song Tu** | +15% tốc độ nhận Tu Vi cùng đạo lữ |

---

## 🧪 Luyện Đan

### Tổng quan
- **18 công thức** đan dược cấu hình trong `danduoc.yml`
- Sử dụng **nguyên liệu Minecraft thật** (Lúa Mì, Thỏi Vàng, Nước Mắt Ghast...)
- Tên nguyên liệu hiển thị **bằng tiếng Việt** trong GUI
- Hỗ trợ **shift-click luyện x10** cùng lúc

### Ví dụ công thức

| Đan Dược | Nguyên Liệu | Tỷ Lệ |
|----------|------------|--------|
| Tích Cốc Đan | Lúa Mì x16, Nấm Nâu x8, Đường x4 | 75% |
| Trúc Cơ Đan | Thỏi Vàng x16, Que Lửa x8, Nước Mắt Ghast x4, Mụn Địa Ngục x16 | 45% |
| Nghịch Thiên Đan | Trứng Rồng x1, Ngôi Sao Địa Ngục x4, Vật Tổ Bất Tử x4, Táo Vàng Phù Phép x4, Hơi Thở Rồng x32 | 10% |

### Nâng cấp Đan Lò (Vault)

| Cấp | Giá | Kinh Nghiệm | Bonus |
|-----|-----|-------------|-------|
| 1→2 | 500K | 3,000 lần | +5% |
| 2→3 | 2M | 8,000 lần | +10% |
| 3→4 | 8M | 20,000 lần | +15% |
| 4→5 | 25M | 60,000 lần | +20% |
| 5→6 | 100M | 150,000 lần | +25% |

### Khu vực luyện đan (WorldGuard)

Tích hợp WorldGuard để giới hạn khu vực luyện đan:

```yaml
# danduoc.yml
KhuVucLuyenDan:
  BatBuoc: true          # true = phải đứng trong region
  TenRegion: "dan-lo"    # Tên region WorldGuard
```

```bash
# Tạo khu vực trên server:
//pos1              # Chọn điểm 1
//pos2              # Chọn điểm 2
/rg define dan-lo   # Tạo region
```

---

## 🏪 Vạn Giới Các (Cửa Hàng)

Cửa hàng chia thành **4 phân khu**, cấu hình hoàn toàn trong `VanGioiCacGUI.yml`:

| Phân Khu | Nội Dung | Cảnh Giới |
|----------|---------|----------|
| 🌿 Nguyên Liệu | 16 loại thảo dược, khoáng vật | Phàm Nhân+ |
| ⚔️ Nhất-Tam Phẩm | 6 đan dược cấp thấp | Phàm Nhân+ |
| 🛡️ Tứ-Lục Phẩm | 4 đan dược cấp trung | Kim Đan+ |
| 💎 Thất Phẩm+ | 2 thần đan huyền thoại | Hóa Thần+ |

- Thanh toán bằng **Vault Economy**
- Giới hạn mua theo **cảnh giới**
- Hiển thị **số dư** trong GUI

---

## ⚡ Đột Phá & Lôi Kiếp

- **Phàm Nhân → Trúc Cơ**: Đột phá an toàn (cần Tu Vi + Đan dược)
- **Kim Đan trở lên**: Kích hoạt **Cửu Đạo Thiên Kiếp** — 9 cú sét liên tiếp
  - Sát thương tăng dần: 3% → 25% HP tối đa
  - **Thất bại** = mất 10% Tu Vi + thông báo toàn server
  - **Thành công** = thăng cấp + hồi đầy HP/Mana + thông báo toàn server

---

## 🏯 Tông Môn (Bang Hội)

- Yêu cầu lập: Cảnh giới **Hóa Thần** trở lên
- Tự động tạo **kết giới** (hàng rào gỗ) 50×50 block
- Hệ thống: **Tông Chủ** → **Đệ Tử**
- Teleport về tông môn bất kỳ lúc nào

---

## 🗡️ Ngự Kiếm Phi Hành

- Yêu cầu: **Kim Đan** trở lên
- Bật/tắt chế độ bay bằng lệnh
- Tiêu hao **Linh Lực** khi bay

---

## 👑 Cấm Thuật Tiên Đế

Chỉ dành cho cảnh giới **Tiên Đế** — đỉnh cao tu luyện:

| Thần Thông | Linh Lực | Hiệu Ứng |
|-----------|---------|----------|
| 🌩️ Chân Thiên Diệt | 500 | Sét + 2000 DMG AoE 20 blocks |
| 🌪️ Hô Phong Hoán Vũ | 300 | Bão sét 5 giây |
| 🏔️ Địa Liệt Sơn Băng | 400 | Hất tung + 1000 DMG AoE |
| ⚔️ Tát Đậu Thành Binh | 200 | Triệu hồi 3 Iron Golem |
| ❤️ Huyết Khí Vô Tận | 600 | Full HP + buff 1 phút |

---

## 📝 Danh Sách Lệnh

### Lệnh người chơi

| Lệnh | Mô tả |
|-------|--------|
| `/tutien` | Xem danh sách lệnh |
| `/tutien menu` | Mở giao diện chính |
| `/tutien tuluyen` | Nhập định tọa thiền (AFK nhận Tu Vi) |
| `/tutien xem [tên]` | Xem thông tin tu vi |
| `/tutien luyendan` | Mở Bát Quái Đan Lò |
| `/tutien dokiep` | Mở giao diện đột phá |
| `/tutien ngukiem` | Bật/tắt ngự kiếm phi hành |
| `/tutien tuido` | Mở Túi Đồ Hư Không |
| `/tutien laptong <tên>` | Lập Tông Môn (Hóa Thần+) |
| `/tutien tongmon moi <tên>` | Thu nhận đệ tử |
| `/tutien tongmon duoi <tên>` | Trục xuất đệ tử |
| `/tutien tongmon roi` | Rời tông môn |
| `/tutien tongmon ve` | Teleport về tông môn |

### Lệnh Tiên Đế

| Lệnh | Mô tả |
|-------|--------|
| `/tiende thiendiet` | Chân Thiên Diệt |
| `/tiende hophong` | Hô Phong Hoán Vũ |
| `/tiende dialiet` | Địa Liệt Sơn Băng |
| `/tiende tatdau` | Tát Đậu Thành Binh |
| `/tiende huyetkhi` | Huyết Khí Vô Tận |

### Lệnh Admin (`tutien.admin`)

```
/tutien admin tuvi <add/set/remove> <tên> <số>
/tutien admin linhluc <add/set/remove> <tên> <số>
/tutien admin canhgioi <tên> <CANH_GIOI>
/tutien admin linhcan <tên> <LINH_CAN>
/tutien admin he <tên> <HE>
```

---

## 📊 PlaceholderAPI

| Placeholder | Giá trị |
|------------|---------|
| `%tutien_canhgioi%` | Tên cảnh giới |
| `%tutien_tuvi%` | Tu Vi hiện tại |
| `%tutien_tuvi_max%` | Tu Vi cần đột phá |
| `%tutien_linhcan%` | Linh Căn |
| `%tutien_linhluc%` | Linh Lực |
| `%tutien_linhluc_max%` | Linh Lực tối đa |
| `%tutien_bonus_damage%` | DMG bonus từ đan |
| `%tutien_bonus_health%` | HP bonus từ đan |
| `%tutien_danlo_cap%` | Cấp lò luyện |
| `%tutien_luyen_count%` | Số lần đã luyện |
| `%tutien_balance%` | Số dư Vault |

---

## ⚙️ Cấu Hình

| File | Mô tả |
|------|-------|
| `canhgioi.yml` | Chỉ số 17 cảnh giới (HP, DMG, Tu Vi yêu cầu) |
| `danduoc.yml` | 18 công thức đan dược + nâng cấp lò + khu vực WorldGuard |
| `VanGioiCacGUI.yml` | Cửa hàng 4 phân khu (vật phẩm, giá, cảnh giới) |
| `config.yml` | Cấu hình GUI chính |

### Ví dụ cấu hình đan dược

```yaml
DanDuoc:
  TICH_COC_DAN:
    TenHienThi: "Tích Cốc Đan"
    Material: DRIED_KELP
    MoTa: "Nhịn đói 1 tháng."
    CanhGioiMin: PHAM_NHAN
    TyLe: 75
    BonusDamage: 0.1
    BonusHealth: 0.0
    BonusTuVi: 5
    NguyenLieu:
      - "WHEAT:16:Lúa Mì"           # Hiển thị tiếng Việt trong GUI
      - "BROWN_MUSHROOM:8:Nấm Nâu"
      - "SUGAR:4:Đường"
```

---

## 📂 Cấu Trúc Mã Nguồn

```
src/main/java/tutien/
├── core/               # Hạt nhân
│   ├── TuTienPlugin    # Main class
│   ├── PlayerDataManager
│   ├── CanhGioi        # 17 cảnh giới
│   ├── LinhCan         # 6 linh căn
│   ├── HeTuLuyen       # 7 đại đạo
│   ├── EconomyManager  # Vault Economy
│   └── TuTienExpansion # PlaceholderAPI
├── command/            # Lệnh
│   ├── TuTienCommand   # /tutien
│   └── TienDeCommand   # /tiende
├── event/              # Sự kiện
│   ├── PlayerListener
│   ├── CombatListener
│   ├── ItemListener
│   ├── TeleportListener
│   ├── TongMonListener
│   └── AutoPickupListener
├── tutien/gui/         # Giao diện
│   ├── MainMenu
│   ├── VanGioiCacGUI   # Cửa hàng (data-driven)
│   ├── LuyenDanGUI     # Luyện đan (data-driven)
│   ├── DotPhaGUI       # Đột phá
│   ├── ChonHeGUI       # Chọn hệ
│   └── TuiDoGUI        # Túi đồ
├── combat/
│   └── StatsManager    # Chỉ số chiến đấu
├── inventory/
│   └── TuiDoManager    # Quản lý túi đồ ảo
├── tongmon/
│   └── TongMonManager  # Quản lý bang hội
└── task/
    └── TuLuyenTask     # Tọa thiền mỗi giây
```

---

## 👨‍💻 Tác Giả

- **Tài Dev Nguyễn**
- Phiên bản: `1.0`
- API: Spigot 1.20.6

---

> *"Vạn pháp quy nhất, nhất kiếm phá vạn pháp."* ⚔️
