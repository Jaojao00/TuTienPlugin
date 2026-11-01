package tutien.dungeon.bicanh;

import tutien.core.CanhGioi;

/**
 * 4 Tầng Bí Cảnh Phân Tầng Tu Tiên (Phase 5).
 */
public enum BiCanhTier {
    TANG_1(1, "§aTầng 1: U Cốc Thí Luyện", CanhGioi.LUYEN_KHI, 100, 500, 3),
    TANG_2(2, "§bTầng 2: Huyết Hải Bí Cảnh", CanhGioi.KIM_DAN, 300, 1500, 5),
    TANG_3(3, "§d§lTầng 3: Thái Hư Thần Vực", CanhGioi.HOA_THAN, 800, 4000, 7),
    TANG_4(4, "§6§lTầng 4: Hỗn Độn Ma Uyên", CanhGioi.VAN_DINH, 2000, 10000, 10);

    private final int level;
    private final String displayName;
    private final CanhGioi requiredRank;
    private final int baseTuViReward;
    private final int baseDaoNiemReward;
    private final int bossWaves;

    BiCanhTier(int level, String displayName, CanhGioi requiredRank, int baseTuViReward, int baseDaoNiemReward, int bossWaves) {
        this.level = level;
        this.displayName = displayName;
        this.requiredRank = requiredRank;
        this.baseTuViReward = baseTuViReward;
        this.baseDaoNiemReward = baseDaoNiemReward;
        this.bossWaves = bossWaves;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CanhGioi getRequiredRank() {
        return requiredRank;
    }

    public int getBaseTuViReward() {
        return baseTuViReward;
    }

    public int getBaseDaoNiemReward() {
        return baseDaoNiemReward;
    }

    public int getBossWaves() {
        return bossWaves;
    }

    public static BiCanhTier fromLevel(int lvl) {
        for (BiCanhTier t : values()) {
            if (t.level == lvl) return t;
        }
        return TANG_1;
    }
}
