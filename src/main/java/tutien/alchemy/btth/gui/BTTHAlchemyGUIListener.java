package tutien.alchemy.btth.gui;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import tutien.alchemy.btth.model.BTTHAlchemySession;
import tutien.alchemy.btth.model.BTTHFormula;
import tutien.alchemy.btth.service.*;
import tutien.core.TuTienPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xử lý sự kiện GUI Đan Phòng & Cẩm Nang Luyện Dược:
 * - KHÓA CHẶT 100% không cho người chơi rút, kéo, shift-click vật phẩm trong GUI ra ngoài.
 * - Xử lý click tương tác thời gian thực: Tăng/Giảm hỏa, Tinh luyện, Dung hợp, Thu đan.
 */
public class BTTHAlchemyGUIListener implements Listener {

    private final TuTienPlugin plugin;

    public BTTHAlchemyGUIListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("ĐAN PHÒNG") || title.contains("CHỌN ĐAN PHƯƠNG") || title.contains("CẨM NANG LUYỆN DƯỢC")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean isBTTH = title.contains("ĐAN PHÒNG") || title.contains("CHỌN ĐAN PHƯƠNG") || title.contains("CẨM NANG LUYỆN DƯỢC");

        if (!isBTTH) return;

        // KHÓA TUYỆT ĐỐI KHÔNG CHO RÚT VẬT PHẨM
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        int rawSlot = event.getRawSlot();

        BTTHProcessController controller = plugin != null ? plugin.getBtthProcessController() : null;
        SoulControlService soulService = plugin != null ? plugin.getBtthSoulService() : null;
        AlchemistRankService rankService = plugin != null ? plugin.getBtthRankService() : null;
        AlchemyFlameService flameService = plugin != null ? plugin.getBtthFlameService() : null;
        AlchemyCauldronService cauldronService = plugin != null ? plugin.getBtthCauldronService() : null;
        BTTHFormulaManager formulaManager = plugin != null ? plugin.getBtthFormulaManager() : null;

        // 1. XỬ LÝ TRONG GUI CẨM NANG HƯỚNG DẪN
        if (title.contains("CẨM NANG LUYỆN DƯỢC")) {
            if (rawSlot == 49) {
                // Quay lại Đan Phòng
                BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        // 2. XỬ LÝ TRONG GUI CHỌN ĐAN PHƯƠNG
        if (title.contains("CHỌN ĐAN PHƯƠNG")) {
            if (rawSlot == 49) {
                // Quay lại Đan Phòng
                BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                return;
            }

            // Người chơi click vào một cuốn đan phương
            String name = clickedItem.getItemMeta().getDisplayName();
            Pattern p = Pattern.compile("\\[([A-Z0-9_]+)\\]");
            Matcher m = p.matcher(name);
            if (m.find()) {
                String formulaId = m.group(1);
                if (controller != null) {
                    BTTHProcessController.ProcessResult res = controller.startSession(player, formulaId);
                    if (res.isSuccess()) {
                        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.2f);
                        BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                    } else {
                        player.sendMessage("§c§l[Thất Bại] §f" + res.getMessage());
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                }
            }
            return;
        }

        // 3. XỬ LÝ TRONG GUI ĐAN PHÒNG CHÍNH
        if (title.contains("ĐAN PHÒNG")) {
            if (rawSlot >= 54) return; // Click vào túi đồ cá nhân

            switch (rawSlot) {
                case 4 -> {
                    // Mở Chọn Đan Phương
                    BTTHFormulaSelectGUI.open(player, formulaManager, rankService, soulService);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
                }
                case 8 -> {
                    // Mở Hướng Dẫn Luyện Đan
                    BTTHGuideGUI.open(player);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
                }
                case 20 -> {
                    // Giảm hỏa -50°C
                    if (controller != null) {
                        BTTHProcessController.ProcessResult res = controller.adjustHeat(player, -50.0);
                        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.8f);
                        BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                    }
                }
                case 21 -> {
                    // Giảm hỏa -10°C
                    if (controller != null) {
                        BTTHProcessController.ProcessResult res = controller.adjustHeat(player, -10.0);
                        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.2f);
                        BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                    }
                }
                case 23 -> {
                    // Tăng hỏa +10°C
                    if (controller != null) {
                        BTTHProcessController.ProcessResult res = controller.adjustHeat(player, 10.0);
                        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.2f);
                        BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                    }
                }
                case 24 -> {
                    // Tăng hỏa +50°C
                    if (controller != null) {
                        BTTHProcessController.ProcessResult res = controller.adjustHeat(player, 50.0);
                        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
                        BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                    }
                }
                case 28, 29, 30 -> {
                    // Tinh luyện thảo dược theo ô
                    if (controller != null) {
                        BTTHAlchemySession session = controller.getSession(player.getUniqueId());
                        if (session != null) {
                            int ingIndex = rawSlot - 28;
                            if (ingIndex < session.getFormula().getIngredients().size()) {
                                String ingId = session.getFormula().getIngredients().get(ingIndex).getIngredientId();
                                if (!session.getExtractedIngredients().contains(ingId)) {
                                    BTTHProcessController.ProcessResult res = controller.extractIngredient(player, ingId);
                                    if (!res.isSuccess()) {
                                        player.sendMessage("§c§l[Cảnh Báo] §f" + res.getMessage());
                                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                                    }
                                    BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                                }
                            }
                        }
                    }
                }
                case 31 -> {
                    // Dung hợp dược dịch
                    if (controller != null) {
                        BTTHAlchemySession session = controller.getSession(player.getUniqueId());
                        if (session != null && session.getExtractedIngredients().size() >= session.getFormula().getIngredients().size()) {
                            controller.fuseLiquids(player, session.getFormula().getFusionOrder());
                            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
                            BTTHAlchemyGUI.open(player, controller, soulService, rankService, flameService, cauldronService);
                        } else {
                            player.sendMessage("§c§l[Chưa Thể Dung Hợp] §fHãy tinh luyện toàn bộ thảo dược trước!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        }
                    }
                }
                case 40 -> {
                    // Thu Đan
                    if (controller != null) {
                        BTTHAlchemySession session = controller.getSession(player.getUniqueId());
                        if (session != null) {
                            controller.collectPill(player);
                            player.closeInventory();
                        } else {
                            player.sendMessage("§c§l[Chưa Có Đan Dược] §fHãy bắt đầu phiên luyện chế trước!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        }
                    }
                }
                case 45 -> {
                    // Hủy phiên luyện đan
                    if (controller != null) {
                        controller.failSession(player, "Đạo hữu chủ động dập tắt hỏa diễm, hủy bỏ phiên luyện đan.");
                        player.closeInventory();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            BTTHAlchemyGUI.cleanGlitchItems(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropGlitchItem(PlayerDropItemEvent event) {
        if (BTTHAlchemyGUI.isGlitchItem(event.getItemDrop().getItemStack())) {
            event.getItemDrop().remove();
            event.setCancelled(true);
            BTTHAlchemyGUI.cleanGlitchItems(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlaceGlitchItem(BlockPlaceEvent event) {
        if (BTTHAlchemyGUI.isGlitchItem(event.getItemInHand())) {
            event.setCancelled(true);
            BTTHAlchemyGUI.cleanGlitchItems(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractGlitchItem(PlayerInteractEvent event) {
        if (BTTHAlchemyGUI.isGlitchItem(event.getItem())) {
            event.setCancelled(true);
            BTTHAlchemyGUI.cleanGlitchItems(event.getPlayer());
        }
    }
}
