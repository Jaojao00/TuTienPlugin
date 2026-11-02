package tutien.alchemy.btth.model;

/**
 * Phẩm Chất Đan Dược Đấu Phá Thương Khung:
 * Tách biệt 3 thuộc tính:
 * 1. pillTier: Phẩm cấp (1 - 10).
 * 2. grade: Phẩm chất (Hạ phẩm, Trung phẩm, Thượng phẩm, Cực phẩm).
 * 3. danLines: Số đan văn (0 - 3 văn).
 */
public class BTTHPillQuality {

    public enum PillGrade {
        HA_PHAM("Hạ Phẩm", "§8[Hạ Phẩm]", 0.65, 0.74, 15.0),
        TRUNG_PHAM("Trung Phẩm", "§7[Trung Phẩm]", 0.75, 0.89, 8.0),
        THUONG_PHAM("Thượng Phẩm", "§a[Thượng Phẩm]", 0.90, 0.98, 2.0),
        CUC_PHAM("Cực Phẩm", "§6§l[Cực Phẩm]", 0.99, 1.00, 0.0);

        private final String displayName;
        private final String tag;
        private final double minPurity;
        private final double maxPurity;
        private final double defaultToxicity;

        PillGrade(String displayName, String tag, double minPurity, double maxPurity, double defaultToxicity) {
            this.displayName = displayName;
            this.tag = tag;
            this.minPurity = minPurity;
            this.maxPurity = maxPurity;
            this.defaultToxicity = defaultToxicity;
        }

        public String getDisplayName() { return displayName; }
        public String getTag() { return tag; }
        public double getMinPurity() { return minPurity; }
        public double getMaxPurity() { return maxPurity; }
        public double getDefaultToxicity() { return defaultToxicity; }

        public static PillGrade fromPurity(double purity) {
            if (purity >= 0.99) return CUC_PHAM;
            if (purity >= 0.90) return THUONG_PHAM;
            if (purity >= 0.75) return TRUNG_PHAM;
            return HA_PHAM;
        }
    }

    private final int pillTier;
    private final PillGrade grade;
    private final int danLines;
    private final double purity;
    private final double danDoc;

    public BTTHPillQuality(int pillTier, PillGrade grade, int danLines, double purity, double danDoc) {
        this.pillTier = pillTier;
        this.grade = grade != null ? grade : PillGrade.HA_PHAM;
        this.danLines = Math.max(0, Math.min(3, danLines));
        this.purity = Math.max(0.50, Math.min(1.00, purity));
        this.danDoc = Math.max(0.0, danDoc);
    }

    public int getPillTier() { return pillTier; }
    public PillGrade getGrade() { return grade; }
    public int getDanLines() { return danLines; }
    public double getPurity() { return purity; }
    public double getDanDoc() { return danDoc; }

    public String getFormattedDisplay(String pillName) {
        StringBuilder sb = new StringBuilder();
        sb.append(grade.getTag()).append(" §f").append(pillName);
        if (danLines > 0) {
            sb.append(" §e[").append(danLines).append(" Văn]");
        }
        return sb.toString();
    }
}
