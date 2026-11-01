package tutien.cultivation.model;

import tutien.core.CanhGioi;

/**
 * Phẩm cấp công pháp chuẩn tu tiên:
 * - HOANG (Hoàng Giai): Giới hạn truyền thừa Luyện Khí Tầng 13.
 * - HUYEN (Huyền Giai): Giới hạn truyền thừa Trúc Cơ Đại Viên Mãn.
 * - DIA (Địa Giai): Giới hạn truyền thừa Nguyên Anh Đại Viên Mãn.
 * - THIEN (Thiên Giai): Giới hạn truyền thừa Hóa Thần - Vấn Đỉnh.
 * - TIEN (Tiên Giai): Giới hạn truyền thừa các cảnh giới hậu kỳ hướng tới Đạp Thiên.
 */
public enum CongPhapGrade {
    HOANG("Hoàng Giai", "§e§lHoàng Giai", "§e", CanhGioi.LUYEN_KHI, 1.0),
    HUYEN("Huyền Giai", "§a§lHuyền Giai", "§a", CanhGioi.TRUC_CO, 1.25),
    DIA("Địa Giai", "§b§lĐịa Giai", "§b", CanhGioi.NGUYEN_ANH, 1.60),
    THIEN("Thiên Giai", "§d§lThiên Giai", "§d", CanhGioi.VAN_DINH, 2.00),
    TIEN("Tiên Giai", "§c§lTiên Giai", "§c", CanhGioi.DAP_THIEN, 2.60);

    private final String name;
    private final String displayName;
    private final String color;
    private final CanhGioi defaultMaxRealm;
    private final double baseMultiplier;

    CongPhapGrade(String name, String displayName, String color, CanhGioi defaultMaxRealm, double baseMultiplier) {
        this.name = name;
        this.displayName = displayName;
        this.color = color;
        this.defaultMaxRealm = defaultMaxRealm;
        this.baseMultiplier = baseMultiplier;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public CanhGioi getDefaultMaxRealm() {
        return defaultMaxRealm;
    }

    public double getBaseMultiplier() {
        return baseMultiplier;
    }

    public static CongPhapGrade fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return HOANG;
        }
        String clean = str.trim().toUpperCase();
        for (CongPhapGrade grade : values()) {
            if (grade.name().equals(clean) || grade.name.equalsIgnoreCase(clean)) {
                return grade;
            }
        }
        return HOANG;
    }
}
