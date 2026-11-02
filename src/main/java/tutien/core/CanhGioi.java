package tutien.core;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Enum định nghĩa các Đại Cảnh Giới.
 * Cấu trúc chuẩn theo lộ trình Tiên Nghịch & Phàm Nhân Tu Tiên:
 *   - PHAM_NHAN: Trạng thái tiền tu luyện (Linh lực = 0, chưa khai mở đan điền).
 *   - LUYEN_KHI: 13 Tầng (Tầng 1 -> Tầng 13).
 *   - TRUC_CO -> TIEN_DE: Sơ Kỳ, Trung Kỳ, Hậu Kỳ, Đại Viên Mãn.
 */
public enum CanhGioi {
    // Tên, TuViMoiTang, MaxMau, MaxLinhLuc, BaseDamage, Speed, Armor, Toughness, KnockbackResist, DmgReduction, TichCocLevel
    PHAM_NHAN ("Phàm Nhân",  100,      20.0,    0,        1.0,    0.200, 0.0,  0.0,  0.00, 0.00, 0),
    LUYEN_KHI ("Luyện Khí",  150,      24.0,    150,      2.0,    0.205, 2.0,  0.0,  0.05, 0.02, 1),
    TRUC_CO   ("Trúc Cơ",    1500,     30.0,    500,      4.0,    0.210, 4.0,  1.0,  0.10, 0.04, 2),
    KIM_DAN   ("Kim Đan",    6000,     40.0,    1000,     7.0,    0.220, 7.0,  2.0,  0.20, 0.08, 3),
    NGUYEN_ANH("Nguyên Anh", 15000,    60.0,    2500,     12.0,   0.230, 10.0, 4.0,  0.30, 0.12, 4),
    HOA_THAN  ("Hóa Thần",   36000,    100.0,   6000,     20.0,   0.245, 14.0, 6.0,  0.45, 0.16, 4),
    ANH_BIEN  ("Anh Biến",   90000,    150.0,   15000,    35.0,   0.260, 18.0, 8.0,  0.60, 0.20, 4),
    VAN_DINH  ("Vấn Đỉnh",   200000,   250.0,   35000,    60.0,   0.275, 22.0, 10.0, 0.75, 0.24, 4),
    AM_HU     ("Âm Hư",      450000,   400.0,   80000,    100.0,  0.290, 26.0, 12.0, 0.85, 0.28, 4),
    DUONG_THUC("Dương Thực", 900000,   600.0,   150000,   150.0,  0.305, 30.0, 14.0, 1.00, 0.32, 4),
    KHUY_NIET ("Khuy Niết",  1800000,  1000.0,  300000,   250.0,  0.320, 35.0, 16.0, 1.00, 0.35, 4),
    TINH_NIET ("Tịnh Niết",  3600000,  1500.0,  600000,   400.0,  0.335, 40.0, 18.0, 1.00, 0.38, 4),
    TOAI_NIET ("Toái Niết",  7500000,  2500.0,  1200000,  700.0,  0.350, 45.0, 20.0, 1.00, 0.40, 4),
    KHONG_NIET("Không Niết", 15000000, 4000.0,  2500000,  1200.0, 0.365, 50.0, 22.0, 1.00, 0.42, 4),
    KHONG_LINH("Không Linh", 30000000, 7000.0,  6000000,  2000.0, 0.380, 55.0, 25.0, 1.00, 0.45, 4),
    KHONG_HUYEN("Không Huyền", 60000000, 12000.0, 15000000, 4000.0, 0.390, 60.0, 28.0, 1.00, 0.48, 4),
    DAP_THIEN ("Đạp Thiên",  150000000, 20000.0, 50000000, 8000.0, 0.400, 70.0, 30.0, 1.00, 0.50, 4);

    private String tenHienThi;
    private int tuViMoiTang;
    private double maxMau;
    private int maxLinhLuc;
    private double baseDamage;
    private double speed;
    private double armor;
    private double armorToughness;
    private double knockbackResistance;
    private double damageReduction;
    private int tichCocLevel;

    CanhGioi(String tenHienThi, int tuViMoiTang, double maxMau, int maxLinhLuc, double baseDamage,
             double speed, double armor, double armorToughness, double knockbackResistance,
             double damageReduction, int tichCocLevel) {
        this.tenHienThi = tenHienThi;
        this.tuViMoiTang = tuViMoiTang;
        this.maxMau = maxMau;
        this.maxLinhLuc = maxLinhLuc;
        this.baseDamage = baseDamage;
        this.speed = speed;
        this.armor = armor;
        this.armorToughness = armorToughness;
        this.knockbackResistance = knockbackResistance;
        this.damageReduction = damageReduction;
        this.tichCocLevel = tichCocLevel;
    }

    public String getTenHienThi() { return tenHienThi; }
    public int getTuViMoiTang() { return tuViMoiTang; }
    public double getMaxMau() { return maxMau; }
    public int getMaxLinhLuc() { return maxLinhLuc; }
    public double getBaseDamage() { return baseDamage; }
    public double getSpeed() { return speed; }
    public double getArmor() { return armor; }
    public double getArmorToughness() { return armorToughness; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public double getDamageReduction() { return damageReduction; }
    public int getTichCocLevel() { return tichCocLevel; }

    public CanhGioi getCanhGioiTiepTheo() {
        int nextOrdinal = this.ordinal() + 1;
        return (nextOrdinal < values().length) ? values()[nextOrdinal] : null;
    }

    public boolean isAtLeast(CanhGioi other) {
        if (other == null) return true;
        return this.ordinal() >= other.ordinal();
    }

    public boolean isHigherThan(CanhGioi other) {
        if (other == null) return true;
        return this.ordinal() > other.ordinal();
    }

    public int getTier() {
        return this.ordinal();
    }

    /**
     * Số tiểu cảnh giới tối đa trong đại cảnh giới này:
     * - PHAM_NHAN: 1 (Phàm thai)
     * - LUYEN_KHI: 13 (Tầng 1 -> 13)
     * - Các cảnh giới khác: 4 (Sơ kỳ, Trung kỳ, Hậu kỳ, Đại viên mãn)
     */
    public int getMaxSubStages() {
        if (this == PHAM_NHAN) return 1;
        if (this == LUYEN_KHI) return 13;
        return 4;
    }

    /**
     * Tổng tu vi cần để hoàn thành toàn bộ cảnh giới này.
     */
    public int getTuViYeuCau() {
        return tuViMoiTang * getMaxSubStages();
    }

    /**
     * Tính toán chính xác tầng / tiểu cảnh giới tương ứng với số tu vi hiện tại.
     * Cảnh Giới và Tu Vi luôn đi đôi với nhau, không thể lệch lạc.
     */
    public static int calculateTang(CanhGioi cg, int tuVi) {
        if (cg == null) return 1;
        int maxTang = cg.getMaxSubStages();
        if (maxTang <= 1) return 1;
        int tuViMoiTang = Math.max(1, cg.getTuViMoiTang());
        int calculated = (Math.max(0, tuVi) / tuViMoiTang) + 1;
        return Math.max(1, Math.min(maxTang, calculated));
    }

    /**
     * Tên tiểu cảnh giới hiển thị
     */
    public String getTieuCanhGioi(int subStage) {
        if (this == PHAM_NHAN) {
            return "§7[Phàm Thai]";
        }
        if (this == LUYEN_KHI) {
            if (subStage <= 1) return "§7[Tầng 1]";
            if (subStage == 2) return "§7[Tầng 2]";
            if (subStage == 3) return "§e[Tầng 3]";
            if (subStage == 4) return "§e[Tầng 4]";
            if (subStage == 5) return "§a[Tầng 5]";
            if (subStage == 6) return "§a[Tầng 6]";
            if (subStage == 7) return "§b[Tầng 7]";
            if (subStage == 8) return "§b[Tầng 8]";
            if (subStage == 9) return "§d[Tầng 9]";
            if (subStage == 10) return "§d[Tầng 10]";
            if (subStage == 11) return "§6[Tầng 11]";
            if (subStage == 12) return "§6[Tầng 12]";
            return "§c§l[Tầng 13 - Viên Mãn]";
        }

        return switch (subStage) {
            case 1 -> "§7[Sơ Kỳ]";
            case 2 -> "§e[Trung Kỳ]";
            case 3 -> "§b[Hậu Kỳ]";
            default -> "§c§l[Đại Viên Mãn]";
        };
    }

    public static void loadFromConfig(FileConfiguration config) {
        for (CanhGioi cg : values()) {
            String path = cg.name() + ".";
            if (config.contains(cg.name())) {
                cg.tenHienThi = config.getString(path + "TenHienThi", cg.tenHienThi);
                cg.tuViMoiTang = config.getInt(path + "TuViMoiTang", cg.tuViMoiTang);
                cg.maxMau = config.getDouble(path + "MaxMau", cg.maxMau);
                cg.maxLinhLuc = config.getInt(path + "MaxLinhLuc", cg.maxLinhLuc);
                cg.baseDamage = config.getDouble(path + "BaseDamage", cg.baseDamage);
                cg.speed = config.getDouble(path + "Speed", cg.speed);
                cg.armor = config.getDouble(path + "Armor", cg.armor);
                cg.armorToughness = config.getDouble(path + "ArmorToughness", cg.armorToughness);
                cg.knockbackResistance = config.getDouble(path + "KnockbackResistance", cg.knockbackResistance);
                cg.damageReduction = config.getDouble(path + "DamageReduction", cg.damageReduction);
                cg.tichCocLevel = config.getInt(path + "TichCocLevel", cg.tichCocLevel);
            }
        }
    }
}
