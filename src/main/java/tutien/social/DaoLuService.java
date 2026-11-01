package tutien.social;

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
 * Quản lý Hệ Thống Đạo Lữ & Song Tu (Phase 6).
 * - Kết duyên đạo lữ (Cặp đôi tu tiên)
 * - Tọa thiền song tu: Gia tăng x2.0 tốc độ tu vi khi ngồi thiền cạnh nhau
 * - Hợp thể kỹ năng: Tăng 15% sát thương và 10% kháng đòn khi ở gần nhau.
 */
public class DaoLuService {

    public static class DaoLuBond {
        public UUID player1;
        public UUID player2;
        public long bondedAt;
        public int intimacy; // Điểm thân mật (0 -> 1000)

        public DaoLuBond(UUID player1, UUID player2, long bondedAt, int intimacy) {
            this.player1 = player1;
            this.player2 = player2;
            this.bondedAt = bondedAt;
            this.intimacy = intimacy;
        }

        public UUID getPartner(UUID pId) {
            if (pId.equals(player1)) return player2;
            if (pId.equals(player2)) return player1;
            return null;
        }
    }

    private final TuTienPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, DaoLuBond> bondMap = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>(); // targetId -> requesterId

    public DaoLuService(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.file = (plugin != null) ? new File(plugin.getDataFolder(), "daolu.yml") : new File("target/daolu.yml");
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
        bondMap.clear();

        if (config.contains("Bonds")) {
            for (String key : config.getConfigurationSection("Bonds").getKeys(false)) {
                try {
                    String p1Str = config.getString("Bonds." + key + ".P1");
                    String p2Str = config.getString("Bonds." + key + ".P2");
                    long time = config.getLong("Bonds." + key + ".Time");
                    int intimacy = config.getInt("Bonds." + key + ".Intimacy", 100);

                    UUID p1 = UUID.fromString(p1Str);
                    UUID p2 = UUID.fromString(p2Str);
                    DaoLuBond bond = new DaoLuBond(p1, p2, time, intimacy);
                    bondMap.put(p1, bond);
                    bondMap.put(p2, bond);
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveData() {
        if (config == null) return;
        config.set("Bonds", null);
        Set<DaoLuBond> saved = new HashSet<>(bondMap.values());
        int idx = 0;
        for (DaoLuBond bond : saved) {
            String path = "Bonds.Bond_" + idx;
            config.set(path + ".P1", bond.player1.toString());
            config.set(path + ".P2", bond.player2.toString());
            config.set(path + ".Time", bond.bondedAt);
            config.set(path + ".Intimacy", bond.intimacy);
            idx++;
        }
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    public boolean hasPartner(UUID playerId) {
        return bondMap.containsKey(playerId);
    }

    public DaoLuBond getBond(UUID playerId) {
        return bondMap.get(playerId);
    }

    public boolean pairDaoLu(UUID p1, UUID p2) {
        if (p1 == null || p2 == null || p1.equals(p2)) return false;
        if (hasPartner(p1) || hasPartner(p2)) return false;

        DaoLuBond bond = new DaoLuBond(p1, p2, System.currentTimeMillis(), 100);
        bondMap.put(p1, bond);
        bondMap.put(p2, bond);
        saveData();
        return true;
    }

    public boolean dissolveBond(UUID playerId) {
        DaoLuBond bond = bondMap.get(playerId);
        if (bond == null) return false;

        bondMap.remove(bond.player1);
        bondMap.remove(bond.player2);
        saveData();
        return true;
    }

    /**
     * Kiểm tra xem 2 đạo lữ có đang ở gần nhau không (phạm vi 15 blocks)
     */
    public boolean arePartnersNear(Player p1, Player p2) {
        if (p1 == null || p2 == null || p1.getWorld() == null || p2.getWorld() == null) return false;
        if (!p1.getWorld().getName().equalsIgnoreCase(p2.getWorld().getName())) return false;
        return p1.getLocation().distanceSquared(p2.getLocation()) <= 225.0; // 15^2
    }

    public boolean requestPair(Player requester, Player target) {
        if (requester == null || target == null || requester.getUniqueId().equals(target.getUniqueId())) return false;
        if (hasPartner(requester.getUniqueId()) || hasPartner(target.getUniqueId())) return false;
        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());
        target.sendMessage("§d§l[Đạo Lữ] §fTu sĩ §e" + requester.getName() + " §fmuốn cùng ngươi kết duyên Đạo Lữ Song Tu!");
        target.sendMessage("§d§l[Đạo Lữ] §fGõ §e/tutien daolu chapnhan §fđể đồng ý kết duyên.");
        requester.sendMessage("§d§l[Đạo Lữ] §fĐã gửi lời mời kết duyên tới §e" + target.getName() + "§f. Chờ đối phương phản hồi.");
        return true;
    }

    public boolean acceptPair(Player target) {
        if (target == null) return false;
        UUID requesterId = pendingRequests.remove(target.getUniqueId());
        if (requesterId == null) {
            target.sendMessage("§c§l[Đạo Lữ] §fNgươi không có lời mời kết duyên nào đang chờ.");
            return false;
        }
        boolean success = pairDaoLu(requesterId, target.getUniqueId());
        if (success) {
            target.sendMessage("§d§l[Đạo Lữ] §fChúc mừng! Hai người đã chính thức kết duyên Đạo Lữ!");
            if (org.bukkit.Bukkit.getServer() != null) {
                Player req = org.bukkit.Bukkit.getPlayer(requesterId);
                if (req != null && req.isOnline()) {
                    req.sendMessage("§d§l[Đạo Lữ] §fChúc mừng! §e" + target.getName() + " §fđã đồng ý kết duyên Đạo Lữ cùng ngươi!");
                }
            }
        }
        return success;
    }

    public double getSongTuTuViMultiplier(Player p1) {
        if (p1 == null) return 1.0;
        DaoLuBond bond = getBond(p1.getUniqueId());
        if (bond == null) return 1.0;
        UUID partnerId = bond.getPartner(p1.getUniqueId());
        if (partnerId == null || org.bukkit.Bukkit.getServer() == null) return 1.0;
        Player partner = org.bukkit.Bukkit.getPlayer(partnerId);
        if (partner != null && partner.isOnline() && arePartnersNear(p1, partner)) {
            if (plugin != null && plugin.getPlayerDataManager() != null) {
                if (plugin.getPlayerDataManager().isDangTuLuyen(p1) && plugin.getPlayerDataManager().isDangTuLuyen(partner)) {
                    return 2.0;
                }
            } else {
                return 2.0;
            }
        }
        return 1.0;
    }

    public double getPartnerCombatBonus(Player p1) {
        if (p1 == null) return 1.0;
        DaoLuBond bond = getBond(p1.getUniqueId());
        if (bond == null) return 1.0;
        UUID partnerId = bond.getPartner(p1.getUniqueId());
        if (partnerId == null || org.bukkit.Bukkit.getServer() == null) return 1.0;
        Player partner = org.bukkit.Bukkit.getPlayer(partnerId);
        if (partner != null && partner.isOnline() && arePartnersNear(p1, partner)) {
            return 1.15;
        }
        return 1.0;
    }

    /**
     * Hệ số nhân tu vi khi Song Tu Tọa Thiền:
     * Nếu 2 đạo lữ cùng tọa thiền cạnh nhau và cùng đang tọa thiền -> x2.0 tốc độ tu vi!
     */
    public double getSongTuTuViMultiplier(Player p1, Player p2) {
        if (arePartnersNear(p1, p2)) {
            if (plugin != null && plugin.getPlayerDataManager() != null) {
                if (plugin.getPlayerDataManager().isDangTuLuyen(p1) && plugin.getPlayerDataManager().isDangTuLuyen(p2)) {
                    return 2.0;
                }
                return 1.0;
            }
            return 2.0; // Nhân đôi tốc độ tu vi
        }
        return 1.0;
    }

    /**
     * Kỹ năng Hợp Thể Đạo Lữ: Tăng 15% sát thương khi chiến đấu bên cạnh nhau
     */
    public double getPartnerCombatBonus(Player p1, Player p2) {
        if (arePartnersNear(p1, p2)) {
            return 1.15; // +15% sát thương
        }
        return 1.0;
    }
}
