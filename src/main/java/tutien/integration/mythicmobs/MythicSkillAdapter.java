package tutien.integration.mythicmobs;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Adapter thi triển kỹ năng MythicMobs qua Reflection an toàn (Phase 2.4).
 * Hỗ trợ cả MythicMobs v5 và các phiên bản tương thích.
 */
public class MythicSkillAdapter {

    /**
     * Thi triển kỹ năng thông qua MythicMobs.
     *
     * @param caster Người thi triển
     * @param skillName Tên kỹ năng định nghĩa trong MythicMobs
     * @return true nếu thi triển thành công, false nếu có lỗi hoặc không tìm thấy kỹ năng
     */
    public static boolean castSkill(Player caster, String skillName) {
        if (!MythicMobsHook.isAvailable() || caster == null || skillName == null || skillName.trim().isEmpty()) {
            return false;
        }

        try {
            // Thử phiên bản MythicMobs v5 (io.lumine.mythic.bukkit.MythicBukkit)
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method getApiMethod = mythicBukkitClass.getMethod("getAPI");
            Object api = getApiMethod.invoke(null);

            Method getHelperMethod = api.getClass().getMethod("getAPIHelper");
            Object apiHelper = getHelperMethod.invoke(api);

            Method castSkillMethod = apiHelper.getClass().getMethod("castSkill", Entity.class, String.class);
            Object result = castSkillMethod.invoke(apiHelper, caster, skillName);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return true;
        } catch (ClassNotFoundException e) {
            // Thử phiên bản cũ hơn (io.lumine.xikage.mythicmobs.MythicMobs)
            try {
                Class<?> legacyClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
                Method instMethod = legacyClass.getMethod("inst");
                Object inst = instMethod.invoke(null);

                Method getApiHelperMethod = inst.getClass().getMethod("getAPIHelper");
                Object helper = getApiHelperMethod.invoke(inst);

                Method castSkillMethod = helper.getClass().getMethod("castSkill", Entity.class, String.class);
                Object result = castSkillMethod.invoke(helper, caster, skillName);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true;
            } catch (Exception ignored) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
