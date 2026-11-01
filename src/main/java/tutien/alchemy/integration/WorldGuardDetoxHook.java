package tutien.alchemy.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Soft-hook an toàn vào WorldGuard để xác định vùng Linh Tuyền Tẩy Trần.
 * Nếu server không cài WorldGuard, tự động fallback an toàn không gây lỗi khởi động.
 */
public final class WorldGuardDetoxHook {

    private static Boolean available = null;

    private WorldGuardDetoxHook() {}

    public static boolean isAvailable() {
        if (available != null) return available;
        try {
            if (Bukkit.getServer() == null) {
                available = false;
                return false;
            }
            available = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        } catch (Throwable t) {
            available = false;
        }
        return available;
    }

    public static boolean isInRegion(Player player, String regionId) {
        if (player == null || regionId == null || regionId.isEmpty()) return false;
        if (!isAvailable()) return false;

        try {
            return WorldGuardAdapter.checkRegion(player.getLocation(), regionId);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Inner class chỉ được tải khi WorldGuard thực sự có mặt trên server
     */
    private static class WorldGuardAdapter {
        private static boolean checkRegion(Location loc, String regionId) {
            try {
                com.sk89q.worldguard.protection.regions.RegionContainer container =
                        com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
                if (container == null) return false;

                com.sk89q.worldguard.protection.managers.RegionManager rm =
                        container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(loc.getWorld()));
                if (rm == null) return false;

                com.sk89q.worldguard.protection.ApplicableRegionSet set =
                        rm.getApplicableRegions(com.sk89q.worldedit.bukkit.BukkitAdapter.asBlockVector(loc));
                for (com.sk89q.worldguard.protection.regions.ProtectedRegion pr : set) {
                    if (pr.getId().equalsIgnoreCase(regionId)) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
            return false;
        }
    }
}
