package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.LeaderboardManager;
import tutien.core.TuTienPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI Bảng Xếp Hạng — hiển thị Top 10 theo nhiều hạng mục.
 * v2.1
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

        // Header
        setItem(inv, 4, Material.GOLDEN_HELMET, "§6§l🏆 Bảng Xếp Hạng Tu Tiên",
            Arrays.asList("§7Top 10 cường giả trong thiên hạ.", "§7Cập nhật mỗi 5 phút."));

        // === CẢNH GIỚI (Column 1: slots 10-18 vertical) ===
        setItem(inv, 9, Material.NETHER_STAR, "§d§l◈ Cảnh Giới", Arrays.asList("§7Top 10 theo Cảnh Giới"));
        fillCategory(inv, new int[]{10,11,12,13,14,15,16,17,18}, lb.getTopCanhGioi(), "§d", true);

        // === TU VI (Column 2: slots 19-27) ===
        setItem(inv, 27, Material.EXPERIENCE_BOTTLE, "§a§l◈ Tu Vi", Arrays.asList("§7Top 10 theo Tu Vi"));
        fillCategory(inv, new int[]{28,29,30,31,32,33,34,35}, lb.getTopTuVi(), "§a", false);

        // === KHOÁNG THẠCH (Row 3 slots 37-44) ===
        setItem(inv, 36, Material.RAW_GOLD, "§6§l◈ Khoáng Thạch", Arrays.asList("§7Top 10 Khoáng Thạch"));
        fillCategory(inv, new int[]{37,38,39,40,41,42,43,44}, lb.getTopKhoangThach(), "§6", false);

        // === ĐẠO NIỆM (slots 46-50) ===
        setItem(inv, 45, Material.AMETHYST_CLUSTER, "§b§l◈ Đạo Niệm", Arrays.asList("§7Top 10 Đạo Niệm"));
        fillCategory(inv, new int[]{46,47,48,49,50,51,52,53}, lb.getTopDaoNiem(), "§b", false);

        // Fill empty
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta(); gm.setDisplayName(" "); glass.setItemMeta(gm);
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, glass);

        player.openInventory(inv);
    }

    private static void fillCategory(Inventory inv, int[] slots, List<LeaderboardManager.LeaderEntry> entries, String color, boolean showExtra) {
        for (int i = 0; i < slots.length && i < entries.size(); i++) {
            LeaderboardManager.LeaderEntry e = entries.get(i);
            Material head = switch (i) {
                case 0 -> Material.GOLDEN_HELMET;
                case 1 -> Material.IRON_HELMET;
                case 2 -> Material.CHAINMAIL_HELMET;
                default -> Material.LEATHER_HELMET;
            };
            String rank = "§7#" + (i + 1) + " ";
            String valueStr = showExtra ? e.extra() : String.valueOf(e.value());
            setItem(inv, slots[i], head, rank + color + e.playerName(),
                Arrays.asList("§7Giá trị: " + color + valueStr));
        }
    }

    private static void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta(); m.setDisplayName(name); m.setLore(lore); item.setItemMeta(m);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) event.setCancelled(true);
    }
}
