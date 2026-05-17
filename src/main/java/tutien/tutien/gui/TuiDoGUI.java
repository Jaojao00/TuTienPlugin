package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.inventory.TuiDoManager;
import tutien.inventory.TuiDoManager.ItemCategory;
import tutien.inventory.TuiDoManager.StoredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GIAO DIỆN TÚI ĐỒ HƯ KHÔNG (PHIÊN BẢN HOÀN CHỈNH)
 * 
 * - Phân loại vật phẩm theo 8 nhóm với tab chuyển đổi
 * - Hiển thị số lượng / 300 (giới hạn mỗi ô)
 * - Bán nhanh: Chuột trái = bán 1, Shift + Chuột trái = bán sạch toàn bộ
 * - Rút đồ: Chuột phải = rút 1 vật phẩm ra túi player
 * - Tự động cất đồ (AFK toggle)
 */
public class TuiDoGUI implements Listener {

    private static final String GUI_MAIN = "§0§lTúi Đồ Hư Không";
    private static final String GUI_CATEGORY_PREFIX = "§0§lTúi Đồ: ";
    private final TuiDoManager tuiDoManager;

    public TuiDoGUI(TuiDoManager tuiDoManager) {
        this.tuiDoManager = tuiDoManager;
    }

    // ==========================================
    // TRANG CHÍNH: HIỂN THỊ CÁC NHÓM VẬT PHẨM
    // ==========================================
    public static void open(Player player, TuiDoManager tuiDoManager) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_MAIN);

        // --- THANH THÔNG TIN (Hàng trên cùng) ---
        ItemStack infoItem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§b§lThông Tin Túi Đồ");
        int totalItems = tuiDoManager.getTotalItemCount(player);
        int usedSlots = tuiDoManager.getUsedSlots(player);
        infoMeta.setLore(List.of(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Tổng vật phẩm: §a" + totalItems,
                "§7Số ô đang dùng: §e" + usedSlots,
                "§7Giới hạn mỗi ô: §c" + TuiDoManager.MAX_STACK_SIZE,
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "",
                "§7Nhấp vào các nhóm bên dưới",
                "§7để xem chi tiết vật phẩm."
        ));
        infoItem.setItemMeta(infoMeta);
        inv.setItem(4, infoItem);

        // --- CÁC NHÓM VẬT PHẨM (Hàng giữa) ---
        setCategoryButton(inv, 19, Material.DIAMOND_CHESTPLATE, ItemCategory.GIAP, tuiDoManager, player);
        setCategoryButton(inv, 20, Material.DIAMOND_SWORD, ItemCategory.KIEM, tuiDoManager, player);
        setCategoryButton(inv, 21, Material.TURTLE_EGG, ItemCategory.LINH_THU, tuiDoManager, player);
        setCategoryButton(inv, 22, Material.ENCHANTED_BOOK, ItemCategory.THAN_THONG, tuiDoManager, player);
        setCategoryButton(inv, 23, Material.EMERALD, ItemCategory.TRANG_SUC, tuiDoManager, player);
        setCategoryButton(inv, 24, Material.NETHER_STAR, ItemCategory.PHU_KIEN, tuiDoManager, player);
        setCategoryButton(inv, 25, Material.BREWING_STAND, ItemCategory.DAN_DUOC, tuiDoManager, player);

        // Nhóm Linh Tinh ở giữa hàng dưới
        setCategoryButton(inv, 31, Material.CHEST, ItemCategory.LINH_TINH, tuiDoManager, player);

        // --- THANH ĐIỀU KHIỂN (Hàng cuối) ---
        // Nút Tự Động Cất Đồ
        ItemStack autoBtn = new ItemStack(Material.HOPPER);
        ItemMeta autoMeta = autoBtn.getItemMeta();
        boolean isAuto = tuiDoManager.isAutoPickup(player);
        autoMeta.setDisplayName("§b§lTự Động Cất Đồ (AFK)");
        autoMeta.setLore(List.of(
                "§7Trạng thái: " + (isAuto ? "§a§l✔ BẬT" : "§c§l✘ TẮT"),
                "",
                "§7Khi bật, mọi vật phẩm tu tiên",
                "§7nhặt được sẽ bay thẳng vào túi.",
                "",
                "§e[!] Nhấp để chuyển đổi"
        ));
        autoBtn.setItemMeta(autoMeta);
        inv.setItem(48, autoBtn);

        // Nút Bán Tất Cả
        ItemStack sellAllBtn = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta sellMeta = sellAllBtn.getItemMeta();
        sellMeta.setDisplayName("§e§lBán Tất Cả Rác Phẩm");
        sellMeta.setLore(List.of(
                "§7Dọn sạch toàn bộ đồ trong túi",
                "§7để quy đổi thành Linh Thạch.",
                "",
                "§c⚠ LƯU Ý: Hành động này không",
                "§cthể hoàn tác!",
                "",
                "§c[!] Shift + Chuột Trái để xác nhận!"
        ));
        sellAllBtn.setItemMeta(sellMeta);
        inv.setItem(50, sellAllBtn);

        // Lấp đầy kính trang trí
        fillEmpty(inv);
        player.openInventory(inv);
    }

    /**
     * Tạo nút nhóm vật phẩm trên trang chính
     */
    private static void setCategoryButton(Inventory inv, int slot, Material icon,
                                           ItemCategory category, TuiDoManager mgr, Player player) {
        ItemStack btn = new ItemStack(icon);
        ItemMeta meta = btn.getItemMeta();
        meta.setDisplayName(category.getDisplayName());

        List<Map.Entry<String, StoredItem>> categoryItems = mgr.getItemsByCategory(player, category);
        int totalInCategory = 0;
        for (Map.Entry<String, StoredItem> e : categoryItems) {
            totalInCategory += e.getValue().amount;
        }

        meta.setLore(List.of(
                "§8━━━━━━━━━━━━━",
                "§7Số loại: §f" + categoryItems.size(),
                "§7Tổng số lượng: §f" + totalInCategory,
                "",
                "§e▶ Nhấp để xem chi tiết"
        ));
        btn.setItemMeta(meta);
        inv.setItem(slot, btn);
    }

    // ==========================================
    // TRANG CHI TIẾT: HIỂN THỊ VẬT PHẨM THEO NHÓM
    // ==========================================
    public static void openCategory(Player player, TuiDoManager tuiDoManager, ItemCategory category) {
        String title = GUI_CATEGORY_PREFIX + category.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<Map.Entry<String, StoredItem>> items = tuiDoManager.getItemsByCategory(player, category);
        int slot = 0;

        for (Map.Entry<String, StoredItem> entry : items) {
            if (slot >= 45) break; // Dành 9 slot cuối cho điều khiển

            StoredItem stored = entry.getValue();
            ItemStack displayItem = stored.sample.clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§8━━━━━━━━━━━━━━━━━━━━━");
                lore.add("§7Số lượng: §a" + stored.amount + " §7/ §c" + TuiDoManager.MAX_STACK_SIZE);
                lore.add("§7Giá bán: §e" + TuiDoManager.getSellPrice(stored) + " Linh Thạch §7/cái");
                lore.add("§8━━━━━━━━━━━━━━━━━━━━━");
                lore.add("§a[Chuột Trái] §fBán 1 cái lấy Linh Thạch");
                lore.add("§c[Shift + Trái] §fBán sạch toàn bộ loại này");
                lore.add("§b[Chuột Phải] §fRút 1 cái ra túi đồ");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            inv.setItem(slot, displayItem);
            slot++;
        }

        // --- NÚT QUAY LẠI ---
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName("§c§l◄ Quay Lại");
        backMeta.setLore(List.of("§7Quay về trang chính Túi Đồ"));
        backBtn.setItemMeta(backMeta);
        inv.setItem(49, backBtn);

        // --- THỐNG KÊ NHÓM ---
        ItemStack statsBtn = new ItemStack(Material.PAPER);
        ItemMeta statsMeta = statsBtn.getItemMeta();
        int totalInCat = 0;
        for (Map.Entry<String, StoredItem> e : items) totalInCat += e.getValue().amount;
        statsMeta.setDisplayName("§f§l" + category.getDisplayName());
        statsMeta.setLore(List.of(
                "§7Tổng loại: §a" + items.size(),
                "§7Tổng số lượng: §a" + totalInCat
        ));
        statsBtn.setItemMeta(statsMeta);
        inv.setItem(45, statsBtn);

        fillEmpty(inv);
        player.openInventory(inv);
    }

    // ==========================================
    // XỬ LÝ SỰ KIỆN CLICK
    // ==========================================
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // === TRANG CHÍNH ===
        if (title.equals(GUI_MAIN)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            if (slot >= 54) return;

            // Nút Tự Động Cất Đồ
            if (slot == 48) {
                tuiDoManager.toggleAutoPickup(player);
                open(player, tuiDoManager);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
                return;
            }

            // Nút Bán Tất Cả
            if (slot == 50) {
                if (event.getClick() == ClickType.SHIFT_LEFT) {
                    // Tính tổng tiền bán
                    Map<String, StoredItem> allItems = tuiDoManager.getItems(player);
                    int totalEarned = 0;
                    for (StoredItem si : allItems.values()) {
                        totalEarned += TuiDoManager.getSellPrice(si) * si.amount;
                    }

                    if (totalEarned > 0) {
                        tuiDoManager.clearItems(player);
                        // Cộng Emerald (Linh Thạch) vào túi
                        giveEmerald(player, totalEarned);
                        open(player, tuiDoManager);
                        player.sendMessage("§e§l[Thương Nhân] §fĐã thanh lý toàn bộ! Thu về §a" + totalEarned + " Linh Thạch§f!");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                    } else {
                        player.sendMessage("§c[Thương Nhân] §fTúi đồ trống, không có gì để bán!");
                    }
                } else {
                    player.sendMessage("§c⚠ Bạn phải giữ phím Shift và Click Trái để tránh bấm nhầm!");
                }
                return;
            }

            // Xử lý nhấp vào nhóm vật phẩm
            ItemCategory clickedCategory = getCategoryFromSlot(slot);
            if (clickedCategory != null) {
                openCategory(player, tuiDoManager, clickedCategory);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        // === TRANG CHI TIẾT NHÓM ===
        if (title.startsWith(GUI_CATEGORY_PREFIX)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            if (slot >= 54) return;

            // Nút Quay Lại
            if (slot == 49) {
                open(player, tuiDoManager);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                return;
            }

            // Xử lý Bán / Rút đồ (chỉ cho slot 0-44)
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR && slot < 45) {
                String id = TuiDoManager.generateId(clicked);
                Map<String, StoredItem> playerItems = tuiDoManager.getItems(player);

                if (!playerItems.containsKey(id)) return;
                StoredItem stored = playerItems.get(id);
                int sellPrice = TuiDoManager.getSellPrice(stored);
                ItemCategory currentCat = stored.category;

                // --- BÁN 1 CÁI (Chuột Trái) ---
                if (event.getClick() == ClickType.LEFT) {
                    if (tuiDoManager.removeItem(player, id, 1)) {
                        giveEmerald(player, sellPrice);
                        player.sendMessage("§e+" + sellPrice + " Linh Thạch");
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                        openCategory(player, tuiDoManager, currentCat);
                    }
                }
                // --- BÁN SẠCH LOẠI NÀY (Shift + Chuột Trái) ---
                else if (event.getClick() == ClickType.SHIFT_LEFT) {
                    int amount = stored.amount;
                    int totalPrice = sellPrice * amount;
                    if (tuiDoManager.removeItem(player, id, amount)) {
                        giveEmerald(player, totalPrice);
                        player.sendMessage("§e§l[Thương Nhân] §fĐã bán §c" + amount + " cái§f, thu về §a" + totalPrice + " Linh Thạch§f!");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                        openCategory(player, tuiDoManager, currentCat);
                    }
                }
                // --- RÚT 1 CÁI (Chuột Phải) ---
                else if (event.getClick() == ClickType.RIGHT) {
                    if (tuiDoManager.removeItem(player, id, 1)) {
                        // Lấy bản mẫu gốc không có lore hướng dẫn
                        ItemStack original = playerItems.containsKey(id) ?
                                playerItems.get(id).sample.clone() :
                                new ItemStack(clicked.getType());
                        player.getInventory().addItem(original);
                        player.sendMessage("§a[Túi Đồ] §fĐã rút 1 vật phẩm ra túi đồ.");
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                        openCategory(player, tuiDoManager, currentCat);
                    }
                }
            }
        }
    }

    // ==========================================
    // HÀM HỖ TRỢ
    // ==========================================

    /**
     * Chuyển slot trên trang chính thành nhóm vật phẩm
     */
    private static ItemCategory getCategoryFromSlot(int slot) {
        return switch (slot) {
            case 19 -> ItemCategory.GIAP;
            case 20 -> ItemCategory.KIEM;
            case 21 -> ItemCategory.LINH_THU;
            case 22 -> ItemCategory.THAN_THONG;
            case 23 -> ItemCategory.TRANG_SUC;
            case 24 -> ItemCategory.PHU_KIEN;
            case 25 -> ItemCategory.DAN_DUOC;
            case 31 -> ItemCategory.LINH_TINH;
            default -> null;
        };
    }

    /**
     * Phát Linh Thạch (Emerald) cho người chơi
     */
    private static void giveEmerald(Player player, int amount) {
        while (amount > 0) {
            int stackSize = Math.min(64, amount);
            player.getInventory().addItem(new ItemStack(Material.EMERALD, stackSize));
            amount -= stackSize;
        }
    }

    /**
     * Lấp đầy các ô trống bằng kính trang trí
     */
    private static void fillEmpty(Inventory inv) {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(" ");
        glass.setItemMeta(m);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }
}