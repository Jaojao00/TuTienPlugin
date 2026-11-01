package tutien.alchemy.service;

import org.bukkit.entity.Player;
import tutien.alchemy.manager.DanDuocManager;
import tutien.alchemy.model.DanDocData;
import tutien.alchemy.model.ResistanceGroup;
import tutien.core.PlayerDataManager;

/**
 * Service quản lý mức thích nghi và Kháng Thuốc (Drug Resistance) của tu sĩ theo từng nhóm độc lập.
 */
public class DrugResistanceService {

    private final PlayerDataManager dataManager;
    private final DanDuocManager danDuocManager;

    // Bộ tích lũy thời gian ONLINE hợp lệ (ngăn chặn tính nhầm thời gian offline)
    private final java.util.Map<java.util.UUID, Integer> onlineSecondsMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Long> lastOnlineTickMap = new java.util.concurrent.ConcurrentHashMap<>();

    public DrugResistanceService(PlayerDataManager dataManager, DanDuocManager danDuocManager) {
        this.dataManager = dataManager;
        this.danDuocManager = danDuocManager;
    }

    public double getResistance(Player player, String group) {
        if (player == null || group == null) return 0.0;
        return getResistance(player.getUniqueId(), group);
    }

    public double getResistance(java.util.UUID uuid, String group) {
        if (uuid == null || group == null) return 0.0;
        DanDocData data = dataManager.getDanDocData(uuid);
        return data != null ? data.getResistance(group) : 0.0;
    }

    public void addResistance(Player player, String group, double gain) {
        if (player == null) return;
        addResistance(player.getUniqueId(), group, gain);
    }

    public void addResistance(java.util.UUID uuid, String group, double gain) {
        if (uuid == null || group == null || gain <= 0) return;
        DanDocData data = dataManager.getDanDocData(uuid);
        if (data == null) return;

        data.addResistance(group, gain);
        data.setLastDoseAt(group, System.currentTimeMillis());
    }

    public void reduceResistance(Player player, String group, double delta) {
        if (player == null) return;
        reduceResistance(player.getUniqueId(), group, delta);
    }

    public void reduceResistance(java.util.UUID uuid, String group, double delta) {
        if (uuid == null || group == null || delta <= 0) return;
        DanDocData data = dataManager.getDanDocData(uuid);
        if (data == null) return;

        data.reduceResistance(group, delta);
    }

    public double getEfficiencyMultiplier(Player player, String group) {
        if (player == null) return 1.0;
        return getEfficiencyMultiplier(player.getUniqueId(), group);
    }

    public double getEfficiencyMultiplier(java.util.UUID uuid, String group) {
        if (uuid == null || group == null) return 1.0;

        double r = getResistance(uuid, group);
        if (r <= 0.0) return 1.0;

        double c = danDuocManager != null ? danDuocManager.getResistanceReductionCoeff() : 0.60;
        double minEfficiency = danDuocManager != null ? danDuocManager.getMinResistanceEfficiency() : 0.20;

        double mr = 1.0 - (r / 100.0) * c;
        return Math.max(minEfficiency, mr);
    }

    /**
     * Xử lý suy giảm kháng thuốc theo thời gian online khi không dùng thuốc cùng nhóm.
     */
    public void processOnlineDecay(Player player, String group, double decayAmount) {
        if (player == null) return;
        processOnlineDecay(player.getUniqueId(), group, decayAmount);
    }

    public void processOnlineDecay(java.util.UUID uuid, String group, double decayAmount) {
        if (uuid == null || group == null || decayAmount <= 0) return;
        reduceResistance(uuid, group, decayAmount);
    }

    /**
     * Tích lũy thời gian online thực tế và kích hoạt suy giảm kháng thuốc khi đạt ngưỡng 180s.
     * Hoàn toàn độc lập với offline: người chơi offline bao lâu cũng không bị trừ kháng.
     */
    public void tickOnlineDecay(Player player) {
        if (player == null || !player.isOnline()) return;
        java.util.UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastTick = lastOnlineTickMap.getOrDefault(uuid, now);
        lastOnlineTickMap.put(uuid, now);

        long deltaMs = now - lastTick;
        if (deltaMs < 500 || deltaMs > 5000) {
            deltaMs = 1000;
        }
        int seconds = (int) (deltaMs / 1000);
        int accumulated = onlineSecondsMap.compute(uuid, (k, v) -> (v == null ? 0 : v) + seconds);

        int decayInterval = 180; // 3 phút online
        double decayAmount = 1.0;
        if (accumulated >= decayInterval) {
            onlineSecondsMap.put(uuid, 0);
            DanDocData data = dataManager.getDanDocData(uuid);
            if (data != null) {
                for (ResistanceGroup grp : ResistanceGroup.values()) {
                    long lastDose = data.getLastDoseAt(grp.name());
                    if (now - lastDose >= decayInterval * 1000L) {
                        reduceResistance(uuid, grp.name(), decayAmount);
                    }
                }
            }
        }
    }

    public void onPlayerQuit(java.util.UUID uuid) {
        if (uuid == null) return;
        onlineSecondsMap.remove(uuid);
        lastOnlineTickMap.remove(uuid);
    }
}
