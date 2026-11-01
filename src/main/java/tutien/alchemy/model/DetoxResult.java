package tutien.alchemy.model;

/**
 * Kết quả trả về của các thao tác thanh lọc và hóa độc.
 */
public class DetoxResult {

    public enum Status {
        SUCCESS,
        INVALID_ITEM,
        NOT_IN_SPRING,
        SPRING_COOLDOWN,
        ALREADY_DETOXING,
        MEDITATING_OTHER,
        NO_TOXICITY,
        CANCELLED,
        DISABLED,
        ERROR
    }

    private final boolean success;
    private final Status status;
    private final double toxicityCleansed;
    private final double remainingToxicity;
    private final String message;

    public DetoxResult(boolean success, Status status, double toxicityCleansed, double remainingToxicity, String message) {
        this.success = success;
        this.status = status;
        this.toxicityCleansed = toxicityCleansed;
        this.remainingToxicity = remainingToxicity;
        this.message = message;
    }

    public static DetoxResult success(double toxicityCleansed, double remainingToxicity, String message) {
        return new DetoxResult(true, Status.SUCCESS, toxicityCleansed, remainingToxicity, message);
    }

    public static DetoxResult fail(Status status, double remainingToxicity, String message) {
        return new DetoxResult(false, status, 0.0, remainingToxicity, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public Status getStatus() {
        return status;
    }

    public double getToxicityCleansed() {
        return toxicityCleansed;
    }

    public double getRemainingToxicity() {
        return remainingToxicity;
    }

    public String getMessage() {
        return message;
    }
}
