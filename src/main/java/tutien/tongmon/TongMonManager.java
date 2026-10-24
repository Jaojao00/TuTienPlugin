package tutien.tongmon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lớp quản lý lãnh địa, thành viên và tài nguyên của Tông Môn.
 * v2.2 Skyblock Edition:
 *   - Không còn dịch chuyển ra tọa độ (10000, 10000) giữa hư không (Void) hay tự động đặt hàng rào phá đảo.
 *   - Lưu trực tiếp tọa độ hiện tại (X, Y, Z) trên đảo Skyblock của Tông Chủ làm Đại Điện Tông Môn.
 *   - Hỗ trợ lệnh /tutien tongmon sethome để Tông Chủ dời vị trí truyền tống.
 *   - Kiểm tra mặt đất an toàn trước khi dịch chuyển thành viên về Tông Môn (chống rơi xuống Void).
 */
public class TongMonManager {

    private final TuTienPlugin plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<UUID, TongMonInfo> tongMonMap = new HashMap<>();
    private int tongMonCount = 0;

    public TongMonManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tongmon.yml");
        loadData();
    }

    public void loadData() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        tongMonMap.clear();

        tongMonCount = config.getInt("TongMonCount", 0);

        if (config.contains("Sects")) {
            for (String uuidStr : config.getConfigurationSection("Sects").getKeys(false)) {
                UUID owner = UUID.fromString(uuidStr);
                String path = "Sects." + uuidStr;
                String name = config.getString(path + ".Name");
                int cx = config.getInt(path + ".CenterX");
                int cy = config.getInt(path + ".CenterY", 100);
                int cz = config.getInt(path + ".CenterZ");
                int radius = config.getInt(path + ".Radius", 25);
                String worldName = config.getString(path + ".World");

                TongMonInfo info = new TongMonInfo(name, cx, cy, cz, radius, worldName);
                info.taiNguyen = config.getInt(path + ".TaiNguyen", 0);

                if (config.contains(path + ".Members")) {
                    for (String memberStr : config.getConfigurationSection(path + ".Members").getKeys(false)) {
                        info.members.put(UUID.fromString(memberStr), config.getString(path + ".Members." + memberStr));
                    }
                }
                tongMonMap.put(owner, info);
            }
        }
    }

    public void saveData() {
        config.set("TongMonCount", tongMonCount);
        config.set("Sects", null);

        for (Map.Entry<UUID, TongMonInfo> entry : tongMonMap.entrySet()) {
            String path = "Sects." + entry.getKey().toString();
            TongMonInfo info = entry.getValue();

            config.set(path + ".Name", info.name);
            config.set(path + ".CenterX", info.centerX);
            config.set(path + ".CenterY", info.centerY);
            config.set(path + ".CenterZ", info.centerZ);
            config.set(path + ".Radius", info.radius);
            config.set(path + ".World", info.world);
            config.set(path + ".TaiNguyen", info.taiNguyen);

            for (Map.Entry<UUID, String> member : info.members.entrySet()) {
                config.set(path + ".Members." + member.getKey().toString(), member.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTongMon(Player owner, String tenTongMon, int cx, int cz, int radius) {
        int cy = owner.getLocation().getBlockY();
        TongMonInfo info = new TongMonInfo(tenTongMon, cx, cy, cz, radius, owner.getWorld().getName());
        info.members.put(owner.getUniqueId(), "TONG_CHU");

        tongMonMap.put(owner.getUniqueId(), info);
        tongMonCount++;
        saveData();
    }

    /**
     * Cập nhật lại điểm truyền tống Tông Môn tại vị trí hiện tại của Tông Chủ.
     */
    public boolean setTongMonHome(Player owner, Location loc) {
        TongMonInfo info = tongMonMap.get(owner.getUniqueId());
        if (info == null || loc.getWorld() == null) return false;
        info.centerX = loc.getBlockX();
        info.centerY = loc.getBlockY();
        info.centerZ = loc.getBlockZ();
        info.world = loc.getWorld().getName();
        saveData();
        return true;
    }

    public boolean addMember(UUID tongMonId, Player target, String role) {
        if (tongMonMap.containsKey(tongMonId)) {
            tongMonMap.get(tongMonId).members.put(target.getUniqueId(), role);
            saveData();
            return true;
        }
        return false;
    }

    public boolean removeMember(UUID tongMonId, Player target) {
        if (tongMonMap.containsKey(tongMonId)) {
            tongMonMap.get(tongMonId).members.remove(target.getUniqueId());
            saveData();
            return true;
        }
        return false;
    }

    public UUID getTongMonIdOfPlayer(Player player) {
        for (Map.Entry<UUID, TongMonInfo> entry : tongMonMap.entrySet()) {
            if (entry.getValue().members.containsKey(player.getUniqueId())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean hasTongMon(Player player) {
        return tongMonMap.containsKey(player.getUniqueId());
    }

    public int getTongMonCount() {
        return tongMonCount;
    }

    public void addTaiNguyen(UUID tongMonId, int amount) {
        if (tongMonMap.containsKey(tongMonId)) {
            tongMonMap.get(tongMonId).taiNguyen += amount;
            saveData();
        }
    }

    /**
     * Lấy tọa độ an toàn để truyền tống về Tông Môn trên Skyblock (không rơi xuống Void).
     */
    public Location getTongMonLocation(UUID tongMonId) {
        if (tongMonMap.containsKey(tongMonId)) {
            TongMonInfo info = tongMonMap.get(tongMonId);
            World world = Bukkit.getWorld(info.world);
            if (world != null) {
                Block ground = world.getBlockAt(info.centerX, info.centerY - 1, info.centerZ);
                if (ground.getType().isSolid()) {
                    return new Location(world, info.centerX + 0.5, info.centerY, info.centerZ + 0.5);
                }
                int highestY = world.getHighestBlockYAt(info.centerX, info.centerZ);
                if (highestY > world.getMinHeight()) {
                    return new Location(world, info.centerX + 0.5, highestY + 1, info.centerZ + 0.5);
                }
            }
        }
        return null;
    }

    public UUID checkKhongChoPhepTuongTac(Location loc, Player player) {
        for (Map.Entry<UUID, TongMonInfo> entry : tongMonMap.entrySet()) {
            TongMonInfo info = entry.getValue();
            if (loc.getWorld() == null || !loc.getWorld().getName().equals(info.world))
                continue;

            if (Math.abs(loc.getBlockX() - info.centerX) <= info.radius &&
                    Math.abs(loc.getBlockZ() - info.centerZ) <= info.radius) {
                if (info.members.containsKey(player.getUniqueId())) {
                    return null;
                } else {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public String getTenTongMon(UUID owner) {
        if (tongMonMap.containsKey(owner)) {
            return tongMonMap.get(owner).name;
        }
        return "Vô Danh Tông";
    }

    private static class TongMonInfo {
        String name;
        int centerX;
        int centerY;
        int centerZ;
        int radius;
        String world;
        int taiNguyen;
        Map<UUID, String> members;

        TongMonInfo(String name, int cx, int cy, int cz, int radius, String world) {
            this.name = name;
            this.centerX = cx;
            this.centerY = cy;
            this.centerZ = cz;
            this.radius = radius;
            this.world = world;
            this.taiNguyen = 0;
            this.members = new HashMap<>();
        }
    }
}
