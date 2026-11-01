package tutien.cultivation.service;

import tutien.core.CanhGioi;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.element.LinhCanData;

import java.util.List;

/**
 * Dịch vụ thẩm định và kiểm tra điều kiện nhập môn, lĩnh hội từng tầng của công pháp:
 * - Kiểm tra Cảnh Giới tối thiểu.
 * - Kiểm tra Ngộ Tính.
 * - Kiểm tra Linh Căn và thuộc tính Ngũ Hành.
 * - Kiểm tra thứ tự tầng truyền thừa (không nhảy cóc).
 */
public class CongPhapValidationService {

    public static class ValidationResult {
        private final boolean success;
        private final String message;

        public ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "§aĐủ điều kiện.");
        }

        public static ValidationResult fail(String msg) {
            return new ValidationResult(false, msg);
        }
    }

    /**
     * Kiểm tra xem tu sĩ có đủ tư chất nhập môn (học chương 1 / kích hoạt) công pháp này không.
     */
    public ValidationResult validateInitiation(CongPhap cp, CanhGioi realm, double ngoTinh, LinhCanData linhCan) {
        if (cp == null) {
            return ValidationResult.fail("§cCông pháp không tồn tại trong điển tịch!");
        }

        // 1. Kiểm tra Cảnh Giới tối thiểu
        if (realm == null || !realm.isAtLeast(cp.getMinRealm())) {
            return ValidationResult.fail("§cCảnh giới chưa đủ! Yêu cầu tối thiểu: §e" + cp.getMinRealm().getTenHienThi());
        }

        // 2. Kiểm tra Ngộ Tính
        if (ngoTinh < cp.getMinNgoTinh()) {
            return ValidationResult.fail("§cNgộ tính chưa đủ để lĩnh ngộ! Yêu cầu: §e" + cp.getMinNgoTinh()
                    + " §7(Hiện tại: " + String.format("%.1f", ngoTinh) + ")");
        }

        // 3. Kiểm tra Linh Căn ngũ hành bắt buộc
        List<String> reqElements = cp.getRequiredElements();
        if (reqElements != null && !reqElements.isEmpty()) {
            if (linhCan == null) {
                return ValidationResult.fail("§cChưa trắc linh, không thể phân định thuộc tính công pháp!");
            }
            boolean match = false;
            for (String reqElem : reqElements) {
                if (linhCan.hasElement(reqElem)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return ValidationResult.fail("§cLinh căn không có thuộc tính tương thích với yêu cầu của công pháp: §6"
                        + String.join(", ", reqElements));
            }
        }

        return ValidationResult.ok();
    }

    /**
     * Kiểm tra xem tu sĩ có đủ điều kiện để lĩnh hội một chương cụ thể không.
     */
    public ValidationResult validateLearnChapter(CongPhap cp, CongPhapProgress progress, int chapter, CanhGioi realm) {
        if (cp == null) {
            return ValidationResult.fail("§cCông pháp không tồn tại!");
        }
        if (chapter <= 0 || chapter > cp.getTotalChapters()) {
            return ValidationResult.fail("§cChương " + chapter + " vượt quá tổng số " + cp.getTotalChapters() + " chương của công pháp!");
        }

        // Phải sở hữu tàn quyển/bí tịch chương này
        if (progress == null || !progress.hasCollected(chapter)) {
            return ValidationResult.fail("§cChưa sở hữu tàn quyển chương " + chapter + "! Cần tìm kiếm bí tịch trước.");
        }

        // Đã học rồi thì không cần học lại
        if (progress.hasLearned(chapter)) {
            return ValidationResult.fail("§eChương " + chapter + " đã được lĩnh hội hoàn tất.");
        }

        // Thứ tự lĩnh hội: nếu chapter > 1, bắt buộc phải học xong chapter - 1
        if (chapter > 1 && !progress.hasLearned(chapter - 1)) {
            return ValidationResult.fail("§cKhông thể nhảy cóc! Ngươi cần lĩnh hội thấu đáo chương " + (chapter - 1) + " trước.");
        }

        // Kiểm tra cảnh giới yêu cầu riêng cho chương đó
        CanhGioi reqRealm = cp.getChapterRequiredRealm(chapter);
        if (realm != null && !realm.isAtLeast(reqRealm)) {
            return ValidationResult.fail("§cChương " + chapter + " yêu cầu cảnh giới tối thiểu: §e" + reqRealm.getTenHienThi());
        }

        return ValidationResult.ok();
    }
}
