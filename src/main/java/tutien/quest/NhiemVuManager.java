package tutien.quest;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import tutien.core.PlayerDataManager;

import java.util.*;

/**
 * Quản lý hệ thống nhiệm vụ hàng ngày.
 * v2.2 Skyblock Edition:
 *   - Thêm nhiệm vụ "Khai Thác Linh Khoáng" (Đào đá/quặng tại máy Generator Skyblock).
 *   - Thưởng thêm Đạo Niệm và Khoáng Thạch khi hoàn thành nhiệm vụ hàng ngày.
 */
public class NhiemVuManager {

    public enum NhiemVu {
        TU_LUYEN ("Tu Luyện Cơ Bản",     "§7Tọa thiền §e30 phút §7trong ngày",       30,  1, "beqDiem"),
        LUYEN_DAN("Thợ Luyện Đan",        "§7Luyện §e20 viên đan §7trong ngày",       20,  2, "danCount"),
        CHE_TAC  ("Rèn Trang Bị",         "§7Chế tác §e5 món trang bị",               5,   3, "cheCount"),
        XAY_DUNG ("Kiến Thiết Đảo Tiên",  "§7Đặt §e30 khối §7trên đảo / Tông Môn",   30,  1, "buildCount"),
        DANH_QUAI("Trừ Ma Vệ Đạo",        "§7Tiêu diệt §e50 quái vật §7bất kỳ",      50,  2, "killCount"),
        KHAI_MO  ("Khai Thác Linh Khoáng","§7Đào §e100 khối đá/quặng §7tại Generator", 100, 2, "mineCount");

        public final String tenHienThi;
        public final String moTa;
        public final int mucTieu;
        public final int rewardMultiplier;
        public final String trackKey;

        NhiemVu(String ten, String moTa, int mucTieu, int mult, String key) {
            this.tenHienThi = ten;
            this.moTa = moTa;
            this.mucTieu = mucTieu;
            this.rewardMultiplier = mult;
            this.trackKey = key;
        }
    }

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

    private void checkReset(UUID uuid) {
        long today = getTodayDay();
        if (lastResetDate.getOrDefault(uuid, 0L) < today) {
            progress.put(uuid, new EnumMap<>(NhiemVu.class));
            completed.put(uuid, new HashSet<>());
            lastResetDate.put(uuid, today);
        }
    }

    private long getTodayDay() {
        return System.currentTimeMillis() / (1000L * 60 * 60 * 24);
    }

    public void addProgress(Player player, NhiemVu nv, int amount) {
        UUID uuid = player.getUniqueId();
        initPlayer(uuid);
        if (completed.get(uuid).contains(nv)) return;

        Map<NhiemVu, Integer> prog = progress.get(uuid);
        int current = prog.getOrDefault(nv, 0) + amount;
        prog.put(nv, current);

        if (current >= nv.mucTieu) {
            completed.get(uuid).add(nv);
            giveReward(player, nv);
        }
    }

    private void giveReward(Player player, NhiemVu nv) {
        long bqReward = nv.rewardMultiplier * 50L;
        int tuViReward = nv.rewardMultiplier * 200;
        long daoNiemReward = nv.rewardMultiplier * 5L;
        long khoangThachReward = nv.rewardMultiplier * 2L;

        dataManager.addBeQuanDiem(player, bqReward);
        dataManager.addTuVi(player, tuViReward);
        dataManager.addDaoNiem(player, daoNiemReward);
        dataManager.addKhoangThach(player, khoangThachReward);

        player.sendMessage("§6§l[Nhiệm Vụ] §fHoàn thành: §e" + nv.tenHienThi + "§f!");
        player.sendMessage("§7Phần thưởng: §e+" + bqReward + " Điểm Bế Quan §7| §e+" + tuViReward + " Tu Vi §7| §b+" + daoNiemReward + " Đạo Niệm §7| §6+" + khoangThachReward + " Khoáng Thạch");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
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
        progress.remove(uuid);
        completed.remove(uuid);
        lastResetDate.remove(uuid);
    }
}
