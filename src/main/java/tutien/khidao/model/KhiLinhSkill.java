package tutien.khidao.model;

/**
 * Các kỹ năng tự động hỗ trợ chiến đấu của Khí Linh (Phase 4).
 */
public enum KhiLinhSkill {
    KIEM_KHI_TRAC_VIET(
            "Kiếm Khí Trác Tuyệt",
            "25% tỷ lệ phóng kiếm khí nguyên tố gây thêm 150% sát thương ngũ hành."
    ),
    HO_CHU_HOANG_KIM(
            "Hộ Chủ Quang Hoàn",
            "Khi máu dưới 30%, tạo kết giới bảo vệ hấp thụ 40% sát thương và hất lùi kẻ thù (Hồi chiêu 45s)."
    ),
    LINH_BA_CUONG_NO(
            "Linh Ba Cuồng Nộ",
            "Gia tăng 50% sát thương ngũ hành khi đối đầu với Boss Thế Giới và quái tinh anh."
    );

    private final String displayName;
    private final String description;

    KhiLinhSkill(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static KhiLinhSkill fromName(String name) {
        if (name == null) return KIEM_KHI_TRAC_VIET;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KIEM_KHI_TRAC_VIET;
        }
    }
}
