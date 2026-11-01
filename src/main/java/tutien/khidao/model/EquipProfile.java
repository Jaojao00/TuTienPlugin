package tutien.khidao.model;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tutien.core.ItemKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hồ sơ trang bị Tu Tiên hoàn chỉnh (Phase 4):
 * Quản lý Phẩm cấp, Ô khảm, Linh Thạch Ngũ Hành đã khảm và Khí Linh thức tỉnh.
 */
public class EquipProfile {

    private EquipTier tier;
    private int socketsMax;
    private int socketsUnlocked;
    private final List<LinhThachData> inlaidGems = new ArrayList<>();
    private KhiLinhData khiLinh;

    public EquipProfile(EquipTier tier) {
        this.tier = (tier == null) ? EquipTier.PHAM_KHI : tier;
        this.socketsMax = this.tier.getMaxSockets();
        // Mặc định mở sẵn số ô theo phẩm cấp ban đầu (tối đa 1 ô miễn phí nếu có)
        this.socketsUnlocked = Math.min(this.socketsMax, this.tier.getLevel() >= 1 ? 1 : 0);
    }

    public EquipTier getTier() {
        return tier;
    }

    public void setTier(EquipTier tier) {
        this.tier = tier;
        this.socketsMax = tier.getMaxSockets();
        if (this.socketsUnlocked > this.socketsMax) {
            this.socketsUnlocked = this.socketsMax;
        }
    }

    public int getSocketsMax() {
        return socketsMax;
    }

    public void setSocketsMax(int socketsMax) {
        this.socketsMax = socketsMax;
    }

    public int getSocketsUnlocked() {
        return socketsUnlocked;
    }

    public void setSocketsUnlocked(int socketsUnlocked) {
        this.socketsUnlocked = Math.max(0, Math.min(socketsMax, socketsUnlocked));
    }

    public boolean unlockNextSocket() {
        if (socketsUnlocked < socketsMax) {
            socketsUnlocked++;
            return true;
        }
        return false;
    }

    public List<LinhThachData> getInlaidGems() {
        return Collections.unmodifiableList(inlaidGems);
    }

    public boolean canInlayGem() {
        return inlaidGems.size() < socketsUnlocked;
    }

    public boolean inlayGem(LinhThachData gem) {
        if (gem == null || !canInlayGem()) return false;
        inlaidGems.add(gem);
        return true;
    }

    public LinhThachData extractGem(int index) {
        if (index >= 0 && index < inlaidGems.size()) {
            return inlaidGems.remove(index);
        }
        return null;
    }

    public KhiLinhData getKhiLinh() {
        return khiLinh;
    }

    public void setKhiLinh(KhiLinhData khiLinh) {
        this.khiLinh = khiLinh;
    }

    public boolean hasKhiLinh() {
        return khiLinh != null;
    }

    // === TÍNH TOÁN CHỈ SỐ KHẢM NẠM ===

    public double getTotalElementalPower(LinhThachType type) {
        double sum = 0.0;
        for (LinhThachData gem : inlaidGems) {
            if (gem.getType() == type) {
                sum += gem.getElementalPower();
            }
        }
        return sum;
    }

    public double getArmorPenetration() {
        double pen = 0.0;
        for (LinhThachData gem : inlaidGems) {
            if (gem.getType() == LinhThachType.KIM) {
                pen += gem.getSpecialRatio();
            }
        }
        return Math.min(0.75, pen); // Tối đa 75% xuyên giáp
    }

    public double getLifestealRatio() {
        double ls = 0.0;
        for (LinhThachData gem : inlaidGems) {
            if (gem.getType() == LinhThachType.MOC) {
                ls += gem.getSpecialRatio();
            }
        }
        return Math.min(0.50, ls); // Tối đa 50% hút máu
    }

    public double getSlowRatio() {
        double slow = 0.0;
        for (LinhThachData gem : inlaidGems) {
            if (gem.getType() == LinhThachType.THUY) {
                slow += gem.getSpecialRatio();
            }
        }
        return Math.min(0.60, slow);
    }

    public double getBurnRatio() {
        double burn = 0.0;
        for (LinhThachData gem : inlaidGems) {
            if (gem.getType() == LinhThachType.HOA) {
                burn += gem.getSpecialRatio();
            }
        }
        return Math.min(0.80, burn);
    }

    public double getReflectRatio() {
        double ref = 0.0;
        for (LinhThachData gem : inlaidGems) {
            if (gem.getType() == LinhThachType.THO) {
                ref += gem.getSpecialRatio();
            }
        }
        return Math.min(0.50, ref); // Tối đa 50% phản đòn
    }

    // === SERIALIZATION TO / FROM ITEMSTACK PDC ===

    public static EquipProfile fromItemStack(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return null;
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

        Integer tierLvl = pdc.get(ItemKeys.KEY_EQUIP_TIER, PersistentDataType.INTEGER);
        if (tierLvl == null) return null;

        EquipTier tier = EquipTier.fromLevel(tierLvl);
        EquipProfile profile = new EquipProfile(tier);

        Integer sMax = pdc.get(ItemKeys.KEY_SOCKETS_MAX, PersistentDataType.INTEGER);
        if (sMax != null) profile.socketsMax = sMax;

        Integer sUnlocked = pdc.get(ItemKeys.KEY_SOCKETS_UNLOCKED, PersistentDataType.INTEGER);
        if (sUnlocked != null) profile.socketsUnlocked = sUnlocked;

        String gemStr = pdc.get(ItemKeys.KEY_INLAID_GEMS, PersistentDataType.STRING);
        if (gemStr != null && !gemStr.isBlank()) {
            for (String g : gemStr.split(",")) {
                LinhThachData data = LinhThachData.deserialize(g.trim());
                if (data != null) profile.inlaidGems.add(data);
            }
        }

        Integer awakened = pdc.get(ItemKeys.KEY_KHI_LINH_AWAKENED, PersistentDataType.INTEGER);
        if (awakened != null && awakened == 1) {
            String klName = pdc.get(ItemKeys.KEY_KHI_LINH_NAME, PersistentDataType.STRING);
            String klElem = pdc.get(ItemKeys.KEY_KHI_LINH_ELEMENT, PersistentDataType.STRING);
            Integer klLvl = pdc.get(ItemKeys.KEY_KHI_LINH_LEVEL, PersistentDataType.INTEGER);
            Integer klSoul = pdc.get(ItemKeys.KEY_KHI_LINH_SOUL, PersistentDataType.INTEGER);
            String klSkill = pdc.get(ItemKeys.KEY_KHI_LINH_SKILL, PersistentDataType.STRING);

            if (klName != null && klElem != null) {
                KhiLinhSkill skill = KhiLinhSkill.fromName(klSkill);
                profile.khiLinh = new KhiLinhData(klName, klElem,
                        klLvl != null ? klLvl : 1,
                        klSoul != null ? klSoul : 0,
                        skill);
            }
        }

        return profile;
    }

    public void applyToItemStack(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING, ItemKeys.TYPE_EQUIP);
        pdc.set(ItemKeys.KEY_EQUIP_TIER, PersistentDataType.INTEGER, tier.getLevel());
        pdc.set(ItemKeys.KEY_EQUIP_TIER_ID, PersistentDataType.STRING, tier.name());
        pdc.set(ItemKeys.KEY_SOCKETS_MAX, PersistentDataType.INTEGER, socketsMax);
        pdc.set(ItemKeys.KEY_SOCKETS_UNLOCKED, PersistentDataType.INTEGER, socketsUnlocked);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inlaidGems.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(inlaidGems.get(i).serialize());
        }
        pdc.set(ItemKeys.KEY_INLAID_GEMS, PersistentDataType.STRING, sb.toString());

        if (khiLinh != null) {
            pdc.set(ItemKeys.KEY_KHI_LINH_AWAKENED, PersistentDataType.INTEGER, 1);
            pdc.set(ItemKeys.KEY_KHI_LINH_NAME, PersistentDataType.STRING, khiLinh.getName());
            pdc.set(ItemKeys.KEY_KHI_LINH_ELEMENT, PersistentDataType.STRING, khiLinh.getElement());
            pdc.set(ItemKeys.KEY_KHI_LINH_LEVEL, PersistentDataType.INTEGER, khiLinh.getLevel());
            pdc.set(ItemKeys.KEY_KHI_LINH_SOUL, PersistentDataType.INTEGER, khiLinh.getSoul());
            pdc.set(ItemKeys.KEY_KHI_LINH_SKILL, PersistentDataType.STRING, khiLinh.getSkill().name());
        } else {
            pdc.remove(ItemKeys.KEY_KHI_LINH_AWAKENED);
            pdc.remove(ItemKeys.KEY_KHI_LINH_NAME);
            pdc.remove(ItemKeys.KEY_KHI_LINH_ELEMENT);
            pdc.remove(ItemKeys.KEY_KHI_LINH_LEVEL);
            pdc.remove(ItemKeys.KEY_KHI_LINH_SOUL);
            pdc.remove(ItemKeys.KEY_KHI_LINH_SKILL);
        }

        // Cập nhật Lore trang bị trực quan
        List<String> lore = new ArrayList<>();
        lore.add("§7Phẩm Cấp: " + tier.getDisplayName());

        Double dmg = pdc.get(ItemKeys.KEY_PILL_DMG, PersistentDataType.DOUBLE);
        if (dmg != null && dmg > 0) {
            lore.add("§7Sát thương vật lý: §c+" + String.format("%.1f", dmg));
        }
        Double hp = pdc.get(ItemKeys.KEY_PILL_HP, PersistentDataType.DOUBLE);
        if (hp != null && hp > 0) {
            lore.add("§7Sinh lực tối đa: §a+" + String.format("%.1f", hp) + " ❤");
        }

        // Khảm Nạm Lore
        if (socketsMax > 0) {
            lore.add("");
            lore.add("§e── Khảm Nạm Ngũ Hành (" + inlaidGems.size() + "/" + socketsUnlocked + " ô) ──");
            for (int i = 0; i < socketsUnlocked; i++) {
                if (i < inlaidGems.size()) {
                    LinhThachData gem = inlaidGems.get(i);
                    lore.add("  §f• Ô " + (i + 1) + ": " + gem.getDisplayName() +
                            " §7(+ " + (int) gem.getElementalPower() + " " + gem.getType().name() +
                            ", " + gem.getType().getSpecialEffectName() + " +" + (int)(gem.getSpecialRatio() * 100) + "%)");
                } else {
                    lore.add("  §f• Ô " + (i + 1) + ": §8[◇ Trống - Có thể khảm]");
                }
            }
            for (int i = socketsUnlocked; i < socketsMax; i++) {
                lore.add("  §f• Ô " + (i + 1) + ": §8[🔒 Chưa đục lỗ]");
            }
        }

        // Khí Linh Lore
        if (khiLinh != null) {
            lore.add("");
            lore.add("§e── Khí Linh Bản Mệnh ──");
            lore.add("  §6✦ " + khiLinh.getName() + " §7(Cấp " + khiLinh.getLevel() + " - Linh Tính: §e" + khiLinh.getSoul() + "/100§7)");
            lore.add("  §7Nguyên Tố: §f" + khiLinh.getElement());
            lore.add("  §7Kỹ Năng: §a" + khiLinh.getSkill().getDisplayName());
            lore.add("    §8" + khiLinh.getSkill().getDescription());
        } else if (tier.isAllowSpirit()) {
            lore.add("");
            lore.add("§7Khí Linh: §8Chưa Thức Tỉnh (Dùng Thần Niệm Khai Quang)");
        }

        lore.add("");
        lore.add("§8[Pháp Bảo Tu Tiên - Phase 4]");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
    }
}
