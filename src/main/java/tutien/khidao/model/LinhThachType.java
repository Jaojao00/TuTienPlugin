package tutien.khidao.model;

import tutien.element.NguHanh;

/**
 * Định nghĩa Ngũ Hành và Phẩm Cấp của Linh Thạch Khảm Nạm (Phase 4).
 */
public enum LinhThachType {
    KIM(NguHanh.KIM, "§eKim Cương Thạch", "Xuyên Giáp", "Tăng sát thương nguyên tố Kim và khả năng phá giáp."),
    MOC(NguHanh.MOC, "§aThanh Mộc Thạch", "Hút Máu", "Tăng sát thương nguyên tố Mộc và hồi phục sinh lực theo sát thương."),
    THUY(NguHanh.THUY, "§bBăng Phách Thạch", "Làm Chậm", "Tăng sát thương nguyên tố Thủy và đóng băng làm chậm đối thủ."),
    HOA(NguHanh.HOA, "§cLiệt Hỏa Thạch", "Thiêu Đốt", "Tăng sát thương nguyên tố Hỏa và đốt cháy thiêu đốt sinh mệnh."),
    THO(NguHanh.THO, "§6Huyền Thổ Thạch", "Phản Đòn", "Tăng phòng ngự hộ thể và phản chấn sát thương đối phương.");

    public enum Tier {
        SO_CAP(0, "§a[Sơ Cấp]", 10.0, 0.05),
        TRUNG_CAP(1, "§b[Trung Cấp]", 20.0, 0.10),
        CAO_CAP(2, "§d[Cao Cấp]", 35.0, 0.15),
        CUC_PHAM(3, "§6§l[Cực Phẩm]", 60.0, 0.20);

        private final int level;
        private final String prefix;
        private final double elementalPower;
        private final double specialRatio; // Tỷ lệ xuyên giáp / hút máu / phản đòn / làm chậm

        Tier(int level, String prefix, double elementalPower, double specialRatio) {
            this.level = level;
            this.prefix = prefix;
            this.elementalPower = elementalPower;
            this.specialRatio = specialRatio;
        }

        public int getLevel() {
            return level;
        }

        public String getPrefix() {
            return prefix;
        }

        public double getElementalPower() {
            return elementalPower;
        }

        public double getSpecialRatio() {
            return specialRatio;
        }

        public static Tier fromLevel(int lvl) {
            for (Tier t : values()) {
                if (t.level == lvl) return t;
            }
            return SO_CAP;
        }

        public static Tier fromName(String name) {
            if (name == null) return SO_CAP;
            try {
                return valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return SO_CAP;
            }
        }
    }

    private final NguHanh nguHanh;
    private final String displayName;
    private final String specialEffectName;
    private final String description;

    LinhThachType(NguHanh nguHanh, String displayName, String specialEffectName, String description) {
        this.nguHanh = nguHanh;
        this.displayName = displayName;
        this.specialEffectName = specialEffectName;
        this.description = description;
    }

    public NguHanh getNguHanh() {
        return nguHanh;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSpecialEffectName() {
        return specialEffectName;
    }

    public String getDescription() {
        return description;
    }

    public static LinhThachType fromElement(String element) {
        if (element == null) return KIM;
        try {
            return valueOf(element.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KIM;
        }
    }
}
