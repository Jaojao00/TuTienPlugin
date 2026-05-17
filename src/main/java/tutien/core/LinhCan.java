package tutien.core;

import java.util.Random;

public enum LinhCan {
    PHE_LINH_CAN("Phế Linh Căn", "§8", 1),       // Tốc độ x1
    TAP_LINH_CAN("Tạp Linh Căn", "§7", 2),       // Tốc độ x2
    CHAN_LINH_CAN("Chân Linh Căn", "§a", 3),      // Tốc độ x3
    DIA_LINH_CAN("Địa Linh Căn", "§d", 5),        // Tốc độ x5
    THIEN_LINH_CAN("Thiên Linh Căn", "§e", 10),    // Tốc độ x10
    HON_DON_LINH_CAN("Hỗn Độn Linh Căn", "§c§l", 20); // Tốc độ x20 (Cực kỳ hiếm)

    private final String ten;
    private final String mau;
    private final int tocDoTuLuyen; // Hệ số nhân khi treo máy

    LinhCan(String ten, String mau, int tocDoTuLuyen) {
        this.ten = ten;
        this.mau = mau;
        this.tocDoTuLuyen = tocDoTuLuyen;
    }

    public String getTenHienThi() { return mau + ten; }
    public int getTocDo() { return tocDoTuLuyen; }

    // Random linh căn theo tỷ lệ (Phế nhiều nhất, Hỗn Độn hiếm nhất)
    public static LinhCan randomLinhCan() {
        int r = new Random().nextInt(1000); // Tỷ lệ phần nghìn
        if (r < 5) return HON_DON_LINH_CAN;   // 0.5%
        if (r < 30) return THIEN_LINH_CAN;    // 2.5%
        if (r < 150) return DIA_LINH_CAN;     // 12%
        if (r < 400) return CHAN_LINH_CAN;    // 25%
        if (r < 700) return TAP_LINH_CAN;     // 30%
        return PHE_LINH_CAN;                  // 30%
    }
}