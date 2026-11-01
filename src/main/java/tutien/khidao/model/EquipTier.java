package tutien.khidao.model;

/**
 * 8 Phẩm cấp Trang Bị & Pháp Bảo Tu Tiên chuẩn mực theo lộ trình Phase 4:
 * Phàm Khí -> Pháp Khí -> Linh Khí -> Bảo Khí -> Cổ Bảo -> Hậu Thiên Linh Bảo -> Tiên Thiên Linh Bảo -> Thông Thiên Linh Bảo.
 */
public enum EquipTier {
    PHAM_KHI(0, "§7Phàm Khí", 0, false, 4.0, 10.0, 2.0, 50),
    PHAP_KHI(1, "§aPháp Khí", 1, false, 8.0, 25.0, 4.0, 25),
    LINH_KHI(2, "§bLinh Khí", 1, false, 14.0, 45.0, 6.0, 12),
    BAO_KHI(3, "§d§lBảo Khí", 2, false, 22.0, 70.0, 9.0, 7),
    CO_BAO(4, "§6§lCổ Bảo", 2, true, 32.0, 100.0, 12.0, 3),
    HAU_THIEN_LINH_BAO(5, "§c§lHậu Thiên Linh Bảo", 3, true, 45.0, 140.0, 16.0, 2),
    TIEN_THIEN_LINH_BAO(6, "§5§lTiên Thiên Linh Bảo", 3, true, 60.0, 190.0, 20.0, 1),
    THONG_THIEN_LINH_BAO(7, "§e§lThông Thiên Linh Bảo", 3, true, 80.0, 250.0, 25.0, 1);

    private final int level;
    private final String displayName;
    private final int maxSockets;
    private final boolean allowSpirit;
    private final double baseDmgBonus;
    private final double baseHpBonus;
    private final double baseArmorBonus;
    private final int baseWeight;

    EquipTier(int level, String displayName, int maxSockets, boolean allowSpirit,
              double baseDmgBonus, double baseHpBonus, double baseArmorBonus, int baseWeight) {
        this.level = level;
        this.displayName = displayName;
        this.maxSockets = maxSockets;
        this.allowSpirit = allowSpirit;
        this.baseDmgBonus = baseDmgBonus;
        this.baseHpBonus = baseHpBonus;
        this.baseArmorBonus = baseArmorBonus;
        this.baseWeight = baseWeight;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxSockets() {
        return maxSockets;
    }

    public boolean isAllowSpirit() {
        return allowSpirit;
    }

    public double getBaseDmgBonus() {
        return baseDmgBonus;
    }

    public double getBaseHpBonus() {
        return baseHpBonus;
    }

    public double getBaseArmorBonus() {
        return baseArmorBonus;
    }

    public int getBaseWeight() {
        return baseWeight;
    }

    public static EquipTier fromLevel(int level) {
        for (EquipTier tier : values()) {
            if (tier.level == level) return tier;
        }
        return PHAM_KHI;
    }

    public static EquipTier fromName(String name) {
        if (name == null) return PHAM_KHI;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PHAM_KHI;
        }
    }
}
