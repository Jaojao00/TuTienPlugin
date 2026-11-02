package tutien.alchemy.btth.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp biểu diễn Đan Phương trong Đấu Phá Thương Khung.
 */
public class BTTHFormula {

    public static class IngredientRequirement {
        private final String ingredientId;
        private final int amount;
        private final String role; // PRIMARY, SECONDARY, CORE

        public IngredientRequirement(String ingredientId, int amount, String role) {
            this.ingredientId = ingredientId;
            this.amount = amount;
            this.role = role != null ? role : "PRIMARY";
        }

        public String getIngredientId() { return ingredientId; }
        public int getAmount() { return amount; }
        public String getRole() { return role; }
    }

    private final String id;
    private final String displayName;
    private final String chineseName;
    private final int tier;
    private final AlchemistRank requiredRank;
    private final double requiredSoulPower;
    private final SoulRealm requiredSoulRealm;
    private final int cauldronTierMin;
    private final double targetHeat;
    private final List<IngredientRequirement> ingredients;
    private final List<String> fusionOrder;
    private final boolean flameSwitchingRequired;
    private final int flameSwitchCount;
    private final int maxLines;
    private final boolean tribulationEnabled;
    private final String effectType;
    private final double effectValue;
    private final String effectLore;

    public BTTHFormula(String id, String displayName, String chineseName, int tier,
                       AlchemistRank requiredRank, double requiredSoulPower,
                       SoulRealm requiredSoulRealm, int cauldronTierMin, double targetHeat,
                       List<IngredientRequirement> ingredients, List<String> fusionOrder,
                       boolean flameSwitchingRequired, int flameSwitchCount,
                       int maxLines, boolean tribulationEnabled,
                       String effectType, double effectValue, String effectLore) {
        this.id = id;
        this.displayName = displayName;
        this.chineseName = chineseName != null ? chineseName : "";
        this.tier = tier;
        this.requiredRank = requiredRank != null ? requiredRank : AlchemistRank.HOC_DO;
        this.requiredSoulPower = requiredSoulPower;
        this.requiredSoulRealm = requiredSoulRealm != null ? requiredSoulRealm : SoulRealm.PHAM_CANH;
        this.cauldronTierMin = cauldronTierMin;
        this.targetHeat = targetHeat;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.fusionOrder = fusionOrder != null ? fusionOrder : new ArrayList<>();
        this.flameSwitchingRequired = flameSwitchingRequired;
        this.flameSwitchCount = flameSwitchCount;
        this.maxLines = maxLines;
        this.tribulationEnabled = tribulationEnabled;
        this.effectType = effectType != null ? effectType : "NONE";
        this.effectValue = effectValue;
        this.effectLore = effectLore != null ? effectLore : "";
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getChineseName() { return chineseName; }
    public int getTier() { return tier; }
    public AlchemistRank getRequiredRank() { return requiredRank; }
    public double getRequiredSoulPower() { return requiredSoulPower; }
    public SoulRealm getRequiredSoulRealm() { return requiredSoulRealm; }
    public int getCauldronTierMin() { return cauldronTierMin; }
    public double getTargetHeat() { return targetHeat; }
    public List<IngredientRequirement> getIngredients() { return Collections.unmodifiableList(ingredients); }
    public List<String> getFusionOrder() { return Collections.unmodifiableList(fusionOrder); }
    public boolean isFlameSwitchingRequired() { return flameSwitchingRequired; }
    public int getFlameSwitchCount() { return flameSwitchCount; }
    public int getMaxLines() { return maxLines; }
    public boolean isTribulationEnabled() { return tribulationEnabled; }
    public String getEffectType() { return effectType; }
    public double getEffectValue() { return effectValue; }
    public String getEffectLore() { return effectLore; }
}
