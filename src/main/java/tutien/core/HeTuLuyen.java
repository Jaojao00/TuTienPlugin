package tutien.core;

/**
 * Danh sách các con đường tu luyện (Nghề nghiệp/Hệ) chuyên biệt.
 * Mỗi hệ sẽ mang lại những bùa lợi (buff) và đặc điểm riêng biệt.
 */
public enum HeTuLuyen {
    CHUA_CHON("§7Vô Đạo", "Chưa chọn đại đạo."),

    // 1. KIẾM TU: Lấy kiếm làm đạo, dùng kiếm ý chém đứt quy tắc.
    KIEM_TU("§b§lKiếm Tu", "§fTăng §c20%§f sát thương Kiếm, giảm §b50%§f tiêu hao Linh lực Ngự Kiếm."),

    // 2. THỂ TU: Nhục thân thành thánh, cứng như tiên khí.
    THE_TU("§6§lThể Tu", "§fNhục thân cường hãn, tăng §c40%§f Máu tối đa và §e10%§f Giảm sát thương nhận vào."),

    // 3. PHÁP TU: Tu luyện tinh thần lực, thần hồn và phù chú.
    PHAP_TU("§d§lPháp Tu", "§fThần hồn mạnh mẽ, tăng §b30%§f giới hạn Linh lực và §d15%§f Sát thương phép."),

    // 4. MA TU: Con đường nguy hiểm nhưng tiến cảnh cực nhanh.
    MA_TU("§4§lMa Tu", "§fSử dụng sát khí, tăng mạnh §c30%§f Sát thương nhưng giảm §820%§f khả năng phòng thủ."),

    // 5. YÊU TU: Hấp thụ nhật nguyệt tinh hoa, sức mạnh thú tính.
    YEU_TU("§2§lYêu Tu", "§fHấp thụ tinh hoa, tăng §a15%§f Tốc độ di chuyển và khả năng §2Hồi phục§f tự nhiên."),

    // 6. ĐAN ĐẠO: Dùng đan dược nâng cao cảnh giới, tinh thông dược lý.
    DAN_TU("§e§lĐan Đạo", "§fTinh thông dược lý, tăng §620%§f tỷ lệ luyện đan thành công và hiệu quả Đan dược."),

    // 7. SONG TU: Trao đổi Âm Dương, cùng tiến bộ nhanh chóng.
    SONG_TU("§d§lSong Tu", "§fCân bằng Âm Dương, tăng §d15%§f tốc độ nhận Tu Vi khi tu luyện cùng đạo lữ.");

    private final String tenHienThi;
    private final String moTa;

    HeTuLuyen(String tenHienThi, String moTa) {
        this.tenHienThi = tenHienThi;
        this.moTa = moTa;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getMoTa() {
        return moTa;
    }
}