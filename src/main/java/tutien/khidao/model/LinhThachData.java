package tutien.khidao.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tutien.core.ItemKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dữ liệu của một viên Linh Thạch Ngũ Hành (Phase 4).
 */
public class LinhThachData {

    private final LinhThachType type;
    private final LinhThachType.Tier tier;

    public LinhThachData(LinhThachType type, LinhThachType.Tier tier) {
        this.type = Objects.requireNonNull(type, "LinhThachType cannot be null");
        this.tier = Objects.requireNonNull(tier, "Tier cannot be null");
    }

    public LinhThachType getType() {
        return type;
    }

    public LinhThachType.Tier getTier() {
        return tier;
    }

    public double getElementalPower() {
        return tier.getElementalPower();
    }

    public double getSpecialRatio() {
        return tier.getSpecialRatio();
    }

    public String getDisplayName() {
        return tier.getPrefix() + " " + type.getDisplayName();
    }

    /**
     * Serialize thành chuỗi dạng TYPE:TIER_LEVEL (vd: KIM:2, HOA:3)
     */
    public String serialize() {
        return type.name() + ":" + tier.getLevel();
    }

    /**
     * Parse từ chuỗi đã serialize
     */
    public static LinhThachData deserialize(String str) {
        if (str == null || !str.contains(":")) return null;
        String[] parts = str.split(":");
        if (parts.length != 2) return null;
        try {
            LinhThachType type = LinhThachType.fromElement(parts[0]);
            int lvl = Integer.parseInt(parts[1]);
            LinhThachType.Tier tier = LinhThachType.Tier.fromLevel(lvl);
            return new LinhThachData(type, tier);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Tạo ItemStack Linh Thạch chuẩn có PDC chống giả mạo
     */
    public ItemStack toItemStack() {
        Material mat = switch (type) {
            case KIM -> Material.EMERALD;
            case MOC -> Material.SLIME_BALL;
            case THUY -> Material.LAPIS_LAZULI;
            case HOA -> Material.BLAZE_POWDER;
            case THO -> Material.RAW_GOLD;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7Ngũ Hành: " + type.getNguHanh().getTenHienThi());
        lore.add("§7Phẩm Cấp: " + tier.getPrefix());
        lore.add("§7Lực Lượng Nguyên Tố: §e+" + (int) getElementalPower());
        lore.add("§7Hiệu Ứng Khảm Nạm: §f" + type.getSpecialEffectName() + " §a(+" + (int)(getSpecialRatio() * 100) + "%)");
        lore.add("");
        lore.add("§8" + type.getDescription());
        lore.add("");
        lore.add("§eDùng tại Lò Rèn (/tutien loren) để khảm vào trang bị.");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING, ItemKeys.TYPE_GEM);
        pdc.set(ItemKeys.KEY_GEM_ELEMENT, PersistentDataType.STRING, type.name());
        pdc.set(ItemKeys.KEY_GEM_TIER, PersistentDataType.INTEGER, tier.getLevel());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Đọc LinhThachData từ một ItemStack (xác thực qua PDC)
     */
    public static LinhThachData fromItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (org.bukkit.Bukkit.getServer() == null) {
            try {
                java.lang.reflect.Field metaField = ItemStack.class.getDeclaredField("meta");
                metaField.setAccessible(true);
                if (metaField.get(item) == null) return null;
            } catch (Throwable ignored) {
                return null;
            }
        } else {
            if (!item.hasItemMeta()) return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String itemType = pdc.get(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING);
        if (!ItemKeys.TYPE_GEM.equals(itemType)) return null;

        String element = pdc.get(ItemKeys.KEY_GEM_ELEMENT, PersistentDataType.STRING);
        Integer tierLvl = pdc.get(ItemKeys.KEY_GEM_TIER, PersistentDataType.INTEGER);
        if (element == null || tierLvl == null) return null;

        LinhThachType type = LinhThachType.fromElement(element);
        LinhThachType.Tier tier = LinhThachType.Tier.fromLevel(tierLvl);
        return new LinhThachData(type, tier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinhThachData that)) return false;
        return type == that.type && tier == that.tier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, tier);
    }
}
