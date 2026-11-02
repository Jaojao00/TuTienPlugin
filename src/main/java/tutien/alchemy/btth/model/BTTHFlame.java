package tutien.alchemy.btth.model;

/**
 * Lớp biểu diễn Hỏa Diễm trong Đấu Phá Thương Khung:
 * - Đấu Khí Chi Hỏa
 * - Thú Hỏa (Ma thú hỏa diễm)
 * - Dị Hỏa (Thiên địa kỳ vật)
 */
public class BTTHFlame {

    public enum FlameCategory {
        DAU_KHI,
        THU_HOA,
        DI_HOA
    }

    private final String id;
    private final String name;
    private final String displayName;
    private final FlameCategory category;
    private final double heatPower;
    private final double heatControl;
    private final double heatPrecision;
    private final double flameStability;
    private final double soulConsumption;
    private final double qualityBonus;
    private final String specialEffect;
    private final String description;

    public BTTHFlame(String id, String name, String displayName, FlameCategory category,
                     double heatPower, double heatControl, double heatPrecision,
                     double flameStability, double soulConsumption, double qualityBonus,
                     String specialEffect, String description) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.heatPower = heatPower;
        this.heatControl = heatControl;
        this.heatPrecision = heatPrecision;
        this.flameStability = flameStability;
        this.soulConsumption = soulConsumption;
        this.qualityBonus = qualityBonus;
        this.specialEffect = specialEffect != null ? specialEffect : "NONE";
        this.description = description != null ? description : "";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public FlameCategory getCategory() { return category; }
    public double getHeatPower() { return heatPower; }
    public double getHeatControl() { return heatControl; }
    public double getHeatPrecision() { return heatPrecision; }
    public double getFlameStability() { return flameStability; }
    public double getSoulConsumption() { return soulConsumption; }
    public double getQualityBonus() { return qualityBonus; }
    public String getSpecialEffect() { return specialEffect; }
    public String getDescription() { return description; }
}
