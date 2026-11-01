package tutien.thienkiep;

/**
 * Các đợt Thiên Kiếp giáng thế (Phase 6).
 */
public enum ThienKiepDot {
    DOT_1(1, "§e§lSơ Kiếp Lôi Quang", 0.35, false, "Lôi đình phủ đầu, thử thách căn cơ nhục thân."),
    DOT_2(2, "§c§lPhá Thể Tử Lôi", 0.60, false, "Tử lôi giáng thế phá hủy hộ thể chân khí, sát thương bạo liệt."),
    DOT_3(3, "§4§lTâm Ma Hắc Kiếp", 0.85, true, "Hắc lôi kèm theo tâm ma xâm lấn thần thức, khảo nghiệm đạo tâm.");

    private final int waveNumber;
    private final String displayName;
    private final double damageRatio; // Tỷ lệ sát thương dựa theo Max HP
    private final boolean isTamMa; // Có phải đợt Tâm Ma không
    private final String description;

    ThienKiepDot(int waveNumber, String displayName, double damageRatio, boolean isTamMa, String description) {
        this.waveNumber = waveNumber;
        this.displayName = displayName;
        this.damageRatio = damageRatio;
        this.isTamMa = isTamMa;
        this.description = description;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDamageRatio() {
        return damageRatio;
    }

    public boolean isTamMa() {
        return isTamMa;
    }

    public String getDescription() {
        return description;
    }
}
