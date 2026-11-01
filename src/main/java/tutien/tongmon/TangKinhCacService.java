package tutien.tongmon;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Tàng Kinh Các của Tông Môn (Phase 6).
 * Cho phép cống hiến công pháp vào kho lưu trữ tông môn và mượn đọc bí tịch.
 */
public class TangKinhCacService {

    private final TuTienPlugin plugin;
    private final File file;
    private FileConfiguration config;
    // SectUUID -> List of CongPhap IDs
    private final Map<UUID, Set<String>> tangKinhCacMap = new ConcurrentHashMap<>();

    public TangKinhCacService(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.file = (plugin != null) ? new File(plugin.getDataFolder(), "tangkinhcac.yml") : new File("target/tangkinhcac.yml");
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
        tangKinhCacMap.clear();

        if (config.contains("SectLibraries")) {
            for (String sectIdStr : config.getConfigurationSection("SectLibraries").getKeys(false)) {
                try {
                    UUID sId = UUID.fromString(sectIdStr);
                    List<String> list = config.getStringList("SectLibraries." + sectIdStr);
                    tangKinhCacMap.put(sId, new HashSet<>(list));
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveData() {
        if (config == null) return;
        config.set("SectLibraries", null);
        for (Map.Entry<UUID, Set<String>> entry : tangKinhCacMap.entrySet()) {
            config.set("SectLibraries." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    public boolean contributeFromPlayer(org.bukkit.entity.Player player, String congPhapId) {
        if (player == null || congPhapId == null || congPhapId.isBlank()) return false;
        if (plugin == null || plugin.getTongMonManager() == null) return false;

        // 1. Kiểm tra tư cách thành viên Tông Môn
        UUID sectId = plugin.getTongMonManager().getTongMonIdOfPlayer(player);
        if (sectId == null) {
            player.sendMessage("§c§l[Tàng Kinh Các] §fNgươi chưa gia nhập Tông Môn nào! Không thể cống hiến bí tịch.");
            return false;
        }

        // 2. Kiểm tra bí tịch đã có trong Tàng Kinh Các chưa TRƯỚC KHI TRỪ ĐỒ
        if (containsCongPhap(sectId, congPhapId)) {
            player.sendMessage("§e§l[Tàng Kinh Các] §fBí tịch này đã có trong Tàng Kinh Các của Tông Môn rồi.");
            return false;
        }

        // 3. Kiểm tra bí tịch trong túi đồ (định danh nghiêm ngặt 100% qua PDC Tag tutien:congphap_id)
        boolean hasBook = false;
        if (player.getInventory() != null) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("tutien", "congphap_id");
            for (org.bukkit.inventory.ItemStack is : player.getInventory().getContents()) {
                if (is != null && is.hasItemMeta()) {
                    boolean match = false;
                    try {
                        if (is.getItemMeta().getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                            String pdcId = is.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                            if (congPhapId.equalsIgnoreCase(pdcId)) {
                                match = true;
                            }
                        }
                    } catch (Throwable ignored) {}
                    if (match) {
                        is.setAmount(is.getAmount() - 1);
                        hasBook = true;
                        break;
                    }
                }
            }
        }

        if (!hasBook) {
            player.sendMessage("§c§l[Tàng Kinh Các] §fNgươi không sở hữu quyển bí tịch này trong túi đồ!");
            return false;
        }

        // 4. Đưa vào Tàng Kinh Các
        boolean added = contributeCongPhap(sectId, congPhapId);
        if (added) {
            if (plugin.getPlayerDataManager() != null) {
                plugin.getPlayerDataManager().addDaoNiem(player, 500); // Thưởng 500 Đạo Niệm / Cống hiến
            }
            player.sendMessage("§a§l[Tàng Kinh Các] §fĐã hiến tặng bí tịch §e" + congPhapId + " §fvào Tàng Kinh Các Tông Môn! Nhận được §6+500 Đạo Niệm§f.");
        }
        return added;
    }

    public boolean linhNgoCongPhap(org.bukkit.entity.Player player, String congPhapId) {
        if (player == null || congPhapId == null || congPhapId.isBlank()) return false;
        if (plugin == null || plugin.getTongMonManager() == null) return false;

        UUID sectId = plugin.getTongMonManager().getTongMonIdOfPlayer(player);
        if (sectId == null) {
            player.sendMessage("§c§l[Tàng Kinh Các] §fNgươi chưa gia nhập Tông Môn nào! Hãy gia nhập tông môn để lĩnh ngộ bí tịch.");
            return false;
        }

        if (!containsCongPhap(sectId, congPhapId)) {
            player.sendMessage("§c§l[Tàng Kinh Các] §fTàng Kinh Các của Tông Môn hiện chưa thu thập bí tịch §e" + congPhapId + "§f!");
            return false;
        }

        long cost = 300; // Tiêu hao 300 Đạo Niệm để lĩnh ngộ
        if (plugin.getPlayerDataManager() != null) {
            long currentDaoNiem = plugin.getPlayerDataManager().getDaoNiem(player);
            if (currentDaoNiem < cost) {
                player.sendMessage("§c§l[Tàng Kinh Các] §fKhông đủ Đạo Niệm để lĩnh ngộ bí tịch! Cần: §e" + cost + "§f, hiện có: §c" + currentDaoNiem);
                return false;
            }
        }

        // Chỉ trừ Đạo Niệm khi việc thu thập hoặc học chương thực sự thành công
        if (plugin.getCongPhapService() != null && plugin.getPlayerDataManager() != null) {
            boolean collected = plugin.getCongPhapService().collectChapter(player, plugin.getPlayerDataManager(), congPhapId, 1);
            boolean learned = plugin.getCongPhapService().learnChapter(player, plugin.getPlayerDataManager(), congPhapId, 1);
            if (!collected && !learned) {
                player.sendMessage("§c§l[Tàng Kinh Các] §fKhông thể lĩnh ngộ thêm chương này (đã đạt giới hạn hoặc không tương thích)!");
                return false;
            }
        }

        if (plugin.getPlayerDataManager() != null) {
            plugin.getPlayerDataManager().addDaoNiem(player, -cost);
        }

        player.sendMessage("§a§l[Tàng Kinh Các] §fĐạo tâm thấu suốt! Ngươi đã lĩnh ngộ thành công bí tịch §e" + congPhapId.toUpperCase() + "§f từ Tàng Kinh Các Tông Môn.");
        return true;
    }

    public boolean contributeCongPhap(UUID sectId, String congPhapId) {
        if (sectId == null || congPhapId == null || congPhapId.isBlank()) return false;
        Set<String> set = tangKinhCacMap.computeIfAbsent(sectId, k -> new HashSet<>());
        boolean added = set.add(congPhapId.toUpperCase());
        if (added) saveData();
        return added;
    }

    public boolean containsCongPhap(UUID sectId, String congPhapId) {
        if (sectId == null || congPhapId == null) return false;
        Set<String> set = tangKinhCacMap.get(sectId);
        return set != null && set.contains(congPhapId.toUpperCase());
    }

    public Set<String> getCongPhapList(UUID sectId) {
        Set<String> set = tangKinhCacMap.get(sectId);
        return (set != null) ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }
}
