package tutien.cultivation.service;

import tutien.core.CanhGioi;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.element.LinhCanData;

import java.util.List;

/**
 * Service xác thực logic công pháp:
 * Kiểm tra tính hợp lệ khi nhập môn, lĩnh hội khẩu quyết và chuyển tu.
 * Tích hợp kiểm tra điều kiện diệt Boss MythicMobs.
 */
public class CongPhapValidationService {

    public static class ValidationResult {
        private final boolean success;
        private final String message;

        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "§aHợp lệ!");
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Kiểm tra xem tu sĩ có đủ điều kiện nhập môn công pháp không.
     */
    public ValidationResult validateInitiation(CongPhap cp, CanhGioi realm, double ngoTinh, LinhCanData linhCan) {
        if (cp == null) {
            return ValidationResult.fail("§cCông pháp không tồn tại!");
        }

        // 1. Kiểm tra Cảnh giới tối thiểu
        if (realm != null && !realm.isAtLeast(cp.getMinRealm())) {
            return ValidationResult.fail("§cCảnh giới chưa đủ! Yêu cầu tối thiểu: §e" + cp.getMinRealm().getTenHienThi());
        }

        // 2. Kiểm tra Ngộ tính
        if (ngoTinh < cp.getMinNgoTinh()) {
            return ValidationResult.fail("§cNgộ tính chưa đủ để lĩnh ngộ! Cần tối thiểu: §e" + cp.getMinNgoTinh());
        }

        // 3. Kiểm tra Ngũ hành linh căn bắt buộc
        List<String> reqElements = cp.getRequiredElements();
        if (reqElements != null && !reqElements.isEmpty()) {
            if (linhCan == null) {
                return ValidationResult.fail("§cKhông xác định được linh căn tu sĩ!");
            }
            for (String elem : reqElements) {
                if (!linhCan.hasElement(elem)) {
                    return ValidationResult.fail("§cKinh mạch thiếu linh căn hệ §e" + elem + "§c, không thể nhập môn công pháp này!");
                }
            }
        }

        return ValidationResult.ok();
    }

    /**
     * Kiểm tra xem tu sĩ có đủ điều kiện để lĩnh hội một chương cụ thể không (không kèm dữ liệu boss).
     */
    public ValidationResult validateLearnChapter(CongPhap cp, CongPhapProgress progress, int chapter, CanhGioi realm) {
        return validateLearnChapter(cp, progress, chapter, realm, null);
    }

    /**
     * Kiểm tra xem tu sĩ có đủ điều kiện để lĩnh hội một chương cụ thể không (kèm dữ liệu boss MythicMobs).
     */
    public ValidationResult validateLearnChapter(CongPhap cp, CongPhapProgress progress, int chapter, CanhGioi realm, PlayerCultivationData data) {
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

        // Kiểm tra điều kiện hạ gục Boss MythicMobs nếu có
        if (data != null && cp.getRequiredMythicBoss() != null && !cp.getRequiredMythicBoss().isEmpty()) {
            if (!data.hasDefeatedBoss(cp.getRequiredMythicBoss())) {
                return ValidationResult.fail("§cNgươi chưa hạ gục Yêu Ma / Boss: §e" + cp.getRequiredMythicBoss() + " §cđể lĩnh ngộ công pháp này!");
            }
        }

        return ValidationResult.ok();
    }
}
