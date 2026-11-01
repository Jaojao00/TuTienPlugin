package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ChonHeGUI implements Listener {

    public static final String GUI_NAME = "§0§lChọn Con Đường Đại Đạo";
    public static final String CONFIRM_GUI_NAME = "§0§lXác Nhận Tu Luyện";

    private final PlayerDataManager dataManager;

    // Lưu tạm lựa chọn của người chơi khi mở bảng xác nhận (Tránh lỗi)
    private static final HashMap<UUID, HeTuLuyen> pendingChoice = new HashMap<>();

    public ChonHeGUI(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    // ==========================================
    // BẢNG 1: CHỌN HỆ (MAIN GUI)
    // ==========================================
    public static void open(Player player, PlayerDataManager dataManager) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);
        HeTuLuyen currentHe = dataManager.getHeTuLuyen(player);

        inv.setItem(10, createIcon(HeTuLuyen.KIEM_TU, currentHe));
        inv.setItem(11, createIcon(HeTuLuyen.THE_TU, currentHe));
        inv.setItem(12, createIcon(HeTuLuyen.PHAP_TU, currentHe));
        inv.setItem(13, createIcon(HeTuLuyen.MA_TU, currentHe));
        inv.setItem(14, createIcon(HeTuLuyen.YEU_TU, currentHe));
        inv.setItem(15, createIcon(HeTuLuyen.DAN_TU, currentHe));
        inv.setItem(16, createIcon(HeTuLuyen.SONG_TU, currentHe));

        player.openInventory(inv);
    }

    // ==========================================
    // BẢNG 2: XÁC NHẬN (CONFIRM GUI)
    // ==========================================
    private void openConfirmGUI(Player player, HeTuLuyen selected) {
        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM_GUI_NAME);
        HeTuLuyen currentHe = dataManager.getHeTuLuyen(player);

        // 1. Nút Xác Nhận (Ô 11)
        ItemStack confirmBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmBtn.getItemMeta();
        confirmMeta.setDisplayName("§a§l[✔] XÁC NHẬN");
        List<String> confirmLore = new ArrayList<>();

        if (currentHe == HeTuLuyen.CHUA_CHON) {
            confirmLore.add("§7Bước vào con đường " + selected.getTenHienThi() + "§7.");
            confirmLore.add("§7Không thể đổi ý sau khi đã chọn!");
        } else {
            confirmLore.add("§c⚠ CẢNH BÁO KHI CHUYỂN ĐẠO:");
            confirmLore.add("§7- Bạn sẽ bị phế bỏ §c50% Tu Vi §7tổng.");
            confirmLore.add("§7- Cảnh giới hiện tại rớt về §fPhàm Nhân§7.");
            confirmLore.add("");
            confirmLore.add("§4§lHÀNH ĐỘNG NÀY KHÔNG THỂ HOÀN TÁC!");
        }
        confirmMeta.setLore(confirmLore);
        confirmBtn.setItemMeta(confirmMeta);

        // 2. Thông tin hệ đang chọn (Ô 13 - Chính giữa)
        ItemStack infoBtn = createIcon(selected, HeTuLuyen.CHUA_CHON);
        ItemMeta infoMeta = infoBtn.getItemMeta();
        infoMeta.setDisplayName("§e§lĐang Chọn: " + selected.getTenHienThi());
        infoBtn.setItemMeta(infoMeta);

        // 3. Nút Hủy Bỏ (Ô 15)
        ItemStack cancelBtn = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelBtn.getItemMeta();
        cancelMeta.setDisplayName("§c§l[✖] HỦY BỎ");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("§7Quay lại bảng chọn hệ đại đạo.");
        cancelMeta.setLore(cancelLore);
        cancelBtn.setItemMeta(cancelMeta);

        inv.setItem(11, confirmBtn);
        inv.setItem(13, infoBtn);
        inv.setItem(15, cancelBtn);

        player.openInventory(inv);
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH (HELPER)
    // ==========================================
    private static Material getMaterialForHe(HeTuLuyen he) {
        switch (he) {
            case KIEM_TU:
                return Material.IRON_SWORD;
            case THE_TU:
                return Material.NETHERITE_CHESTPLATE;
            case PHAP_TU:
                return Material.ENCHANTED_BOOK;
            case MA_TU:
                return Material.WITHER_SKELETON_SKULL;
            case YEU_TU:
                return Material.TOTEM_OF_UNDYING;
            case DAN_TU:
                return Material.BREWING_STAND;
            case SONG_TU:
                return Material.HEART_OF_THE_SEA;
            default:
                return Material.PAPER;
        }
    }

    private static ItemStack createIcon(HeTuLuyen he, HeTuLuyen currentHe) {
        ItemStack item = new ItemStack(getMaterialForHe(he));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(he.getTenHienThi());
        List<String> lore = new ArrayList<>();
        lore.add("§7" + he.getMoTa());
        lore.add("");

        if (currentHe == HeTuLuyen.CHUA_CHON) {
            lore.add("§e▶ Nhấp để bắt đầu tu luyện!");
        } else if (currentHe == he) {
            lore.add("§a✔ Bạn đang theo đuổi đạo này.");
            item.setType(Material.ENCHANTED_BOOK); // Đổi icon thành cuốn sách nếy đã chọn
        } else {
            lore.add("§c⚠ CẢNH BÁO CHUYỂN HỆ:");
            lore.add("§7- Phế bỏ §c50% Tu Vi§7.");
            lore.add("§7- Rớt về §fPhàm Nhân§7.");
            lore.add("");
            lore.add("§6[!] Nhấp để Nghịch Thiên Cải Mệnh!");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // XỬ LÝ SỰ KIỆN CLICK CHUỘT VÀO NÚT
    // ==========================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(GUI_NAME) && !title.equals(CONFIRM_GUI_NAME))
            return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // --- NẾU BẤM Ở BẢNG CHỌN HỆ CHÍNH ---
        if (title.equals(GUI_NAME)) {
            HeTuLuyen currentHe = dataManager.getHeTuLuyen(player);
            HeTuLuyen selected = null;

            switch (slot) {
                case 10:
                    selected = HeTuLuyen.KIEM_TU;
                    break;
                case 11:
                    selected = HeTuLuyen.THE_TU;
                    break;
                case 12:
                    selected = HeTuLuyen.PHAP_TU;
                    break;
                case 13:
                    selected = HeTuLuyen.MA_TU;
                    break;
                case 14:
                    selected = HeTuLuyen.YEU_TU;
                    break;
                case 15:
                    selected = HeTuLuyen.DAN_TU;
                    break;
                case 16:
                    selected = HeTuLuyen.SONG_TU;
                    break;
            }

            if (selected == null || selected == currentHe)
                return;

            // Lưu lựa chọn và mở bảng xác nhận
            pendingChoice.put(player.getUniqueId(), selected);
            openConfirmGUI(player, selected);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }

        // --- NẾU BẤM Ở BẢNG XÁC NHẬN ---
        else if (title.equals(CONFIRM_GUI_NAME)) {
            if (slot == 15) {
                // BẤM HỦY BỎ (NÚT ĐỎ)
                pendingChoice.remove(player.getUniqueId());
                open(player, dataManager); // Quay lại bảng chính
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            } else if (slot == 11) {
                // BẤM XÁC NHẬN (NÚT XANH)
                HeTuLuyen selected = pendingChoice.get(player.getUniqueId());
                if (selected == null) {
                    player.closeInventory();
                    return;
                }

                HeTuLuyen currentHe = dataManager.getHeTuLuyen(player);
                pendingChoice.remove(player.getUniqueId()); // Xóa bộ nhớ đệm

                if (currentHe == HeTuLuyen.CHUA_CHON) {
                    // Xử lý Lần đầu chọn hệ
                    dataManager.setHeTuLuyen(player, selected);
                    player.sendMessage(
                            "§a§l[Thiên Đạo] §fChúc mừng! Ngươi đã bước vào con đường " + selected.getTenHienThi());
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    player.closeInventory();
                } else {
                    // Xử lý Phế tu vi chuyển hệ
                    dataManager.setTuVi(player, dataManager.getTuVi(player) / 2);
                    dataManager.setCanhGioi(player, CanhGioi.PHAM_NHAN);
                    dataManager.setHeTuLuyen(player, selected);

                    player.sendMessage("§c§l[Nghịch Thiên] §fNgươi đã tự phế bỏ tu vi cũ để theo đuổi "
                            + selected.getTenHienThi());
                    player.getWorld().strikeLightningEffect(player.getLocation());
                    player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.5f);
                    player.closeInventory();
                }
            }
        }
    }
}