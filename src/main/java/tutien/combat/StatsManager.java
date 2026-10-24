package tutien.combat;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;
import tutien.tutien.gui.GachaGUI;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lớp quản lý chỉ số thuộc tính (Máu, Sát thương, Tốc độ).
 * v2.2 Skyblock Edition:
 *   - Đọc MaxMau/BaseDamage từ CanhGioi (canhgioi.yml).
 *   - Sửa lỗi cộng dồn 2 lần sát thương của KIEM_TU (chỉ buff khi cầm kiếm trong CombatListener).
 *   - Tự động quét Linh Thú (Pet) và Phụ Kiện Hộ Mệnh trong túi đồ để cộng Sinh Lực, Sát Thương, Tốc Độ.
 */
public class StatsManager {

    public static void applyStats(Player player, PlayerDataManager dataManager) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        HeTuLuyen he = dataManager.getHeTuLuyen(player);

        // 1. Chỉ số cơ bản đọc TRỰC TIẾP từ CanhGioi enum (data từ canhgioi.yml)
        double finalMaxHealth = cg.getMaxMau();
        double finalDamage    = cg.getBaseDamage();
        double finalSpeed     = 0.2;

        // 2. Buff theo Hệ Tu Luyện
        switch (he) {
            case THE_TU -> finalMaxHealth *= 1.4; // +40% Máu tối đa (+10% giảm sát thương trong CombatListener)
            case MA_TU  -> finalDamage *= 1.3;    // +30% Sát thương
            case YEU_TU -> finalSpeed *= 1.15;    // +15% Tốc độ di chuyển
            case KIEM_TU -> {
                // +20% Sát thương khi cầm Kiếm được xử lý chính xác trong CombatListener
            }
            case PHAP_TU -> finalDamage *= 1.15;  // +15% Sát thương phép (+30% Linh Lực max)
            case DAN_TU  -> {
                // +25% hiệu quả đan dược khi sử dụng trong ItemListener
            }
            case SONG_TU -> {
                finalMaxHealth *= 1.05;
                finalDamage    *= 1.05;
            }
            default -> {}
        }

        // 3. Cộng chỉ số vĩnh viễn từ đan dược (Luyện Đan)
        finalDamage    += dataManager.getBonusDamage(player);
        finalMaxHealth += dataManager.getBonusHealth(player);

        // 4. Cộng chỉ số từ Phụ Kiện Hộ Mệnh cao nhất và Linh Thú trong túi đồ
        double bestAccessoryHp = 0;
        double bestAccessoryDmg = 0;
        Set<GachaGUI.LinhThu> activePets = getActivePets(player);

        for (ItemStack is : player.getInventory().getContents()) {
            if (is == null || !is.hasItemMeta()) continue;
            ItemMeta meta = is.getItemMeta();
            if (meta == null) continue;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String type = pdc.get(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING);
            if (ItemKeys.TYPE_ACCESSORY.equals(type)) {
                double hp = pdc.getOrDefault(ItemKeys.KEY_PILL_HP, PersistentDataType.DOUBLE, 0.0);
                double dmg = pdc.getOrDefault(ItemKeys.KEY_PILL_DMG, PersistentDataType.DOUBLE, 0.0);
                if (hp > bestAccessoryHp) bestAccessoryHp = hp;
                if (dmg > bestAccessoryDmg) bestAccessoryDmg = dmg;
            }
        }

        finalMaxHealth += bestAccessoryHp;
        finalDamage    += bestAccessoryDmg;

        if (activePets.contains(GachaGUI.LinhThu.HOA_LINH_MAO)) {
            finalMaxHealth += 5.0;
        }
        if (activePets.contains(GachaGUI.LinhThu.THIEN_PHUONG)) {
            finalMaxHealth += 25.0;
        }
        if (activePets.contains(GachaGUI.LinhThu.KIM_LONG)) {
            finalSpeed *= 1.15;
        }

        // 5. Áp dụng vào Minecraft Attributes
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(finalMaxHealth);
        }
        if (player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(finalDamage);
        }
        if (player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(Math.min(0.4, finalSpeed));
        }

        // Cập nhật tốc độ bay nếu sở hữu Long Mã
        player.setFlySpeed(activePets.contains(GachaGUI.LinhThu.KIM_LONG) ? 0.13f : 0.1f);

        // 6. Đồng bộ HP hiện tại không vượt quá max mới
        double effectiveMax = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                ? player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
                : finalMaxHealth;
        if (player.getHealth() > effectiveMax) {
            player.setHealth(effectiveMax);
        }
    }

    /**
     * Quét túi đồ của người chơi để lấy danh sách các loại Linh Thú đang sở hữu.
     */
    public static Set<GachaGUI.LinhThu> getActivePets(Player player) {
        Set<GachaGUI.LinhThu> pets = EnumSet.noneOf(GachaGUI.LinhThu.class);
        for (ItemStack is : player.getInventory().getContents()) {
            if (is == null || !is.hasItemMeta()) continue;
            ItemMeta meta = is.getItemMeta();
            if (meta == null) continue;

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String petId = pdc.get(ItemKeys.KEY_PET_ID, PersistentDataType.STRING);
            if (petId != null) {
                try {
                    pets.add(GachaGUI.LinhThu.valueOf(petId));
                    continue;
                } catch (IllegalArgumentException ignored) {}
            }

            // Tương thích ngược với Linh Thú tạo trước bản v2.2
            if (is.getType().name().endsWith("_SPAWN_EGG") && meta.hasDisplayName()) {
                String stripped = ChatColor.stripColor(meta.getDisplayName());
                for (GachaGUI.LinhThu lt : GachaGUI.LinhThu.values()) {
                    if (ChatColor.stripColor(lt.tenHienThi).equalsIgnoreCase(stripped)) {
                        pets.add(lt);
                        break;
                    }
                }
            }
        }
        return pets;
    }

    /**
     * Lấy bậc Phụ Kiện Hộ Mệnh cao nhất trong túi đồ (0..8) để tính bonus Tu Vi AFK.
     */
    public static int getBestAccessoryTier(Player player) {
        int bestTier = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is == null || !is.hasItemMeta()) continue;
            ItemMeta meta = is.getItemMeta();
            if (meta == null) continue;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String type = pdc.get(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING);
            if (ItemKeys.TYPE_ACCESSORY.equals(type)) {
                int tier = pdc.getOrDefault(ItemKeys.KEY_EQUIP_TIER, PersistentDataType.INTEGER, 0) + 1;
                if (tier > bestTier) bestTier = tier;
            }
        }
        return bestTier;
    }
}
