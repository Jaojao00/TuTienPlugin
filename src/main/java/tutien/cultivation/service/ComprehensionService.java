package tutien.cultivation.service;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.manager.CongPhapManager;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.element.LinhCanData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dịch vụ Tham Ngộ Lĩnh Hội Công Pháp (Phase 2.3):
 *
 * 1. Công thức tích lũy tiến độ:
 *    Delta P = B * Delta t * N * Phi * D
 *    - B: Tốc độ cơ sở (0.5% mỗi phút).
 *    - Delta t: Thời gian thực tế tham ngộ (giới hạn an toàn chống AFK/offline).
 *    - N: Hệ số Ngộ Tính = Clamp(0.5, 2.0, 1.0 + (NgoTinh - 10.0) / 40.0).
 *    - Phi: Hệ số tương thích nguyên tố từ ElementCompatibilityService.
 *    - D: Hệ số độ khó của tầng = Clamp(0.25, 1.0, 1.0 - (chapter - 1) * 0.05).
 *
 * 2. Tiêu hao Điểm Đạo Ngộ (DaoNgoPoints) để đẩy nhanh tham ngộ.
 * 3. Tự động đột phá và ghi nhận khẩu quyết khi tiến độ đạt 100%.
 */
public class ComprehensionService {

    public static final double BASE_RATE_PER_MINUTE = 0.50; // 0.5% mỗi phút
    public static final double MIN_INTELLIGENCE_FACTOR = 0.50;
    public static final double MAX_INTELLIGENCE_FACTOR = 2.00;
    public static final double MIN_DIFFICULTY_FACTOR = 0.25;
    public static final double MAX_DIFFICULTY_FACTOR = 1.00;

    private final CongPhapManager congPhapManager;
    private final ElementCompatibilityService compatibilityService;
    private final CongPhapValidationService validationService;

    // Lưu mốc thời gian tick trước đó để tính Delta t chính xác theo đồng hồ máy chủ
    private final Map<UUID, Long> lastComprehendTickMap = new ConcurrentHashMap<>();

    public ComprehensionService(CongPhapManager congPhapManager) {
        this.congPhapManager = congPhapManager;
        this.compatibilityService = new ElementCompatibilityService(
                congPhapManager != null ? congPhapManager.getRelationService() : null);
        this.validationService = new CongPhapValidationService();
    }

    public ElementCompatibilityService getCompatibilityService() {
        return compatibilityService;
    }

    /**
     * Tính toán lượng tiến độ tăng thêm trong một khoảng thời gian delta t (tính theo phút).
     */
    public double calculateDeltaProgress(double deltaMinutes, double ngoTinh, double phi, int chapter) {
        // Giới hạn deltaMinutes tối đa 2.0 phút để tránh tích dồn khi lag/AFK
        double clampedMinutes = Math.max(0.0, Math.min(2.0, deltaMinutes));

        // N: Hệ số Ngộ Tính
        double n = Math.max(MIN_INTELLIGENCE_FACTOR, Math.min(MAX_INTELLIGENCE_FACTOR, 1.0 + ((ngoTinh - 10.0) / 40.0)));

        // D: Độ khó của tầng
        double d = Math.max(MIN_DIFFICULTY_FACTOR, Math.min(MAX_DIFFICULTY_FACTOR, 1.0 - ((chapter - 1) * 0.05)));

        return BASE_RATE_PER_MINUTE * clampedMinutes * n * phi * d;
    }

    /**
     * Xử lý một chu kỳ tham ngộ thực tế khi tu sĩ ở chế độ Tọa Thiền Tham Ngộ.
     *
     * @return Tiến độ phần trăm được cộng thêm (0.0 nếu không đủ điều kiện)
     */
    public double processComprehensionTick(Player player, PlayerDataManager dataManager, String congPhapId) {
        return studyTick(player, dataManager, congPhapId);
    }

    /**
     * Tương thích API: Xử lý chu kỳ tham ngộ từng giây/tick (studyTick).
     */
    public double studyTick(Player player, PlayerDataManager dataManager, String congPhapId) {
        if (player == null || dataManager == null || congPhapId == null) return 0.0;

        CongPhap cp = congPhapManager.getCongPhap(congPhapId);
        if (cp == null) return 0.0;

        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        CongPhapProgress prog = data.getOrCreateProgress(congPhapId);
        int targetChapter = prog.getTargetChapter();

        // Kiểm tra xem đã học hết toàn bộ các chương chưa
        if (targetChapter > cp.getTotalChapters()) {
            return 0.0;
        }

        CanhGioi realm = dataManager.getCanhGioi(player);
        CongPhapValidationService.ValidationResult valRes = validationService.validateLearnChapter(cp, prog, targetChapter, realm);
        if (!valRes.isSuccess()) {
            return 0.0;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastTick = lastComprehendTickMap.getOrDefault(uuid, now - 1000L);
        lastComprehendTickMap.put(uuid, now);

        double elapsedMinutes = (now - lastTick) / 60000.0;
        if (elapsedMinutes <= 0.0) return 0.0;

        LinhCanData linhCan = dataManager.getLinhCanData(player);
        double ngoTinh = dataManager.getNgoTinh(player);
        double phi = compatibilityService.calculateCompatibility(linhCan, cp);

        double deltaP = calculateDeltaProgress(elapsedMinutes, ngoTinh, phi, targetChapter);
        prog.addComprehensionProgress(deltaP);

        // Đạt 100% -> Lĩnh hội thành công chương đó!
        if (prog.getComprehensionProgress() >= 100.0) {
            completeChapter(player, cp, prog, targetChapter);
        }

        return deltaP;
    }

    /**
     * Hoàn tất tham ngộ một chương công pháp, thăng tiến tầng mục tiêu và phát hiệu ứng.
     */
    public void completeChapter(Player player, CongPhap cp, CongPhapProgress prog, int chapter) {
        if (prog == null || cp == null) return;
        prog.addLearnedChapter(chapter);
        prog.setComprehensionProgress(0.0);
        if (player != null) {
            player.sendMessage("§b§l✦ THẤU TRIỆT KHẨU QUYẾT ✦ §fNgươi đã hoàn tất tham ngộ: "
                    + cp.getDisplayName() + " §e§l[Tầng " + chapter + "]§f!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }
    }

    /**
     * Đặt lại phiên tham ngộ khi người chơi đổi chế độ hoặc ngắt kết nối.
     */
    public void resetStudySession(UUID uuid) {
        if (uuid != null) {
            lastComprehendTickMap.remove(uuid);
        }
    }

    /**
     * Tiêu hao Điểm Đạo Ngộ (DaoNgoPoints) để đẩy nhanh tiến độ tham ngộ chương mục tiêu.
     *
     * @param points Số điểm Đạo Ngộ muốn tiêu hao
     * @return true nếu thành công
     */
    public boolean accelerateWithDaoNgoPoints(Player player, PlayerDataManager dataManager, String congPhapId, long points) {
        if (player == null || dataManager == null || congPhapId == null || points <= 0) return false;

        CongPhap cp = congPhapManager.getCongPhap(congPhapId);
        if (cp == null) {
            player.sendMessage("§cCông pháp không tồn tại!");
            return false;
        }

        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        CongPhapProgress prog = data.getOrCreateProgress(congPhapId);
        int targetChapter = prog.getTargetChapter();

        if (targetChapter > cp.getTotalChapters()) {
            player.sendMessage("§aCông pháp này đã lĩnh hội đến cảnh giới viên mãn!");
            return false;
        }

        CanhGioi realm = dataManager.getCanhGioi(player);
        CongPhapValidationService.ValidationResult valRes = validationService.validateLearnChapter(cp, prog, targetChapter, realm);
        if (!valRes.isSuccess()) {
            player.sendMessage(valRes.getMessage());
            return false;
        }

        if (!dataManager.hasDaoNgoPoints(player, points)) {
            player.sendMessage("§cKhông đủ Điểm Đạo Ngộ tiêu hao! Hiện có: §e" + dataManager.getDaoNgoPoints(player) + " điểm.");
            return false;
        }

        dataManager.takeDaoNgoPoints(player, points);

        LinhCanData linhCan = dataManager.getLinhCanData(player);
        double phi = compatibilityService.calculateCompatibility(linhCan, cp);

        // Mỗi điểm Đạo Ngộ cộng 1.0% * Phi tiến độ
        double progressGain = points * 1.0 * phi;
        prog.addComprehensionProgress(progressGain);

        player.sendMessage("§e[Đạo Ngộ Thấu Triệt] §fĐã tiêu hao §b" + points + " Đạo Ngộ Điểm§f, tiến độ tham ngộ tăng: §a+"
                + String.format("%.1f%%", progressGain) + " §7(Hiện tại: " + String.format("%.1f%%", prog.getComprehensionProgress()) + ")");

        if (prog.getComprehensionProgress() >= 100.0) {
            prog.addLearnedChapter(targetChapter);
            prog.setComprehensionProgress(0.0);
            player.sendMessage("§b§l✦ THẤU TRIỆT KHẨU QUYẾT ✦ §fNgươi đã hoàn tất tham ngộ: "
                    + cp.getDisplayName() + " §e§l[Tầng " + targetChapter + "]§f!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }

        return true;
    }

    public void clearComprehensionClock(UUID uuid) {
        lastComprehendTickMap.remove(uuid);
    }
}
