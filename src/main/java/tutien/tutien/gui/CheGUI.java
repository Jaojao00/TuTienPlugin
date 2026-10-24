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
import tutien.combat.StatsManager;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.craft.CheManager;
import tutien.quest.NhiemVuManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI Hệ Thống Chế Tác Trang Bị.
 * v2.2 Skyblock Edition:
 *   - Đồng bộ sử dụng Khoáng Thạch ảo từ PlayerDataManager (cũng cho phép dùng Khoáng Thạch đổi tại KhoangThachGUI hoặc đào Generator).
 *   - Chuyển item an toàn qua ItemKeys.giveItemSafe (không mất đồ khi đầy túi).
 *   - Có nút mở nhanh Đổi Khoáng Thạch ngay trong Lò Rèn.
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
        long khoangThach = data.getKhoangThach(player);
        CheManager.CheRecipe[] recipes = CheManager.CheRecipe.values();

        // Info
        setItem(inv, 4, Material.ANVIL, "§e§lLò Rèn [Cấp " + capChe + "]",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Khoáng Thạch hiện có: §e" + khoangThach + " ⛏",
                "§7Tổng lần chế tác: §a" + soLanChe,
                "§7Tỷ lệ phẩm cấp cao: §a+" + (capChe * 3) + "%",
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Cách kiếm Khoáng Thạch:",
                "§7  ▪ Đào đá/quặng tại máy Farm Cobblestone",
                "§7  ▪ Đổi từ Kim Cương, Sắt, Vàng (Ô bên dưới)",
                "§7  ▪ Đổi từ Điểm Bế Quan (/tutien bequan)"
            ));

        // Hiển thị công thức chế tác
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 22};
        for (int i = 0; i < Math.min(recipes.length, slots.length); i++) {
            CheManager.CheRecipe recipe = recipes[i];
            boolean canCraft = khoangThach >= recipe.oreRequired;

            ItemStack item = new ItemStack(recipe.material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((canCraft ? "§a" : "§c") + recipe.displayName);
            List<String> lore = new ArrayList<>();
            lore.add("§7Loại: §e" + recipe.type);
            lore.add("§7Chi phí: " + (canCraft ? "§a" : "§c") + recipe.oreRequired + " §7Khoáng Thạch §8(Có: " + khoangThach + ")");
            lore.add("");
            lore.add("§7Tỷ lệ phẩm cấp:");
            lore.add("§8Phàm §760% §7| §aHạ §715% §7| §bTrung §710%");
            lore.add("§dThượng §77% §7| §6Chí §74% §7| §cLinh §72%");
            lore.add("§5Chân §71.5% §7| §eLinh Khí §70.5%");
            lore.add("");
            if (canCraft) lore.add("§e► Nhấp để chế tác!");
            else lore.add("§c✘ Cần thêm " + (recipe.oreRequired - khoangThach) + " Khoáng Thạch!");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        // Nút mở nhanh Đổi Khoáng Thạch (Slot 47)
        setItem(inv, 47, Material.RAW_GOLD, "§6§l⛏ Đổi Khoáng Thạch",
            Arrays.asList(
                "§7Đổi Kim Cương, Sắt, Vàng, Ngọc Lục Bảo",
                "§7thành Khoáng Thạch để rèn trang bị.",
                "",
                "§e► Nhấp để mở quầy quy đổi!"
            ));

        // Nút nâng cấp lò (Slot 49)
        ItemStack upgradeBtn = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta um = upgradeBtn.getItemMeta();
        int nextCost = 50 + capChe * 50; // Cân bằng theo đơn vị Khoáng Thạch (50 KT = 5000 Kim Cương hoặc đào mỏ)
        um.setDisplayName("§e§lNâng Cấp Lò Rèn");
        um.setLore(Arrays.asList(
            "§7Cấp hiện tại: §e" + capChe,
            "§7Chi phí nâng cấp: " + (khoangThach >= nextCost ? "§a" : "§c") + nextCost + " §7Khoáng Thạch",
            "§7Lợi ích: §a+3% §7tỷ lệ ra phẩm cấp cao",
            "",
            khoangThach >= nextCost ? "§e► Nhấp để nâng cấp!" : "§c✘ Không đủ Khoáng Thạch!"
        ));
        upgradeBtn.setItemMeta(um);
        inv.setItem(49, upgradeBtn);

        // Nút mở Túi Đồ Hư Không (Slot 51)
        setItem(inv, 51, Material.ENDER_CHEST, "§b§lTúi Đồ Hư Không",
            Arrays.asList(
                "§7Mở Túi Đồ Hư Không để cất giữ",
                "§7hoặc thanh lý trang bị dư thừa.",
                "",
                "§e► Nhấp để mở!"
            ));

        // Fill border
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        glass.setItemMeta(gm);
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, glass);

        player.openInventory(inv);
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
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getRawSlot();
        if (slot >= 54) return;

        // Mở Đổi Khoáng Thạch
        if (slot == 47) {
            KhoangThachGUI.open(player, dataManager);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // Mở Túi Đồ Hư Không
        if (slot == 51) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            TuiDoGUI.open(player, plugin.getTuiDoManager());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // Nâng cấp lò
        if (slot == 49) {
            int cap = cheManager.getCapChe(player);
            if (cap >= 15) {
                player.sendMessage("§e[Lò Rèn] §fLò rèn của bạn đã đạt cấp tối đa (Cấp 15)!");
                return;
            }
            int cost = 50 + cap * 50;
            long currentKT = dataManager.getKhoangThach(player);
            if (currentKT < cost) {
                player.sendMessage("§c[Lò Rèn] §fCần §e" + cost + " Khoáng Thạch §fđể nâng cấp! (Hiện có: §e" + currentKT + "§f)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            dataManager.setKhoangThach(player, currentKT - cost);
            cheManager.setCapChe(player, cap + 1);
            player.sendMessage("§6§l[Lò Rèn] §fNâng cấp thành công lên §eCấp " + (cap + 1) + "§f!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            open(player, dataManager, cheManager);
            return;
        }

        // Click vào công thức chế tác
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        CheManager.CheRecipe recipe = null;
        for (CheManager.CheRecipe r : CheManager.CheRecipe.values()) {
            if (r.displayName.equals(name)) {
                recipe = r;
                break;
            }
        }
        if (recipe == null) return;

        long currentKT = dataManager.getKhoangThach(player);
        if (currentKT < recipe.oreRequired) {
            player.sendMessage("§c[Lò Rèn] §fKhông đủ Khoáng Thạch! Cần §e" + recipe.oreRequired + "§f, hiện có §e" + currentKT + "§f.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Trừ Khoáng Thạch
        dataManager.setKhoangThach(player, currentKT - recipe.oreRequired);

        // Lăn tỷ lệ phẩm cấp
        int capBonus = cheManager.getCapChe(player) * 3;
        ItemStack result = CheManager.rollCraft(recipe, capBonus);
        ItemKeys.giveItemSafe(player, result);
        cheManager.addSoLanChe(player, 1);

        // Cập nhật chỉ số nếu rèn Phụ Kiện
        StatsManager.applyStats(player, dataManager);

        String phamCap = result.getItemMeta().getDisplayName();
        player.sendMessage("§6§l[Lò Rèn] §fChế tác thành công: " + phamCap + " §7(-" + recipe.oreRequired + " Khoáng Thạch)");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);

        TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
        if (plugin.getNhiemVuManager() != null) {
            plugin.getNhiemVuManager().addProgress(player, NhiemVuManager.NhiemVu.CHE_TAC, 1);
        }

        open(player, dataManager, cheManager);
    }
}
