package tutien.quest;

import org.bukkit.entity.Player;
import tutien.core.PlayerDataManager;

import java.util.*;

/**
 * Quản lý hệ thống nhiệm vụ hàng ngày.
 * SPRINT 3: Daily Quest giúp người chơi duy trì engagement hàng ngày.
 */
public class NhiemVuManager {

    public enum NhiemVu {
        TU_LUYEN("Tu Luyện Cơ Bản",    "§7Tọa thiền §e30 phút §7liên tục",       30,  1,  "beqDiem"),
        LUYEN_DAN("Thợ Luyện Đan",       "§7Luyện §e20 viên đan §7trong ngày",      20,  2,  "danCount"),
        CHE_TAC("Rèn Trang Bị",          "§7Chế tác §e5 món trang bị",               5,  3,  "cheCount"),
        XAY_DUNG("Tu Bổ Tông Môn",       "§7Đặt §e30 khối §7trong Tông Môn",       30,  1,  "buildCount"),
        DANH_QUAI("Hàng Ngày Giết Quái", "§7Tiêu diệt §e50 quái vật §7bất kỳ",     50,  2,  "killCount");

        public final String tenHienThi;
        public final String moTa;
        public final int mucTieu;
        public final int rewardMultiplier;
        public final String trackKey;

        NhiemVu(String ten, String moTa, int mucTieu, int mult, String key) {
            this.tenHienThi = ten; this.moTa = moTa; this.mucTieu = mucTieu;
            this.rewardMultiplier = mult; this.trackKey = key;
        }
    }

    // Lưu tiến độ nhiệm vụ hàng ngày: UUID -> (NhiemVu -> progress)
    private final Map<UUID, Map<NhiemVu, Integer>> progress = new HashMap<>();
    private final Map<UUID, Set<NhiemVu>> completed = new HashMap<>();
    private final Map<UUID, Long> lastResetDate = new HashMap<>();

    private final PlayerDataManager dataManager;

    public NhiemVuManager(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void initPlayer(UUID uuid) {
        checkReset(uuid);
        progress.computeIfAbsent(uuid, k -> new EnumMap<>(NhiemVu.class));
        completed.computeIfAbsent(uuid, k -> new HashSet<>());
    }

    /** Reset nhiệm vụ nếu sang ngày mới */
    private void checkReset(UUID uuid) {
        long today = getTodayDay();
        if (lastResetDate.getOrDefault(uuid, 0L) < today) {
            progress.put(uuid, new EnumMap<>(NhiemVu.class));
            completed.put(uuid, new HashSet<>());
            lastResetDate.put(uuid, today);
        }
    }

    private long getTodayDay() {
        return System.currentTimeMillis() / (1000 * 60 * 60 * 24);
    }

    /** Tăng tiến độ nhiệm vụ */
    public void addProgress(Player player, NhiemVu nv, int amount) {
        UUID uuid = player.getUniqueId();
        initPlayer(uuid);
        if (completed.get(uuid).contains(nv)) return; // Đã xong

        Map<NhiemVu, Integer> prog = progress.get(uuid);
        int current = prog.getOrDefault(nv, 0) + amount;
        prog.put(nv, current);

        if (current >= nv.mucTieu) {
            completed.get(uuid).add(nv);
            giveReward(player, nv);
        }
    }

    private void giveReward(Player player, NhiemVu nv) {
        // Thưởng Bế Quan điểm
        dataManager.addBeQuanDiem(player, nv.rewardMultiplier * 50L);
        // Thưởng Tu Vi
        dataManager.addTuVi(player, nv.rewardMultiplier * 200);

        player.sendMessage("§6§l[Nhiệm Vụ] §fHoàn thành: §e" + nv.tenHienThi + "§f!");
        player.sendMessage("§7Phần thưởng: §e" + (nv.rewardMultiplier * 50) + " §7Điểm Bế Quan + §e" + (nv.rewardMultiplier * 200) + " §7Tu Vi");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public int getProgress(Player player, NhiemVu nv) {
        initPlayer(player.getUniqueId());
        return progress.get(player.getUniqueId()).getOrDefault(nv, 0);
    }

    public boolean isCompleted(Player player, NhiemVu nv) {
        initPlayer(player.getUniqueId());
        return completed.get(player.getUniqueId()).contains(nv);
    }

    public void cleanupPlayer(UUID uuid) {
        progress.remove(uuid); completed.remove(uuid); lastResetDate.remove(uuid);
    }
}
