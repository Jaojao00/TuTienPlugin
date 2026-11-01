package tutien.dungeon.boss;

/**
 * Danh sách Boss Thế Giới trong Tu Tiên (Phase 5).
 */
public enum WorldBossType {
    THUONG_CO_CU_MA("§c§lThượng Cổ Cự Ma", 500000.0, "HOA", "Rơi: Huyết Ma Tủy, Phôi Cổ Bảo, Hỏa Thạch Cực Phẩm"),
    CUU_U_MINH_XA("§b§lCửu U Minh Xà", 800000.0, "THUY", "Rơi: U Minh Đan Châu, Thủy Thạch Cực Phẩm, Hậu Thiên Linh Bảo"),
    THAI_CO_THAN_LONG("§e§lThái Cổ Thần Long", 1500000.0, "KIM", "Rơi: Thần Long Tinh Huyết, Tiên Thiên Linh Bảo, Kim Thạch Cực Phẩm");

    private final String displayName;
    private final double maxHealth;
    private final String element;
    private final String dropDescription;

    WorldBossType(String displayName, double maxHealth, String element, String dropDescription) {
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.element = element;
        this.dropDescription = dropDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public String getElement() {
        return element;
    }

    public String getDropDescription() {
        return dropDescription;
    }
}
