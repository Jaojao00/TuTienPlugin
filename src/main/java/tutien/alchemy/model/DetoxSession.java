package tutien.alchemy.model;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Phiên Bế Quan Hóa Độc độc lập trong RAM của từng tu sĩ.
 */
public class DetoxSession {

    private final UUID playerId;
    private final long startedAt;
    private long lastTickAt;
    private double totalCleansed;
    private final Location startLocation;
    private volatile boolean active;

    public DetoxSession(UUID playerId, Location startLocation) {
        this.playerId = playerId;
        this.startedAt = System.currentTimeMillis();
        this.lastTickAt = this.startedAt;
        this.totalCleansed = 0.0;
        this.startLocation = startLocation != null ? startLocation.clone() : null;
        this.active = true;
    }

    /**
     * Thực hiện một nhịp thanh lọc an toàn:
     * - Giới hạn delta time (maxTickGapSeconds) chống lag spike.
     * - Trả về lượng độc tố được thanh lọc trong chu kỳ này.
     */
    public synchronized double tick(double ratePerMinute, double maxTickGapSeconds) {
        if (!active) return 0.0;

        long now = System.currentTimeMillis();
        double deltaSeconds = (now - lastTickAt) / 1000.0;

        // Bỏ qua nếu thời gian trôi qua quá nhỏ (< 0.1s)
        if (deltaSeconds < 0.1) return 0.0;

        // Kẹp delta tối đa (chống lag server hoặc time manipulation)
        if (deltaSeconds > maxTickGapSeconds) {
            deltaSeconds = maxTickGapSeconds;
        }

        double cleansed = (ratePerMinute / 60.0) * deltaSeconds;
        this.lastTickAt = now;
        this.totalCleansed += cleansed;

        return cleansed;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getLastTickAt() {
        return lastTickAt;
    }

    public double getTotalCleansed() {
        return totalCleansed;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public boolean isActive() {
        return active;
    }

    public void stop() {
        this.active = false;
    }
}
