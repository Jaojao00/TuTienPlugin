package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cửa hàng đổi Điểm Bế Quan lấy vật phẩm quý.
 * v2.2 Skyblock Edition:
 * - Gắn PersistentDataContainer (ItemKeys) cho mọi Đan Dược để Chuột Phải ăn được và dùng để Đột Phá.
 * - Thêm mục đổi Điểm Bế Quan lấy Đạo Niệm và Khoáng Thạch đúng như hướng dẫn Gacha.
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

        // Hàng 1: Đan Đột Phá & Tâm Pháp
        addShopItem(inv, 10, Material.NETHER_STAR,      "Nguyên Anh Đan",     50,  diem, Arrays.asList("§7Đan dược đột phá Nguyên Anh", "§7Giá: §e50 điểm"));
        addShopItem(inv, 11, Material.MAGMA_CREAM,      "Hóa Thần Đan",      120,  diem, Arrays.asList("§7Đan dược đột phá Hóa Thần",   "§7Giá: §e120 điểm"));
        addShopItem(inv, 12, Material.END_CRYSTAL,      "Luyện Hư Đan",      300,  diem, Arrays.asList("§7Đan dược đột phá Luyện Hư",   "§7Giá: §e300 điểm"));
        addShopItem(inv, 13, Material.DRAGON_BREATH,    "Hợp Thể Đan",       600,  diem, Arrays.asList("§7Đan dược đột phá Hợp Thể",    "§7Giá: §e600 điểm"));
        addShopItem(inv, 14, Material.BEACON,           "Đại Thừa Đan",     1000,  diem, Arrays.asList("§7Đan dược đột phá Đại Thừa",   "§7Giá: §e1000 điểm"));
        addShopItem(inv, 15, Material.TOTEM_OF_UNDYING, "Độ Kiếp Đan",      1800,  diem, Arrays.asList("§7Đan dược đột phá Độ Kiếp",    "§7Giá: §e1800 điểm"));
        addShopItem(inv, 16, Material.EMERALD,          "Hoàng Long Đan",     30,  diem, Arrays.asList("§7Tăng 500 Tu Vi & +0.5 DMG",   "§7Giá: §e30 điểm"));

        // Hàng 2: Đạo Niệm, Khoáng Thạch & Vật phẩm hỗ trợ
        addShopItem(inv, 19, Material.AMETHYST_CLUSTER, "Đạo Niệm x10",      100,  diem, Arrays.asList("§7Cộng trực tiếp §b+10 Đạo Niệm", "§7Dùng để Triệu Hồi Linh Thú", "§7Giá: §e100 điểm"));
        addShopItem(inv, 20, Material.RAW_GOLD,         "Khoáng Thạch x10",   80,  diem, Arrays.asList("§7Cộng trực tiếp §6+10 Khoáng Thạch", "§7Dùng cho Lò Rèn Linh Khí", "§7Giá: §e80 điểm"));
        addShopItem(inv, 21, Material.EXPERIENCE_BOTTLE,"TuVi Đan x5",        15,  diem, Arrays.asList("§7Tăng 200 Tu Vi mỗi viên",     "§7Giá: §e15 điểm"));
        addShopItem(inv, 22, Material.GOLDEN_APPLE,     "Đại Hoàn Đan",       40,  diem, Arrays.asList("§7Hồi phục 100% Linh Lực & +2 HP", "§7Giá: §e40 điểm"));
        addShopItem(inv, 23, Material.ENCHANTED_BOOK,   "Tâm Pháp Đặc Biệt", 100,  diem, Arrays.asList("§7Lĩnh ngộ tâm pháp (+1500 Tu Vi)", "§7Giá: §e100 điểm"));
        addShopItem(inv, 24, Material.FIREWORK_STAR,    "Thần Thông Phù",     80,  diem, Arrays.asList("§7Tăng vĩnh viễn +1.0 DMG & +3.0 HP", "§7Giá: §e80 điểm"));

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
        List<String> lore = new ArrayList<>(loreBase);
        lore.add("");
        if (playerDiem >= cost) {
            lore.add("§aNhấp để đổi!");
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

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        long diem = dataManager.getBeQuanDiem(player);
        long cost;
        ItemStack reward = null;
        long addDaoNiem = 0;
        long addKhoangThach = 0;

        switch (name) {
            case "Nguyên Anh Đan"    -> { cost = 50;   reward = createBreakthroughOrCustomPill("NGUYEN_ANH_DAN", Material.NETHER_STAR, "Nguyên Anh Đan", "Đan dược đột phá Nguyên Anh", 1.2, 4.5, 100, 0); }
            case "Hóa Thần Đan"      -> { cost = 120;  reward = createBreakthroughOrCustomPill("HOA_THAN_DAN", Material.MAGMA_CREAM, "Hóa Thần Đan", "Đan dược đột phá Hóa Thần", 1.8, 6.0, 150, 0); }
            case "Luyện Hư Đan"      -> { cost = 300;  reward = createBreakthroughOrCustomPill("LUYEN_HU_DAN", Material.END_CRYSTAL, "Luyện Hư Đan", "Đan dược đột phá Luyện Hư", 2.2, 8.0, 220, 0); }
            case "Hợp Thể Đan"       -> { cost = 600;  reward = createBreakthroughOrCustomPill("HOP_THE_DAN", Material.DRAGON_BREATH, "Hợp Thể Đan", "Đan dược đột phá Hợp Thể", 2.8, 10.0, 300, 0); }
            case "Đại Thừa Đan"      -> { cost = 1000; reward = createBreakthroughOrCustomPill("DAI_THUA_DAN", Material.BEACON, "Đại Thừa Đan", "Đan dược đột phá Đại Thừa", 3.5, 14.0, 400, 0); }
            case "Độ Kiếp Đan"       -> { cost = 1800; reward = createBreakthroughOrCustomPill("DO_KIEP_DAN", Material.TOTEM_OF_UNDYING, "Độ Kiếp Đan", "Đan dược đột phá Độ Kiếp", 4.5, 18.0, 550, 0); }
            case "Hoàng Long Đan"    -> { cost = 30;   reward = createBreakthroughOrCustomPill("HOANG_LONG_DAN", Material.EMERALD, "Hoàng Long Đan", "Tăng 500 Tu Vi & +0.5 Sát thương", 0.5, 1.0, 500, 0); }
            case "Đạo Niệm x10"      -> { cost = 100;  addDaoNiem = 10; }
            case "Khoáng Thạch x10"  -> { cost = 80;   addKhoangThach = 10; }
            case "TuVi Đan x5"       -> {
                cost = 15;
                reward = createBreakthroughOrCustomPill("TUVI_DAN", Material.EXPERIENCE_BOTTLE, "Tu Vi Đan", "Tăng 200 Tu Vi mỗi viên", 0.0, 0.0, 200, 0);
                reward.setAmount(5);
            }
            case "Đại Hoàn Đan"      -> { cost = 40;   reward = createBreakthroughOrCustomPill("DAI_HOAN_DAN", Material.GOLDEN_APPLE, "Đại Hoàn Đan", "Hồi phục 100% Linh Lực & +2.0 HP", 0.0, 2.0, 50, 999999); }
            case "Tâm Pháp Đặc Biệt" -> { cost = 100;  reward = createBreakthroughOrCustomPill("TAM_PHAP_DAC_BIET", Material.ENCHANTED_BOOK, "Tâm Pháp Đặc Biệt", "Lĩnh ngộ tâm pháp (+1500 Tu Vi)", 0.5, 2.0, 1500, 0); }
            case "Thần Thông Phù"    -> { cost = 80;   reward = createBreakthroughOrCustomPill("THAN_THONG_PHU", Material.FIREWORK_STAR, "Thần Thông Phù", "Tăng vĩnh viễn +1.0 DMG & +3.0 HP", 1.0, 3.0, 100, 0); }
            default -> { return; }
        }

        if (diem < cost) {
            player.sendMessage("§c[Bế Quan] §fKhông đủ điểm! Cần §e" + cost + "§f điểm.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        dataManager.setBeQuanDiem(player, diem - cost);
        if (addDaoNiem > 0) {
            dataManager.addDaoNiem(player, addDaoNiem);
        }
        if (addKhoangThach > 0) {
            dataManager.addKhoangThach(player, addKhoangThach);
        }
        if (reward != null) {
            ItemKeys.giveItemSafe(player, reward);
        }
        player.sendMessage("§a§l[Bế Quan] §fĐã đổi §b" + name + "§f! Còn §e" + dataManager.getBeQuanDiem(player) + "§f điểm.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        open(player, dataManager);
    }

    private ItemStack createBreakthroughOrCustomPill(String key, Material mat, String name, String desc, double dmg, double hp, int tuVi, int mana) {
        LuyenDanGUI.DanDuocData fromConfig = LuyenDanGUI.findPillByKey(key);
        if (fromConfig != null) {
            return LuyenDanGUI.createPillItem(fromConfig);
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b" + name);
        meta.setLore(Arrays.asList(
                "§7" + desc,
                "",
                "§7Sát thương: §c+" + String.format("%.1f", dmg),
                "§7Sinh lực: §a+" + String.format("%.1f", hp),
                "§7Tu vi: §e+" + tuVi,
                "",
                "§e[Chuột Phải] §fđể sử dụng"
        ));
        ItemKeys.markPill(meta, key, dmg, hp, tuVi, mana);
        item.setItemMeta(meta);
        return item;
    }
}
