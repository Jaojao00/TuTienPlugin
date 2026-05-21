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
import tutien.craft.CheManager;

import java.util.Arrays;
import java.util.List;

/**
 * GUI Hệ Thống Chế Tác Trang Bị.
 * SPRINT 3: 8 phẩm cấp từ Phàm phẩm đến Linh Khí.
 */
public class CheGUI implements Listener {

    public static final String GUI_TITLE = "§8⚒ §6§lLò Rèn Linh Khí §8⚒";
    private final PlayerDataManager dataManager;
    private final CheManager cheManager;

    public CheGUI(PlayerDataManager dataManager, CheManager cheManager) {
        this.dataManager = dataManager;
        this.cheManager = cheManager;
    }

    public static void open(Player player, PlayerDataManager data, CheManager che) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        int capChe = che.getCapChe(player);
        int soLanChe = che.getSoLanChe(player);
        CheManager.CheRecipe[] recipes = CheManager.CheRecipe.values();

        // Info
        setItem(inv, 4, Material.ANVIL, "§e§lLò Rèn [Cấp " + capChe + "]",
            Arrays.asList(
                "§7Tổng lần chế: §a" + soLanChe,
                "§7Tỷ lệ phẩm cấp cao hơn: §a+" + (capChe * 3) + "%",
                "",
                "§7Chế tác trang bị từ Khoáng Thạch.",
                "§7Phẩm cấp: Phàm → Hạ → Trung → Thượng →",
                "§7Chí Bảo → Linh Bảo → Chân Bảo → Linh Khí"
            ));

        // Hiển thị công thức chế tác
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25};
        for (int i = 0; i < Math.min(recipes.length, slots.length); i++) {
            CheManager.CheRecipe recipe = recipes[i];
            int hasOre = countMaterial(player, Material.IRON_INGOT) + countMaterial(player, Material.GOLD_INGOT)
                       + countMaterial(player, Material.DIAMOND) + countMaterial(player, Material.NETHERITE_INGOT);
            boolean canCraft = hasOre >= recipe.oreRequired;

            ItemStack item = new ItemStack(recipe.material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((canCraft ? "§a" : "§c") + recipe.displayName);
            List<String> lore = new java.util.ArrayList<>();
            lore.add("§7Loại: §e" + recipe.type);
            lore.add("§7Nguyên liệu: §c" + recipe.oreRequired + " §7Khoáng Thạch (bất kỳ)");
            lore.add("");
            lore.add("§7Tỷ lệ phẩm cấp:");
            lore.add("§8Phàm §760% §7| §aHạ §715% §7| §bTrung §710%");
            lore.add("§dThượng §77% §7| §6Chí §74% §7| §cLinh §72%");
            lore.add("§5Chân §71.5% §7| §eLinh Khí §70.5%");
            lore.add("");
            if (canCraft) lore.add("§eNhấp để chế tác!");
            else lore.add("§cCần " + recipe.oreRequired + " Khoáng Thạch!");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        // Nút nâng cấp lò
        ItemStack upgradeBtn = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta um = upgradeBtn.getItemMeta();
        int nextCost = 500 + capChe * 500;
        um.setDisplayName("§e§lNâng Cấp Lò Rèn");
        um.setLore(Arrays.asList(
            "§7Cấp hiện tại: §e" + capChe,
            "§7Chi phí: §c" + nextCost + " §7Khoáng Thạch",
            "§7Lợi ích: §a+3% §7tỷ lệ phẩm cấp cao",
            "", "§eNhấp để nâng cấp"
        ));
        upgradeBtn.setItemMeta(um);
        inv.setItem(49, upgradeBtn);

        // Fill border
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta(); gm.setDisplayName(" "); glass.setItemMeta(gm);
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, glass);

        player.openInventory(inv);
    }

    private static void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta(); m.setDisplayName(name); m.setLore(lore); item.setItemMeta(m);
        inv.setItem(slot, item);
    }

    private static int countMaterial(Player player, Material mat) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents())
            if (is != null && is.getType() == mat) count += is.getAmount();
        return count;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getRawSlot();
        if (slot == 49) {
            // Nâng cấp lò
            int cap = cheManager.getCapChe(player);
            int cost = 500 + cap * 500;
            // Kiểm tra khoáng thạch (dùng diamond làm đại diện)
            int totalOre = countMaterial(player, Material.DIAMOND);
            if (totalOre < cost) {
                player.sendMessage("§c[Lò Rèn] §fCần §e" + cost + " §7Kim Cương để nâng cấp!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            removeMaterial(player, Material.DIAMOND, cost);
            cheManager.setCapChe(player, cap + 1);
            player.sendMessage("§6§l[Lò Rèn] §fNâng cấp lên §eCấp " + (cap + 1) + "§f!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            open(player, dataManager, cheManager);
            return;
        }

        // Click vào công thức chế tác
        if (!clicked.getItemMeta().getDisplayName().startsWith("§a")) return;
        String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        CheManager.CheRecipe recipe = null;
        for (CheManager.CheRecipe r : CheManager.CheRecipe.values()) {
            if (r.displayName.equals(name)) { recipe = r; break; }
        }
        if (recipe == null) return;

        // Kiểm tra và trừ nguyên liệu
        if (!removeAnyOre(player, recipe.oreRequired)) {
            player.sendMessage("§c[Lò Rèn] §fKhông đủ nguyên liệu!");
            return;
        }

        // Lăn tỷ lệ phẩm cấp
        int capBonus = cheManager.getCapChe(player) * 3;
        ItemStack result = CheManager.rollCraft(recipe, capBonus);
        player.getInventory().addItem(result);
        cheManager.addSoLanChe(player, 1);
        String phamCap = result.getItemMeta().getDisplayName();
        player.sendMessage("§6§l[Lò Rèn] §fChế tác thành công: " + phamCap);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        
        // Thêm tiến độ nhiệm vụ
        tutien.core.TuTienPlugin plugin = tutien.core.TuTienPlugin.getPlugin(tutien.core.TuTienPlugin.class);
        if (plugin.getNhiemVuManager() != null) {
            plugin.getNhiemVuManager().addProgress(player, tutien.quest.NhiemVuManager.NhiemVu.CHE_TAC, 1);
        }
        
        open(player, dataManager, cheManager);
    }

    private static boolean removeAnyOre(Player player, int needed) {
        Material[] ores = {Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.NETHERITE_INGOT};
        int total = 0;
        for (Material ore : ores) total += countMaterial(player, ore);
        if (total < needed) return false;
        // Trừ theo thứ tự từ thấp đến cao
        int remaining = needed;
        for (Material ore : ores) {
            if (remaining <= 0) break;
            int have = countMaterial(player, ore);
            int take = Math.min(have, remaining);
            removeMaterial(player, ore, take);
            remaining -= take;
        }
        return true;
    }

    private static void removeMaterial(Player player, Material mat, int amount) {
        int toRemove = amount;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is == null || is.getType() != mat) continue;
            if (is.getAmount() > toRemove) { is.setAmount(is.getAmount() - toRemove); return; }
            else { toRemove -= is.getAmount(); is.setAmount(0); }
            if (toRemove <= 0) return;
        }
    }
}
