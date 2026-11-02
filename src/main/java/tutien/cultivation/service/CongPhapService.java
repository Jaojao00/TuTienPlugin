package tutien.cultivation.service;

import org.bukkit.entity.Player;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.manager.CongPhapManager;
import tutien.cultivation.model.AdaptationData;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.element.LinhCanData;

import java.util.*;

/**
 * Service điều phối nghiệp vụ công pháp:
 * Thu thập tàn quyển, lĩnh hội khẩu quyết, kích hoạt chủ tu / phụ tu,
 * và tính toán các chỉ số vận hành thực tế.
 */
public class CongPhapService {

    private final CongPhapManager congPhapManager;
    private final CongPhapValidationService validationService;
    private final CongPhapTransitionService transitionService;

    public CongPhapService(CongPhapManager congPhapManager) {
        this.congPhapManager = congPhapManager;
        this.validationService = new CongPhapValidationService();
        this.transitionService = new CongPhapTransitionService(congPhapManager != null ? congPhapManager.getRelationService() : null);
    }

    public CongPhapManager getCongPhapManager() {
        return congPhapManager;
    }

    public CongPhapValidationService getValidationService() {
        return validationService;
    }

    public CongPhapTransitionService getTransitionService() {
        return transitionService;
    }

    /**
     * Thu thập một chương tàn quyển của công pháp.
     */
    public boolean collectChapter(Player player, PlayerDataManager dataManager, String congPhapId, int chapter) {
        if (player == null || dataManager == null || congPhapId == null || chapter <= 0) return false;
        CongPhap cp = congPhapManager.getCongPhap(congPhapId);
        if (cp == null || chapter > cp.getTotalChapters()) return false;

        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        CongPhapProgress prog = data.getOrCreateProgress(congPhapId);
        boolean added = prog.addCollectedChapter(chapter);
        if (added) {
            player.sendMessage("§a§l✦ [Bí Tịch Cổ Điển] §fNgươi đã thu thập được: " + cp.getDisplayName()
                    + " §e§l[Tầng " + chapter + "]§f!");
        }
        return added;
    }

    /**
     * Lĩnh hội chính thức một chương của công pháp.
     */
    public boolean learnChapter(Player player, PlayerDataManager dataManager, String congPhapId, int chapter) {
        if (player == null || dataManager == null || congPhapId == null || chapter <= 0) return false;
        CongPhap cp = congPhapManager.getCongPhap(congPhapId);
        if (cp == null) {
            player.sendMessage("§cCông pháp không tồn tại!");
            return false;
        }

        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        CongPhapProgress prog = data.getOrCreateProgress(congPhapId);
        CanhGioi realm = dataManager.getCanhGioi(player);

        CongPhapValidationService.ValidationResult result = validationService.validateLearnChapter(cp, prog, chapter, realm, data);
        if (!result.isSuccess()) {
            player.sendMessage(result.getMessage());
            return false;
        }

        prog.addLearnedChapter(chapter);
        player.sendMessage("§b§l✦ LĨNH HỘI THÀNH CÔNG ✦ §fNgươi đã thấu triệt " + cp.getDisplayName()
                + " §e§l[Tầng " + chapter + "]§f!");

        // Thông báo thần thông mới nếu có
        List<String> unlockedNow = new ArrayList<>();
        for (CongPhap.AbilityUnlock au : cp.getAbilities()) {
            if (au.getUnlockChapter() == chapter) {
                unlockedNow.add(au.getId());
            }
        }
        if (!unlockedNow.isEmpty()) {
            player.sendMessage("§d§l[Thần Thông Khai Mở] §fĐã giác ngộ bí thuật: §e" + String.join(", ", unlockedNow));
        }

        return true;
    }

    /**
     * Thiết lập công pháp chủ tu (nếu là công pháp mới, gọi qua TransitionService).
     */
    public boolean setPrimaryCongPhap(Player player, PlayerDataManager dataManager, String targetCongPhapId, boolean hasHuMaiDan) {
        if (player == null || dataManager == null || targetCongPhapId == null) return false;
        CongPhap targetCp = congPhapManager.getCongPhap(targetCongPhapId);
        if (targetCp == null) {
            player.sendMessage("§cCông pháp không tồn tại!");
            return false;
        }

        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        String currentPrimaryId = data.getActivePrimary();
        CongPhap currentCp = currentPrimaryId != null ? congPhapManager.getCongPhap(currentPrimaryId) : null;

        // Nếu chuyển sang công pháp mới
        int currentTuVi = dataManager.getTuVi(player);
        CanhGioi realm = dataManager.getCanhGioi(player);

        CongPhapTransitionService.TransitionResult res = transitionService.transition(
                data, currentCp, targetCp, currentTuVi, realm, hasHuMaiDan);

        if (!res.isSuccess()) {
            player.sendMessage(res.getMessage());
            return false;
        }

        // Cập nhật Tu Vi nếu có tổn thất
        if (res.getTuViLost() > 0) {
            dataManager.setTuVi(player, Math.max(0, currentTuVi - res.getTuViLost()));
            player.sendMessage("§7Kinh mạch chuyển thế hao tổn: §c-" + res.getTuViLost() + " Tu Vi §7(tiểu cảnh giới).");
        }

        player.sendMessage(res.getMessage());
        return true;
    }

    /**
     * Kích hoạt một công pháp phụ tu.
     */
    public boolean addAuxiliaryCongPhap(Player player, PlayerDataManager dataManager, String congPhapId) {
        if (player == null || dataManager == null || congPhapId == null) return false;
        CongPhap cp = congPhapManager.getCongPhap(congPhapId);
        if (cp == null) {
            player.sendMessage("§cCông pháp không tồn tại!");
            return false;
        }

        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        CongPhapProgress prog = data.getCongPhapProgress(congPhapId);
        if (prog == null || prog.getLearnedChapters().isEmpty()) {
            player.sendMessage("§cNgươi chưa lĩnh hội được tầng nào của công pháp này!");
            return false;
        }

        boolean ok = data.addAuxiliary(congPhapId, PlayerCultivationData.MAX_AUXILIARY_DEFAULT);
        if (!ok) {
            player.sendMessage("§cKhông thể phụ tu thêm! Đã đạt giới hạn tối đa hoặc đang là chủ tu.");
            return false;
        }

        player.sendMessage("§aĐã kích hoạt phụ tu: " + cp.getDisplayName());
        return true;
    }

    /**
     * Tính toán tốc độ hấp thu linh khí thực tế của người chơi từ công pháp chủ tu và phụ tu.
     */
    public double getEffectiveAbsorptionRate(Player player, PlayerDataManager dataManager) {
        if (player == null || dataManager == null) return 1.0;
        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        double rate = 1.0;

        // Chủ tu
        if (data.getActivePrimary() != null) {
            CongPhap primary = congPhapManager.getCongPhap(data.getActivePrimary());
            if (primary != null) {
                rate = primary.getAbsorptionRate();
            }
        }

        // Phụ tu (đóng góp tối đa 15% hiệu quả mỗi bộ, chỉ tính phần thặng dư > 1.0)
        for (String auxId : data.getActiveAuxiliary()) {
            CongPhap aux = congPhapManager.getCongPhap(auxId);
            if (aux != null && aux.getAbsorptionRate() > 1.0) {
                rate += (aux.getAbsorptionRate() - 1.0) * 0.15;
            }
        }

        // Phạt thích nghi nếu đang trong giai đoạn hòa hợp kinh mạch
        AdaptationData adapt = data.getAdaptation();
        if (adapt.isActive()) {
            rate *= (1.0 - (adapt.getPenaltyRatio() * 0.50));
        }

        // Giới hạn hợp lý tránh lạm phát số [0.40 -> 2.50]
        return Math.max(0.40, Math.min(2.50, rate));
    }

    /**
     * Lấy toàn bộ danh sách ID thần thông / bí thuật đã mở khóa của người chơi.
     */
    public Set<String> getUnlockedAbilities(Player player, PlayerDataManager dataManager) {
        Set<String> abilities = new LinkedHashSet<>();
        if (player == null || dataManager == null) return abilities;
        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);

        // Duyệt qua tất cả công pháp đã học
        for (Map.Entry<String, CongPhapProgress> entry : data.getLearnedTechniques().entrySet()) {
            String cpId = entry.getKey();
            CongPhapProgress prog = entry.getValue();
            CongPhap cp = congPhapManager.getCongPhap(cpId);
            if (cp != null) {
                for (int chapter : prog.getLearnedChapters()) {
                    abilities.addAll(cp.getAbilitiesUnlockedAt(chapter));
                }
            }
        }
        return abilities;
    }
}
