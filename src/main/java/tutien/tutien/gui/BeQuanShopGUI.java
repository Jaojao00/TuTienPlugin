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
import tutien.core.PlayerDataManager;

import java.util.Arrays;
import java.util.List;

/**
 * Cửa hàng đổi Điểm Bế Quan lấy vật phẩm quý.
 * SPRINT 2: Hệ thống Bế Quan hoàn chỉnh.
 */
public class BeQuanShopGUI implements Listener {

    public static final String GUI_TITLE = "§5§l⚑ Cửa Hàng Bế Quan ⚑";
    private final PlayerDataManager dataManager;

    public BeQuanShopGUI(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public static void open(Player player, PlayerDataManager data) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        long diem = data.getBeQuanDiem(player);

        // Info slot
        setItem(inv, 4, Material.ENDER_EYE, "§6§lĐiểm Bế Quan Của Bạn",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━",
                "§7Điểm hiện có: §e" + diem,
                "§8━━━━━━━━━━━━━━━━━",
                "§7Tọa thiền để tích lũy điểm.",
                "§71 phút tọa thiền = §e1 điểm§7."
            ));

        // Các vật phẩm đổi
        addShopItem(inv, 10, Material.NETHER_STAR,      "§bNguyên Anh Đan",    50,  diem, Arrays.asList("§7Đan dược đột phá Nguyên Anh", "§7Giá: §e50 điểm"));
        addShopItem(inv, 11, Material.BEACON,            "§dHóa Thần Đan",     120,  diem, Arrays.asList("§7Đan dược đột phá Hóa Thần",   "§7Giá: §e120 điểm"));
        addShopItem(inv, 12, Material.END_CRYSTAL,       "§cLuyện Hư Đan",     300,  diem, Arrays.asList("§7Đan dược đột phá Luyện Hư",   "§7Giá: §e300 điểm"));
        addShopItem(inv, 13, Material.DRAGON_EGG,        "§6Hợp Thể Đan",      600,  diem, Arrays.asList("§7Đan dược đột phá Hợp Thể",    "§7Giá: §e600 điểm"));
        addShopItem(inv, 14, Material.TOTEM_OF_UNDYING,  "§eTâm Pháp Đặc Biệt",100,  diem, Arrays.asList("§7Tâm pháp bí ẩn tăng tu vi",   "§7Giá: §e100 điểm"));
        addShopItem(inv, 15, Material.DIAMOND,           "§bLinh Thạch x100",   20,   diem, Arrays.asList("§7Tiền tệ linh khí",             "§7Giá: §e20 điểm"));
        addShopItem(inv, 16, Material.EMERALD,           "§aHoàng Long Đan",    30,   diem, Arrays.asList("§7Tăng 500 Tu Vi",               "§7Giá: §e30 điểm"));

        addShopItem(inv, 19, Material.EXPERIENCE_BOTTLE, "§eTuVi Đan x10",     15,   diem, Arrays.asList("§7Tăng 100 Tu Vi mỗi viên",     "§7Giá: §e15 điểm"));
        addShopItem(inv, 20, Material.GOLDEN_APPLE,      "§6Đại Hoàn Đan",     40,   diem, Arrays.asList("§7Hồi phục hoàn toàn Linh Lực",  "§7Giá: §e40 điểm"));
        addShopItem(inv, 21, Material.FIREWORK_STAR,     "§cThần Thông Phù",   80,   diem, Arrays.asList("§7Kích hoạt thần thông đặc biệt", "§7Giá: §e80 điểm"));

        // Fill border
        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }

        player.openInventory(inv);
    }

    private static void addShopItem(Inventory inv, int slot, Material mat, String name, long cost, long playerDiem, List<String> loreBase) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((playerDiem >= cost ? "§a" : "§c") + name);
        List<String> lore = new java.util.ArrayList<>(loreBase);
        lore.add("");
        if (playerDiem >= cost) {
            lore.add("§aNhấp để mua!");
        } else {
            lore.add("§cCần thêm §e" + (cost - playerDiem) + "§c điểm!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private static void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        long diem = dataManager.getBeQuanDiem(player);
        long cost = 0;
        ItemStack reward = null;

        switch (name) {
            case "Nguyên Anh Đan"    -> { cost = 50;  reward = createDan(Material.NETHER_STAR,      "Nguyên Anh Đan",    "Đan dược đột phá Nguyên Anh"); }
            case "Hóa Thần Đan"      -> { cost = 120; reward = createDan(Material.BEACON,            "Hóa Thần Đan",     "Đan dược đột phá Hóa Thần"); }
            case "Luyện Hư Đan"      -> { cost = 300; reward = createDan(Material.END_CRYSTAL,       "Luyện Hư Đan",     "Đan dược đột phá Luyện Hư"); }
            case "Hợp Thể Đan"       -> { cost = 600; reward = createDan(Material.DRAGON_EGG,        "Hợp Thể Đan",      "Đan dược đột phá Hợp Thể"); }
            case "Linh Thạch x100"   -> { cost = 20;  reward = new ItemStack(Material.DIAMOND, 100); }
            case "Hoàng Long Đan"    -> { cost = 30;  reward = createDan(Material.EMERALD,           "Hoàng Long Đan",   "Tăng 500 Tu Vi"); }
            case "TuVi Đan x10"      -> { cost = 15;  reward = new ItemStack(Material.EXPERIENCE_BOTTLE, 10); }
            case "Đại Hoàn Đan"      -> { cost = 40;  reward = createDan(Material.GOLDEN_APPLE,      "Đại Hoàn Đan",    "Hồi phục Linh Lực"); }
            case "Thần Thông Phù"    -> { cost = 80;  reward = createDan(Material.FIREWORK_STAR,     "Thần Thông Phù",  "Kích hoạt thần thông"); }
            default -> { return; }
        }

        if (diem < cost) {
            player.sendMessage("§c[Bế Quan] §fKhông đủ điểm! Cần §e" + cost + "§f điểm.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        dataManager.setBeQuanDiem(player, diem - cost);
        if (reward != null) {
            player.getInventory().addItem(reward);
        }
        player.sendMessage("§a§l[Bế Quan] §fĐã đổi §b" + name + "§f! Còn §e" + dataManager.getBeQuanDiem(player) + "§f điểm.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        open(player, dataManager);
    }

    private ItemStack createDan(Material mat, String name, String desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b" + name);
        meta.setLore(Arrays.asList("§7" + desc, "", "§e[Chuột Phải] §fđể sử dụng"));
        item.setItemMeta(meta);
        return item;
    }
}
