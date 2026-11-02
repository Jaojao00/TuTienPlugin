package tutien.alchemy.btth.model;

/**
 * Các giai đoạn trong quy trình luyện đan chuẩn Đấu Phá Thương Khung:
 * 1. PREPARATION: Chuẩn bị nguyên liệu, kiểm tra hỏa diễm, dược đỉnh & linh hồn lực.
 * 2. HEATING: Thao hỏa điều ôn (điều chỉnh dược đỉnh về dải nhiệt độ thích hợp).
 * 3. EXTRACTING_HERBS: Tinh luyện thảo dược thành dược dịch / dược phấn, bài trừ tạp chất.
 * 4. EXTRACTING_CORE: Tinh luyện ma hạch, phá vỡ lớp phòng hộ chiết xuất khối năng lượng tinh thuần.
 * 5. FUSION: Dược dịch dung hợp (đưa từng loại dịch thể vào theo thứ tự dược tính).
 * 6. CONDENSATION: Ngưng đan & tạo đan phôi.
 * 7. NOURISHMENT: Ôn dưỡng dược tính bằng hỏa diễm ổn định.
 * 8. COLLECTION: Thu đan đúng thời khắc quyết định phẩm chất & đan văn.
 * 9. TRIBULATION: Vượt qua Đan Lôi (chỉ xuất hiện từ Thất phẩm trở lên).
 * 10. COMPLETED: Đại thành công.
 * 11. FAILED: Thất bại (cháy thuốc, nổ lò, hồn kiệt...).
 */
public enum BTTHAlchemyStage {
    PREPARATION("Chuẩn Bị Luyện Chế"),
    HEATING("Thao Hỏa Điều Ôn"),
    EXTRACTING_HERBS("Tinh Luyện Thảo Dược"),
    EXTRACTING_CORE("Tinh Luyện Ma Hạch"),
    FUSION("Dược Dịch Dung Hợp"),
    CONDENSATION("Ngưng Đan Thành Phôi"),
    NOURISHMENT("Ôn Dưỡng Đan Dược"),
    COLLECTION("Thu Đan"),
    TRIBULATION("Đan Lôi Thiên Kiếp"),
    COMPLETED("Luyện Chế Thành Công"),
    FAILED("Luyện Chế Thất Bại");

    private final String description;

    BTTHAlchemyStage(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
