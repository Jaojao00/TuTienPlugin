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
import tutien.quest.NhiemVuManager;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI Nhiệm Vụ Hàng Ngày.
 * SPRINT 3: Daily Quest UI.
 */
public class NhiemVuGUI implements Listener {

    public static final String GUI_TITLE = "§2§l📜 Nhiệm Vụ Hàng Ngày 📜";
    private final NhiemVuManager nhiemVuManager;

    public NhiemVuGUI(NhiemVuManager manager) {
        this.nhiemVuManager = manager;
    }

    public static void open(Player player, NhiemVuManager manager) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Header
        setItem(inv, 4, Material.PAPER, "§a§lNhiệm Vụ Hàng Ngày",
            List.of("§7Hoàn thành để nhận Điểm Bế Quan + Tu Vi!", "§7Reset lúc §e00:00 §7mỗi ngày."));

        // Hiển thị 5 nhiệm vụ
        NhiemVuManager.NhiemVu[] allNV = NhiemVuManager.NhiemVu.values();
        int[] slots = {10, 11, 12, 13, 14};
        Material[] icons = {Material.CLOCK, Material.BREWING_STAND, Material.ANVIL,
                            Material.BRICKS, Material.BONE};

        for (int i = 0; i < allNV.length && i < slots.length; i++) {
            NhiemVuManager.NhiemVu nv = allNV[i];
            boolean done = manager.isCompleted(player, nv);
            int prog = manager.getProgress(player, nv);

            ItemStack item = new ItemStack(done ? Material.LIME_DYE : icons[i]);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((done ? "§a✔ " : "§e") + nv.tenHienThi);

            List<String> lore = new ArrayList<>();
            lore.add(nv.moTa);
            lore.add("");
            // Progress bar
            int barFilled = (int)(((double)prog / nv.mucTieu) * 10);
            StringBuilder bar = new StringBuilder("§7[");
            for (int b = 0; b < 10; b++) bar.append(b < barFilled ? "§a█" : "§8█");
            bar.append("§7] §f").append(prog).append("/").append(nv.mucTieu);
            lore.add(bar.toString());
            lore.add("");
            lore.add("§7Phần thưởng: §e" + (nv.rewardMultiplier * 50) + " §7BQ Điểm + §e" + (nv.rewardMultiplier * 200) + " §7Tu Vi");
            if (done) lore.add("§a✔ Đã hoàn thành!");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        // Fill border
        ItemStack glass = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta(); gm.setDisplayName(" "); glass.setItemMeta(gm);
        for (int i = 0; i < 27; i++) if (inv.getItem(i) == null) inv.setItem(i, glass);

        player.openInventory(inv);
    }

    private static void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta(); m.setDisplayName(name); m.setLore(lore); item.setItemMeta(m);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
    }
}
