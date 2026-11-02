package tutien.alchemy.btth.model;

/**
 * 10 Phẩm Cấp Luyện Dược Sư theo Đấu Phá Thương Khung:
 * - HOC_DO: Luyện Dược Học Đồ
 * - NHAT_PHAM -> CUU_PHAM: Nhất Phẩm đến Cửu Phẩm Luyện Dược Sư
 * - DE_PHAM: Đế Phẩm Luyện Dược Sư (Cảnh giới đặc biệt tối cao)
 */
public enum AlchemistRank {
    HOC_DO(0, "Luyện Dược Học Đồ", "§7[Học Đồ]", 1, 0, 50.0, SoulRealm.PHAM_CANH),
    NHAT_PHAM(1, "Nhất Phẩm Luyện Dược Sư", "§a[Nhất Phẩm Dược Sư]", 1, 500, 100.0, SoulRealm.PHAM_CANH),
    NHI_PHAM(2, "Nhị Phẩm Luyện Dược Sư", "§b[Nhị Phẩm Dược Sư]", 2, 2000, 250.0, SoulRealm.PHAM_CANH),
    TAM_PHAM(3, "Tam Phẩm Luyện Dược Sư", "§e[Tam Phẩm Dược Sư]", 3, 6000, 500.0, SoulRealm.PHAM_CANH),
    TU_PHAM(4, "Tứ Phẩm Luyện Dược Sư", "§6[Tứ Phẩm Dược Sư]", 4, 15000, 1000.0, SoulRealm.PHAM_CANH),
    NGU_PHAM(5, "Ngũ Phẩm Luyện Dược Sư", "§d[Ngũ Phẩm Dược Sư]", 5, 40000, 2500.0, SoulRealm.PHAM_CANH),
    LUC_PHAM(6, "Lục Phẩm Luyện Dược Sư", "§c[Lục Phẩm Dược Sư]", 6, 100000, 6000.0, SoulRealm.PHAM_CANH),
    THAT_PHAM(7, "Thất Phẩm Luyện Dược Sư", "§4§l[Thất Phẩm Tông Sư]", 7, 250000, 15000.0, SoulRealm.PHAM_CANH),
    BAT_PHAM(8, "Bát Phẩm Luyện Dược Sư", "§5§l[Bát Phẩm Đại Tông Sư]", 8, 600000, 40000.0, SoulRealm.LINH_CANH),
    CUU_PHAM(9, "Cửu Phẩm Luyện Dược Sư", "§6§l[Cửu Phẩm Đan Thánh]", 9, 1500000, 100000.0, SoulRealm.THIEN_CANH),
    DE_PHAM(10, "Đế Phẩm Luyện Dược Sư", "§e§l✦ ĐẾ PHẨM ĐAN ĐẾ ✦", 10, 5000000, 500000.0, SoulRealm.DE_CANH);

    private final int tier;
    private final String displayName;
    private final String prefix;
    private final int maxPillTier;
    private final long expRequired;
    private final double soulPowerRequired;
    private final SoulRealm requiredSoulRealm;

    AlchemistRank(int tier, String displayName, String prefix, int maxPillTier,
                  long expRequired, double soulPowerRequired, SoulRealm requiredSoulRealm) {
        this.tier = tier;
        this.displayName = displayName;
        this.prefix = prefix;
        this.maxPillTier = maxPillTier;
        this.expRequired = expRequired;
        this.soulPowerRequired = soulPowerRequired;
        this.requiredSoulRealm = requiredSoulRealm;
    }

    public int getTier() { return tier; }
    public String getDisplayName() { return displayName; }
    public String getPrefix() { return prefix; }
    public int getMaxPillTier() { return maxPillTier; }
    public long getExpRequired() { return expRequired; }
    public double getSoulPowerRequired() { return soulPowerRequired; }
    public SoulRealm getRequiredSoulRealm() { return requiredSoulRealm; }

    public AlchemistRank getNextRank() {
        int next = this.ordinal() + 1;
        return (next < values().length) ? values()[next] : null;
    }
}
