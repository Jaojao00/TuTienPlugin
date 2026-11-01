package tutien.alchemy.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Quản lý phiên luyện đan mini-game trong bộ nhớ máy chủ (RAM Session).
 * Tích hợp Ký Quỹ Escrow nguyên liệu để chống bug dupe đồ và chống mất đồ khi server crash.
 */
public class AlchemySession {

    private final UUID transactionId;
    private final UUID playerId;
    private final AlchemyRecipe recipe;
    private double currentTemperature;
    private final int totalDurationSeconds;
    private int elapsedSeconds;

    private int inToleranceTicks;
    private int outOfToleranceTicks;
    private int consecutiveOverheatTicks;

    private final List<ItemStack> escrowItems = new ArrayList<>();
    private boolean finished = false;
    private boolean cancelled = false;

    private final long startTimeMillis;
    private long lastTickTimeMillis;

    public AlchemySession(UUID playerId, AlchemyRecipe recipe, List<ItemStack> lockedIngredients) {
        this(UUID.randomUUID(), playerId, recipe, lockedIngredients);
    }

    public AlchemySession(UUID transactionId, UUID playerId, AlchemyRecipe recipe, List<ItemStack> lockedIngredients) {
        this.transactionId = transactionId != null ? transactionId : UUID.randomUUID();
        this.playerId = playerId;
        this.recipe = recipe;
        // Bắt đầu ở nhiệt độ phòng (khoảng 30°C) hoặc 30% dải nhiệt lý tưởng
        this.currentTemperature = 35.0;
        this.totalDurationSeconds = recipe != null ? recipe.getDurationSeconds() : 15;
        this.elapsedSeconds = 0;
        this.inToleranceTicks = 0;
        this.outOfToleranceTicks = 0;
        this.consecutiveOverheatTicks = 0;

        if (lockedIngredients != null) {
            for (ItemStack item : lockedIngredients) {
                if (item != null) {
                    this.escrowItems.add(item.clone());
                }
            }
        }

        this.startTimeMillis = System.currentTimeMillis();
        this.lastTickTimeMillis = this.startTimeMillis;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public AlchemyRecipe getRecipe() {
        return recipe;
    }

    public double getCurrentTemperature() {
        return currentTemperature;
    }

    public void adjustTemperature(double delta) {
        if (finished || cancelled) return;
        this.currentTemperature = Math.max(20.0, Math.min(1200.0, this.currentTemperature + delta));
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getRemainingSeconds() {
        return Math.max(0, totalDurationSeconds - elapsedSeconds);
    }

    public int getInToleranceTicks() {
        return inToleranceTicks;
    }

    public int getOutOfToleranceTicks() {
        return outOfToleranceTicks;
    }

    public int getConsecutiveOverheatTicks() {
        return consecutiveOverheatTicks;
    }

    public List<ItemStack> getEscrowItems() {
        return Collections.unmodifiableList(escrowItems);
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getLastTickTimeMillis() {
        return lastTickTimeMillis;
    }

    /**
     * Tiến trình cập nhật mỗi giây (được gọi từ AlchemyTask)
     * @return true nếu lò phát nổ do quá nhiệt liên tục
     */
    public boolean tickOneSecond() {
        if (finished || cancelled) return false;

        elapsedSeconds++;
        lastTickTimeMillis = System.currentTimeMillis();

        // 1. Tự nhiên nguội dần theo tốc độ làm nguội của công thức
        double naturalCooling = recipe != null ? recipe.getNaturalCoolingRate() : 10.0;
        currentTemperature = Math.max(25.0, currentTemperature - naturalCooling);

        // 2. Kiểm tra nhiệt độ trong dải dung sai
        if (recipe != null && recipe.isInTolerance(currentTemperature)) {
            inToleranceTicks++;
            consecutiveOverheatTicks = 0;
        } else {
            outOfToleranceTicks++;
        }

        // 3. Kiểm tra nguy cơ nổ lò do quá nhiệt
        if (recipe != null && currentTemperature >= recipe.getExplosionTemp()) {
            consecutiveOverheatTicks++;
            if (consecutiveOverheatTicks >= 3) {
                // Quá nhiệt liên tục 3 giây -> Lò phát nổ!
                return true;
            }
        } else {
            consecutiveOverheatTicks = 0;
        }

        return false;
    }

    /**
     * Tính toán điểm số hỏa hầu (0..100)
     */
    public double calculateScore() {
        int totalTicks = inToleranceTicks + outOfToleranceTicks;
        if (totalTicks <= 0) return 0.0;

        double ratio = (double) inToleranceTicks / totalTicks;
        // Điểm cơ bản dựa trên tỷ lệ thời gian giữ đúng nhiệt độ (0..100)
        double score = ratio * 100.0;

        // Phạt nặng nếu thời gian kết đan quá ngắn (dưới 50% thời gian chuẩn)
        if (elapsedSeconds < totalDurationSeconds / 2) {
            score *= 0.5;
        }

        return Math.max(0.0, Math.min(100.0, score));
    }
}
