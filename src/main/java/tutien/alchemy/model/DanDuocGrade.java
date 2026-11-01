package tutien.alchemy.model;

/**
 * Phẩm cấp Đan Dược trong thế giới Tu Tiên.
 */
public enum DanDuocGrade {
    HOANG("§7Hoàng Giai", 1),
    HUYEN("§bHuyền Giai", 2),
    DIA("§dĐịa Giai", 3),
    THIEN("§6Thiên Giai", 4),
    TIEN("§c§lTiên Giai", 5);

    private final String displayName;
    private final int tier;

    DanDuocGrade(String displayName, int tier) {
        this.displayName = displayName;
        this.tier = tier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTier() {
        return tier;
    }

    public static DanDuocGrade fromString(String name) {
        if (name == null) return HOANG;
        try {
            return DanDuocGrade.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return HOANG;
        }
    }
}
