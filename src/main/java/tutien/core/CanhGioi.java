package tutien.core;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Enum định nghĩa các Đại Cảnh Giới.
 * Logic mới: Mỗi Đại Cảnh Giới được chia thành 10 Tiểu Cảnh Giới (Nhất Trọng -> Thập Trọng).
 */
public enum CanhGioi {
    PHAM_NHAN("Phàm Nhân", 0, 20.0, 50, 1.0),
    LUYEN_KHI("Luyện Khí", 100, 24.0, 150, 2.0),
    TRUC_CO("Trúc Cơ", 500, 30.0, 400, 4.0),
    KIM_DAN("Kim Đan", 2000, 40.0, 1000, 7.0),
    NGUYEN_ANH("Nguyên Anh", 5000, 60.0, 2500, 12.0),
    HOA_THAN("Hóa Thần", 12000, 100.0, 6000, 20.0),
    LUYEN_HU("Luyện Hư", 30000, 150.0, 15000, 35.0),
    HOP_THE("Hợp Thể", 70000, 250.0, 35000, 60.0),
    DAI_THUA("Đại Thừa", 150000, 400.0, 80000, 100.0),
    DO_KIEP("Độ Kiếp", 300000, 600.0, 150000, 150.0),
    CHAN_TIEN("Chân Tiên", 600000, 1000.0, 300000, 250.0),
    DIA_TIEN("Địa Tiên", 1200000, 1500.0, 600000, 400.0),
    THIEN_TIEN("Thiên Tiên", 2500000, 2500.0, 1200000, 700.0),
    HUYEN_TIEN("Huyền Tiên", 5000000, 4000.0, 2500000, 1200.0),
    TIEN_QUAN("Tiên Quân", 10000000, 7000.0, 6000000, 2000.0),
    TIEN_VUONG("Tiên Vương", 25000000, 12000.0, 15000000, 4000.0),
    TIEN_DE("Tiên Đế", 50000000, 20000.0, 50000000, 8000.0);

    private String tenHienThi;
    private int tuViYeuCau;
    private double maxMau;
    private int maxLinhLuc;
    private double baseDamage;

    CanhGioi(String tenHienThi, int tuViYeuCau, double maxMau, int maxLinhLuc, double baseDamage) {
        this.tenHienThi = tenHienThi;
        this.tuViYeuCau = tuViYeuCau;
        this.maxMau = maxMau;
        this.maxLinhLuc = maxLinhLuc;
        this.baseDamage = baseDamage;
    }

    public String getTenHienThi() { return tenHienThi; }
    public int getTuViYeuCau() { return tuViYeuCau; }
    public double getMaxMau() { return maxMau; }
    public int getMaxLinhLuc() { return maxLinhLuc; }
    public double getBaseDamage() { return baseDamage; }

    public CanhGioi getCanhGioiTiepTheo() {
        int nextOrdinal = this.ordinal() + 1;
        return (nextOrdinal < values().length) ? values()[nextOrdinal] : null;
    }

    /**
     * Hàm lấy tên Tiểu Cảnh Giới (Nhất Trọng -> Thập Trọng)
     */
    public String getTieuCanhGioi(int tuViHienTai) {
        if (this == TIEN_DE) return "§6[Viên Mãn]";

        CanhGioi tiepTheo = getCanhGioiTiepTheo();
        if (tiepTheo == null) return "";

        // Tính toán khoảng cách tu vi của cảnh giới này
        int startTuVi = this.tuViYeuCau;
        int endTuVi = tiepTheo.getTuViYeuCau();
        int range = endTuVi - startTuVi;

        // Nếu đạt mốc của cảnh giới tiếp theo
        if (tuViHienTai >= endTuVi) return "§c§l[Viên Mãn]";

        // Tính xem đang ở tầng thứ mấy (1 -> 10)
        int currentProgress = tuViHienTai - startTuVi;
        int tier = (int) ((currentProgress * 10.0) / range) + 1;

        // Chuyển số thành tên Trọng cho chuyên nghiệp
        return switch (tier) {
            case 1 -> "§7[Nhất Trọng]";
            case 2 -> "§7[Nhị Trọng]";
            case 3 -> "§e[Tam Trọng]";
            case 4 -> "§e[Tứ Trọng]";
            case 5 -> "§a[Ngũ Trọng]";
            case 6 -> "§a[Lục Trọng]";
            case 7 -> "§b[Thất Trọng]";
            case 8 -> "§b[Bát Trọng]";
            case 9 -> "§d[Cửu Trọng]";
            case 10 -> "§d[Thập Trọng]";
            default -> "§c§l[Viên Mãn]";
        };
    }

    public static void loadFromConfig(FileConfiguration config) {
        for (CanhGioi cg : values()) {
            String path = cg.name() + ".";
            if (config.contains(cg.name())) {
                cg.tenHienThi = config.getString(path + "TenHienThi", cg.tenHienThi);
                cg.tuViYeuCau = config.getInt(path + "TuViYeuCau", cg.tuViYeuCau);
                cg.maxMau = config.getDouble(path + "MaxMau", cg.maxMau);
                cg.maxLinhLuc = config.getInt(path + "MaxLinhLuc", cg.maxLinhLuc);
                cg.baseDamage = config.getDouble(path + "BaseDamage", cg.baseDamage);
            }
        }
    }
}