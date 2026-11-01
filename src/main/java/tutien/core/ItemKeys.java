package tutien.core;

import org.bukkit.Bukkit;
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
    public static final NamespacedKey KEY_MEDICINE_ID = new NamespacedKey("tutien", "medicine_id");
    public static final NamespacedKey KEY_ITEM_VERSION = new NamespacedKey("tutien", "item_version");
    public static final NamespacedKey KEY_PILL_DMG = new NamespacedKey("tutien", "pill_dmg");
    public static final NamespacedKey KEY_PILL_HP = new NamespacedKey("tutien", "pill_hp");
    public static final NamespacedKey KEY_PILL_TUVI = new NamespacedKey("tutien", "pill_tuvi");
    public static final NamespacedKey KEY_PILL_MANA = new NamespacedKey("tutien", "pill_mana");
    public static final NamespacedKey KEY_EQUIP_TIER = new NamespacedKey("tutien", "equip_tier");
    public static final NamespacedKey KEY_PET_ID = new NamespacedKey("tutien", "pet_id");

    // === Phase 4: Khí Đạo & Trang Bị PDC Keys ===
    public static final NamespacedKey KEY_EQUIP_TIER_ID = new NamespacedKey("tutien", "equip_tier_id");
    public static final NamespacedKey KEY_SOCKETS_MAX = new NamespacedKey("tutien", "equip_sockets_max");
    public static final NamespacedKey KEY_SOCKETS_UNLOCKED = new NamespacedKey("tutien", "equip_sockets_unlocked");
    public static final NamespacedKey KEY_INLAID_GEMS = new NamespacedKey("tutien", "equip_inlaid_gems");
    public static final NamespacedKey KEY_GEM_ELEMENT = new NamespacedKey("tutien", "gem_element");
    public static final NamespacedKey KEY_GEM_TIER = new NamespacedKey("tutien", "gem_tier");
    public static final NamespacedKey KEY_KHI_LINH_AWAKENED = new NamespacedKey("tutien", "khi_linh_awakened");
    public static final NamespacedKey KEY_KHI_LINH_NAME = new NamespacedKey("tutien", "khi_linh_name");
    public static final NamespacedKey KEY_KHI_LINH_ELEMENT = new NamespacedKey("tutien", "khi_linh_element");
    public static final NamespacedKey KEY_KHI_LINH_LEVEL = new NamespacedKey("tutien", "khi_linh_level");
    public static final NamespacedKey KEY_KHI_LINH_SOUL = new NamespacedKey("tutien", "khi_linh_soul");
    public static final NamespacedKey KEY_KHI_LINH_SKILL = new NamespacedKey("tutien", "khi_linh_skill");

    // Các hằng số loại vật phẩm
    public static final String TYPE_PILL = "PILL";
    public static final String TYPE_INGREDIENT = "INGREDIENT";
    public static final String TYPE_EQUIP = "EQUIP";
    public static final String TYPE_ACCESSORY = "ACCESSORY";
    public static final String TYPE_PET = "PET";
    public static final String TYPE_CURRENCY = "CURRENCY";
    public static final String TYPE_SPECIAL = "SPECIAL";
    public static final String TYPE_BOSS_LOOT = "BOSS_LOOT";
    public static final String TYPE_GEM = "GEM";
    public static final String TYPE_KHI_LINH_ITEM = "KHI_LINH_ITEM";

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
        pdc.set(KEY_MEDICINE_ID, PersistentDataType.STRING, id);
        pdc.set(KEY_ITEM_VERSION, PersistentDataType.INTEGER, 1);
    }

    /**
     * Đánh dấu vật phẩm Đan Dược Phase 3 với PDC chống giả mạo
     */
    public static void markMedicine(ItemMeta meta, String medicineId, int version) {
        if (meta == null || medicineId == null) return;
        markItem(meta, TYPE_PILL, medicineId);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_MEDICINE_ID, PersistentDataType.STRING, medicineId);
        pdc.set(KEY_ITEM_VERSION, PersistentDataType.INTEGER, version);
    }

    /**
     * Lấy ID Đan Dược từ PDC (hỗ trợ cả Phase 3 KEY_MEDICINE_ID và legacy KEY_ID)
     */
    public static String getPillId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_MEDICINE_ID, PersistentDataType.STRING)) {
            return pdc.get(KEY_MEDICINE_ID, PersistentDataType.STRING);
        }
        if (pdc.has(KEY_TYPE, PersistentDataType.STRING) && TYPE_PILL.equals(pdc.get(KEY_TYPE, PersistentDataType.STRING))) {
            return pdc.get(KEY_ID, PersistentDataType.STRING);
        }
        return null;
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
        if (Bukkit.getServer() == null) return true; // Hỗ trợ môi trường kiểm thử không có Bukkit server
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
        if (player == null || items == null || player.getInventory() == null) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items);
        if (leftovers != null && !leftovers.isEmpty()) {
            if (player.getWorld() != null && player.getLocation() != null) {
                for (ItemStack drop : leftovers.values()) {
                    if (drop != null && !drop.getType().isAir()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }
            player.sendMessage("§e[Túi Đồ] §fTúi đồ đã đầy! Vật phẩm dư đã được đặt dưới chân bạn.");
        }
    }
}
