package tutien.cultivation.model;

/**
 * Bậc phẩm chất công pháp độc lập với phẩm cấp:
 * - HA (Hạ phẩm)
 * - TRUNG (Trung phẩm)
 * - THUONG (Thượng phẩm)
 */
public enum CongPhapQuality {
    HA("Hạ Phẩm", "§7Hạ Phẩm", 0.95),
    TRUNG("Trung Phẩm", "§bTrung Phẩm", 1.05),
    THUONG("Thượng Phẩm", "§6Thượng Phẩm", 1.15);

    private final String name;
    private final String displayName;
    private final double multiplier;

    CongPhapQuality(String name, String displayName, double multiplier) {
        this.name = name;
        this.displayName = displayName;
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public static CongPhapQuality fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return TRUNG;
        }
        String clean = str.trim().toUpperCase();
        for (CongPhapQuality q : values()) {
            if (q.name().equals(clean) || q.name.equalsIgnoreCase(clean)) {
                return q;
            }
        }
        return TRUNG;
    }
}
