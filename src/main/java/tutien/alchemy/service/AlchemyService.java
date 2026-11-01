package tutien.alchemy.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.alchemy.escrow.AlchemyEscrowManager;
import tutien.alchemy.escrow.EscrowTransaction;
import tutien.alchemy.manager.AlchemyManager;
import tutien.alchemy.manager.DanDuocManager;
import tutien.alchemy.model.*;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Dịch vụ xử lý mini-game Luyện Đan, Hỏa Hầu và Ký Quỹ Escrow nguyên liệu.
 * Đảm bảo Zero-Loss, Zero-Duplication và tính toán phẩm chất chính xác.
 */
public class AlchemyService {

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final AlchemyManager alchemyManager;
    private final DanDuocManager danDuocManager;
    private final AlchemyEscrowManager escrowManager;
    private final Logger logger;

    private final Map<UUID, AlchemySession> activeSessions = new ConcurrentHashMap<>();

    public AlchemyService(TuTienPlugin plugin, PlayerDataManager dataManager,
                          AlchemyManager alchemyManager, DanDuocManager danDuocManager) {
        this(plugin, dataManager, alchemyManager, danDuocManager,
                plugin != null && plugin.getAlchemyEscrowManager() != null
                        ? plugin.getAlchemyEscrowManager()
                        : new AlchemyEscrowManager(plugin));
    }

    public AlchemyService(TuTienPlugin plugin, PlayerDataManager dataManager,
                          AlchemyManager alchemyManager, DanDuocManager danDuocManager,
                          AlchemyEscrowManager escrowManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.alchemyManager = alchemyManager;
        this.danDuocManager = danDuocManager;
        this.escrowManager = escrowManager != null ? escrowManager : new AlchemyEscrowManager(plugin);
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("AlchemyService");
    }

    public AlchemySession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }

    public boolean isAlchemizing(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public AlchemyEscrowManager getEscrowManager() {
        return escrowManager;
    }

    /**
     * Bắt đầu một phiên luyện đan mới.
     * Sử dụng máy trạng thái WAL: PREPARED -> Trừ đồ -> ACTIVE bền vững trên đĩa.
     */
    public synchronized AlchemyResult startSession(Player player, String recipeId) {
        if (player == null) return AlchemyResult.error("Người chơi không hợp lệ.");

        UUID uuid = player.getUniqueId();
        if (activeSessions.containsKey(uuid)) {
            return AlchemyResult.error("Ngươi đang trong một phiên luyện đan khác!");
        }

        AlchemyRecipe recipe = alchemyManager.getRecipe(recipeId);
        if (recipe == null) {
            return AlchemyResult.error("Công thức luyện đan không tồn tại: " + recipeId);
        }

        // 1. Kiểm tra cảnh giới
        CanhGioi currentRealm = dataManager.getCanhGioi(player);
        if (currentRealm.ordinal() < recipe.getMinRealm().ordinal()) {
            return AlchemyResult.error("Cảnh giới chưa đủ! Yêu cầu tối thiểu: " + recipe.getMinRealm().getTenHienThi());
        }

        // 2. Kiểm tra nguyên liệu trong túi đồ
        for (AlchemyRecipe.Ingredient ing : recipe.getIngredients()) {
            int has = countMaterial(player, ing.getMaterial());
            if (has < ing.getAmount()) {
                return AlchemyResult.error("Thiếu nguyên liệu: " + ing.getVietnameseName() + " (" + has + "/" + ing.getAmount() + ")");
            }
        }

        // 3. Xây dựng Kế hoạch Trừ Nguyên Liệu (Debit Plan) từ chính các ItemStack thực tế trong Inventory
        ItemStack[] beforeContents = null;
        try {
            beforeContents = player.getInventory().getStorageContents();
            if (beforeContents == null) beforeContents = player.getInventory().getContents();
        } catch (Throwable ignored) {}
        String beforeHash = AlchemyEscrowManager.computeInventoryFingerprint(beforeContents);

        ItemStack[] simulatedClone = AlchemyEscrowManager.cloneContents(beforeContents);
        List<DebitPlanEntry> debitPlan = new ArrayList<>();
        List<ItemStack> plannedLockedItems = new ArrayList<>();

        for (AlchemyRecipe.Ingredient ing : recipe.getIngredients()) {
            int remainingNeeded = ing.getAmount();
            for (int slot = 0; slot < simulatedClone.length; slot++) {
                ItemStack is = simulatedClone[slot];
                if (is != null && is.getType() == ing.getMaterial() && ItemKeys.isValidRawMaterial(is)) {
                    int take = Math.min(is.getAmount(), remainingNeeded);
                    ItemStack lockedClone = is.clone();
                    lockedClone.setAmount(take);

                    debitPlan.add(new DebitPlanEntry(slot, take, lockedClone));
                    plannedLockedItems.add(lockedClone);

                    if (is.getAmount() <= take) {
                        simulatedClone[slot] = null;
                    } else {
                        is.setAmount(is.getAmount() - take);
                    }
                    remainingNeeded -= take;
                    if (remainingNeeded <= 0) break;
                }
            }
            if (remainingNeeded > 0) {
                return AlchemyResult.error("Không đủ nguyên liệu hợp lệ để lập kế hoạch trừ đồ!");
            }
        }

        String expectedAfterHash = AlchemyEscrowManager.computeInventoryFingerprint(simulatedClone);

        // Ghi trước bản kê nguyên liệu THẬT xuống đĩa (State: PREPARED)
        // WAL lưu trọn vẹn 100% ItemMeta & PDC của từng ItemStack thực tế
        EscrowTransaction tx = escrowManager.prepareTransaction(uuid, recipeId, plannedLockedItems, beforeHash, expectedAfterHash);
        if (tx == null) {
            return AlchemyResult.error("Không thể ghi nhận giao dịch Ký Quỹ Escrow xuống đĩa! Hủy phiên để bảo vệ nguyên liệu.");
        }

        // 4. Kiểm tra đối soát trước khi trừ (Pre-debit Verification)
        // Đảm bảo toàn bộ các slot trong Debit Plan vẫn còn nguyên vẹn đúng vật phẩm và số lượng
        ItemStack[] currentStorage = null;
        try {
            currentStorage = player.getInventory().getStorageContents();
            if (currentStorage == null) currentStorage = player.getInventory().getContents();
        } catch (Throwable ignored) {}

        for (DebitPlanEntry entry : debitPlan) {
            ItemStack actual = null;
            try {
                if (player.getInventory() != null) {
                    actual = player.getInventory().getItem(entry.slot);
                }
            } catch (Throwable ignored) {}
            if (actual == null && currentStorage != null && entry.slot >= 0 && entry.slot < currentStorage.length) {
                actual = currentStorage[entry.slot];
            }
            if (actual == null || actual.getType() != entry.itemClone.getType() || actual.getAmount() < entry.amountToDebit) {
                logger.severe("[AlchemyService] Túi đồ người chơi bị dịch chuyển hoặc thiếu nguyên liệu trước khi trừ! Hủy phiên để bảo vệ nguyên liệu.");
                // Kiểm tra đối soát: nếu túi đồ vẫn còn nguyên (chưa hề trừ đồ), tuyệt đối không hoàn trả để chống nhân đôi!
                String currentHash = AlchemyEscrowManager.computeInventoryFingerprint(currentStorage);
                if (beforeHash != null && beforeHash.equals(currentHash)) {
                    escrowManager.discardPreparedTransaction(tx.getTransactionId(), "Hủy phiên an toàn: Chưa trừ đồ và túi đồ còn nguyên trạng.");
                } else {
                    tx.setState(tutien.alchemy.escrow.EscrowState.NEEDS_RECONCILIATION);
                    tx.setReconciliationNote("Túi đồ biến động bất thường trước khi thực thi trừ nguyên liệu.");
                    escrowManager.saveTransactionsToDisk();
                }
                return AlchemyResult.error("Vật phẩm trong túi đồ đã bị dịch chuyển hoặc không đủ! Đã hủy phiên an toàn.");
            }
        }

        // 5. Thực thi trừ nguyên liệu thực tế theo đúng Debit Plan đã ghi bền vững
        for (DebitPlanEntry entry : debitPlan) {
            try {
                if (player.getInventory() != null) {
                    ItemStack actual = null;
                    try {
                        actual = player.getInventory().getItem(entry.slot);
                    } catch (Throwable ignored) {}
                    if (actual == null && currentStorage != null && entry.slot >= 0 && entry.slot < currentStorage.length) {
                        actual = currentStorage[entry.slot];
                    }
                    if (actual != null && actual.getType() == entry.itemClone.getType()) {
                        if (actual.getAmount() <= entry.amountToDebit) {
                            try {
                                player.getInventory().setItem(entry.slot, null);
                            } catch (Throwable ignored) {}
                            if (currentStorage != null && entry.slot >= 0 && entry.slot < currentStorage.length) {
                                currentStorage[entry.slot] = null;
                            }
                        } else {
                            actual.setAmount(actual.getAmount() - entry.amountToDebit);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 6. Kiểm tra đối soát sau khi trừ (Post-debit Actual Fingerprint Verification)
        ItemStack[] actualAfterContents = null;
        try {
            actualAfterContents = player.getInventory().getStorageContents();
            if (actualAfterContents == null) actualAfterContents = player.getInventory().getContents();
        } catch (Throwable ignored) {}
        String actualAfterHash = AlchemyEscrowManager.computeInventoryFingerprint(actualAfterContents);

        if (expectedAfterHash != null && !expectedAfterHash.equals(actualAfterHash)) {
            logger.severe("[AlchemyService] Hash túi đồ sau khi trừ không khớp với dự kiến! Kích hoạt hoàn trả tức thì.");
            escrowManager.processRefund(player, tx.getTransactionId());
            return AlchemyResult.error("Lỗi đối soát trạng thái túi đồ sau khi trừ! Đã kích hoạt hoàn trả khẩn cấp.");
        }

        // 7. Lưu playerdata đồng bộ
        try {
            player.saveData();
        } catch (Throwable t) {
            logger.severe("[AlchemyService] Không thể lưu playerdata sau khi trừ đồ! Đang kích hoạt refund khẩn cấp.");
            escrowManager.processRefund(player, tx.getTransactionId());
            return AlchemyResult.error("Lỗi đồng bộ dữ liệu người chơi! Đã hoàn trả nguyên liệu an toàn.");
        }

        // 8. Xác nhận trừ đồ thành công -> Commit state: ACTIVE trên đĩa
        boolean committed = escrowManager.commitActive(tx.getTransactionId(), plannedLockedItems);
        if (!committed) {
            logger.severe("[AlchemyService] Không thể ghi ACTIVE xuống WAL! Dừng phiên và hoàn trả đồ.");
            escrowManager.processRefund(player, tx.getTransactionId());
            return AlchemyResult.error("Lỗi ghi nhận WAL trạng thái ACTIVE! Đã hoàn trả nguyên liệu an toàn.");
        }

        AlchemySession session = new AlchemySession(tx.getTransactionId(), uuid, recipe, plannedLockedItems);
        activeSessions.put(uuid, session);

        player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1f, 1f);
        player.sendMessage("§6§l[Đan Lò] §fĐã khai lò luyện §b" + recipe.getDisplayName() + "§f! Hãy điều chỉnh hỏa hầu.");

        return AlchemyResult.success(null, 0.0, null, 0, "Khai lò thành công!");
    }

    /**
     * Điều chỉnh nhiệt độ thông qua các thao tác hỏa hầu
     */
    public AlchemyResult applyHeatAction(Player player, AlchemyHeatAction action) {
        if (player == null || action == null) return AlchemyResult.error("Yêu cầu không hợp lệ.");

        AlchemySession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return AlchemyResult.error("Ngươi chưa khai lò luyện đan!");
        }

        switch (action) {
            case VAN_HOA, VU_HOA, HA_HOA -> {
                session.adjustTemperature(action.getTempDelta());
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.2f);
                return AlchemyResult.success(null, session.calculateScore(), null, 0, "Đã điều chỉnh: " + action.getDisplayName());
            }
            case THUAN_LUA -> {
                player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.5f);
                return AlchemyResult.success(null, session.calculateScore(), null, 0, "Đã vận chuyển linh lực thuần hỏa.");
            }
            case KET_DAN -> {
                return completeSession(player, false);
            }
            case HUY_LO -> {
                return cancelSession(player, true, "Đã dập lửa hủy lò, nguyên liệu đã được hoàn trả!");
            }
            default -> {
                return AlchemyResult.error("Thao tác không hỗ trợ.");
            }
        }
    }

    /**
     * Tiến trình cập nhật 1 giây (được gọi từ AlchemyTask)
     */
    public void tickSession(UUID playerId) {
        AlchemySession session = activeSessions.get(playerId);
        if (session == null || session.isFinished() || session.isCancelled()) return;

        Player player = Bukkit.getPlayer(playerId);

        // 1. Cập nhật 1 giây
        boolean exploded = session.tickOneSecond();
        if (exploded) {
            handleExplosion(session, player);
            return;
        }

        // 2. Tự động kết đan khi hết thời gian
        if (session.getRemainingSeconds() <= 0) {
            if (player != null && player.isOnline()) {
                completeSession(player, true);
            } else {
                handleOfflineCompletion(session);
            }
        }
    }

    /**
     * Hoàn tất mẻ đan và tính toán chất lượng thành phẩm
     */
    public synchronized AlchemyResult completeSession(Player player, boolean timeExpired) {
        if (player == null) return AlchemyResult.error("Người chơi offline.");

        AlchemySession session = activeSessions.remove(player.getUniqueId());
        if (session == null || session.isFinished()) {
            return AlchemyResult.error("Phiên luyện đan không tồn tại hoặc đã kết thúc!");
        }

        session.setFinished(true);
        AlchemyRecipe recipe = session.getRecipe();

        // 1. Tính toán điểm hỏa hầu
        double baseScore = session.calculateScore();

        // 2. Thưởng từ Cấp Lò & Hệ Đan Tu
        int capLo = dataManager.getDanLoCapDo(player);
        double cauldronBonus = (capLo - 1) * 3.0; // Mỗi cấp lò +3 điểm
        double danTuBonus = (dataManager.getHeTuLuyen(player) == HeTuLuyen.DAN_TU) ? 10.0 : 0.0;

        double finalScore = Math.min(100.0, baseScore + cauldronBonus + danTuBonus);
        AlchemyQuality quality = AlchemyQuality.fromScore(finalScore);

        // 3. Kiểm tra tỷ lệ thành công cơ bản
        double totalSuccessChance = recipe.getBaseSuccessRate() + (finalScore >= 75.0 ? 0.20 : 0.0);
        boolean isSuccess = (Math.random() <= totalSuccessChance) && (quality != AlchemyQuality.PHE_DAN);

        if (!isSuccess) {
            // Thất bại: Đánh dấu Escrow đã hoàn tất, tạo Tro Tàn có receipt
            ItemStack ashItem = createAshItem();
            escrowManager.completeTransaction(session.getTransactionId(), List.of(ashItem));
            escrowManager.processClaim(player, session.getTransactionId());

            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 0.8f);
            player.sendMessage("§c§l[Đan Lò] §fHỏa hầu dao động quá lớn! Dược liệu đã biến thành tro bụi (Điểm: "
                    + String.format("%.1f", finalScore) + "/100).");

            return AlchemyResult.failureAshes(finalScore, "Luyện đan thất bại!");
        }

        // 4. Thành công: Chế tạo đan dược với PDC chuẩn
        int baseYield = 1;
        int totalYield = baseYield + quality.getBonusYield() - 1; // Cực phẩm = 2 viên

        ItemStack pillItem = null;
        if (danDuocManager != null) {
            pillItem = danDuocManager.createPillItem(recipe.getDanDuocId(), totalYield);
        }

        if (pillItem == null) {
            pillItem = new ItemStack(Material.FIREWORK_STAR, totalYield);
            ItemMeta meta = pillItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + recipe.getDisplayName());
                meta.setLore(List.of(quality.getDisplayName(), quality.getDescription()));
                ItemKeys.markMedicine(meta, recipe.getDanDuocId(), totalYield);
                pillItem.setItemMeta(meta);
            }
        } else {
            // Thêm ghi chú phẩm chất vào lore của đan
            ItemMeta meta = pillItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(0, quality.getDisplayName());
                meta.setLore(lore);
                pillItem.setItemMeta(meta);
            }
        }

        // 5. Giao dịch thành phẩm: Ghi nhận COMPLETED trên đĩa trước khi trao
        boolean completedSaved = escrowManager.completeTransaction(session.getTransactionId(), List.of(pillItem));
        boolean claimed = escrowManager.processClaim(player, session.getTransactionId());

        if (!completedSaved || !claimed) {
            player.sendMessage("§e§l[Đan Lò] §fLuyện đan hoàn tất nhưng thành phẩm đang chờ nhận an toàn trong Hòm Thư (/tutien homthu).");
            return AlchemyResult.success(quality, finalScore, pillItem, totalYield, "Luyện hoàn tất (thành phẩm chuyển vào Hòm Thư)");
        }

        dataManager.addSoLanLuyenDan(player, 1);

        if (plugin != null && plugin.getNhiemVuManager() != null) {
            plugin.getNhiemVuManager().addProgress(player, tutien.quest.NhiemVuManager.NhiemVu.LUYEN_DAN, 1);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        player.sendMessage("§6§l[Đan Lò] §fLuyện đan thành công! Phẩm chất: " + quality.getDisplayName()
                + " §f(Điểm hỏa hầu: §a" + String.format("%.1f", finalScore) + "/100§f) §e[x" + totalYield + "]");
        player.sendMessage("§7" + quality.getDescription());

        return AlchemyResult.success(quality, finalScore, pillItem, totalYield, "Luyện thành công!");
    }

    /**
     * Hủy mẻ luyện và hoàn trả 100% nguyên liệu ký quỹ có tính Idempotent
     */
    public synchronized AlchemyResult cancelSession(Player player, boolean refund, String msg) {
        if (player == null) return AlchemyResult.error("Người chơi không hợp lệ.");

        AlchemySession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            return AlchemyResult.error("Không có mẻ luyện nào đang diễn ra.");
        }

        session.setCancelled(true);

        if (refund) {
            escrowManager.markRefundPending(session.getTransactionId());
            escrowManager.processRefund(player, session.getTransactionId());
            player.sendMessage(ChatColor.YELLOW + msg);
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
        }

        return AlchemyResult.cancelled(session.getEscrowItems(), msg);
    }

    /**
     * Xử lý phát nổ do quá nhiệt hỏa hầu
     */
    private void handleExplosion(AlchemySession session, Player player) {
        activeSessions.remove(session.getPlayerId());
        session.setFinished(true);

        escrowManager.markRefundPending(session.getTransactionId());
        // Tiêu hủy nguyên liệu trong mẻ nổ
        EscrowTransaction tx = escrowManager.getTransaction(session.getTransactionId());
        if (tx != null) {
            tx.setState(tutien.alchemy.escrow.EscrowState.DISCARDED);
            escrowManager.saveTransactionsToDisk();
        }

        if (player != null && player.isOnline()) {
            player.damage(4.0); // Sát thương nổ lò nhẹ
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
            player.sendMessage("§4§l[NỔ LÒ!] §cDo nhiệt độ quá cao vượt mức kiểm soát, đan lò đã phát nổ! Toàn bộ nguyên liệu bị thiêu rụi.");
        }
    }

    private void handleOfflineCompletion(AlchemySession session) {
        activeSessions.remove(session.getPlayerId());
        session.setFinished(true);
        escrowManager.markRefundPending(session.getTransactionId());
        logger.warning("[AlchemyService] Người chơi " + session.getPlayerId() + " offline khi kết đan. Đã bảo toàn mẻ đan vào REFUND_PENDING trên đĩa.");
    }

    /**
     * An toàn hoàn trả nguyên liệu khi người chơi ngắt kết nối
     */
    public void onPlayerQuit(Player player) {
        if (player == null) return;
        AlchemySession session = activeSessions.remove(player.getUniqueId());
        if (session != null && !session.isFinished()) {
            session.setCancelled(true);
            escrowManager.markRefundPending(session.getTransactionId());
            escrowManager.processRefund(player, session.getTransactionId());
            logger.info("[AlchemyService] Đã bảo toàn và hoàn trả Ký Quỹ Escrow cho " + player.getName() + " khi ngắt kết nối.");
        }
    }

    /**
     * An toàn khi reload hoặc tắt server: Đánh dấu REFUND_PENDING trên đĩa trước khi shutdown
     */
    public void onDisable() {
        for (Map.Entry<UUID, AlchemySession> entry : activeSessions.entrySet()) {
            AlchemySession session = entry.getValue();
            if (session != null && !session.isFinished()) {
                session.setCancelled(true);
                escrowManager.markRefundPending(session.getTransactionId());
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && p.isOnline()) {
                    escrowManager.processRefund(p, session.getTransactionId());
                }
            }
        }
        activeSessions.clear();
        escrowManager.saveTransactionsToDisk();
        logger.info("[AlchemyService] Đã bảo toàn bền vững toàn bộ giao dịch Ký Quỹ Escrow xuống đĩa.");
    }

    // --- HÀM TIỆN ÍCH XỬ LÝ KHO ĐỒ ---

    private int countMaterial(Player player, Material mat) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat && ItemKeys.isValidRawMaterial(is)) {
                count += is.getAmount();
            }
        }
        return count;
    }

    private List<ItemStack> removeMaterialAndCollect(Player player, Material mat, int amount) {
        List<ItemStack> collected = new ArrayList<>();
        int toRemove = amount;

        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack is : contents) {
            if (is != null && is.getType() == mat && ItemKeys.isValidRawMaterial(is)) {
                if (is.getAmount() > toRemove) {
                    ItemStack clone = is.clone();
                    clone.setAmount(toRemove);
                    collected.add(clone);
                    is.setAmount(is.getAmount() - toRemove);
                    toRemove = 0;
                    break;
                } else {
                    collected.add(is.clone());
                    toRemove -= is.getAmount();
                    is.setAmount(0);
                }
            }
            if (toRemove <= 0) break;
        }
        return collected;
    }

    private ItemStack createAshItem() {
        ItemStack ash = new ItemStack(Material.GUNPOWDER, 1);
        ItemMeta meta = ash.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8Tro Tàn Đan Dược");
            meta.setLore(List.of(
                    "§7Tàn tích từ một mẻ luyện đan thất bại.",
                    "§8Có thể dùng để bón cây hoặc thanh lý."
            ));
            ash.setItemMeta(meta);
        }
        return ash;
    }

    public static class DebitPlanEntry {
        public final int slot;
        public final int amountToDebit;
        public final ItemStack itemClone;

        public DebitPlanEntry(int slot, int amountToDebit, ItemStack itemClone) {
            this.slot = slot;
            this.amountToDebit = amountToDebit;
            this.itemClone = itemClone;
        }
    }

    public Map<UUID, AlchemySession> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }
}
