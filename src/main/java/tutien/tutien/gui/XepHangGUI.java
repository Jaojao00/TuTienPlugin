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
import tutien.core.LeaderboardManager;
import tutien.core.TuTienPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * GUI Bảng Xếp Hạng — hiển thị Top 10 theo 5 hạng mục rõ ràng, không bị tràn dòng.
 * v2.2 Skyblock Edition
 */
public class XepHangGUI implements Listener {

    public static final String GUI_TITLE = "§0§l🏆 Bảng Xếp Hạng 🏆";

    public static void open(Player player, TuTienPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        LeaderboardManager lb = plugin.getLeaderboardManager();
        if (lb == null) {
            player.sendMessage("§c[Xếp Hạng] Hệ thống chưa sẵn sàng!");
            return;
        }

        // Cập nhật ngay lập tức từ RAM/Disk để hiển thị chính xác nhất
        lb.refresh();

        setItem(inv, 4, Material.GOLDEN_HELMET, "§6§l🏆 Bảng Xếp Hạng Tu Tiên",
            Arrays.asList(
                "§7Top 8 cường giả dẫn đầu mỗi hạng mục.",
                "§7Hỗ trợ PlaceholderAPI: §e%tutien_canhgioi_1% §7-> §e_10%"
            ));

        // Hàng 1 (Slots 9 -> 17): CẢNH GIỚI
        setItem(inv, 9, Material.NETHER_STAR, "§d§l◈ Top Cảnh Giới", Arrays.asList("§7Xếp hạng theo bậc Cảnh Giới"));
        fillCategory(inv, new int[]{10, 11, 12, 13, 14, 15, 16, 17}, lb.getTopCanhGioi(), "§d", true);

        // Hàng 2 (Slots 18 -> 26): TU VI
        setItem(inv, 18, Material.EXPERIENCE_BOTTLE, "§a§l◈ Top Tu Vi", Arrays.asList("§7Xếp hạng theo điểm Tu Vi"));
        fillCategory(inv, new int[]{19, 20, 21, 22, 23, 24, 25, 26}, lb.getTopTuVi(), "§a", false);

        // Hàng 3 (Slots 27 -> 35): KHOÁNG THẠCH
        setItem(inv, 27, Material.RAW_GOLD, "§6§l◈ Top Khoáng Thạch", Arrays.asList("§7Xếp hạng theo số Khoáng Thạch"));
        fillCategory(inv, new int[]{28, 29, 30, 31, 32, 33, 34, 35}, lb.getTopKhoangThach(), "§6", false);

        // Hàng 4 (Slots 36 -> 44): ĐẠO NIỆM
        setItem(inv, 36, Material.AMETHYST_CLUSTER, "§b§l◈ Top Đạo Niệm", Arrays.asList("§7Xếp hạng theo số Đạo Niệm"));
        fillCategory(inv, new int[]{37, 38, 39, 40, 41, 42, 43, 44}, lb.getTopDaoNiem(), "§b", false);

        // Hàng 5 (Slots 45 -> 53): ĐIỂM BẾ QUAN
        setItem(inv, 45, Material.ENDER_EYE, "§e§l◈ Top Điểm Bế Quan", Arrays.asList("§7Xếp hạng theo Điểm Bế Quan"));
        fillCategory(inv, new int[]{46, 47, 48, 49, 50, 51, 52, 53}, lb.getTopBeQuanDiem(), "§e", false);

        // Fill empty
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        glass.setItemMeta(gm);
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, glass);

        player.openInventory(inv);
    }

    private static void fillCategory(Inventory inv, int[] slots, List<LeaderboardManager.LeaderEntry> entries, String color, boolean showExtra) {
        for (int i = 0; i < slots.length && i < entries.size(); i++) {
            LeaderboardManager.LeaderEntry e = entries.get(i);
            Material head = switch (i) {
                case 0 -> Material.NETHERITE_HELMET;
                case 1 -> Material.DIAMOND_HELMET;
                case 2 -> Material.GOLDEN_HELMET;
                default -> Material.IRON_HELMET;
            };
            String rank = "§e#" + (i + 1) + " ";
            String valueStr = showExtra ? e.extra() : String.valueOf(e.value());
            setItem(inv, slots[i], head, rank + color + e.playerName(),
                Arrays.asList("§7Thành tích: " + color + valueStr));
        }
    }

    private static void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(name);
        m.setLore(lore);
        item.setItemMeta(m);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            }
        }
    }
}
