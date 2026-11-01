package tutien.cultivation.service;

import tutien.core.CanhGioi;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.AdaptationData;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.element.ElementRelationService;

/**
 * Dịch vụ xử lý Chuyển Tu Công Pháp an toàn tuyệt đối:
 *
 * NGUYÊN TẮC BẤT BIẾN:
 * 1. TUYỆT ĐỐI KHÔNG HẠ ĐẠI CẢNH GIỚI (Trúc Cơ không bao giờ tụt về Luyện Khí).
 * 2. TUYỆT ĐỐI KHÔNG XÓA CĂN CƠ, LINH CĂN, THẦN THỨC, THỂ CHẤT, NGHỀ NGHIỆP.
 * 3. BẢO TOÀN NGUYÊN VẸN các chương tàn quyển và khẩu quyết đã học của công pháp cũ.
 * 4. Tổn thất (nếu có) CHỈ TRỪ TRÊN TIẾN ĐỘ TIỂU CẢNH GIỚI HIỆN TẠI (Tu Vi thặng dư của tầng).
 * 5. Thiết lập giai đoạn Thích Nghi với thời gian end-timestamp lưu chuẩn xác qua restart server.
 */
public class CongPhapTransitionService {

    private final ElementRelationService relationService;

    public CongPhapTransitionService(ElementRelationService relationService) {
        this.relationService = relationService != null ? relationService : new ElementRelationService();
    }

    public static class TransitionResult {
        private final boolean success;
        private final String message;
        private final int tuViLost;
        private final long adaptationMinutes;

        public TransitionResult(boolean success, String message, int tuViLost, long adaptationMinutes) {
            this.success = success;
            this.message = message;
            this.tuViLost = tuViLost;
            this.adaptationMinutes = adaptationMinutes;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getTuViLost() {
            return tuViLost;
        }

        public long getAdaptationMinutes() {
            return adaptationMinutes;
        }

        public static TransitionResult fail(String msg) {
            return new TransitionResult(false, msg, 0, 0);
        }

        public static TransitionResult success(String msg, int lost, long minutes) {
            return new TransitionResult(true, msg, lost, minutes);
        }
    }

    public double calculatePenaltyRatio(CongPhap oldCp, CongPhap targetCp, boolean hasHuMaiDan) {
        if (targetCp == null) return 0.15;
        double penaltyRatio = targetCp.getTransitionPenaltyRatio();
        if (oldCp != null && !oldCp.getElements().isEmpty() && !targetCp.getElements().isEmpty()) {
            String oldElem = oldCp.getElements().get(0);
            String newElem = targetCp.getElements().get(0);
            double rel = relationService.getRelation(oldElem, newElem);

            if (rel >= 1.50) {
                penaltyRatio *= 0.50;
            } else if (rel >= 1.20) {
                penaltyRatio *= 0.75;
            } else if (rel <= 0.60 && !hasHuMaiDan) {
                penaltyRatio *= 1.40;
            }
        }
        if (hasHuMaiDan) {
            penaltyRatio *= 0.50;
        }
        return Math.max(0.0, Math.min(0.50, penaltyRatio));
    }

    public long calculateAdaptationMinutes(CongPhap oldCp, CongPhap targetCp, boolean hasHuMaiDan) {
        if (targetCp == null) return 15;
        long durationMinutes;
        switch (targetCp.getGradeTier()) {
            case HOANG: durationMinutes = 10; break;
            case HUYEN: durationMinutes = 25; break;
            case DIA:   durationMinutes = 60; break;
            case THIEN: durationMinutes = 120; break;
            case TIEN:  durationMinutes = 240; break;
            default:    durationMinutes = 15; break;
        }
        if (hasHuMaiDan) {
            durationMinutes = Math.max(5, durationMinutes / 2);
        }
        return durationMinutes;
    }

    /**
     * Thực hiện chuyển đổi công pháp chủ tu.
     *
     * @param data Dữ liệu tu luyện của người chơi
     * @param oldCp Công pháp chủ tu hiện tại (có thể null nếu chưa có)
     * @param targetCp Công pháp muốn chuyển sang
     * @param currentTuVi Điểm Tu Vi hiện tại của người chơi
     * @param realm Cảnh giới đại diện hiện tại
     * @param hasHuMaiDan Người chơi có sử dụng Hộ Mạch Đan không
     * @return Kết quả chuyển tu và số tu vi tổn thất (để PlayerDataManager cập nhật)
     */
    public TransitionResult transition(PlayerCultivationData data, CongPhap oldCp, CongPhap targetCp,
                                       int currentTuVi, CanhGioi realm, boolean hasHuMaiDan) {
        if (data == null) {
            return TransitionResult.fail("§cDữ liệu tu luyện không hợp lệ!");
        }
        if (targetCp == null) {
            return TransitionResult.fail("§cCông pháp mục tiêu không tồn tại!");
        }

        String targetId = targetCp.getId();
        String currentPrimary = data.getActivePrimary();

        // Đang tu chính công pháp này rồi
        if (targetId.equalsIgnoreCase(currentPrimary)) {
            return TransitionResult.fail("§eNgươi đang vận hành công pháp này làm chủ tu!");
        }

        // Kiểm tra xem công pháp mục tiêu có cho phép chuyển tu không
        if (!targetCp.canTransition()) {
            return TransitionResult.fail("§cĐiển tịch này là bí truyền cấm kỵ, không thể tùy ý chuyển tu!");
        }

        // Bắt buộc phải có tàn quyển hoặc đã lĩnh hội ít nhất chương 1 của công pháp mục tiêu
        CongPhapProgress targetProg = data.getCongPhapProgress(targetId);
        if (targetProg == null || (!targetProg.hasCollected(1) && !targetProg.hasLearned(1))) {
            return TransitionResult.fail("§cNgươi chưa sở hữu khẩu quyết chương 1 của " + targetCp.getDisplayName() + "!");
        }

        // Kiểm tra Hộ Mạch Đan nếu công pháp yêu cầu nghiêm ngặt
        if (targetCp.isRequiresHuMaiDan() && !hasHuMaiDan) {
            return TransitionResult.fail("§cKinh mạch chưa được bảo hộ! Công pháp này hung hiểm, bắt buộc phải dùng Hộ Mạch Đan để hộ tống đan điền.");
        }

        // Tính toán hệ số tổn thất và thời gian thích nghi
        double penaltyRatio = calculatePenaltyRatio(oldCp, targetCp, hasHuMaiDan);

        // Tính tổn thất CHỈ TRÊN TIẾN ĐỘ TIỂU CẢNH GIỚI HIỆN TẠI (Tuyệt đối không tụt đại cảnh giới)
        int tuViPerTang = (realm != null && realm.getTuViMoiTang() > 0) ? realm.getTuViMoiTang() : 100;
        int stageTuVi = currentTuVi % tuViPerTang;
        if (stageTuVi == 0 && currentTuVi > 0) {
            stageTuVi = tuViPerTang;
        }
        int tuViLost = (int) Math.round(stageTuVi * penaltyRatio);

        // Tính thời gian thích nghi theo phẩm cấp công pháp mục tiêu
        long durationMinutes = calculateAdaptationMinutes(oldCp, targetCp, hasHuMaiDan);
        long endTimestamp = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);

        // Cập nhật dữ liệu tu luyện
        data.setActivePrimary(targetId);
        // Tự động gỡ khỏi phụ tu nếu đang phụ tu công pháp này
        data.removeAuxiliary(targetId);

        // Đảm bảo chương 1 được ghi nhận
        if (!targetProg.hasLearned(1)) {
            targetProg.addLearnedChapter(1);
        }

        // Thiết lập Adaptation
        AdaptationData adapt = new AdaptationData(true, endTimestamp, oldCp != null ? oldCp.getId() : "NONE", penaltyRatio);
        data.setAdaptation(adapt);

        String msg = "§a§l[Chuyển Tu Thành Công] §fĐã chuyển sang chủ tu: " + targetCp.getDisplayName()
                + "§f! Thời gian hòa hợp kinh mạch: §e" + durationMinutes + " phút§f.";

        return TransitionResult.success(msg, tuViLost, durationMinutes);
    }
}
