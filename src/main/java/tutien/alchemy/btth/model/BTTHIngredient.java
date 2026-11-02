package tutien.alchemy.btth.model;

import org.bukkit.Material;

/**
 * Lớp biểu diễn Thảo Dược và Ma Hạch trong Đấu Phá Thương Khung.
 */
public class BTTHIngredient {

    public enum IngredientType {
        HERB,        // Thảo dược chiết xuất thành dược dịch / dược phấn
        BEAST_CORE,  // Ma hạch ma thú chiết xuất thành khối năng lượng tinh thuần
        MINERAL,     // Khoáng thạch phụ trợ
        LIQUID       // Linh dịch đặc thù
    }

    private final String id;
    private final String name;
    private final String displayName;
    private final IngredientType type;
    private final String element;
    private final double optimalHeatMin;
    private final double optimalHeatMax;
    private final int extractTimeTicks;
    private final double baseImpurity;
    private final Material material;
    private final String description;

    public BTTHIngredient(String id, String name, String displayName, IngredientType type,
                          String element, double optimalHeatMin, double optimalHeatMax,
                          int extractTimeTicks, double baseImpurity, Material material,
                          String description) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.element = element != null ? element : "NONE";
        this.optimalHeatMin = optimalHeatMin;
        this.optimalHeatMax = optimalHeatMax;
        this.extractTimeTicks = extractTimeTicks;
        this.baseImpurity = Math.max(0.0, Math.min(1.0, baseImpurity));
        this.material = material != null ? material : Material.PAPER;
        this.description = description != null ? description : "";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public IngredientType getType() { return type; }
    public String getElement() { return element; }
    public double getOptimalHeatMin() { return optimalHeatMin; }
    public double getOptimalHeatMax() { return optimalHeatMax; }
    public int getExtractTimeTicks() { return extractTimeTicks; }
    public double getBaseImpurity() { return baseImpurity; }
    public Material getMaterial() { return material; }
    public String getDescription() { return description; }

    public boolean isWithinOptimalHeat(double heat) {
        return heat >= optimalHeatMin && heat <= optimalHeatMax;
    }
}
