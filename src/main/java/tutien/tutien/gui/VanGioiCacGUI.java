package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.EconomyManager;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.util.*;

/**
 * Vạn Giới Các - Cửa hàng Tu Tiên (DATA-DRIVEN từ VanGioiCacGUI.yml)
 * Hỗ trợ: Vault Economy, phân khu nguyên liệu + đan dược, giới hạn cảnh giới.
 */
public class VanGioiCacGUI implements Listener {

    private static FileConfiguration shopConfig;
    private static String MAIN_TITLE;

    /**
     * Nạp cấu hình từ file VanGioiCacGUI.yml
     */
    public static void loadConfig(TuTienPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "VanGioiCacGUI.yml");
        if (!file.exists()) plugin.saveResource("VanGioiCacGUI.yml", false);
        shopConfig = YamlConfiguration.loadConfiguration(file);
        MAIN_TITLE = shopConfig.getString("MainTitle", "§0§l⛩ Vạn Giới Các ⛩");
    }

    /**
     * Mở menu chính - chọn phân khu
     */
    public static void open(Player player) {
        if (shopConfig == null) {
            player.sendMessage("§c[Vạn Giới Các] Cấu hình chưa được nạp!");
            return;
        }

        ConfigurationSection categories = shopConfig.getConfigurationSection("PhanKhu");
        if (categories == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);

        for (String key : categories.getKeys(false)) {
            ConfigurationSection cat = categories.getConfigurationSection(key);
            if (cat == null) continue;

            int slot = cat.getInt("Slot", 0);
            Material icon = Material.matchMaterial(cat.getString("Icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(cat.getString("IconName", "§f" + key));
            meta.setLore(cat.getStringList("IconLore"));
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }

        fillEmpty(inv);
        player.openInventory(inv);
    }

    /**
     * Mở cửa hàng con theo phân khu
     */
    public static void openSubShop(Player player, String categoryKey) {
        ConfigurationSection cat = shopConfig.getConfigurationSection("PhanKhu." + categoryKey);
        if (cat == null) return;

        String title = cat.getString("Title", "§0VGC: " + categoryKey);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Nút quay lại
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§c§lQuay Lại");
        back.setItemMeta(bm);
        inv.setItem(49, back);

        // Hiển thị số dư
        TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
        EconomyManager eco = plugin.getEconomyManager();
        ItemStack balanceItem = new ItemStack(Material.SUNFLOWER);
        ItemMeta balMeta = balanceItem.getItemMeta();
        balMeta.setDisplayName("§6§lSố Dư Của Bạn");
        if (eco.isVaultEnabled()) {
            balMeta.setLore(List.of("§7Tài khoản: §a" + eco.formatMoney(eco.getBalance(player))));
        } else {
            balMeta.setLore(List.of("§cVault chưa kết nối!"));
        }
        balanceItem.setItemMeta(balMeta);
        inv.setItem(45, balanceItem);

        // Đổ vật phẩm
        ConfigurationSection items = cat.getConfigurationSection("Items");
        if (items != null) {
            int slotIndex = 0;
            int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};

            for (String itemKey : items.getKeys(false)) {
                if (slotIndex >= slots.length) break;
                ConfigurationSection itemSec = items.getConfigurationSection(itemKey);
                if (itemSec == null) continue;

                Material mat = Material.matchMaterial(itemSec.getString("Material", "STONE"));
                if (mat == null) mat = Material.STONE;

                int price = itemSec.getInt("Price", 0);
                int amount = itemSec.getInt("Amount", 1);
                String name = itemSec.getString("Name", "§f" + itemKey);
                String canhGioiStr = itemSec.getString("CanhGioiMin", "PHAM_NHAN");
                String currencyName = eco.isVaultEnabled() ? eco.getCurrencyName() : "Linh Thạch";

                ItemStack shopItem = new ItemStack(mat, amount);
                ItemMeta meta = shopItem.getItemMeta();
                meta.setDisplayName(name);

                List<String> lore = new ArrayList<>(itemSec.getStringList("Lore"));
                lore.add("");
                lore.add("§7Giá: §a" + price + " " + currencyName);
                lore.add("§7Số lượng: §f" + amount);
                try {
                    CanhGioi req = CanhGioi.valueOf(canhGioiStr);
                    lore.add("§7Cảnh giới: §e" + req.getTenHienThi() + "+");
                } catch (Exception e) {
                    lore.add("§7Cảnh giới: §aPhàm Nhân+");
                }
                lore.add("");
                lore.add("§e[!] Nhấp để mua");

                meta.setLore(lore);
                shopItem.setItemMeta(meta);
                inv.setItem(slots[slotIndex], shopItem);
                slotIndex++;
            }
        }

        fillEmpty(inv);
        player.openInventory(inv);
    }

    /**
     * Tìm key phân khu từ tên icon được click
     */
    private static String findCategoryKeyByIconName(String iconName) {
        ConfigurationSection categories = shopConfig.getConfigurationSection("PhanKhu");
        if (categories == null) return null;
        for (String key : categories.getKeys(false)) {
            String name = categories.getString(key + ".IconName", "");
            if (name.equals(iconName)) return key;
        }
        return null;
    }

    /**
     * Tìm dữ liệu item từ tên hiển thị trong phân khu
     */
    private static ConfigurationSection findItemByName(String categoryKey, String displayName) {
        ConfigurationSection items = shopConfig.getConfigurationSection("PhanKhu." + categoryKey + ".Items");
        if (items == null) return null;
        for (String key : items.getKeys(false)) {
            String name = items.getString(key + ".Name", "");
            if (name.equals(displayName)) return items.getConfigurationSection(key);
        }
        return null;
    }

    /**
     * Tìm key phân khu từ title cửa hàng con
     */
    private static String findCategoryKeyByTitle(String title) {
        ConfigurationSection categories = shopConfig.getConfigurationSection("PhanKhu");
        if (categories == null) return null;
        for (String key : categories.getKeys(false)) {
            String t = categories.getString(key + ".Title", "");
            if (t.equals(title)) return key;
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (shopConfig == null) return;
        String title = event.getView().getTitle();

        boolean isMain = title.equals(MAIN_TITLE);
        String catKey = isMain ? null : findCategoryKeyByTitle(title);

        if (!isMain && catKey == null) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Menu chính → mở phân khu
        if (isMain) {
            String clickedName = clicked.getItemMeta().getDisplayName();
            String targetKey = findCategoryKeyByIconName(clickedName);
            if (targetKey != null) {
                openSubShop(player, targetKey);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        // Nút quay lại
        if (clicked.getType() == Material.ARROW) {
            open(player);
            return;
        }

        // Bỏ qua nút số dư & kính
        if (clicked.getType() == Material.SUNFLOWER) return;
        if (clicked.getType().name().contains("STAINED_GLASS_PANE")) return;

        // Mua hàng
        String itemName = clicked.getItemMeta().getDisplayName();
        ConfigurationSection itemData = findItemByName(catKey, itemName);
        if (itemData == null) return;

        int price = itemData.getInt("Price", 0);
        int amount = itemData.getInt("Amount", 1);
        String canhGioiStr = itemData.getString("CanhGioiMin", "PHAM_NHAN");

        // Kiểm tra cảnh giới
        TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
        PlayerDataManager data = plugin.getPlayerDataManager();
        EconomyManager eco = plugin.getEconomyManager();

        try {
            CanhGioi reqRank = CanhGioi.valueOf(canhGioiStr);
            CanhGioi playerRank = data.getCanhGioi(player);
            if (playerRank.ordinal() < reqRank.ordinal()) {
                player.sendMessage("§c[Vạn Giới Các] §fCảnh giới không đủ! Cần §e" + reqRank.getTenHienThi() + "+§f!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        } catch (Exception ignored) {}

        // Kiểm tra Vault
        if (!eco.isVaultEnabled()) {
            player.sendMessage("§c[Vạn Giới Các] §fHệ thống kinh tế chưa sẵn sàng!");
            return;
        }

        if (eco.hasMoney(player, price)) {
            eco.takeMoney(player, price);

            // Tạo vật phẩm sạch (không kèm lore giá cả)
            Material mat = Material.matchMaterial(itemData.getString("Material", "STONE"));
            if (mat == null) mat = Material.STONE;
            ItemStack bought = new ItemStack(mat, amount);
            ItemMeta meta = bought.getItemMeta();
            meta.setDisplayName(itemName);
            List<String> originalLore = itemData.getStringList("Lore");
            if (!originalLore.isEmpty()) meta.setLore(originalLore);
            bought.setItemMeta(meta);

            player.getInventory().addItem(bought);
            player.sendMessage("§a[Vạn Giới Các] §fMua thành công §e" + itemName + " §fx" + amount + " §f(§c-" + eco.formatMoney(price) + "§f)");
            player.sendMessage("§7Số dư: §a" + eco.formatMoney(eco.getBalance(player)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        } else {
            player.sendMessage("§c[Vạn Giới Các] §fKhông đủ tiền! Cần §e" + eco.formatMoney(price) + " §f- Có: §e" + eco.formatMoney(eco.getBalance(player)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

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