package tutien.tongmon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
 * Cập nhật: Đảm bảo tích hợp đầy đủ lệnh truyền tống (Về Tông Môn).
 */
public class TongMonManager {

    private final TuTienPlugin plugin;
    private final File file;
    private FileConfiguration config;

    // Lưu trữ thông tin Tông môn: UUID Chủ Tông -> Dữ liệu Tông Môn
    private final Map<UUID, TongMonInfo> tongMonMap = new HashMap<>();
    private int tongMonCount = 0; // Đếm số lượng tông môn để tính toán tọa độ đất trống

    public TongMonManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tongmon.yml");
        loadData();
    }

    public void loadData() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
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
                int cz = config.getInt(path + ".CenterZ");
                int radius = config.getInt(path + ".Radius");
                String worldName = config.getString(path + ".World");

                TongMonInfo info = new TongMonInfo(name, cx, cz, radius, worldName);
                info.taiNguyen = config.getInt(path + ".TaiNguyen", 0);

                // Nạp danh sách thành viên
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
        config.set("Sects", null); // Xóa dữ liệu cũ để ghi đè dữ liệu mới cho an toàn

        for (Map.Entry<UUID, TongMonInfo> entry : tongMonMap.entrySet()) {
            String path = "Sects." + entry.getKey().toString();
            TongMonInfo info = entry.getValue();

            config.set(path + ".Name", info.name);
            config.set(path + ".CenterX", info.centerX);
            config.set(path + ".CenterZ", info.centerZ);
            config.set(path + ".Radius", info.radius);
            config.set(path + ".World", info.world);
            config.set(path + ".TaiNguyen", info.taiNguyen);

            // Lưu thành viên
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

    // Đổi tên hàm từ saveTongMon thành createTongMon cho chuẩn logic
    public void createTongMon(Player owner, String tenTongMon, int cx, int cz, int radius) {
        TongMonInfo info = new TongMonInfo(tenTongMon, cx, cz, radius, owner.getWorld().getName());

        // Người tạo mặc định là TÔNG CHỦ
        info.members.put(owner.getUniqueId(), "TONG_CHU");

        tongMonMap.put(owner.getUniqueId(), info);
        tongMonCount++;
        saveData(); // Lưu ngay lập tức
    }

    // Thêm đệ tử vào tông môn
    public boolean addMember(UUID tongMonId, Player target, String role) {
        if (tongMonMap.containsKey(tongMonId)) {
            tongMonMap.get(tongMonId).members.put(target.getUniqueId(), role);
            saveData();
            return true;
        }
        return false;
    }

    // Đuổi đệ tử hoặc đệ tử tự rời đi
    public boolean removeMember(UUID tongMonId, Player target) {
        if (tongMonMap.containsKey(tongMonId)) {
            tongMonMap.get(tongMonId).members.remove(target.getUniqueId());
            saveData();
            return true;
        }
        return false;
    }

    // Lấy ID (UUID của chủ tông) của Tông Môn mà người chơi này đang tham gia
    public UUID getTongMonIdOfPlayer(Player player) {
        for (Map.Entry<UUID, TongMonInfo> entry : tongMonMap.entrySet()) {
            if (entry.getValue().members.containsKey(player.getUniqueId())) {
                return entry.getKey();
            }
        }
        return null; // Không thuộc tông môn nào
    }

    // Kiểm tra xem đã tạo tông môn (làm Tông Chủ) chưa
    public boolean hasTongMon(Player player) {
        return tongMonMap.containsKey(player.getUniqueId());
    }

    public int getTongMonCount() {
        return tongMonCount;
    }

    // Quản lý tài nguyên Tông Môn
    public void addTaiNguyen(UUID tongMonId, int amount) {
        if (tongMonMap.containsKey(tongMonId)) {
            tongMonMap.get(tongMonId).taiNguyen += amount;
            saveData();
        }
    }

    // [QUAN TRỌNG] Lấy tọa độ an toàn để truyền tống về Tông Môn (Hỗ trợ lệnh
    // /tutien tongmon ve)
    public Location getTongMonLocation(UUID tongMonId) {
        if (tongMonMap.containsKey(tongMonId)) {
            TongMonInfo info = tongMonMap.get(tongMonId);
            org.bukkit.World world = Bukkit.getWorld(info.world);
            if (world != null) {
                // Lấy block cao nhất để tránh bị kẹt trong lòng đất
                int highestY = world.getHighestBlockYAt(info.centerX, info.centerZ);
                return new Location(world, info.centerX + 0.5, highestY + 1, info.centerZ + 0.5);
            }
        }
        return null;
    }

    // Hàm kiểm tra xem một block có nằm trong vùng đất của ai không
    public UUID checkKhongChoPhepTuongTac(Location loc, Player player) {
        for (Map.Entry<UUID, TongMonInfo> entry : tongMonMap.entrySet()) {
            TongMonInfo info = entry.getValue();
            if (!loc.getWorld().getName().equals(info.world))
                continue;

            // Kiểm tra ranh giới
            if (Math.abs(loc.getBlockX() - info.centerX) <= info.radius &&
                    Math.abs(loc.getBlockZ() - info.centerZ) <= info.radius) {

                // Nếu người đập ĐANG NẰM TRONG DANH SÁCH THÀNH VIÊN -> Cho phép đập phá
                if (info.members.containsKey(player.getUniqueId())) {
                    return null;
                } else {
                    return entry.getKey(); // Báo lỗi: Đây là đất của Tông Môn khác
                }
            }
        }
        return null; // Đất vô chủ, tự do tương tác
    }

    public String getTenTongMon(UUID owner) {
        if (tongMonMap.containsKey(owner)) {
            return tongMonMap.get(owner).name;
        }
        return "Vô Danh Tông";
    }

    // Class nội bộ lưu dữ liệu bao gồm cả thành viên và tài nguyên
    private static class TongMonInfo {
        String name;
        int centerX;
        int centerZ;
        int radius;
        String world;
        int taiNguyen; // Kho bạc tông môn
        Map<UUID, String> members; // UUID -> Chức vụ (Ví dụ: TONG_CHU, TRUONG_LAO, DE_TU)

        TongMonInfo(String name, int cx, int cz, int radius, String world) {
            this.name = name;
            this.centerX = cx;
            this.centerZ = cz;
            this.radius = radius;
            this.world = world;
            this.taiNguyen = 0;
            this.members = new HashMap<>();
        }
    }
}