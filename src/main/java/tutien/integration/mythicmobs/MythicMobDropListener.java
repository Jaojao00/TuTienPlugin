package tutien.integration.mythicmobs;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.item.BiTichItem;
import tutien.cultivation.model.CongPhap;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

/**
 * Xử lý rớt Bí Tịch Tàn Quyển và ghi nhận tiêu diệt Boss MythicMobs:
 * - Khi quái MythicMobs chết, kiểm tra điều kiện rơi tàn quyển công pháp tương ứng.
 * - Ghi nhận chiến tích diệt Boss để mở khóa phong ấn tu luyện công pháp cao cấp.
 * - Tương thích mềm (Soft-hook) an toàn 100%, có hỗ trợ fallback cho Boss vanilla.
 */
public class MythicMobDropListener implements Listener {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    public MythicMobDropListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();

        String mobInternalName = getMobIdentifier(entity);
        if (mobInternalName == null || mobInternalName.isEmpty()) return;

        if (killer != null && plugin.getPlayerDataManager() != null) {
            PlayerDataManager dataManager = plugin.getPlayerDataManager();
            PlayerCultivationData cultData = dataManager.getPlayerCultivationData(killer);
            if (cultData != null) {
                // 1. Ghi nhận chiến tích tiêu diệt boss
                if (!cultData.hasDefeatedBoss(mobInternalName)) {
                    cultData.addDefeatedBoss(mobInternalName);
                    killer.sendMessage("§a§l[Đột Phá Xiềng Xích] §fNgươi đã trảm sát Ma Tôn / Yêu Thú: §e"
                            + mobInternalName + "§f, mở khóa phong ấn tu luyện công pháp tương ứng!");
                }
            }
        }

        // 2. Kiểm tra tỉ lệ rơi Bí Tịch / Tàn Quyển công pháp
        if (plugin.getCongPhapManager() == null) return;
        Collection<CongPhap> allCp = plugin.getCongPhapManager().getAllCongPhap();

        for (CongPhap cp : allCp) {
            String dropTarget = cp.getDropMythicMob();
            if (dropTarget == null || dropTarget.isEmpty()) continue;

            if (dropTarget.equalsIgnoreCase(mobInternalName)) {
                double rate = cp.getDropRate();
                if (rate > 0.0 && random.nextDouble() <= rate) {
                    // Chọn ngẫu nhiên 1 tầng của công pháp này để rơi
                    int chapter = 1 + random.nextInt(cp.getTotalChapters());
                    ItemStack book = BiTichItem.createBiTich(cp, chapter);

                    if (book != null) {
                        entity.getWorld().dropItemNaturally(entity.getLocation(), book);

                        if (killer != null) {
                            killer.sendMessage("§6§l✦ THẦN BINH DIỆT MA ✦ §eYêu Ma §c" + mobInternalName
                                    + " §ebị tiêu diệt, rớt ra: §f" + cp.getDisplayName() + " §e§l[Tầng " + chapter + "]§e!");
                            killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                        }
                    }
                }
            }
        }
    }

    /**
     * Nhận diện tên định danh MythicMob hoặc mob vanilla qua Reflection an toàn.
     */
    public static String getMobIdentifier(Entity entity) {
        if (entity == null) return null;

        // 1. Thử qua MythicMobs reflection
        if (MythicMobsHook.isAvailable()) {
            try {
                // MythicMobs v5
                Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                Method getApiMethod = mythicBukkitClass.getMethod("getAPI");
                Object api = getApiMethod.invoke(null);
                Method getMobManager = api.getClass().getMethod("getMobManager");
                Object mobManager = getMobManager.invoke(api);
                Method getActiveMob = mobManager.getClass().getMethod("getActiveMob", UUID.class);
                Object activeMob = getActiveMob.invoke(mobManager, entity.getUniqueId());
                if (activeMob != null) {
                    Method getType = activeMob.getClass().getMethod("getType");
                    Object mobType = getType.invoke(activeMob);
                    Method getInternalName = mobType.getClass().getMethod("getInternalName");
                    return (String) getInternalName.invoke(mobType);
                }
            } catch (Throwable ignored) {}

            try {
                // MythicMobs v4 legacy
                Class<?> legacyClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
                Method instMethod = legacyClass.getMethod("inst");
                Object inst = instMethod.invoke(null);
                Method getApiHelperMethod = inst.getClass().getMethod("getAPIHelper");
                Object helper = getApiHelperMethod.invoke(inst);
                Method isMythicMob = helper.getClass().getMethod("isMythicMob", Entity.class);
                if ((Boolean) isMythicMob.invoke(helper, entity)) {
                    Method getMythicMobInstance = helper.getClass().getMethod("getMythicMobInstance", Entity.class);
                    Object activeMob = getMythicMobInstance.invoke(helper, entity);
                    if (activeMob != null) {
                        Method getType = activeMob.getClass().getMethod("getType");
                        Object mobType = getType.invoke(activeMob);
                        Method getInternalName = mobType.getClass().getMethod("getInternalName");
                        return (String) getInternalName.invoke(mobType);
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Thử qua metadata
        if (entity.hasMetadata("MYTHIC_TYPE")) {
            return entity.getMetadata("MYTHIC_TYPE").get(0).asString();
        }

        // 3. Fallback cho EntityType vanilla
        return entity.getType().name();
    }
}
