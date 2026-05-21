package tutien.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Lớp quản lý toàn bộ dữ liệu cốt lõi: Tu Vi, Cảnh Giới, Linh Căn, Linh Lực, Hệ Tu Luyện.
 * ĐÃ NÂNG CẤP: Thêm hệ thống Luyện Đan (cấp lò, số lần luyện, chỉ số đan dược vĩnh viễn).
 */
public class PlayerDataManager {

    private final TuTienPlugin plugin;

    // Các bản đồ (HashMap) lưu trữ dữ liệu trên RAM
    private final HashMap<UUID, Integer> tuViMap = new HashMap<>();
    private final HashMap<UUID, CanhGioi> canhGioiMap = new HashMap<>();
    private final HashMap<UUID, LinhCan> linhCanMap = new HashMap<>();
    private final HashMap<UUID, Integer> linhLucMap = new HashMap<>();
    private final HashMap<UUID, HeTuLuyen> heTuLuyenMap = new HashMap<>();

    // [THÊM MỚI]: Quản lý trạng thái Tọa Thiền và Kho dược lực (Digestion Buffer)
    private final HashMap<UUID, Boolean> tuLuyenMode = new HashMap<>();
    private final HashMap<UUID, Integer> digestionBuffer = new HashMap<>();

    // =============================================
    // [HỆ THỐNG LUYỆN ĐAN] - Dữ liệu mới
    // =============================================
    private final HashMap<UUID, Integer> danLoCapDo = new HashMap<>();
    private final HashMap<UUID, Integer> soLanLuyenDan = new HashMap<>();
    private final HashMap<UUID, Double> bonusDamage = new HashMap<>();
    private final HashMap<UUID, Double> bonusHealth = new HashMap<>();
    private final HashMap<UUID, Integer> bonusTuVi = new HashMap<>();
    private final HashMap<UUID, Boolean> autoLuyenDan = new HashMap<>();
    private final HashMap<UUID, Boolean> autoUseDan = new HashMap<>();

    // =============================================
    // [SPRINT 2] - Bế Quan điểm + Độ Kiếp cooldown
    // =============================================
    private final HashMap<UUID, Long> beQuanDiem = new HashMap<>();      // Tổng điểm bế quan tích lũy
    private final HashMap<UUID, Long> dokiepCooldown = new HashMap<>();   // Timestamp lần độ kiếp gần nhất

    // v2.1: Khoáng Thạch & Đạo Niệm (tiền tệ ảo)
    private final HashMap<UUID, Long> khoangThachMap = new HashMap<>();
    private final HashMap<UUID, Long> daoNiemMap = new HashMap<>();

    public PlayerDataManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    private File getPlayerFile(UUID uuid) {
        File folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, uuid.toString() + ".yml");
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid);

        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            tuViMap.put(uuid, config.getInt("TuVi", 0));
            linhLucMap.put(uuid, config.getInt("LinhLuc", 100));

            // Tải lượng dược lực đang tiêu hóa dở từ file
            digestionBuffer.put(uuid, config.getInt("DigestionBuffer", 0));

            try { canhGioiMap.put(uuid, CanhGioi.valueOf(config.getString("CanhGioi", CanhGioi.PHAM_NHAN.name()))); }
            catch (IllegalArgumentException e) { canhGioiMap.put(uuid, CanhGioi.PHAM_NHAN); }

            try { linhCanMap.put(uuid, LinhCan.valueOf(config.getString("LinhCan", LinhCan.PHE_LINH_CAN.name()))); }
            catch (IllegalArgumentException e) { linhCanMap.put(uuid, LinhCan.PHE_LINH_CAN); }

            try { heTuLuyenMap.put(uuid, HeTuLuyen.valueOf(config.getString("HeTuLuyen", HeTuLuyen.CHUA_CHON.name()))); }
            catch (IllegalArgumentException e) { heTuLuyenMap.put(uuid, HeTuLuyen.CHUA_CHON); }

            // Tải dữ liệu Luyện Đan
            danLoCapDo.put(uuid, config.getInt("DanLo.CapDo", 1));
            soLanLuyenDan.put(uuid, config.getInt("DanLo.SoLanLuyen", 0));
            bonusDamage.put(uuid, config.getDouble("DanLo.BonusDamage", 0.0));
            bonusHealth.put(uuid, config.getDouble("DanLo.BonusHealth", 0.0));
            bonusTuVi.put(uuid, config.getInt("DanLo.BonusTuVi", 0));
            autoLuyenDan.put(uuid, config.getBoolean("DanLo.AutoLuyen", false));
            autoUseDan.put(uuid, config.getBoolean("DanLo.AutoUse", false));

            // Tải Sprints 2 - Bế Quan & Cooldown
            beQuanDiem.put(uuid, config.getLong("BeQuan.Diem", 0L));
            dokiepCooldown.put(uuid, 0L); // Không lưu cooldown vào file (reset mỗi lần vào)
            khoangThachMap.put(uuid, config.getLong("KhoangThach", 0L));
            daoNiemMap.put(uuid, config.getLong("DaoNiem", 0L));

        } else {
            // Khởi tạo cho người chơi mới
            tuViMap.put(uuid, 0);
            canhGioiMap.put(uuid, CanhGioi.PHAM_NHAN);
            linhLucMap.put(uuid, 100);
            heTuLuyenMap.put(uuid, HeTuLuyen.CHUA_CHON);
            digestionBuffer.put(uuid, 0);

            // Khởi tạo Luyện Đan mặc định
            danLoCapDo.put(uuid, 1);
            soLanLuyenDan.put(uuid, 0);
            bonusDamage.put(uuid, 0.0);
            bonusHealth.put(uuid, 0.0);
            bonusTuVi.put(uuid, 0);
            autoLuyenDan.put(uuid, false);
            autoUseDan.put(uuid, false);
            beQuanDiem.put(uuid, 0L);
            dokiepCooldown.put(uuid, 0L);
            khoangThachMap.put(uuid, 0L);
            daoNiemMap.put(uuid, 0L);

            LinhCan randomLC = LinhCan.randomLinhCan();
            linhCanMap.put(uuid, randomLC);

            player.sendMessage("§e§l[Thiên Đạo] §fNgươi vừa thức tỉnh được " + randomLC.getTenHienThi() + "§f!");
        }
    }

    public void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        writeDataToConfig(player, config);

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Khong the luu du lieu cho " + player.getName()); }

        // Dọn dẹp RAM (chỉ gọi khi player thoát)
        clearFromRam(uuid);
    }

    /**
     * Lưu dữ liệu KHÔNG xóa RAM — dùng cho AutoSave định kỳ.
     * Gọi trên async thread để không block main thread.
     */
    public void savePlayerAsync(Player player) {
        UUID uuid = player.getUniqueId();
        if (!tuViMap.containsKey(uuid)) return; // Chưa load, bỏ qua

        File file = getPlayerFile(uuid);
        FileConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

        // Snapshot data an toàn (tránh race condition)
        synchronized (this) {
            writeDataToConfig(player, config);
        }

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().warning("[AutoSave] Không thể lưu " + player.getName()); }
        // KHÔNG xóa RAM — player vẫn đang online
    }

    private void writeDataToConfig(Player player, FileConfiguration config) {
        UUID uuid = player.getUniqueId();
        config.set("TuVi", getTuVi(player));
        config.set("CanhGioi", getCanhGioi(player).name());
        config.set("LinhCan", getLinhCan(player).name());
        config.set("LinhLuc", getLinhLuc(player));
        config.set("HeTuLuyen", getHeTuLuyen(player).name());
        config.set("DigestionBuffer", getDigestionBuffer(player));
        config.set("DanLo.CapDo", getDanLoCapDo(player));
        config.set("DanLo.SoLanLuyen", getSoLanLuyenDan(player));
        config.set("DanLo.BonusDamage", getBonusDamage(player));
        config.set("DanLo.BonusHealth", getBonusHealth(player));
        config.set("DanLo.BonusTuVi", getBonusTuVi(player));
        config.set("DanLo.AutoLuyen", isAutoLuyenDan(player));
        config.set("DanLo.AutoUse", isAutoUseDan(player));
        // Sprint 2: Bế Quan
        config.set("BeQuan.Diem", getBeQuanDiem(player));
        config.set("KhoangThach", khoangThachMap.getOrDefault(uuid, 0L));
        config.set("DaoNiem", daoNiemMap.getOrDefault(uuid, 0L));
        config.set("TenNguoiChoi", player.getName());
    }

    private void clearFromRam(UUID uuid) {
        tuViMap.remove(uuid);
        canhGioiMap.remove(uuid);
        linhCanMap.remove(uuid);
        linhLucMap.remove(uuid);
        heTuLuyenMap.remove(uuid);
        tuLuyenMode.remove(uuid);
        digestionBuffer.remove(uuid);
        danLoCapDo.remove(uuid);
        soLanLuyenDan.remove(uuid);
        bonusDamage.remove(uuid);
        bonusHealth.remove(uuid);
        bonusTuVi.remove(uuid);
        autoLuyenDan.remove(uuid);
        autoUseDan.remove(uuid);
        beQuanDiem.remove(uuid);
        dokiepCooldown.remove(uuid);
        khoangThachMap.remove(uuid); daoNiemMap.remove(uuid);
    }

    public void addTuVi(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int currentTuVi = getTuVi(player) + amount;
        tuViMap.put(uuid, Math.max(0, currentTuVi));

        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent("§a" + (amount >= 0 ? "+" : "") + amount + " Tu Vi §8| §eTổng: " + getTuVi(player)));

        kiemTraSanSangDoKiep(player);
    }

    private void kiemTraSanSangDoKiep(Player player) {
        CanhGioi hienTai = getCanhGioi(player);
        CanhGioi tiepTheo = hienTai.getCanhGioiTiepTheo();
        int currentTuVi = getTuVi(player);

        if (tiepTheo != null && currentTuVi >= tiepTheo.getTuViYeuCau()) {
            player.sendMessage("§e§l[Thiên Đạo] §fTu vi của bạn đã đạt bình cảnh! Hãy tìm nơi an toàn và dùng lệnh §c/tutien dokiep §fđể đột phá!");
        }
    }

    // --- CÁC PHƯƠNG THỨC TỌA THIỀN & TIÊU HÓA ---

    public boolean isDangTuLuyen(Player p) {
        return tuLuyenMode.getOrDefault(p.getUniqueId(), false);
    }

    public void setTuLuyenMode(Player p, boolean status) {
        tuLuyenMode.put(p.getUniqueId(), status);
    }

    public int getDigestionBuffer(Player p) {
        return digestionBuffer.getOrDefault(p.getUniqueId(), 0);
    }

    public void addDigestionBuffer(Player p, int amount) {
        digestionBuffer.put(p.getUniqueId(), getDigestionBuffer(p) + amount);
    }

    public void setDigestionBuffer(Player p, int amount) {
        digestionBuffer.put(p.getUniqueId(), Math.max(0, amount));
    }

    // =============================================
    // HỆ THỐNG LUYỆN ĐAN - GETTER / SETTER
    // =============================================

    public int getDanLoCapDo(Player p) { return danLoCapDo.getOrDefault(p.getUniqueId(), 1); }
    public void setDanLoCapDo(Player p, int cap) { danLoCapDo.put(p.getUniqueId(), cap); }

    public int getSoLanLuyenDan(Player p) { return soLanLuyenDan.getOrDefault(p.getUniqueId(), 0); }
    public void addSoLanLuyenDan(Player p, int amount) { soLanLuyenDan.put(p.getUniqueId(), getSoLanLuyenDan(p) + amount); }

    public double getBonusDamage(Player p) { return bonusDamage.getOrDefault(p.getUniqueId(), 0.0); }
    public void addBonusDamage(Player p, double amount) { bonusDamage.put(p.getUniqueId(), getBonusDamage(p) + amount); }

    public double getBonusHealth(Player p) { return bonusHealth.getOrDefault(p.getUniqueId(), 0.0); }
    public void addBonusHealth(Player p, double amount) { bonusHealth.put(p.getUniqueId(), getBonusHealth(p) + amount); }

    public int getBonusTuVi(Player p) { return bonusTuVi.getOrDefault(p.getUniqueId(), 0); }
    public void addBonusTuVi(Player p, int amount) { bonusTuVi.put(p.getUniqueId(), getBonusTuVi(p) + amount); }

    public boolean isAutoLuyenDan(Player p) { return autoLuyenDan.getOrDefault(p.getUniqueId(), false); }
    public void toggleAutoLuyenDan(Player p) { autoLuyenDan.put(p.getUniqueId(), !isAutoLuyenDan(p)); }

    public boolean isAutoUseDan(Player p) { return autoUseDan.getOrDefault(p.getUniqueId(), false); }
    public void toggleAutoUseDan(Player p) { autoUseDan.put(p.getUniqueId(), !isAutoUseDan(p)); }

    // --- CÁC HÀM GETTER / SETTER QUAN TRỌNG ---

    public CanhGioi getCanhGioi(Player player) {
        return canhGioiMap.getOrDefault(player.getUniqueId(), CanhGioi.PHAM_NHAN);
    }

    public void setCanhGioi(Player player, CanhGioi cg) {
        canhGioiMap.put(player.getUniqueId(), cg);
        tutien.combat.StatsManager.applyStats(player, this);
    }

    public int getTuVi(Player player) { return tuViMap.getOrDefault(player.getUniqueId(), 0); }
    public void setTuVi(Player player, int amount) { tuViMap.put(player.getUniqueId(), Math.max(0, amount)); }

    public LinhCan getLinhCan(Player player) { return linhCanMap.getOrDefault(player.getUniqueId(), LinhCan.PHE_LINH_CAN); }
    public void setLinhCan(Player player, LinhCan lc) { linhCanMap.put(player.getUniqueId(), lc); }

    public int getLinhLuc(Player player) { return linhLucMap.getOrDefault(player.getUniqueId(), 100); }
    public void setLinhLuc(Player player, int amount) { linhLucMap.put(player.getUniqueId(), amount); }

    public HeTuLuyen getHeTuLuyen(Player player) { return heTuLuyenMap.getOrDefault(player.getUniqueId(), HeTuLuyen.CHUA_CHON); }
    public void setHeTuLuyen(Player player, HeTuLuyen he) {
        heTuLuyenMap.put(player.getUniqueId(), he);
        tutien.combat.StatsManager.applyStats(player, this);
    }

    // =============================================
    // [SPRINT 2] BẾ QUAN ĐIỂM
    // =============================================
    public long getBeQuanDiem(Player p) { return beQuanDiem.getOrDefault(p.getUniqueId(), 0L); }
    public void addBeQuanDiem(Player p, long amount) { beQuanDiem.put(p.getUniqueId(), getBeQuanDiem(p) + amount); }
    public void setBeQuanDiem(Player p, long amount) { beQuanDiem.put(p.getUniqueId(), Math.max(0, amount)); }

    // =============================================
    // [SPRINT 2] ĐỘ KIẾP COOLDOWN
    // =============================================
    public long getDokiepCooldown(Player p) { return dokiepCooldown.getOrDefault(p.getUniqueId(), 0L); }
    public void setDokiepCooldown(Player p, long timestamp) { dokiepCooldown.put(p.getUniqueId(), timestamp); }
    public boolean isDokiepOnCooldown(Player p) {
        return (System.currentTimeMillis() - getDokiepCooldown(p)) < 30_000L; // 30 giây
    }

    // =============================================
    // [SPRINT 2] PHÁP TU: Lấy Max Linh Lực có bonus +30%
    // =============================================
    public int getMaxLinhLucEffective(Player p) {
        int base = getCanhGioi(p).getMaxLinhLuc();
        if (getHeTuLuyen(p) == HeTuLuyen.PHAP_TU) {
            return (int)(base * 1.30);
        }
        return base;
    }

    // v2.1: Khoáng Thạch
    public long getKhoangThach(Player p) { return khoangThachMap.getOrDefault(p.getUniqueId(), 0L); }
    public void addKhoangThach(Player p, long amount) { khoangThachMap.put(p.getUniqueId(), getKhoangThach(p) + amount); }
    public void setKhoangThach(Player p, long amount) { khoangThachMap.put(p.getUniqueId(), Math.max(0, amount)); }

    // v2.1: Đạo Niệm
    public long getDaoNiem(Player p) { return daoNiemMap.getOrDefault(p.getUniqueId(), 0L); }
    public void addDaoNiem(Player p, long amount) { daoNiemMap.put(p.getUniqueId(), getDaoNiem(p) + amount); }
    public void setDaoNiem(Player p, long amount) { daoNiemMap.put(p.getUniqueId(), Math.max(0, amount)); }

    // v2.1: Lấy tất cả UUID đã lưu (dùng cho Leaderboard)
    public java.util.Set<UUID> getAllStoredUUIDs() {
        File folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) return java.util.Collections.emptySet();
        java.util.Set<UUID> result = new java.util.HashSet<>();
        for (File f : folder.listFiles()) {
            if (f.getName().endsWith(".yml")) {
                try { result.add(UUID.fromString(f.getName().replace(".yml", ""))); }
                catch (Exception ignored) {}
            }
        }
        return result;
    }

    // v2.1: Đọc data offline player (cho Leaderboard)
    public java.util.Map<String, Object> readOfflineData(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return java.util.Collections.emptyMap();
        org.bukkit.configuration.file.FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("TenNguoiChoi", config.getString("TenNguoiChoi", "???"));
        data.put("TuVi", config.getInt("TuVi", 0));
        data.put("CanhGioi", config.getString("CanhGioi", "PHAM_NHAN"));
        data.put("KhoangThach", config.getLong("KhoangThach", 0L));
        data.put("DaoNiem", config.getLong("DaoNiem", 0L));
        data.put("BeQuanDiem", config.getLong("BeQuan.Diem", 0L));
        return data;
    }
}