package tutien.cultivation.gui;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import tutien.core.CanhGioi;
import tutien.core.MeditationType;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.cultivation.service.CongPhapTransitionService;
import tutien.tutien.gui.MainMenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Listener xử lý sự kiện tương tác và bảo mật kho đồ cho CongPhapGUI, ThamNgoGUI, ChuyenTuConfirmGUI (Phase 2.4).
 * Nhận diện qua InventoryHolder, chống hoàn toàn việc lấy/kéo trộm vật phẩm và spam click.
 */
public class CongPhapGUIListener implements Listener {

    private final TuTienPlugin plugin;
    private final java.util.Map<java.util.UUID, Long> clickDebounceMap = new java.util.concurrent.ConcurrentHashMap<>();

    public CongPhapGUIListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isCustomGUI(org.bukkit.inventory.InventoryView view) {
        if (view == null || view.getTopInventory() == null) return false;
        org.bukkit.inventory.InventoryHolder holder = view.getTopInventory().getHolder();
        return holder instanceof CongPhapHolder ||
               holder instanceof ThamNgoHolder ||
               holder instanceof ChuyenTuConfirmHolder;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isCustomGUI(event.getView())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isCustomGUI(event.getView())) return;

        // Chặn hoàn toàn mọi rủi ro về kho đồ
        event.setCancelled(true);

        // Chặn tuyệt đối phím tắt, swap offhand, double click gom đồ, shift click
        ClickType clickType = event.getClick();
        if (clickType.isShiftClick() ||
            clickType == ClickType.NUMBER_KEY ||
            clickType == ClickType.SWAP_OFFHAND ||
            clickType == ClickType.DOUBLE_CLICK) {
            return;
        }

        // Chỉ xử lý tương tác diễn ra trên TopInventory (giao diện GUI custom)
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        // Chống spam click liên tục (debounce 200ms)
        long now = System.currentTimeMillis();
        long lastClick = clickDebounceMap.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastClick < 200L) {
            return;
        }
        clickDebounceMap.put(player.getUniqueId(), now);

        // ==========================================
        // 1. XỬ LÝ CONG PHAP GUI
        // ==========================================
        if (event.getInventory().getHolder() instanceof CongPhapHolder holder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

            CongPhapGUIConfig cfg = plugin.getCongPhapGUIConfig();
            PlayerDataManager dataManager = plugin.getPlayerDataManager();
            PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);

            // Nút mở Tham Ngộ (Slot 49)
            if (slot == cfg.getCongPhapOpenComprehensionSlot()) {
                ThamNgoGUI.open(player, plugin);
                return;
            }

            // Nút quay lại MainMenu (Slot 45)
            if (slot == cfg.getCongPhapBackSlot()) {
                MainMenu.open(player, dataManager);
                return;
            }

            // Nút trang trước (Slot 47)
            if (slot == cfg.getCongPhapPrevPageSlot()) {
                CongPhapGUI.open(player, plugin, holder.getPage() - 1);
                return;
            }

            // Nút trang sau (Slot 51)
            if (slot == cfg.getCongPhapNextPageSlot()) {
                CongPhapGUI.open(player, plugin, holder.getPage() + 1);
                return;
            }

            // Click vào Primary Slot (Slot 4)
            if (slot == cfg.getCongPhapPrimarySlot()) {
                String primary = cultData.getActivePrimary();
                if (primary != null && !primary.equalsIgnoreCase("CHUA_CO")) {
                    ThamNgoGUI.open(player, plugin);
                }
                return;
            }

            // Click vào các ô Phụ Tu (Auxiliary Slots)
            List<Integer> auxSlots = cfg.getCongPhapAuxiliarySlots();
            int auxIndex = auxSlots.indexOf(slot);
            if (auxIndex != -1) {
                List<String> activeAux = cultData.getActiveAuxiliary();
                if (auxIndex < activeAux.size()) {
                    String toRemove = activeAux.get(auxIndex);
                    cultData.removeAuxiliary(toRemove);
                    player.sendMessage("§e[Công Pháp] §fĐã gỡ bỏ công pháp phụ tu: §b" + toRemove);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                    CongPhapGUI.open(player, plugin, holder.getPage());
                }
                return;
            }

            // Click vào Danh mục Công Pháp (Technique List)
            int listStart = cfg.getCongPhapListStart();
            int listEnd = cfg.getCongPhapListEnd();
            if (slot >= listStart && slot <= listEnd) {
                int pageSize = (listEnd - listStart) + 1;
                int itemIndex = (holder.getPage() * pageSize) + (slot - listStart);

                List<CongPhap> allCp = new ArrayList<>(plugin.getCongPhapManager().getAllCongPhap());
                allCp.sort(Comparator.comparing(CongPhap::getGradeTier).thenComparing(CongPhap::getId));

                if (itemIndex >= 0 && itemIndex < allCp.size()) {
                    CongPhap targetCp = allCp.get(itemIndex);
                    String cpId = targetCp.getId();

                    // Click Chuột Phải: Phụ Tu
                    if (event.getClick() == ClickType.RIGHT) {
                        if (cpId.equalsIgnoreCase(cultData.getActivePrimary())) {
                            player.sendMessage("§c§l[!] §fCông pháp này đang là Chủ Tu, không thể đặt làm Phụ Tu!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                            return;
                        }
                        if (cultData.isAuxiliary(cpId)) {
                            cultData.removeAuxiliary(cpId);
                            player.sendMessage("§e[Công Pháp] §fĐã gỡ bỏ công pháp phụ tu: §b" + targetCp.getDisplayName());
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                            CongPhapGUI.open(player, plugin, holder.getPage());
                            return;
                        }
                        int maxAux = CongPhapGUI.getMaxAuxiliarySlots(dataManager.getCanhGioi(player));
                        if (cultData.getActiveAuxiliary().size() >= maxAux) {
                            player.sendMessage("§c§l[!] §fĐã đạt giới hạn tối đa " + maxAux + " công pháp phụ tu!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                            return;
                        }
                        cultData.addAuxiliary(cpId, maxAux);
                        player.sendMessage("§a§l[Công Pháp] §fĐã thiết lập phụ tu: §b" + targetCp.getDisplayName());
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        CongPhapGUI.open(player, plugin, holder.getPage());
                        return;
                    }

                    // Click Chuột Trái: Chuyển Tu (Mở màn hình xác nhận)
                    if (event.getClick() == ClickType.LEFT) {
                        if (cpId.equalsIgnoreCase(cultData.getActivePrimary())) {
                            player.sendMessage("§e[Công Pháp] §fNgươi đang chủ tu công pháp này!");
                            ThamNgoGUI.open(player, plugin);
                            return;
                        }

                        // Mở màn hình xác nhận chuyển tu 2 bước
                        ChuyenTuConfirmGUI.open(player, plugin, cpId);
                    }
                }
            }
            return;
        }

        // ==========================================
        // 2. XỬ LÝ THAM NGO GUI
        // ==========================================
        if (event.getInventory().getHolder() instanceof ThamNgoHolder holder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

            CongPhapGUIConfig cfg = plugin.getCongPhapGUIConfig();
            PlayerDataManager dataManager = plugin.getPlayerDataManager();
            String cpId = holder.getCongPhapId();

            // Nút quay lại CongPhapGUI (Slot 40)
            if (slot == cfg.getThamNgoBackSlot()) {
                CongPhapGUI.open(player, plugin, 0);
                return;
            }

            // Nút Bật/Tắt Tọa Thiền Tham Ngộ (Slot 33)
            if (slot == cfg.getThamNgoToggleSlot()) {
                MeditationType currentMed = dataManager.getMeditationType(player);
                if (currentMed == MeditationType.THAM_NGO) {
                    dataManager.setMeditationType(player, MeditationType.NONE);
                    player.sendMessage("§e§l[Tu Tiên] §fNgươi đã xuất định, tạm dừng quá trình tham ngộ khẩu quyết.");
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
                } else {
                    dataManager.setMeditationType(player, MeditationType.THAM_NGO);
                    Location loc = player.getLocation();
                    loc.setX(loc.getBlockX() + 0.5);
                    loc.setZ(loc.getBlockZ() + 0.5);
                    player.teleport(loc);
                    player.sendMessage("§b§l[Tu Tiên] §fNgươi đã nhập định tọa thiền tham ngộ khẩu quyết công pháp. Hãy tĩnh tâm...");
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
                }
                ThamNgoGUI.open(player, plugin);
                return;
            }

            // Nút Tiêu Hao 10 Điểm Đạo Ngộ (Slot 29)
            if (slot == cfg.getThamNgoDaoNgo10Slot()) {
                boolean success = plugin.getCongPhapManager().getComprehensionService().accelerateWithDaoNgoPoints(
                        player, dataManager, cpId, 10);
                if (success) {
                    ThamNgoGUI.open(player, plugin);
                }
                return;
            }

            // Nút Tiêu Hao 50 Điểm Đạo Ngộ (Slot 31)
            if (slot == cfg.getThamNgoDaoNgo50Slot()) {
                boolean success = plugin.getCongPhapManager().getComprehensionService().accelerateWithDaoNgoPoints(
                        player, dataManager, cpId, 50);
                if (success) {
                    ThamNgoGUI.open(player, plugin);
                }
                return;
            }
            return;
        }

        // ==========================================
        // 3. XỬ LÝ CHUYEN TU CONFIRM GUI
        // ==========================================
        if (event.getInventory().getHolder() instanceof ChuyenTuConfirmHolder holder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;

            // Slot 11: Xác Nhận Chuyển Tu
            if (slot == 11) {
                PlayerDataManager dataManager = plugin.getPlayerDataManager();
                PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);
                String targetCpId = holder.getTargetCongPhapId();
                CongPhap newCp = plugin.getCongPhapManager().getCongPhap(targetCpId);
                String oldPrimaryId = cultData.getActivePrimary();
                CongPhap oldCp = oldPrimaryId != null ? plugin.getCongPhapManager().getCongPhap(oldPrimaryId) : null;

                if (newCp == null) {
                    player.sendMessage("§c§l[!] §fCông pháp mục tiêu không hợp lệ!");
                    CongPhapGUI.open(player, plugin, 0);
                    return;
                }

                // Thực hiện Chuyển Tu an toàn
                int currentTuVi = dataManager.getTuVi(player);
                CanhGioi realm = dataManager.getCanhGioi(player);

                CongPhapTransitionService.TransitionResult result = plugin.getCongPhapManager().getTransitionService().transition(
                        cultData, oldCp, newCp, currentTuVi, realm, false);

                if (result.isSuccess()) {
                    if (result.getTuViLost() > 0) {
                        dataManager.setTuVi(player, Math.max(0, currentTuVi - result.getTuViLost()));
                    }
                    dataManager.setCongPhapChuTu(player, targetCpId);
                    player.sendMessage("§a§l✦ CHUYỂN TU THÀNH CÔNG ✦ §fNgươi đã chính thức tu luyện: " + newCp.getDisplayName() + "§f!");
                    if (result.getTuViLost() > 0) {
                        player.sendMessage("§c[Tổn Thất] §7Tiểu cảnh giới hao tổn: §e-" + result.getTuViLost() + " Tu Vi§7 (Đại cảnh giới được bảo toàn).");
                    }
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                    CongPhapGUI.open(player, plugin, 0);
                } else {
                    player.sendMessage("§c§l[!] §fKhông thể chuyển tu: " + result.getMessage());
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                    CongPhapGUI.open(player, plugin, 0);
                }
                return;
            }

            // Slot 15: Hủy Bỏ
            if (slot == 15) {
                player.sendMessage("§7Đã hủy bỏ chuyển đổi công pháp.");
                CongPhapGUI.open(player, plugin, 0);
                return;
            }
        }
    }
}
