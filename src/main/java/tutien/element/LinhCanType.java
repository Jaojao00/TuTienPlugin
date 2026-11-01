package tutien.element;

/**
 * Phân loại phẩm cách Linh Căn chuẩn nguyên tác Phàm Nhân Tu Tiên (Chương 127):
 *
 * 1. THIEN_LINH_CAN: 1 thuộc tính ngũ hành duy nhất. Tốc độ tu luyện 2.0x - 3.0x,
 *    có ưu thế cực lớn khi vượt bình cảnh Kết Đan.
 * 2. CHAN_LINH_CAN: 2 - 3 thuộc tính ngũ hành. Tư chất tốt, tốc độ tu luyện 1.2x - 1.8x.
 * 3. NGUY_LINH_CAN: 4 - 5 thuộc tính (Tạp Linh Căn). Tư chất phàm nhân tầm thường (như Hàn Lập 4 thuộc tính),
 *    tốc độ tu luyện 0.8x - 1.0x, đòi hỏi kiên trì, đan dược và đại cơ duyên.
 * 4. DI_LINH_CAN: Thuộc tính biến dị (Lôi, Băng, Phong,...). Tốc độ 1.8x - 2.5x, uy lực công kích vượt trội
 *    nhưng không có đặc quyền miễn bình cảnh Kết Đan của Thiên Linh Căn.
 */
public enum LinhCanType {
    THIEN_LINH_CAN("Thiên Linh Căn", "§e§lThiên Linh Căn", "§e", 2.5, 1, 1,
            "Thiên chi kiêu tử, 1 thuộc tính thuần nhất, tốc độ tu luyện cực nhanh, hữu thế đột phá Kết Đan."),
    CHAN_LINH_CAN("Chân Linh Căn", "§a§lChân Linh Căn", "§a", 1.5, 2, 3,
            "Tư chất xuất chúng, 2-3 thuộc tính ngũ hành hài hòa, tiến cảnh vững vàng."),
    NGUY_LINH_CAN("Ngụy Linh Căn", "§7§lNgụy Linh Căn", "§7", 1.0, 4, 5,
            "Tạp linh căn 4-5 thuộc tính, linh khí khó ngưng tụ, con đường gian nan cần nghị lực phi thường."),
    DI_LINH_CAN("Dị Linh Căn", "§d§lDị Linh Căn", "§d", 2.0, 1, 2,
            "Linh căn biến dị đặc thù, sát thương thần thông kinh người, huyền diệu khó lường.");

    private final String ten;
    private final String tenHienThi;
    private final String mau;
    private final double heSoTocDoTuLuyen;
    private final int minElements;
    private final int maxElements;
    private final String moTa;

    LinhCanType(String ten, String tenHienThi, String mau, double heSoTocDoTuLuyen,
                int minElements, int maxElements, String moTa) {
        this.ten = ten;
        this.tenHienThi = tenHienThi;
        this.mau = mau;
        this.heSoTocDoTuLuyen = heSoTocDoTuLuyen;
        this.minElements = minElements;
        this.maxElements = maxElements;
        this.moTa = moTa;
    }

    public String getTen() {
        return ten;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getMau() {
        return mau;
    }

    public double getHeSoTocDoTuLuyen() {
        return heSoTocDoTuLuyen;
    }

    public int getMinElements() {
        return minElements;
    }

    public int getMaxElements() {
        return maxElements;
    }

    public String getMoTa() {
        return moTa;
    }
}
