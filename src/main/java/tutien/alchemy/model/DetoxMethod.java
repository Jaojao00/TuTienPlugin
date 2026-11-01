package tutien.alchemy.model;

/**
 * 3 Phương pháp thanh lọc Đan Độc độc lập trong hệ thống Tu Tiên.
 */
public enum DetoxMethod {
    THANH_DAN("Thanh Đan Giải Độc", "Tiêu hao đan dược chuyên dụng để hóa giải đan độc tức thời."),
    LINH_TUYEN("Linh Tuyền Tẩy Trần", "Ngâm mình tại linh mạch suối nguồn để tẩy trừ trần cấu và độc tính."),
    BE_QUAN_HOA_DOC("Bế Quan Hóa Độc", "Tọa thiền định tâm, vận chuyển linh lực trục xuất độc khí ra khỏi kinh mạch.");

    private final String displayName;
    private final String description;

    DetoxMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
