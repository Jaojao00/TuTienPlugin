package tutien.alchemy.model;

/**
 * 6 Nhóm Kháng Thuốc độc lập của hệ thống Tu Tiên.
 */
public enum ResistanceGroup {
    CULTIVATION("Tu Luyện & Hấp Thu", "Ảnh hưởng đến đan tăng tu vi, tốc độ hấp thu linh khí."),
    RECOVERY("Hồi Phục Sinh & Khí", "Ảnh hưởng đến đan hồi máu, hồi linh lực."),
    BREAKTHROUGH("Hỗ Trợ Đột Phá", "Ảnh hưởng đến các đan tăng tỷ lệ vượt cảnh giới."),
    BODY("Cường Thân Tôi Thể", "Ảnh hưởng đến đan tăng thể chất, phòng ngự, tôi thể."),
    SPIRIT("Ngưng Thần Luyện Hồn", "Ảnh hưởng đến đan tăng thần thức, ngộ tính."),
    COMBAT("Cương Hóa Chiến Đấu", "Ảnh hưởng đến đan tăng sát thương, bộc phát sức mạnh.");

    private final String displayName;
    private final String description;

    ResistanceGroup(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static ResistanceGroup fromString(String name) {
        if (name == null) return CULTIVATION;
        try {
            return ResistanceGroup.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return CULTIVATION;
        }
    }
}
