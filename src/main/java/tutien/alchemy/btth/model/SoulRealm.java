package tutien.alchemy.btth.model;

/**
 * 4 Đại Cảnh Giới Linh Hồn theo Đấu Phá Thương Khung:
 * - PHAM_CANH: Nền tảng điều khiển hỏa diễm, luyện đan phẩm cấp thấp đến cao (Nhất -> Thất phẩm).
 * - LINH_CANH: Bắt buộc để luyện Bát phẩm (ban tặng linh tính cho đan dược).
 * - THIEN_CANH: Bắt buộc để luyện Cửu phẩm (tạo linh trí và linh thể đan dược).
 * - DE_CANH: Cảnh giới linh hồn tối cao luyện Đế Phẩm Đan Dược.
 */
public enum SoulRealm {
    PHAM_CANH("Phàm Cảnh", "§7[Phàm Cảnh]", 1, 7),
    LINH_CANH("Linh Cảnh", "§b§l[Linh Cảnh]", 8, 8),
    THIEN_CANH("Thiên Cảnh", "§d§l[Thiên Cảnh]", 9, 9),
    DE_CANH("Đế Cảnh", "§e§l✦ ĐẾ CẢNH ✦", 10, 10);

    private final String displayName;
    private final String formattedTag;
    private final int minTierSupported;
    private final int maxTierSupported;

    SoulRealm(String displayName, String formattedTag, int minTierSupported, int maxTierSupported) {
        this.displayName = displayName;
        this.formattedTag = formattedTag;
        this.minTierSupported = minTierSupported;
        this.maxTierSupported = maxTierSupported;
    }

    public String getDisplayName() { return displayName; }
    public String getFormattedTag() { return formattedTag; }
    public int getMinTierSupported() { return minTierSupported; }
    public int getMaxTierSupported() { return maxTierSupported; }

    public boolean canCraftTier(int tier) {
        return this.ordinal() >= getRequiredRealmForTier(tier).ordinal();
    }

    public static SoulRealm getRequiredRealmForTier(int tier) {
        if (tier >= 10) return DE_CANH;
        if (tier == 9) return THIEN_CANH;
        if (tier == 8) return LINH_CANH;
        return PHAM_CANH;
    }
}
