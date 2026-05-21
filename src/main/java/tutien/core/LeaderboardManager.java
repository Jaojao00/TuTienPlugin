package tutien.core;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Quản lý bảng xếp hạng — cache Top 10 cho nhiều hạng mục.
 * v2.1: Refresh tự động mỗi 5 phút.
 */
public class LeaderboardManager {

    public record LeaderEntry(String playerName, UUID uuid, long value, String extra) {}

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;

    private List<LeaderEntry> topCanhGioi = new ArrayList<>();
    private List<LeaderEntry> topTuVi = new ArrayList<>();
    private List<LeaderEntry> topKhoangThach = new ArrayList<>();
    private List<LeaderEntry> topDaoNiem = new ArrayList<>();
    private List<LeaderEntry> topBeQuanDiem = new ArrayList<>();

    public LeaderboardManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
        // Refresh mỗi 5 phút (6000 ticks) — chạy async
        new BukkitRunnable() {
            @Override
            public void run() {
                refresh();
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 6000L);
    }

    public void refresh() {
        Set<UUID> allUUIDs = dataManager.getAllStoredUUIDs();
        List<Map.Entry<String, Map<String, Object>>> allData = new ArrayList<>();

        for (UUID uuid : allUUIDs) {
            Map<String, Object> data = dataManager.readOfflineData(uuid);
            if (data.isEmpty()) continue;
            allData.add(Map.entry(uuid.toString(), data));
        }

        // Sort & build top lists
        topTuVi = buildTop(allData, "TuVi", null);
        topKhoangThach = buildTop(allData, "KhoangThach", null);
        topDaoNiem = buildTop(allData, "DaoNiem", null);
        topBeQuanDiem = buildTop(allData, "BeQuanDiem", null);

        // Cảnh giới: sort by ordinal
        topCanhGioi = allData.stream()
            .map(e -> {
                Map<String, Object> d = e.getValue();
                String name = (String) d.getOrDefault("TenNguoiChoi", "???");
                String cgStr = (String) d.getOrDefault("CanhGioi", "PHAM_NHAN");
                int ordinal = 0;
                try { ordinal = CanhGioi.valueOf(cgStr).ordinal(); } catch (Exception ignored) {}
                String tenCG = "";
                try { tenCG = CanhGioi.valueOf(cgStr).getTenHienThi(); } catch (Exception ignored) { tenCG = cgStr; }
                return new LeaderEntry(name, UUID.fromString(e.getKey()), ordinal, tenCG);
            })
            .sorted((a, b) -> Long.compare(b.value(), a.value()))
            .limit(10)
            .collect(Collectors.toList());
    }

    private List<LeaderEntry> buildTop(List<Map.Entry<String, Map<String, Object>>> allData, String key, String extraKey) {
        return allData.stream()
            .map(e -> {
                Map<String, Object> d = e.getValue();
                String name = (String) d.getOrDefault("TenNguoiChoi", "???");
                long val = 0;
                Object obj = d.get(key);
                if (obj instanceof Number n) val = n.longValue();
                String extra = extraKey != null ? String.valueOf(d.getOrDefault(extraKey, "")) : null;
                return new LeaderEntry(name, UUID.fromString(e.getKey()), val, extra);
            })
            .sorted((a, b) -> Long.compare(b.value(), a.value()))
            .limit(10)
            .collect(Collectors.toList());
    }

    // Getters
    public List<LeaderEntry> getTopCanhGioi()    { return topCanhGioi; }
    public List<LeaderEntry> getTopTuVi()        { return topTuVi; }
    public List<LeaderEntry> getTopKhoangThach() { return topKhoangThach; }
    public List<LeaderEntry> getTopDaoNiem()     { return topDaoNiem; }
    public List<LeaderEntry> getTopBeQuanDiem()  { return topBeQuanDiem; }

    /**
     * Lấy entry top N cho 1 hạng mục.
     * @param category: canhgioi, tuvi, khoangthach, daoniem, bequandiem
     * @param rank: 1-10
     */
    public LeaderEntry getEntry(String category, int rank) {
        List<LeaderEntry> list = switch (category.toLowerCase()) {
            case "canhgioi" -> topCanhGioi;
            case "tuvi" -> topTuVi;
            case "khoangthach" -> topKhoangThach;
            case "daoniem" -> topDaoNiem;
            case "bequandiem" -> topBeQuanDiem;
            default -> Collections.emptyList();
        };
        if (rank < 1 || rank > list.size()) return null;
        return list.get(rank - 1);
    }
}
