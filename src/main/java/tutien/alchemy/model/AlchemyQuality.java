package tutien.alchemy.model;

import org.bukkit.ChatColor;

/**
 * Phẩm chất đan dược sau khi luyện thành trong Mini-Game Hỏa Hầu
 */
public enum AlchemyQuality {
    CUC_PHAM("§6§lCỰC PHẨM", 1.5, 0.5, 2, "§6★ Thần quang chiếu rọi, đan hương ngập tràn! Đan dược đạt cảnh giới viên mãn."),
    THUONG_PHAM("§a§lTHƯỢNG PHẨM", 1.2, 0.8, 1, "§a✔ Dược lực tinh thuần, phẩm chất xuất sắc."),
    TRUNG_PHAM("§b§lTRUNG PHẨM", 1.0, 1.0, 1, "§b✔ Luyện đan thành công, dược lực ổn định."),
    HA_PHAM("§e§lHẠ PHẨM", 0.8, 1.3, 1, "§e⚠ Hỏa hầu dao động, dược lực hơi suy giảm và đan độc tăng nhẹ."),
    PHE_DAN("§c§lPHẾ ĐAN (TRO TÀN)", 0.0, 0.0, 0, "§c✘ Hỏa hầu thất khống, toàn bộ dược liệu biến thành tro tàn!");

    private final String displayName;
    private final double potencyMultiplier;
    private final double toxicityMultiplier;
    private final int bonusYield;
    private final String description;

    AlchemyQuality(String displayName, double potencyMultiplier, double toxicityMultiplier, int bonusYield, String description) {
        this.displayName = displayName;
        this.potencyMultiplier = potencyMultiplier;
        this.toxicityMultiplier = toxicityMultiplier;
        this.bonusYield = bonusYield;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPotencyMultiplier() {
        return potencyMultiplier;
    }

    public double getToxicityMultiplier() {
        return toxicityMultiplier;
    }

    public int getBonusYield() {
        return bonusYield;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Xác định phẩm chất dựa trên điểm ổn định hỏa hầu (0..100)
     */
    public static AlchemyQuality fromScore(double score) {
        if (score >= 92.0) return CUC_PHAM;
        if (score >= 75.0) return THUONG_PHAM;
        if (score >= 55.0) return TRUNG_PHAM;
        if (score >= 35.0) return HA_PHAM;
        return PHE_DAN;
    }
}
