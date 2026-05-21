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
import tutien.core.TuTienPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * GUI Đổi nguyên liệu lấy Khoáng Thạch.
 * v2.1: Khoáng Thạch là tiền tệ ảo dùng cho Chế Tác và các hệ thống khác.
 */
public class KhoangThachGUI implements Listener {

    public static final String GUI_TITLE = "§8§l⛏ Đổi Khoáng Thạch ⛏";
    private final PlayerDataManager dataManager;

    public KhoangThachGUI(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public static void open(Player player, PlayerDataManager data) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        long kt = data.getKhoangThach(player);

        // Info
        setItem(inv, 4, Material.RAW_GOLD, "§6§lKhoáng Thạch Của Bạn",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━",
                "§7Số lượng: §e" + kt + " §7Khoáng Thạch",
                "§8━━━━━━━━━━━━━━━━━━━",
                "§7Khoáng Thạch dùng để Chế Tác,",
                "§7Nâng Cấp Lò Rèn, và trao đổi."
            ));

        // Exchange items
        addExchangeItem(inv, 10, Material.DIAMOND,          "§bKim Cương",        100, kt, "100 Kim Cương = 1 Khoáng Thạch");
        addExchangeItem(inv, 11, Material.EMERALD,          "§aNgọc Lục Bảo",    200, kt, "200 Ngọc Lục Bảo = 1 Khoáng Thạch");
        addExchangeItem(inv, 12, Material.IRON_INGOT,       "§fThỏi Sắt",        500, kt, "500 Thỏi Sắt = 1 Khoáng Thạch");
        addExchangeItem(inv, 13, Material.GOLD_INGOT,       "§6Thỏi Vàng",       300, kt, "300 Thỏi Vàng = 1 Khoáng Thạch");
        addExchangeItem(inv, 14, Material.NETHERITE_INGOT,  "§4Netherite",         50, kt, "50 Netherite = 1 Khoáng Thạch");
        addExchangeItem(inv, 15, Material.COPPER_INGOT,     "§cThỏi Đồng",       800, kt, "800 Thỏi Đồng = 1 Khoáng Thạch");
        addExchangeItem(inv, 16, Material.AMETHYST_SHARD,   "§dThạch Anh Tím",   150, kt, "150 Mảnh Thạch Anh = 1 Khoáng Thạch");

        // Fill
        ItemStack glass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta(); gm.setDisplayName(" "); glass.setItemMeta(gm);
        for (int i = 0; i < 27; i++) if (inv.getItem(i) == null) inv.setItem(i, glass);

        player.openInventory(inv);
    }

    private static void addExchangeItem(Inventory inv, int slot, Material mat, String name, int cost, long playerKT, String desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + name);
        meta.setLore(Arrays.asList(
            "§7" + desc,
            "",
            "§7Nhấp §e1 lần §7= đổi §a1 §7Khoáng Thạch",
            "§7Shift-click §7= đổi §a10 §7Khoáng Thạch",
            "",
            "§7Khoáng Thạch: §e" + playerKT
        ));
        item.setItemMeta(meta);
        inv.setItem(slot, item);
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
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        if (clicked.getType().name().contains("STAINED_GLASS_PANE")) return;
        if (clicked.getType() == Material.RAW_GOLD) return; // info slot

        int exchangeCount = event.getClick().isShiftClick() ? 10 : 1;

        Material mat = clicked.getType();
        int costPer = switch (mat) {
            case DIAMOND -> 100;
            case EMERALD -> 200;
            case IRON_INGOT -> 500;
            case GOLD_INGOT -> 300;
            case NETHERITE_INGOT -> 50;
            case COPPER_INGOT -> 800;
            case AMETHYST_SHARD -> 150;
            default -> 0;
        };
        if (costPer == 0) return;

        int totalCost = costPer * exchangeCount;
        int playerHas = countMaterial(player, mat);

        if (playerHas < totalCost) {
            player.sendMessage("§c[Khoáng Thạch] §fKhông đủ nguyên liệu! Cần §e" + totalCost + "§f, có §e" + playerHas + "§f.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        removeMaterial(player, mat, totalCost);
        dataManager.addKhoangThach(player, exchangeCount);

        player.sendMessage("§6§l[Khoáng Thạch] §fĐã đổi §e" + exchangeCount + " §fKhoáng Thạch! (§7-" + totalCost + " " + getVietnameseName(mat) + "§f)");
        player.sendMessage("§7Tổng Khoáng Thạch: §e" + dataManager.getKhoangThach(player));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
        open(player, dataManager);
    }

    private static String getVietnameseName(Material mat) {
        return switch (mat) {
            case DIAMOND -> "Kim Cương";
            case EMERALD -> "Ngọc Lục Bảo";
            case IRON_INGOT -> "Thỏi Sắt";
            case GOLD_INGOT -> "Thỏi Vàng";
            case NETHERITE_INGOT -> "Netherite";
            case COPPER_INGOT -> "Thỏi Đồng";
            case AMETHYST_SHARD -> "Thạch Anh Tím";
            default -> mat.name();
        };
    }

    private static int countMaterial(Player player, Material mat) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents())
            if (is != null && is.getType() == mat) count += is.getAmount();
        return count;
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
