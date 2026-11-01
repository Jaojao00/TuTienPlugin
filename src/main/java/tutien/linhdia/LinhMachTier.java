package tutien.linhdia;

/**
 * Các bậc Linh Mạch của Động Phủ / Linh Địa (Phase 5).
 */
public enum LinhMachTier {
    NHAT_PHAM(1, "§aNhất Phẩm Linh Mạch", 1.15, 2.0, 1000),
    NHI_PHAM(2, "§bNhị Phẩm Linh Mạch", 1.30, 4.0, 3000),
    TAM_PHAM(3, "§d§lTam Phẩm Linh Mạch", 1.45, 6.0, 8000),
    TU_PHAM(4, "§6§lTứ Phẩm Linh Mạch", 1.60, 8.0, 20000),
    NGU_PHAM(5, "§e§lNgũ Phẩm Cực Linh", 1.75, 10.0, 50000);

    private final int level;
    private final String displayName;
    private final double tuViMultiplier; // Hệ số gia tăng tốc độ tu vi (+15% -> +75%)
    private final double manaRegenPerSec; // Hồi phục linh lực mỗi giây (+2 -> +10)
    private final int upgradeCost;

    LinhMachTier(int level, String displayName, double tuViMultiplier, double manaRegenPerSec, int upgradeCost) {
        this.level = level;
        this.displayName = displayName;
        this.tuViMultiplier = tuViMultiplier;
        this.manaRegenPerSec = manaRegenPerSec;
        this.upgradeCost = upgradeCost;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getTuViMultiplier() {
        return tuViMultiplier;
    }

    public double getManaRegenPerSec() {
        return manaRegenPerSec;
    }

    public int getUpgradeCost() {
        return upgradeCost;
    }

    public static LinhMachTier fromLevel(int lvl) {
        for (LinhMachTier t : values()) {
            if (t.level == lvl) return t;
        }
        return NHAT_PHAM;
    }
}
