package tutien.craft;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.*;

/**
 * Quản lý hệ thống chế tác trang bị với 8 phẩm cấp.
 * v2.2 Skyblock Edition:
 *   - Lưu Cấp Chế & Số Lần Chế vào PlayerDataManager (bền vững qua restart).
 *   - Tách biệt tên Kiếm Thần Linh và Kiếm Huyền Thiết.
 *   - Gắn AttributeModifier thực tế cho Giáp và Kiếm để tăng Sinh Lực (HP) vượt Thiên Kiếp.
 *   - Gắn PDC tag để chống giả mạo bằng Đe (Anvil).
 */
public class CheManager {

    // 8 phẩm cấp từ thấp đến cao
    public enum PhamCap {
        PHAM("§7Phàm Phẩm", 60),
        HA("§aHạ Phẩm", 15),
        TRUNG("§bTrung Phẩm", 10),
        THUONG("§dThượng Phẩm", 7),
        CHI_BAO("§6Chí Bảo", 4),
        LINH_BAO("§cLinh Bảo", 2),
        CHAN_BAO("§5Chân Bảo", 1),
        LINH_KHI("§e§lLinh Khí", 1);

        public final String display;
        public final int weight;

        PhamCap(String display, int weight) {
            this.display = display;
            this.weight = weight;
        }
    }

    // Các công thức chế tác (tách riêng tên hiển thị cho từng món)
    public enum CheRecipe {
        KIEM_CAO("Kiếm Thần Linh",      Material.NETHERITE_SWORD,      15, "Kiếm",     1.5, EquipmentSlot.HAND),
        KIEM_TRUNG("Kiếm Huyền Thiết",  Material.IRON_SWORD,           5,  "Kiếm",     1.0, EquipmentSlot.HAND),
        GIAP_THAN("Giáp Thần Lực",      Material.NETHERITE_CHESTPLATE, 20, "Giáp",     1.5, EquipmentSlot.CHEST),
        GIAP_TRUNG("Giáp Thiên Phong",  Material.IRON_CHESTPLATE,      8,  "Giáp",     1.0, EquipmentSlot.CHEST),
        MU_THAN("Mũ Linh Quang",        Material.NETHERITE_HELMET,     8,  "Giáp",     1.2, EquipmentSlot.HEAD),
        QUAN_THAN("Quần Linh Phong",    Material.NETHERITE_LEGGINGS,   10, "Giáp",     1.3, EquipmentSlot.LEGS),
        GIAY_THAN("Giày Linh Vân",      Material.NETHERITE_BOOTS,      8,  "Giáp",     1.2, EquipmentSlot.FEET),
        PHU_KIEN("Phụ Kiện Hộ Mệnh",    Material.NETHER_STAR,          12, "Phụ Kiện", 1.0, EquipmentSlot.OFF_HAND);

        public final String displayName;
        public final Material material;
        public final int oreRequired;
        public final String type;
        public final double tierScale;
        public final EquipmentSlot slot;

        CheRecipe(String displayName, Material material, int oreRequired, String type, double tierScale, EquipmentSlot slot) {
            this.displayName = displayName;
            this.material = material;
            this.oreRequired = oreRequired;
            this.type = type;
            this.tierScale = tierScale;
            this.slot = slot;
        }
    }

    private final TuTienPlugin plugin;
    private static final Random random = new Random();

    public CheManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    private PlayerDataManager data() {
        return plugin.getPlayerDataManager();
    }

    public int getCapChe(Player p) {
        return data().getCapChe(p);
    }

    public void setCapChe(Player p, int cap) {
        data().setCapChe(p, cap);
    }

    public int getSoLanChe(Player p) {
        return data().getSoLanChe(p);
    }

    public void addSoLanChe(Player p, int n) {
        data().addSoLanChe(p, n);
    }

    /**
     * Lăn phẩm cấp của trang bị dựa trên trọng số và bonus cấp lò.
     */
    public static ItemStack rollCraft(CheRecipe recipe, int capBonus) {
        PhamCap[] tiers = PhamCap.values();
        int[] weights = new int[tiers.length];

        // Cấp lò rèn càng cao: giảm trọng số Phàm Phẩm, tăng trọng số các bậc cao
        weights[0] = Math.max(10, tiers[0].weight - capBonus);
        int bonusPool = tiers[0].weight - weights[0];
        for (int i = 1; i < tiers.length; i++) {
            weights[i] = tiers[i].weight + (bonusPool / (tiers.length - 1));
        }

        int totalWeight = 0;
        for (int w : weights) totalWeight += w;

        int roll = random.nextInt(Math.max(1, totalWeight));
        int cumulative = 0;
        PhamCap selected = PhamCap.PHAM;
        for (int i = 0; i < tiers.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                selected = tiers[i];
                break;
            }
        }

        ItemStack item = new ItemStack(recipe.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(selected.display + " " + recipe.displayName);

        int phamCapIndex = selected.ordinal(); // 0=Phàm, 7=Linh Khí
        List<String> lore = new ArrayList<>();
        lore.add("§7Phẩm cấp: " + selected.display);
        lore.add("§7Loại: §e" + recipe.type);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.KEY_EQUIP_TIER, PersistentDataType.INTEGER, phamCapIndex);

        if (recipe.type.equals("Kiếm")) {
            pdc.set(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING, ItemKeys.TYPE_EQUIP);
            double dmg = Math.round((4.0 + phamCapIndex * 3.5) * recipe.tierScale * 10.0) / 10.0;
            pdc.set(ItemKeys.KEY_PILL_DMG, PersistentDataType.DOUBLE, dmg);
            lore.add("§7Sát thương: §c+" + String.format("%.1f", dmg));

            AttributeModifier dmgMod = new AttributeModifier(
                    UUID.randomUUID(), "tutien_sword_dmg", dmg,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, dmgMod);
        } else if (recipe.type.equals("Giáp")) {
            pdc.set(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING, ItemKeys.TYPE_EQUIP);
            double hp = Math.round((6.0 + phamCapIndex * 8.0) * recipe.tierScale * 10.0) / 10.0;
            double armor = Math.round((2.0 + phamCapIndex * 1.5) * recipe.tierScale * 10.0) / 10.0;
            pdc.set(ItemKeys.KEY_PILL_HP, PersistentDataType.DOUBLE, hp);
            lore.add("§7Sinh lực tối đa: §a+" + String.format("%.1f", hp) + " ❤");
            lore.add("§7Hộ thể: §b+" + String.format("%.1f", armor) + " 🛡");

            AttributeModifier hpMod = new AttributeModifier(
                    UUID.randomUUID(), "tutien_armor_hp", hp,
                    AttributeModifier.Operation.ADD_NUMBER, recipe.slot);
            AttributeModifier armorMod = new AttributeModifier(
                    UUID.randomUUID(), "tutien_armor_def", armor,
                    AttributeModifier.Operation.ADD_NUMBER, recipe.slot);
            meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, hpMod);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorMod);
        } else {
            pdc.set(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING, ItemKeys.TYPE_ACCESSORY);
            double bonusHp = 10.0 + phamCapIndex * 12.0;
            double bonusDmg = 2.0 + phamCapIndex * 2.5;
            pdc.set(ItemKeys.KEY_PILL_HP, PersistentDataType.DOUBLE, bonusHp);
            pdc.set(ItemKeys.KEY_PILL_DMG, PersistentDataType.DOUBLE, bonusDmg);
            lore.add("§7Linh Khí Hộ Thể: §dCấp " + (phamCapIndex + 1));
            lore.add("§7Sinh lực cộng thêm: §a+" + String.format("%.0f", bonusHp));
            lore.add("§7Công kích cộng thêm: §c+" + String.format("%.1f", bonusDmg));
            lore.add("§7Tốc độ Tu Vi AFK: §e+" + ((phamCapIndex + 1) * 3) + "%");
            lore.add("§8(Để trong túi đồ hoặc tay phụ để kích hoạt)");
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        lore.add("");
        lore.add("§8[Trang bị Tu Tiên]");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
