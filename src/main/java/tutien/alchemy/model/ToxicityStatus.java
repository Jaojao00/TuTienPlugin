package tutien.alchemy.model;

/**
 * Trạng thái Đan Độc của tu sĩ (thang chuẩn 0 - 1000).
 */
public enum ToxicityStatus {
    THANH_KHIET("§aThanh Khiết", 0.0, 199.99, 1.0, "Không có tác dụng phụ."),
    DOC_NHE("§eĐộc Nhẹ", 200.0, 399.99, 0.95, "Giảm 5% hiệu suất tu luyện và hấp thu."),
    DOC_TRUNG("§6Độc Trung", 400.0, 599.99, 0.85, "Giảm 15% hiệu quả hấp thu linh khí."),
    DOC_NANG("§cĐộc Nặng", 600.0, 799.99, 0.70, "Giảm 30% hiệu suất, bắt đầu chịu áp lực độc tính."),
    DOC_PHAT("§4§lĐộc Phát", 800.0, 1000.0, 0.50, "Cực kỳ nguy hiểm! Có nguy cơ tẩu hỏa nhập ma và tổn thương kinh mạch.");

    private final String displayName;
    private final double minDoc;
    private final double maxDoc;
    private final double cultivationEfficiency;
    private final String description;

    ToxicityStatus(String displayName, double minDoc, double maxDoc, double cultivationEfficiency, String description) {
        this.displayName = displayName;
        this.minDoc = minDoc;
        this.maxDoc = maxDoc;
        this.cultivationEfficiency = cultivationEfficiency;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMinDoc() {
        return minDoc;
    }

    public double getMaxDoc() {
        return maxDoc;
    }

    public double getCultivationEfficiency() {
        return cultivationEfficiency;
    }

    public String getDescription() {
        return description;
    }

    public static ToxicityStatus fromDanDoc(double danDoc) {
        if (danDoc < 200.0) return THANH_KHIET;
        if (danDoc < 400.0) return DOC_NHE;
        if (danDoc < 600.0) return DOC_TRUNG;
        if (danDoc < 800.0) return DOC_NANG;
        return DOC_PHAT;
    }
}
