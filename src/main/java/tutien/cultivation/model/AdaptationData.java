package tutien.cultivation.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quản lý trạng thái thích nghi khi chuyển tu công pháp:
 * Trong thời gian thích nghi:
 * - Kinh mạch điều chỉnh tuần hoàn chân nguyên theo công pháp mới.
 * - Hiệu suất tu luyện và tinh luyện chịu một phần suy giảm tạm thời.
 * - Kết thúc thời gian thích nghi, công pháp hoàn toàn hòa hợp với đan điền.
 */
public class AdaptationData {

    private boolean active;
    private long endTimestamp;
    private String previousCongPhap;
    private double penaltyRatio;

    public AdaptationData() {
        this.active = false;
        this.endTimestamp = 0L;
        this.previousCongPhap = null;
        this.penaltyRatio = 0.0;
    }

    public AdaptationData(boolean active, long endTimestamp, String previousCongPhap, double penaltyRatio) {
        this.active = active;
        this.endTimestamp = endTimestamp;
        this.previousCongPhap = previousCongPhap;
        this.penaltyRatio = Math.max(0.0, Math.min(1.0, penaltyRatio));
    }

    public boolean isActive() {
        if (!active) return false;
        if (System.currentTimeMillis() >= endTimestamp) {
            this.active = false;
            return false;
        }
        return true;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public String getPreviousCongPhap() {
        return previousCongPhap;
    }

    public void setPreviousCongPhap(String previousCongPhap) {
        this.previousCongPhap = previousCongPhap;
    }

    public double getPenaltyRatio() {
        return penaltyRatio;
    }

    public void setPenaltyRatio(double penaltyRatio) {
        this.penaltyRatio = Math.max(0.0, Math.min(1.0, penaltyRatio));
    }

    public long getRemainingSeconds() {
        if (!isActive()) return 0L;
        long diff = endTimestamp - System.currentTimeMillis();
        return Math.max(0L, diff / 1000L);
    }

    public void clear() {
        this.active = false;
        this.endTimestamp = 0L;
        this.previousCongPhap = null;
        this.penaltyRatio = 0.0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("active", active);
        map.put("end-timestamp", endTimestamp);
        if (previousCongPhap != null) {
            map.put("previous-cong-phap", previousCongPhap);
        }
        map.put("penalty-ratio", penaltyRatio);
        return map;
    }

    public static AdaptationData fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new AdaptationData();
        }
        boolean active = false;
        if (map.containsKey("active")) {
            active = Boolean.parseBoolean(String.valueOf(map.get("active")));
        }
        long endTimestamp = 0L;
        if (map.containsKey("end-timestamp")) {
            try { endTimestamp = Long.parseLong(String.valueOf(map.get("end-timestamp"))); }
            catch (Exception ignored) {}
        }
        String previous = null;
        if (map.containsKey("previous-cong-phap")) {
            previous = String.valueOf(map.get("previous-cong-phap"));
        }
        double penalty = 0.0;
        if (map.containsKey("penalty-ratio")) {
            try { penalty = Double.parseDouble(String.valueOf(map.get("penalty-ratio"))); }
            catch (Exception ignored) {}
        }
        return new AdaptationData(active, endTimestamp, previous, penalty);
    }
}
