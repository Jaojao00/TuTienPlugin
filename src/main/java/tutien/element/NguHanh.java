package tutien.element;

import java.util.Arrays;
import java.util.Optional;

/**
 * Ngũ Hành Nguyên Thủy trong Đạo giáo và Tiên hiệp:
 * Kim (金), Mộc (木), Thủy (水), Hỏa (火), Thổ (土).
 *
 * Quy luật chuẩn:
 * - Tương Sinh: Mộc -> Hỏa -> Thổ -> Kim -> Thủy -> Mộc.
 * - Tương Khắc: Mộc -> Thổ -> Thủy -> Hỏa -> Kim -> Mộc.
 */
public enum NguHanh {
    KIM("Kim", "§f§lKim", "§f", "§e"),
    MOC("Mộc", "§a§lMộc", "§a", "§2"),
    THUY("Thủy", "§b§lThủy", "§b", "§9"),
    HOA("Hỏa", "§c§lHỏa", "§c", "§4"),
    THO("Thổ", "§6§lThổ", "§6", "§e");

    private final String ten;
    private final String tenHienThi;
    private final String mauChinh;
    private final String mauPhu;

    NguHanh(String ten, String tenHienThi, String mauChinh, String mauPhu) {
        this.ten = ten;
        this.tenHienThi = tenHienThi;
        this.mauChinh = mauChinh;
        this.mauPhu = mauPhu;
    }

    public String getTen() {
        return ten;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getMauChinh() {
        return mauChinh;
    }

    public String getMauPhu() {
        return mauPhu;
    }

    /**
     * Vòng Tương Sinh:
     * Mộc sinh Hỏa, Hỏa sinh Thổ, Thổ sinh Kim, Kim sinh Thủy, Thủy sinh Mộc.
     * @return Thuộc tính được sinh ra bởi thuộc tính này (this -> result)
     */
    public NguHanh getSinh() {
        switch (this) {
            case MOC: return HOA;
            case HOA: return THO;
            case THO: return KIM;
            case KIM: return THUY;
            case THUY: return MOC;
            default: return this;
        }
    }

    /**
     * Thuộc tính sinh ra thuộc tính này (source -> this).
     */
    public NguHanh getDuocSinhTu() {
        switch (this) {
            case HOA: return MOC;
            case THO: return HOA;
            case KIM: return THO;
            case THUY: return KIM;
            case MOC: return THUY;
            default: return this;
        }
    }

    /**
     * Kiểm tra xem thuộc tính này có sinh ra target không (this sinh target).
     */
    public boolean isSinh(NguHanh target) {
        return target != null && getSinh() == target;
    }

    /**
     * Kiểm tra xem thuộc tính này có được source sinh ra không (source sinh this).
     */
    public boolean isDuocSinh(NguHanh source) {
        return source != null && source.getSinh() == this;
    }

    /**
     * Vòng Tương Khắc:
     * Mộc khắc Thổ, Thổ khắc Thủy, Thủy khắc Hỏa, Hỏa khắc Kim, Kim khắc Mộc.
     * @return Thuộc tính bị khắc bởi thuộc tính này (this khắc result)
     */
    public NguHanh getKhac() {
        switch (this) {
            case MOC: return THO;
            case THO: return THUY;
            case THUY: return HOA;
            case HOA: return KIM;
            case KIM: return MOC;
            default: return this;
        }
    }

    /**
     * Thuộc tính khắc thuộc tính này (source khắc this).
     */
    public NguHanh getBiKhacBoi() {
        switch (this) {
            case THO: return MOC;
            case THUY: return THO;
            case HOA: return THUY;
            case KIM: return HOA;
            case MOC: return KIM;
            default: return this;
        }
    }

    /**
     * Kiểm tra xem thuộc tính này có khắc target không (this khắc target).
     */
    public boolean isKhac(NguHanh target) {
        return target != null && getKhac() == target;
    }

    /**
     * Kiểm tra xem thuộc tính này có bị attacker khắc không (attacker khắc this).
     */
    public boolean isBiKhac(NguHanh attacker) {
        return attacker != null && attacker.getKhac() == this;
    }

    /**
     * Tìm NguHanh theo tên hoặc chuỗi đại diện (không phân biệt hoa thường).
     */
    public static Optional<NguHanh> fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Optional.empty();
        }
        String clean = str.trim().toUpperCase();
        for (NguHanh nh : values()) {
            if (nh.name().equals(clean) || nh.ten.equalsIgnoreCase(clean)) {
                return Optional.of(nh);
            }
        }
        return Optional.empty();
    }
}
