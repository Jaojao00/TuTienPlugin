package tutien.alchemy.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Các hiệu quả mà Đan Dược đem lại cho tu sĩ.
 */
public class DanDuocEffect {

    private double cultivationBonus = 0.0;
    private int instantTuVi = 0;
    private int instantLinhLuc = 0;
    private double instantHeal = 0.0;
    private double bonusDamage = 0.0;
    private double bonusHealth = 0.0;
    private double breakthroughChance = 0.0;
    private int durationSeconds = 0;
    private String stackPolicy = "REFRESH"; // REFRESH, STACK, EXTEND
    private Map<String, Integer> potionEffects = new HashMap<>();

    public DanDuocEffect() {}

    public double getCultivationBonus() {
        return cultivationBonus;
    }

    public void setCultivationBonus(double cultivationBonus) {
        this.cultivationBonus = cultivationBonus;
    }

    public int getInstantTuVi() {
        return instantTuVi;
    }

    public void setInstantTuVi(int instantTuVi) {
        this.instantTuVi = instantTuVi;
    }

    public int getInstantLinhLuc() {
        return instantLinhLuc;
    }

    public void setInstantLinhLuc(int instantLinhLuc) {
        this.instantLinhLuc = instantLinhLuc;
    }

    public double getInstantHeal() {
        return instantHeal;
    }

    public void setInstantHeal(double instantHeal) {
        this.instantHeal = instantHeal;
    }

    public double getBonusDamage() {
        return bonusDamage;
    }

    public void setBonusDamage(double bonusDamage) {
        this.bonusDamage = bonusDamage;
    }

    public double getBonusHealth() {
        return bonusHealth;
    }

    public void setBonusHealth(double bonusHealth) {
        this.bonusHealth = bonusHealth;
    }

    public double getBreakthroughChance() {
        return breakthroughChance;
    }

    public void setBreakthroughChance(double breakthroughChance) {
        this.breakthroughChance = breakthroughChance;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getStackPolicy() {
        return stackPolicy != null ? stackPolicy : "REFRESH";
    }

    public void setStackPolicy(String stackPolicy) {
        this.stackPolicy = stackPolicy;
    }

    public Map<String, Integer> getPotionEffects() {
        return potionEffects;
    }

    public void setPotionEffects(Map<String, Integer> potionEffects) {
        this.potionEffects = potionEffects != null ? potionEffects : new HashMap<>();
    }
}
