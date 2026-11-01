package tutien.integration.mythicmobs;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Trình kiểm tra kết nối mềm (Soft-hook) cho plugin MythicMobs (Phase 2.4).
 * Tuyệt đối không import trực tiếp class của MythicMobs để tránh NoClassDefFoundError khi server không cài đặt.
 */
public class MythicMobsHook {

    private static Boolean cachedAvailable = null;

    public static boolean isAvailable() {
        if (cachedAvailable == null) {
            try {
                if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
                    cachedAvailable = false;
                    return false;
                }
                Plugin p = Bukkit.getPluginManager().getPlugin("MythicMobs");
                cachedAvailable = (p != null && p.isEnabled());
            } catch (Throwable t) {
                cachedAvailable = false;
            }
        }
        return cachedAvailable;
    }

    public static void resetCache() {
        cachedAvailable = null;
    }
}
