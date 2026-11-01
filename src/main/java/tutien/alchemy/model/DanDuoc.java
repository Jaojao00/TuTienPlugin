package tutien.alchemy.model;

import org.bukkit.Material;
import tutien.core.CanhGioi;

import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho một loại Đan Dược trong danh mục catalog.
 */
public class DanDuoc {

    private final String id;
    private String displayName;
    private Material material;
    private String moTa;
    private DanDuocGrade grade = DanDuocGrade.HOANG;
    private CanhGioi requiredRealm = CanhGioi.PHAM_NHAN;

    private double danDocGain = 5.0;
    private String resistanceGroup = "CULTIVATION";
    private double resistanceGain = 1.0;

    private DanDuocEffect effect = new DanDuocEffect();
    private int successRate = 60;
    private List<Ingredient> ingredients = new ArrayList<>();

    public DanDuoc(String id) {
        this.id = id != null ? id.toUpperCase().trim() : "UNKNOWN";
    }

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

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Material getMaterial() {
        return material != null ? material : Material.POTION;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getMoTa() {
        return moTa != null ? moTa : "";
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public DanDuocGrade getGrade() {
        return grade != null ? grade : DanDuocGrade.HOANG;
    }

    public void setGrade(DanDuocGrade grade) {
        this.grade = grade;
    }

    public CanhGioi getRequiredRealm() {
        return requiredRealm != null ? requiredRealm : CanhGioi.PHAM_NHAN;
    }

    public void setRequiredRealm(CanhGioi requiredRealm) {
        this.requiredRealm = requiredRealm;
    }

    public double getDanDocGain() {
        return danDocGain;
    }

    public void setDanDocGain(double danDocGain) {
        this.danDocGain = Math.max(0.0, danDocGain);
    }

    public String getResistanceGroup() {
        return resistanceGroup != null ? resistanceGroup : "CULTIVATION";
    }

    public void setResistanceGroup(String resistanceGroup) {
        this.resistanceGroup = resistanceGroup;
    }

    public double getResistanceGain() {
        return resistanceGain;
    }

    public void setResistanceGain(double resistanceGain) {
        this.resistanceGain = Math.max(0.0, resistanceGain);
    }

    public DanDuocEffect getEffect() {
        if (effect == null) effect = new DanDuocEffect();
        return effect;
    }

    public void setEffect(DanDuocEffect effect) {
        this.effect = effect;
    }

    public int getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(int successRate) {
        this.successRate = Math.max(1, Math.min(100, successRate));
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }
}
