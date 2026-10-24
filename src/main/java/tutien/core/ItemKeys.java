package tutien.core;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * Tiện ích quản lý PersistentDataContainer (PDC) chống giả mạo vật phẩm bằng Đê (Anvil)
 * và hỗ trợ trao vật phẩm an toàn không bị mất khi đầy túi đồ trên Skyblock.
 */
public final class ItemKeys {

    public static final NamespacedKey KEY_TYPE = new NamespacedKey("tutien", "item_type");
    public static final NamespacedKey KEY_ITEM_TYPE = KEY_TYPE;
    public static final NamespacedKey KEY_ID = new NamespacedKey("tutien", "item_id");
    public static final NamespacedKey KEY_PILL_ID = KEY_ID;
    public static final NamespacedKey KEY_PILL_DMG = new NamespacedKey("tutien", "pill_dmg");
    public static final NamespacedKey KEY_PILL_HP = new NamespacedKey("tutien", "pill_hp");
    public static final NamespacedKey KEY_PILL_TUVI = new NamespacedKey("tutien", "pill_tuvi");
    public static final NamespacedKey KEY_PILL_MANA = new NamespacedKey("tutien", "pill_mana");
    public static final NamespacedKey KEY_EQUIP_TIER = new NamespacedKey("tutien", "equip_tier");
    public static final NamespacedKey KEY_PET_ID = new NamespacedKey("tutien", "pet_id");

    // Các hằng số loại vật phẩm
    public static final String TYPE_PILL = "PILL";
    public static final String TYPE_INGREDIENT = "INGREDIENT";
    public static final String TYPE_EQUIP = "EQUIP";
    public static final String TYPE_ACCESSORY = "ACCESSORY";
    public static final String TYPE_PET = "PET";
    public static final String TYPE_CURRENCY = "CURRENCY";
    public static final String TYPE_SPECIAL = "SPECIAL";
    public static final String TYPE_BOSS_LOOT = "BOSS_LOOT";

    private ItemKeys() {}

    /**
     * Đánh dấu một ItemStack là vật phẩm của TuTienPlugin
     */
    public static void markItem(ItemMeta meta, String type, String id) {
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_TYPE, PersistentDataType.STRING, type);
        if (id != null) {
            pdc.set(KEY_ID, PersistentDataType.STRING, id);
        }
    }

    /**
     * Đánh dấu thông số Đan Dược lên ItemMeta
     */
    public static void markPill(ItemMeta meta, String id, double dmg, double hp, int tuVi, int mana) {
        if (meta == null) return;
        markItem(meta, TYPE_PILL, id);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_PILL_DMG, PersistentDataType.DOUBLE, dmg);
        pdc.set(KEY_PILL_HP, PersistentDataType.DOUBLE, hp);
        pdc.set(KEY_PILL_TUVI, PersistentDataType.INTEGER, tuVi);
        pdc.set(KEY_PILL_MANA, PersistentDataType.INTEGER, mana);
    }

    /**
     * Kiểm tra xem vật phẩm có phải đồ Tu Tiên hợp lệ không (tránh đồ Vanilla đổi tên bằng Anvil)
     */
    public static boolean isTuTienItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(KEY_TYPE, PersistentDataType.STRING)
                || pdc.has(new NamespacedKey("oraxen", "id"), PersistentDataType.STRING);
    }

    /**
     * Kiểm tra vật phẩm có phải nguyên liệu thô (Vanilla hoặc nguyên liệu từ Vạn Giới Các) không.
     * Ngăn chặn việc nuốt nhầm Đan Dược, Pháp Bảo, Linh Thú hoặc Boss Loot khi luyện đan / đổi khoáng thạch.
     */
    public static boolean isValidRawMaterial(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return true; // Đồ Vanilla nguyên bản
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_TYPE, PersistentDataType.STRING)) {
            String type = pdc.get(KEY_TYPE, PersistentDataType.STRING);
            return TYPE_INGREDIENT.equals(type);
        }
        // Không nhận các vật phẩm có tên đặc biệt của hệ thống cũ (Đan, Boss Loot, Đạo Niệm, Linh Khí...)
        if (meta.hasDisplayName()) {
            String name = meta.getDisplayName();
            if (name.contains("Đan") || name.contains("Đạo Niệm") || name.contains("Boss Loot")
                    || name.contains("Phẩm") || name.contains("Chí Bảo") || name.contains("Linh Bảo")
                    || name.contains("Chân Bảo") || name.contains("Linh Khí") || name.contains("Phù")) {
                return false;
            }
        }
        return true;
    }

    public static String getItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY_TYPE, PersistentDataType.STRING);
    }

    public static String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY_ID, PersistentDataType.STRING);
    }

    /**
     * Trao vật phẩm an toàn cho người chơi:
     * Nếu túi đồ đầy, phần dư sẽ được thả ngay dưới chân người chơi (không bị mất đồ).
     */
    public static void giveItemSafe(Player player, ItemStack... items) {
        if (player == null || items == null) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items);
        if (!leftovers.isEmpty()) {
            for (ItemStack drop : leftovers.values()) {
                if (drop != null && !drop.getType().isAir()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            player.sendMessage("§e[Túi Đồ] §fTúi đồ đã đầy! Vật phẩm dư đã được đặt dưới chân bạn.");
        }
    }
}
