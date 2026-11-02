package tutien.alchemy.btth.model;

import org.bukkit.entity.Player;

import java.util.*;

/**
 * Phiên luyện đan Đấu Phá Thương Khung lưu trữ trạng thái chi tiết theo thời gian thực.
 */
public class BTTHAlchemySession {

    private final UUID playerId;
    private final BTTHFormula formula;
    private final BTTHFlame initialFlame;
    private BTTHFlame currentFlame;
    private final BTTHCauldron cauldron;

    private BTTHAlchemyStage currentStage;
    private double currentHeat;
    private int flameSwitchesCount;
    private final Set<String> switchedFlameIds = new HashSet<>();

    private final List<String> extractedIngredients = new ArrayList<>();
    private final Map<String, Double> ingredientPurities = new HashMap<>();
    private double overallPurity;
    private int danLines;

    private final long startTime;
    private boolean finished;
    private String failureReason;

    public BTTHAlchemySession(Player player, BTTHFormula formula, BTTHFlame flame, BTTHCauldron cauldron) {
        this.playerId = player.getUniqueId();
        this.formula = formula;
        this.initialFlame = flame;
        this.currentFlame = flame;
        this.cauldron = cauldron;
        this.currentStage = BTTHAlchemyStage.PREPARATION;
        this.currentHeat = 25.0; // Nhiệt độ phòng ban đầu
        this.flameSwitchesCount = 0;
        this.overallPurity = 0.85;
        this.danLines = 0;
        this.startTime = System.currentTimeMillis();
        this.finished = false;
        if (flame != null) {
            switchedFlameIds.add(flame.getId());
        }
    }

    public UUID getPlayerId() { return playerId; }
    public BTTHFormula getFormula() { return formula; }
    public BTTHFlame getInitialFlame() { return initialFlame; }
    public BTTHFlame getCurrentFlame() { return currentFlame; }
    public void setCurrentFlame(BTTHFlame flame) {
        if (flame != null) {
            this.currentFlame = flame;
            if (switchedFlameIds.add(flame.getId())) {
                flameSwitchesCount++;
            }
        }
    }

    public BTTHCauldron getCauldron() { return cauldron; }
    public BTTHAlchemyStage getCurrentStage() { return currentStage; }
    public void setCurrentStage(BTTHAlchemyStage currentStage) { this.currentStage = currentStage; }
    public double getCurrentHeat() { return currentHeat; }
    public void setCurrentHeat(double currentHeat) { this.currentHeat = Math.max(0.0, currentHeat); }
    public void addHeat(double delta) { setCurrentHeat(this.currentHeat + delta); }

    public int getFlameSwitchesCount() { return flameSwitchesCount; }
    public List<String> getExtractedIngredients() { return extractedIngredients; }
    public Map<String, Double> getIngredientPurities() { return ingredientPurities; }

    public void recordExtracted(String ingredientId, double purity) {
        extractedIngredients.add(ingredientId);
        ingredientPurities.put(ingredientId, purity);
    }

    public double getOverallPurity() { return overallPurity; }
    public void setOverallPurity(double overallPurity) { this.overallPurity = Math.max(0.50, Math.min(1.00, overallPurity)); }
    public void addOverallPurity(double delta) { setOverallPurity(this.overallPurity + delta); }

    public int getDanLines() { return danLines; }
    public void setDanLines(int danLines) { this.danLines = Math.max(0, Math.min(3, danLines)); }

    public long getStartTime() { return startTime; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
