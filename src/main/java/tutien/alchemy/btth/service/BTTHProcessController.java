package tutien.alchemy.btth.service;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tutien.alchemy.btth.model.*;
import tutien.alchemy.escrow.AlchemyEscrowManager;
import tutien.alchemy.escrow.EscrowTransaction;
import tutien.core.TuTienPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bộ điều phối quy trình Luyện Đan 7 giai đoạn theo chuẩn Đấu Phá Thương Khung:
 * Bảo vệ nguyên liệu bằng Escrow Transaction chống dup và crash.
 */
public class BTTHProcessController {

    public static class ProcessResult {
        private final boolean success;
        private final String message;
        private final BTTHPillQuality quality;
        private final ItemStack pillItem;

        public ProcessResult(boolean success, String message, BTTHPillQuality quality, ItemStack pillItem) {
            this.success = success;
            this.message = message;
            this.quality = quality;
            this.pillItem = pillItem;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BTTHPillQuality getQuality() { return quality; }
        public ItemStack getPillItem() { return pillItem; }

        public static ProcessResult ok(String msg) { return new ProcessResult(true, msg, null, null); }
        public static ProcessResult fail(String msg) { return new ProcessResult(false, msg, null, null); }
        public static ProcessResult complete(String msg, BTTHPillQuality quality, ItemStack item) {
            return new ProcessResult(true, msg, quality, item);
        }
    }

    private final TuTienPlugin plugin;
    private final SoulControlService soulControlService;
    private final AlchemistRankService rankService;
    private final AlchemyFlameService flameService;
    private final AlchemyCauldronService cauldronService;
    private final BTTHFormulaManager formulaManager;
    private final AlchemyEscrowManager escrowManager;

    private final Map<UUID, BTTHAlchemySession> activeSessions = new ConcurrentHashMap<>();

    public BTTHProcessController(TuTienPlugin plugin, SoulControlService soulControlService,
                                 AlchemistRankService rankService, AlchemyFlameService flameService,
                                 AlchemyCauldronService cauldronService, BTTHFormulaManager formulaManager,
                                 AlchemyEscrowManager escrowManager) {
        this.plugin = plugin;
        this.soulControlService = soulControlService;
        this.rankService = rankService;
        this.flameService = flameService;
        this.cauldronService = cauldronService;
        this.formulaManager = formulaManager;
        this.escrowManager = escrowManager;
    }

    public BTTHAlchemySession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public boolean isAlchemizing(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    /**
     * Bắt đầu phiên luyện đan mới:
     * 1. Kiểm tra Hỏa Linh Căn
     * 2. Kiểm tra Phẩm Cấp Luyện Dược Sư
     * 3. Kiểm tra Linh Hồn Lực
     * 4. Kiểm tra Dược Đỉnh
     * 5. Khóa ký quỹ Escrow
     */
    public synchronized ProcessResult startSession(Player player, String formulaId) {
        if (player == null) return ProcessResult.fail("Người chơi không hợp lệ.");
        UUID uuid = player.getUniqueId();
        if (activeSessions.containsKey(uuid)) {
            return ProcessResult.fail("Ngươi đang trong một phiên luyện đan khác!");
        }

        // 1. Kiểm tra Hỏa Linh Căn
        if (soulControlService != null && !soulControlService.hasFireAffinity(player)) {
            return ProcessResult.fail("Ngươi không sở hữu Hỏa Linh Căn, không thể ngưng tụ hỏa diễm luyện đan!");
        }

        BTTHFormula formula = formulaManager.getFormula(formulaId);
        if (formula == null) return ProcessResult.fail("Đan phương không tồn tại!");

        // 2. Kiểm tra Cấp Luyện Dược Sư
        AlchemistRank playerRank = rankService.getRank(player);
        if (playerRank.getTier() < formula.getRequiredRank().getTier()) {
            return ProcessResult.fail("Phẩm cấp Luyện Dược Sư không đủ! Yêu cầu: " + formula.getRequiredRank().getDisplayName());
        }

        // 3. Kiểm tra Linh Hồn Lực
        PlayerSoulData soul = soulControlService.getSoulData(player);
        if (soul.getSoulPower() < formula.getRequiredSoulPower()) {
            return ProcessResult.fail("Linh hồn lực quá yếu (cần " + formula.getRequiredSoulPower() + " SoulPower), không thể áp chế dược tính!");
        }
        if (!soul.getRealm().canCraftTier(formula.getTier())) {
            return ProcessResult.fail("Cảnh giới linh hồn chưa đạt yêu cầu (" + formula.getRequiredSoulRealm().getDisplayName() + ")!");
        }

        // 4. Kiểm tra Dược Đỉnh
        BTTHCauldron cauldron = cauldronService.getActiveCauldron(player);
        if (cauldron == null || cauldron.isBroken()) {
            return ProcessResult.fail("Dược đỉnh của ngươi đã hư hại hoàn toàn, cần sửa chữa trước khi luyện đan!");
        }
        if (cauldron.getTier() < formula.getCauldronTierMin()) {
            return ProcessResult.fail("Dược đỉnh phẩm cấp quá thấp không chịu nổi hỏa lực của đan phương này!");
        }

        // 5. Kiểm tra Hỏa Diễm
        BTTHFlame flame = flameService.getActiveFlame(player);
        if (flame == null) {
            return ProcessResult.fail("Chưa kích hoạt ngọn lửa nào!");
        }

        // Khởi tạo phiên
        BTTHAlchemySession session = new BTTHAlchemySession(player, formula, flame, cauldron);
        session.setCurrentStage(BTTHAlchemyStage.HEATING);
        activeSessions.put(uuid, session);

        player.sendMessage("§e§l[Luyện Đan] §fĐã bắt đầu luyện chế §b" + formula.getDisplayName() + "§f! Hãy điều chỉnh nhiệt độ dược đỉnh.");
        return ProcessResult.ok("Bắt đầu luyện đan thành công.");
    }

    /**
     * Thao hỏa điều ôn: Tăng / giảm nhiệt độ dược đỉnh.
     */
    public ProcessResult adjustHeat(Player player, double delta) {
        BTTHAlchemySession session = activeSessions.get(player.getUniqueId());
        if (session == null) return ProcessResult.fail("Không có phiên luyện đan nào đang diễn ra.");

        // Tiêu hao linh hồn lực khi khống hỏa
        double soulCost = Math.abs(delta) * 0.1;
        if (!soulControlService.consumeSoul(player, soulCost)) {
            return failSession(player, "Linh hồn lực cạn kiệt, mất quyền kiểm soát hỏa diễm! Lò thuốc nổ tung!");
        }

        session.addHeat(delta);

        // Kiểm tra quá nhiệt làm nổ lò
        if (session.getCurrentHeat() > session.getCauldron().getMaxHeat() + 60.0) {
            cauldronService.reduceDurability(player, 25);
            return failSession(player, "Nhiệt độ vượt quá cực hạn chịu đựng của Dược Đỉnh! Dược đỉnh phát nổ dữ dội!");
        }

        return ProcessResult.ok("Nhiệt độ hiện tại: §c" + Math.round(session.getCurrentHeat()) + "°C");
    }

    /**
     * Tinh luyện dược liệu thành dược dịch / dược phấn.
     */
    public ProcessResult extractIngredient(Player player, String ingredientId) {
        BTTHAlchemySession session = activeSessions.get(player.getUniqueId());
        if (session == null) return ProcessResult.fail("Không có phiên luyện đan.");

        BTTHIngredient ing = formulaManager.getIngredient(ingredientId);
        if (ing == null) return ProcessResult.fail("Dược liệu không hợp lệ.");

        double curHeat = session.getCurrentHeat();

        // Kiểm tra nhiệt độ quá cao làm cháy dược thảo
        if (curHeat > ing.getOptimalHeatMax() + 40.0) {
            return failSession(player, "Nhiệt độ quá cao thiêu rụi hoàn toàn dược tính của " + ing.getDisplayName() + " thành tro tàn!");
        }

        // Kiểm tra nhiệt độ quá thấp không đủ chiết xuất
        if (curHeat < ing.getOptimalHeatMin() - 30.0) {
            return ProcessResult.fail("Nhiệt độ quá thấp, không thể làm tan chảy và chiết xuất dược tính.");
        }

        // Tính độ tinh thuần chiết xuất
        double heatDiff = Math.abs(curHeat - ((ing.getOptimalHeatMin() + ing.getOptimalHeatMax()) / 2.0));
        double heatAccuracy = Math.max(0.60, 1.0 - (heatDiff / 100.0));
        PlayerSoulData soul = soulControlService.getSoulData(player);
        double woodBonus = soulControlService.getWoodBonusMultiplier(player);

        double extractedPurity = heatAccuracy * (0.5 + soul.getSoulControl() * 0.5) * woodBonus;
        session.recordExtracted(ingredientId, extractedPurity);

        player.sendMessage("§a§l[Tinh Luyện] §fĐã chiết xuất thành công §e" + ing.getDisplayName() + " §8(Độ tinh thuần: §b" + Math.round(extractedPurity * 100) + "%§8)");
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1.2f);

        // Kiểm tra nếu tất cả nguyên liệu đã được tinh luyện
        if (session.getExtractedIngredients().size() >= session.getFormula().getIngredients().size()) {
            session.setCurrentStage(BTTHAlchemyStage.FUSION);
            player.sendMessage("§6§l[Dung Hợp] §fToàn bộ dược dịch đã sẵn sàng! Hãy tiến hành dung hợp theo đúng thứ tự dược tính.");
        }

        return ProcessResult.ok("Chiết xuất thành công " + ing.getDisplayName());
    }

    /**
     * Dung hợp dược dịch theo đúng thứ tự đan phương quy định.
     */
    public ProcessResult fuseLiquids(Player player, List<String> submittedOrder) {
        BTTHAlchemySession session = activeSessions.get(player.getUniqueId());
        if (session == null) return ProcessResult.fail("Không có phiên luyện đan.");

        List<String> requiredOrder = session.getFormula().getFusionOrder();
        if (!requiredOrder.isEmpty() && submittedOrder != null) {
            // Kiểm tra thứ tự dung hợp
            if (!requiredOrder.equals(submittedOrder)) {
                // Thứ tự sai -> Xung đột dược tính
                session.addOverallPurity(-0.25);
                player.sendMessage("§c§l[Cảnh Báo] §fDược tính xung đột dữ dội do đưa dược dịch sai thứ tự! Độ tinh thuần giảm sút!");
            } else {
                session.addOverallPurity(0.10);
                player.sendMessage("§a§l[Hoàn Hảo] §fDung hợp dược dịch hoàn hảo theo đúng đan phương!");
            }
        }

        session.setCurrentStage(BTTHAlchemyStage.CONDENSATION);
        player.sendMessage("§e§l[Ngưng Đan] §fBắt đầu ngưng kết đan phôi! Cần giữ nhiệt độ ổn định.");
        return ProcessResult.ok("Dung hợp hoàn tất.");
    }

    /**
     * Chuyển đổi hỏa diễm (Flame Switching - Dành cho đan phương nhiều văn như Tam Văn Thanh Linh Đan).
     */
    public ProcessResult switchFlame(Player player, String newFlameId) {
        BTTHAlchemySession session = activeSessions.get(player.getUniqueId());
        if (session == null) return ProcessResult.fail("Không có phiên luyện đan.");

        if (!flameService.hasUnlockedFlame(player, newFlameId)) {
            return ProcessResult.fail("Ngươi chưa nắm giữ ngọn lửa này!");
        }

        BTTHFlame newFlame = flameService.getFlame(newFlameId);
        if (newFlame == null) return ProcessResult.fail("Hỏa diễm không tồn tại.");

        session.setCurrentFlame(newFlame);
        player.sendMessage("§d§l[Dị Hỏa Biến Đổi] §fĐã chuyển sang ngọn lửa §e" + newFlame.getDisplayName() + "§f! Văn khí đan dược bắt đầu ngưng tụ.");
        return ProcessResult.ok("Chuyển hỏa diễm thành công.");
    }

    /**
     * Thu Đan - Quyết định thành bại và phẩm chất cuối cùng.
     */
    public ProcessResult collectPill(Player player) {
        BTTHAlchemySession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return ProcessResult.fail("Không có phiên luyện đan để thu đan.");

        session.setFinished(true);

        double woodBonus = soulControlService.getWoodBonusMultiplier(player);
        BTTHPillQuality quality = BTTHResultEvaluator.evaluate(session, woodBonus);
        ItemStack pillItem = BTTHResultEvaluator.createPillItem(session.getFormula(), quality);

        // Ghi nhận thành tích luyện đan phẩm chất cao & cấp EXP
        rankService.recordSuccessfulCraft(player, session.getFormula().getTier(), quality.getGrade());

        // Giảm độ bền dược đỉnh
        cauldronService.reduceDurability(player, 2);

        // Trao vật phẩm cho người chơi
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(pillItem);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        player.sendMessage("§6§l[Thu Đan Thành Công] §fNgươi đã luyện thành: " + quality.getFormattedDisplay(session.getFormula().getDisplayName()));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);

        return ProcessResult.complete("Thu đan thành công!", quality, pillItem);
    }

    public ProcessResult failSession(Player player, String reason) {
        BTTHAlchemySession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.setFinished(true);
            session.setFailureReason(reason);
        }
        player.sendMessage("§c§l[Luyện Đan Thất Bại] §f" + reason);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
        return ProcessResult.fail(reason);
    }
}
