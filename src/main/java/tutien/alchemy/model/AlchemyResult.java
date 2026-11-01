package tutien.alchemy.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Kết quả thu được sau khi kết thúc một mẻ luyện đan
 */
public class AlchemyResult {

    public enum Status {
        SUCCESS,
        FAILURE_ASHES,
        FAILURE_EXPLOSION,
        CANCELLED_REFUNDED,
        ERROR
    }

    private final Status status;
    private final AlchemyQuality quality;
    private final double finalScore;
    private final ItemStack craftedPill;
    private final int quantity;
    private final List<ItemStack> refundedItems;
    private final String message;

    public AlchemyResult(Status status, AlchemyQuality quality, double finalScore,
                         ItemStack craftedPill, int quantity,
                         List<ItemStack> refundedItems, String message) {
        this.status = status;
        this.quality = quality;
        this.finalScore = finalScore;
        this.craftedPill = craftedPill;
        this.quantity = quantity;
        this.refundedItems = refundedItems != null ? new ArrayList<>(refundedItems) : new ArrayList<>();
        this.message = message;
    }

    public static AlchemyResult success(AlchemyQuality quality, double score, ItemStack pill, int qty, String msg) {
        return new AlchemyResult(Status.SUCCESS, quality, score, pill, qty, Collections.emptyList(), msg);
    }

    public static AlchemyResult failureAshes(double score, String msg) {
        return new AlchemyResult(Status.FAILURE_ASHES, AlchemyQuality.PHE_DAN, score, null, 0, Collections.emptyList(), msg);
    }

    public static AlchemyResult failureExplosion(double score, String msg) {
        return new AlchemyResult(Status.FAILURE_EXPLOSION, AlchemyQuality.PHE_DAN, score, null, 0, Collections.emptyList(), msg);
    }

    public static AlchemyResult cancelled(List<ItemStack> refunded, String msg) {
        return new AlchemyResult(Status.CANCELLED_REFUNDED, null, 0.0, null, 0, refunded, msg);
    }

    public static AlchemyResult error(String msg) {
        return new AlchemyResult(Status.ERROR, null, 0.0, null, 0, Collections.emptyList(), msg);
    }

    public Status getStatus() {
        return status;
    }

    public AlchemyQuality getQuality() {
        return quality;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public ItemStack getCraftedPill() {
        return craftedPill;
    }

    public int getQuantity() {
        return quantity;
    }

    public List<ItemStack> getRefundedItems() {
        return Collections.unmodifiableList(refundedItems);
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS;
    }
}
