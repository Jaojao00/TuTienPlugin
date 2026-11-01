package tutien.element;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Dị Biến Linh Căn: Thuộc tính biến dị từ Ngũ Hành hoặc biến dị đặc thù.
 *
 * Theo nguyên tác Phàm Nhân Tu Tiên (Chương 127) và thiết kế hệ thống:
 * - Lôi Linh Căn: Dị biến từ Thổ + Thủy (土 + 水).
 * - Băng Linh Căn: Dị biến từ Kim + Thủy (金 + 水).
 * - Phong Linh Căn: Dị biến từ Mộc + tự nhiên (hoặc Mộc + Thủy).
 * - Ám Linh Căn: Dị biến Ma Đạo / Hắc ám thuần túy.
 * - Quang Linh Căn: Dị biến Thuần Dương / Thần thánh.
 * - Huyết Linh Căn: Dị biến Khí Huyết Ma Đạo.
 * - Mộng Linh Căn: Dị biến Huyễn Cảnh / Thần Thức.
 */
public enum DiLinhCan {
    LOI("Lôi", "§9§lLôi", "§9", Arrays.asList(NguHanh.THO, NguHanh.THUY), "Dị biến từ Thổ và Thủy, cuồng bạo bá đạo, khắc chế tà ma"),
    BANG("Băng", "§b§lBăng", "§b", Arrays.asList(NguHanh.KIM, NguHanh.THUY), "Dị biến từ Kim và Thủy, băng hàn thấu xương, ngưng đọng vạn vật"),
    PHONG("Phong", "§3§lPhong", "§3", Arrays.asList(NguHanh.MOC), "Dị biến từ Mộc, vô tung vô ảnh, tốc độ cực hạn"),
    AM("Ám", "§8§lÁm", "§8", Collections.emptyList(), "Biến dị dị chất hắc ám, thôn phệ linh khí, bí ẩn quỷ dị"),
    QUANG("Quang", "§e§lQuang", "§e", Collections.emptyList(), "Biến dị thuần dương cực trí, tịnh hóa tà khí, chiếu rọi hư không"),
    HUYET("Huyết", "§4§lHuyết", "§4", Collections.emptyList(), "Biến dị huyết đạo, liên kết sinh mệnh và khí huyết bản nguyên"),
    MONG("Mộng", "§d§lMộng", "§d", Collections.emptyList(), "Biến dị thần thức ảo cảnh, dệt mộng mê hồn, công kích tâm thần");

    private final String ten;
    private final String tenHienThi;
    private final String mauChinh;
    private final List<NguHanh> nguHanhGoc;
    private final String moTa;

    DiLinhCan(String ten, String tenHienThi, String mauChinh, List<NguHanh> nguHanhGoc, String moTa) {
        this.ten = ten;
        this.tenHienThi = tenHienThi;
        this.mauChinh = mauChinh;
        this.nguHanhGoc = nguHanhGoc;
        this.moTa = moTa;
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

    public List<NguHanh> getNguHanhGoc() {
        return Collections.unmodifiableList(nguHanhGoc);
    }

    public String getMoTa() {
        return moTa;
    }

    /**
     * Kiểm tra xem dị biến này có nguồn gốc từ ngũ hành cụ thể không.
     */
    public boolean containsNguHanhGoc(NguHanh nh) {
        return nguHanhGoc.contains(nh);
    }

    public static Optional<DiLinhCan> fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Optional.empty();
        }
        String clean = str.trim().toUpperCase();
        for (DiLinhCan d : values()) {
            if (d.name().equals(clean) || d.ten.equalsIgnoreCase(clean)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }
}
