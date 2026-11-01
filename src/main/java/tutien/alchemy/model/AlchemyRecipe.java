package tutien.alchemy.model;

import org.bukkit.Material;
import tutien.core.CanhGioi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Công thức luyện đan trong hệ thống Mini-Game Hỏa Hầu
 */
public class AlchemyRecipe {

    private final String id;
    private final String danDuocId;
    private final String displayName;
    private final CanhGioi minRealm;
    private final List<Ingredient> ingredients;
    private final double idealTemperature;
    private final double minToleranceTemp;
    private final double maxToleranceTemp;
    private final int durationSeconds;
    private final double naturalCoolingRate; // Tốc độ nguội tự nhiên (°C/giây)
    private final double baseSuccessRate;
    private final double explosionTemp;

    public static class Ingredient {
        private final Material material;
        private final int amount;
        private final String vietnameseName;

        public Ingredient(Material material, int amount, String vietnameseName) {
            this.material = material;
            this.amount = amount;
            this.vietnameseName = vietnameseName;
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        public String getVietnameseName() {
            return vietnameseName;
        }
    }

    public AlchemyRecipe(String id, String danDuocId, String displayName, CanhGioi minRealm,
                         List<Ingredient> ingredients, double idealTemperature,
                         double minToleranceTemp, double maxToleranceTemp,
                         int durationSeconds, double naturalCoolingRate,
                         double baseSuccessRate, double explosionTemp) {
        this.id = id;
        this.danDuocId = danDuocId;
        this.displayName = displayName;
        this.minRealm = minRealm != null ? minRealm : CanhGioi.PHAM_NHAN;
        this.ingredients = ingredients != null ? new ArrayList<>(ingredients) : new ArrayList<>();
        this.idealTemperature = idealTemperature;
        this.minToleranceTemp = minToleranceTemp;
        this.maxToleranceTemp = maxToleranceTemp;
        this.durationSeconds = Math.max(5, durationSeconds);
        this.naturalCoolingRate = naturalCoolingRate;
        this.baseSuccessRate = Math.max(0.1, Math.min(1.0, baseSuccessRate));
        this.explosionTemp = explosionTemp;
    }

    public String getId() {
        return id;
    }

    public String getDanDuocId() {
        return danDuocId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CanhGioi getMinRealm() {
        return minRealm;
    }

    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public double getIdealTemperature() {
        return idealTemperature;
    }

    public double getMinToleranceTemp() {
        return minToleranceTemp;
    }

    public double getMaxToleranceTemp() {
        return maxToleranceTemp;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public double getNaturalCoolingRate() {
        return naturalCoolingRate;
    }

    public double getBaseSuccessRate() {
        return baseSuccessRate;
    }

    public double getExplosionTemp() {
        return explosionTemp;
    }

    /**
     * Kiểm tra xem nhiệt độ có nằm trong dải dung sai cho phép không
     */
    public boolean isInTolerance(double temp) {
        return temp >= minToleranceTemp && temp <= maxToleranceTemp;
    }
}
