package tutien.cultivation.model;

/**
 * Giai đoạn tiểu cảnh giới giới hạn của công pháp:
 * - SO_KY (Sơ Kỳ)
 * - TRUNG_KY (Trung Kỳ)
 * - HAU_KY (Hậu Kỳ)
 * - DAI_VIEN_MAN (Đại Viên Mãn)
 */
public enum CongPhapStage {
    SO_KY("Sơ Kỳ", "§7Sơ Kỳ"),
    TRUNG_KY("Trung Kỳ", "§bTrung Kỳ"),
    HAU_KY("Hậu Kỳ", "§eHậu Kỳ"),
    DAI_VIEN_MAN("Đại Viên Mãn", "§6§lĐại Viên Mãn");

    private final String name;
    private final String displayName;

    CongPhapStage(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CongPhapStage fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return DAI_VIEN_MAN;
        }
        String clean = str.trim().toUpperCase();
        for (CongPhapStage s : values()) {
            if (s.name().equals(clean) || s.name.equalsIgnoreCase(clean)) {
                return s;
            }
        }
        return DAI_VIEN_MAN;
    }
}
