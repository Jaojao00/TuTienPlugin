package tutien.core;

/**
 * Các chế độ tọa thiền độc lập của tu sĩ (Phase 2.4):
 * - NONE: Không ở trạng thái tọa thiền.
 * - TU_LUYEN: Tọa thiền hấp thu linh khí thiên địa, tăng Tu Vi và Linh Lực.
 * - THAM_NGO: Tọa thiền lĩnh hội khẩu quyết công pháp (Comprehension), tăng tiến độ chương mục tiêu.
 */
public enum MeditationType {
    NONE("Không", "§7Chưa tọa thiền"),
    TU_LUYEN("Tọa Thiền Tu Luyện", "§d§lTọa Thiền Tu Luyện"),
    THAM_NGO("Tọa Thiền Tham Ngộ", "§b§lTọa Thiền Tham Ngộ");

    private final String name;
    private final String displayName;

    MeditationType(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
