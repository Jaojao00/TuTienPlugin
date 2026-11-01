package tutien.alchemy.model;

/**
 * Các thao tác điều chỉnh hỏa hầu trong Lò Đan
 */
public enum AlchemyHeatAction {
    VAN_HOA("§e§lVăn Hỏa (+15°C)", 15.0, "Dùng lửa nhỏ, nâng nhiệt độ từ từ."),
    VU_HOA("§c§lVũ Hỏa (+40°C)", 40.0, "Dùng lửa lớn, cấp tốc nâng nhiệt khi lò quá nguội."),
    HA_HOA("§b§lHạ Hỏa (-25°C)", -25.0, "Hạ nhiệt gấp khi lò sắp bị quá nhiệt."),
    THUAN_LUA("§d§lThuần Hỏa (±0°C)", 0.0, "Dùng linh lực ổn định hỏa diễm, giảm biến động nhiệt."),
    KET_DAN("§6§lKết Đan", 0.0, "Ngưng tụ dược lực, hoàn tất mẻ đan."),
    HUY_LO("§7§lHủy Lò", 0.0, "Dập lửa và thu hồi toàn bộ nguyên liệu ký quỹ.");

    private final String displayName;
    private final double tempDelta;
    private final String description;

    AlchemyHeatAction(String displayName, double tempDelta, String description) {
        this.displayName = displayName;
        this.tempDelta = tempDelta;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getTempDelta() {
        return tempDelta;
    }

    public String getDescription() {
        return description;
    }
}
