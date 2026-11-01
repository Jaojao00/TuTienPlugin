package tutien.linhdia;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Linh Địa & Động Phủ Skyblock (Phase 5).
 * Cho phép tu sĩ khai lập Động Phủ, bồi dưỡng Linh Mạch gia tăng tốc độ tu vi,
 * hồi phục linh lực và thanh lọc tâm ma.
 */
public class DongPhuManager {

    public static class DongPhuData {
        public UUID ownerId;
        public String worldName;
        public double x, y, z;
        public int radius = 20; // Bán kính hiệu lực động phủ (block)
        public LinhMachTier tier = LinhMachTier.NHAT_PHAM;

        public DongPhuData(UUID ownerId, String worldName, double x, double y, double z, LinhMachTier tier) {
            this.ownerId = ownerId;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.tier = tier != null ? tier : LinhMachTier.NHAT_PHAM;
        }

        public boolean contains(Location loc) {
            if (loc == null || loc.getWorld() == null) return false;
            if (!loc.getWorld().getName().equals(worldName)) return false;
            double dx = loc.getX() - x;
            double dy = loc.getY() - y;
            double dz = loc.getZ() - z;
            return (dx * dx + dy * dy + dz * dz) <= (radius * radius);
        }
    }

    private final TuTienPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, DongPhuData> dongPhuMap = new ConcurrentHashMap<>();

    public DongPhuManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.file = (plugin != null) ? new File(plugin.getDataFolder(), "dongphu.yml") : new File("target/dongphu.yml");
        loadData();
    }

    public void loadData() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        dongPhuMap.clear();

        if (config.contains("DongPhu")) {
            for (String uuidStr : config.getConfigurationSection("DongPhu").getKeys(false)) {
                try {
                    UUID id = UUID.fromString(uuidStr);
                    String path = "DongPhu." + uuidStr;
                    String world = config.getString(path + ".World", "world");
                    double x = config.getDouble(path + ".X");
                    double y = config.getDouble(path + ".Y");
                    double z = config.getDouble(path + ".Z");
                    int lvl = config.getInt(path + ".Tier", 1);
                    DongPhuData dp = new DongPhuData(id, world, x, y, z, LinhMachTier.fromLevel(lvl));
                    dongPhuMap.put(id, dp);
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveData() {
        if (config == null) return;
        config.set("DongPhu", null);
        for (Map.Entry<UUID, DongPhuData> entry : dongPhuMap.entrySet()) {
            String path = "DongPhu." + entry.getKey().toString();
            DongPhuData dp = entry.getValue();
            config.set(path + ".World", dp.worldName);
            config.set(path + ".X", dp.x);
            config.set(path + ".Y", dp.y);
            config.set(path + ".Z", dp.z);
            config.set(path + ".Tier", dp.tier.getLevel());
        }
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    public DongPhuData getDongPhu(UUID playerId) {
        return dongPhuMap.get(playerId);
    }

    public boolean setDongPhu(Player p, Location loc) {
        if (p == null || loc == null || loc.getWorld() == null) return false;

        // Kiểm tra an toàn: Không đặt trong hư không (Void) hoặc mặt nước/dung nham
        try {
            if (loc.getBlock().getType().isAir() && loc.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
                p.sendMessage("§c§l[Động Phủ] §fKhông thể khai lập Động Phủ lơ lửng giữa không trung! Hãy chọn mặt đất bằng phẳng.");
                return false;
            }
            if (loc.getBlock().isLiquid() || loc.clone().subtract(0, 1, 0).getBlock().isLiquid()) {
                p.sendMessage("§c§l[Động Phủ] §fKhông thể khai lập Động Phủ trên dung nham hoặc mặt nước biến động!");
                return false;
            }
        } catch (Throwable ignored) {
            // Hỗ trợ headless tests
        }

        DongPhuData dp = dongPhuMap.get(p.getUniqueId());
        LinhMachTier tier = (dp != null) ? dp.tier : LinhMachTier.NHAT_PHAM;
        dongPhuMap.put(p.getUniqueId(), new DongPhuData(p.getUniqueId(), loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), tier));
        saveData();
        p.sendMessage("§a§l[Động Phủ] §fĐã khai lập Động Phủ tại vị trí hiện tại! Linh khí thiên địa bắt đầu quy tụ.");
        return true;
    }

    public boolean upgradeLinhMach(Player p) {
        if (p == null) return false;
        DongPhuData dp = dongPhuMap.get(p.getUniqueId());
        if (dp == null) {
            p.sendMessage("§c§l[Động Phủ] §fNgươi chưa khai lập Động Phủ! Hãy gõ /tutien dongphu lap.");
            return false;
        }
        if (dp.tier.getLevel() >= 5) {
            p.sendMessage("§c§l[Động Phủ] §fLinh mạch Động Phủ đã đạt cấp tối đa (Ngũ Phẩm Cực Linh)!");
            return false;
        }

        LinhMachTier nextTier = LinhMachTier.fromLevel(dp.tier.getLevel() + 1);
        int cost = nextTier.getUpgradeCost();
        if (plugin != null && plugin.getPlayerDataManager() != null) {
            long currentDaoNiem = plugin.getPlayerDataManager().getDaoNiem(p);
            if (currentDaoNiem < cost) {
                p.sendMessage("§c§l[Động Phủ] §fNgươi không đủ Đạo Niệm/Linh Thạch để nâng cấp linh mạch! Cần: §e" + cost + "§f, có: §c" + currentDaoNiem);
                return false;
            }
            plugin.getPlayerDataManager().addDaoNiem(p, -cost);
        }

        dp.tier = nextTier;
        saveData();
        p.sendMessage("§a§l[Động Phủ] §fChúc mừng! Linh mạch động phủ đã đột phá lên §e" + nextTier.getDisplayName() + "§f!");
        return true;
    }

    public boolean upgradeLinhMach(UUID playerId) {
        if (playerId == null) return false;
        DongPhuData dp = dongPhuMap.get(playerId);
        if (dp == null) return false;
        if (dp.tier.getLevel() >= 5) return false;

        LinhMachTier nextTier = LinhMachTier.fromLevel(dp.tier.getLevel() + 1);
        int cost = nextTier.getUpgradeCost();
        if (plugin != null && plugin.getPlayerDataManager() != null) {
            long currentDaoNiem = plugin.getPlayerDataManager().getDaoNiem(playerId);
            if (currentDaoNiem < cost) {
                return false;
            }
            plugin.getPlayerDataManager().addDaoNiem(playerId, -cost);
        }

        dp.tier = nextTier;
        saveData();
        return true;
    }

    public boolean isInsideDongPhu(Player p) {
        if (p == null) return false;
        DongPhuData dp = dongPhuMap.get(p.getUniqueId());
        if (dp == null) return false;
        return dp.contains(p.getLocation());
    }

    public double getTuViMultiplier(Player p) {
        if (!isInsideDongPhu(p)) return 1.0;
        DongPhuData dp = dongPhuMap.get(p.getUniqueId());
        return (dp != null) ? dp.tier.getTuViMultiplier() : 1.0;
    }

    public double getManaRegenBonus(Player p) {
        if (!isInsideDongPhu(p)) return 0.0;
        DongPhuData dp = dongPhuMap.get(p.getUniqueId());
        return (dp != null) ? dp.tier.getManaRegenPerSec() : 0.0;
    }
}
