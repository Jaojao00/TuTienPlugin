package tutien.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp quản lý toàn bộ dữ liệu cốt lõi: Tu Vi, Cảnh Giới, Linh Căn, Linh Lực, Hệ Tu Luyện.
 * v2.2 Skyblock Edition:
 * - Chuyển toàn bộ bộ nhớ đệm sang ConcurrentHashMap để an toàn đa luồng (Async AutoSave & Leaderboard).
 * - Lưu trữ bền vững Cấp Lò Rèn, Số Lần Chế Tác, Pity Gacha và Đan Dược chọn tự động luyện.
 * - Hỗ trợ lấy dữ liệu trực tiếp từ RAM cho người chơi đang Online khi tính Bảng Xếp Hạng.
 */
public class PlayerDataManager {

    private final TuTienPlugin plugin;

    // Các bản đồ (ConcurrentHashMap) lưu trữ dữ liệu trên RAM an toàn đa luồng
    private final Map<UUID, Integer> tuViMap = new ConcurrentHashMap<>();
    private final Map<UUID, CanhGioi> canhGioiMap = new ConcurrentHashMap<>();
    private final Map<UUID, LinhCan> linhCanMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> linhLucMap = new ConcurrentHashMap<>();
    private final Map<UUID, HeTuLuyen> heTuLuyenMap = new ConcurrentHashMap<>();

    // Quản lý trạng thái Tọa Thiền và Kho dược lực (Digestion Buffer)
    private final Map<UUID, Boolean> tuLuyenMode = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> digestionBuffer = new ConcurrentHashMap<>();

    // =============================================
    // [HỆ THỐNG LUYỆN ĐAN]
    // =============================================
    private final Map<UUID, Integer> danLoCapDo = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> soLanLuyenDan = new ConcurrentHashMap<>();
    private final Map<UUID, Double> bonusDamage = new ConcurrentHashMap<>();
    private final Map<UUID, Double> bonusHealth = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bonusTuVi = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> autoLuyenDan = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> autoUseDan = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSelectedPill = new ConcurrentHashMap<>();

    // =============================================
    // [SPRINT 2-4 & v2.2] - Bế Quan, Chế Tác, Gacha, Tiền tệ
    // =============================================
    private final Map<UUID, Long> beQuanDiem = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dokiepCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> khoangThachMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> daoNiemMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> capCheMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> soLanCheMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> gachaPityMap = new ConcurrentHashMap<>();

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
            lastSelectedPill.put(uuid, config.getString("DanLo.LastPill", "TICH_COC_DAN"));

            // Tải Bế Quan, Tiền tệ, Chế Tác, Gacha
            beQuanDiem.put(uuid, config.getLong("BeQuan.Diem", 0L));
            dokiepCooldown.put(uuid, 0L);
            khoangThachMap.put(uuid, config.getLong("KhoangThach", 0L));
            daoNiemMap.put(uuid, config.getLong("DaoNiem", 0L));
            capCheMap.put(uuid, config.getInt("CheTac.CapDo", 1));
            soLanCheMap.put(uuid, config.getInt("CheTac.SoLanChe", 0));
            gachaPityMap.put(uuid, config.getInt("Gacha.Pity", 0));

        } else {
            // Khởi tạo cho người chơi mới
            tuViMap.put(uuid, 0);
            canhGioiMap.put(uuid, CanhGioi.PHAM_NHAN);
            linhLucMap.put(uuid, 100);
            heTuLuyenMap.put(uuid, HeTuLuyen.CHUA_CHON);
            digestionBuffer.put(uuid, 0);

            danLoCapDo.put(uuid, 1);
            soLanLuyenDan.put(uuid, 0);
            bonusDamage.put(uuid, 0.0);
            bonusHealth.put(uuid, 0.0);
            bonusTuVi.put(uuid, 0);
            autoLuyenDan.put(uuid, false);
            autoUseDan.put(uuid, false);
            lastSelectedPill.put(uuid, "TICH_COC_DAN");

            beQuanDiem.put(uuid, 0L);
            dokiepCooldown.put(uuid, 0L);
            khoangThachMap.put(uuid, 0L);
            daoNiemMap.put(uuid, 0L);
            capCheMap.put(uuid, 1);
            soLanCheMap.put(uuid, 0);
            gachaPityMap.put(uuid, 0);

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
     */
    public void savePlayerAsync(Player player) {
        UUID uuid = player.getUniqueId();
        if (!tuViMap.containsKey(uuid)) return;

        File file = getPlayerFile(uuid);
        FileConfiguration config = new YamlConfiguration();

        synchronized (this) {
            writeDataToConfig(player, config);
        }

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().warning("[AutoSave] Không thể lưu " + player.getName()); }
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
        config.set("DanLo.LastPill", getLastSelectedPill(player));
        config.set("BeQuan.Diem", getBeQuanDiem(player));
        config.set("KhoangThach", khoangThachMap.getOrDefault(uuid, 0L));
        config.set("DaoNiem", daoNiemMap.getOrDefault(uuid, 0L));
        config.set("CheTac.CapDo", getCapChe(player));
        config.set("CheTac.SoLanChe", getSoLanChe(player));
        config.set("Gacha.Pity", getGachaPity(player));
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
        lastSelectedPill.remove(uuid);
        beQuanDiem.remove(uuid);
        dokiepCooldown.remove(uuid);
        khoangThachMap.remove(uuid);
        daoNiemMap.remove(uuid);
        capCheMap.remove(uuid);
        soLanCheMap.remove(uuid);
        gachaPityMap.remove(uuid);
    }

    public void addTuVi(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int prevTuVi = getTuVi(player);
        int currentTuVi = Math.max(0, prevTuVi + amount);
        tuViMap.put(uuid, currentTuVi);

        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent("§a" + (amount >= 0 ? "+" : "") + amount + " Tu Vi §8| §eTổng: " + currentTuVi));

        kiemTraSanSangDoKiep(player, prevTuVi, currentTuVi);
    }

    private void kiemTraSanSangDoKiep(Player player, int prevTuVi, int currentTuVi) {
        CanhGioi hienTai = getCanhGioi(player);
        CanhGioi tiepTheo = hienTai.getCanhGioiTiepTheo();

        // Chỉ thông báo khi vừa cán mốc bình cảnh (tránh spam mỗi giây)
        if (tiepTheo != null && prevTuVi < tiepTheo.getTuViYeuCau() && currentTuVi >= tiepTheo.getTuViYeuCau()) {
            player.sendMessage("§e§l[Thiên Đạo] §fTu vi của bạn đã đạt bình cảnh! Hãy chuẩn bị đan dược và dùng §c/tutien dokiep §fđể đột phá!");
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

    public String getLastSelectedPill(Player p) { return lastSelectedPill.getOrDefault(p.getUniqueId(), "TICH_COC_DAN"); }
    public void setLastSelectedPill(Player p, String pillKey) { if (pillKey != null) lastSelectedPill.put(p.getUniqueId(), pillKey); }

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
    public void setLinhLuc(Player player, int amount) { linhLucMap.put(player.getUniqueId(), Math.max(0, amount)); }

    public HeTuLuyen getHeTuLuyen(Player player) { return heTuLuyenMap.getOrDefault(player.getUniqueId(), HeTuLuyen.CHUA_CHON); }
    public void setHeTuLuyen(Player player, HeTuLuyen he) {
        heTuLuyenMap.put(player.getUniqueId(), he);
        tutien.combat.StatsManager.applyStats(player, this);
    }

    // =============================================
    // BẾ QUAN ĐIỂM & ĐỘ KIẾP COOLDOWN
    // =============================================
    public long getBeQuanDiem(Player p) { return beQuanDiem.getOrDefault(p.getUniqueId(), 0L); }
    public void addBeQuanDiem(Player p, long amount) { beQuanDiem.put(p.getUniqueId(), getBeQuanDiem(p) + amount); }
    public void setBeQuanDiem(Player p, long amount) { beQuanDiem.put(p.getUniqueId(), Math.max(0, amount)); }

    public long getDokiepCooldown(Player p) { return dokiepCooldown.getOrDefault(p.getUniqueId(), 0L); }
    public void setDokiepCooldown(Player p, long timestamp) { dokiepCooldown.put(p.getUniqueId(), timestamp); }
    public boolean isDokiepOnCooldown(Player p) {
        return (System.currentTimeMillis() - getDokiepCooldown(p)) < 30_000L;
    }

    public int getMaxLinhLucEffective(Player p) {
        int base = getCanhGioi(p).getMaxLinhLuc();
        if (getHeTuLuyen(p) == HeTuLuyen.PHAP_TU) {
            return (int)(base * 1.30);
        }
        return base;
    }

    // =============================================
    // KHOÁNG THẠCH & ĐẠO NIỆM
    // =============================================
    public long getKhoangThach(Player p) { return khoangThachMap.getOrDefault(p.getUniqueId(), 0L); }
    public void addKhoangThach(Player p, long amount) { khoangThachMap.put(p.getUniqueId(), Math.max(0, getKhoangThach(p) + amount)); }
    public void setKhoangThach(Player p, long amount) { khoangThachMap.put(p.getUniqueId(), Math.max(0, amount)); }

    public long getDaoNiem(Player p) { return daoNiemMap.getOrDefault(p.getUniqueId(), 0L); }
    public void addDaoNiem(Player p, long amount) { daoNiemMap.put(p.getUniqueId(), Math.max(0, getDaoNiem(p) + amount)); }
    public void setDaoNiem(Player p, long amount) { daoNiemMap.put(p.getUniqueId(), Math.max(0, amount)); }

    // =============================================
    // CHẾ TÁC & GACHA PITY (PERSISTENT)
    // =============================================
    public int getCapChe(Player p) { return capCheMap.getOrDefault(p.getUniqueId(), 1); }
    public void setCapChe(Player p, int cap) { capCheMap.put(p.getUniqueId(), Math.max(1, cap)); }
    public int getSoLanChe(Player p) { return soLanCheMap.getOrDefault(p.getUniqueId(), 0); }
    public void addSoLanChe(Player p, int n) { soLanCheMap.put(p.getUniqueId(), getSoLanChe(p) + n); }

    public int getGachaPity(Player p) { return gachaPityMap.getOrDefault(p.getUniqueId(), 0); }
    public void setGachaPity(Player p, int pity) { gachaPityMap.put(p.getUniqueId(), Math.max(0, pity)); }

    // =============================================
    // HỖ TRỢ LEADERBOARD (ƯU TIÊN DỮ LIỆU RAM CHO PLAYER ONLINE)
    // =============================================
    public Set<UUID> getAllStoredUUIDs() {
        Set<UUID> result = new HashSet<>(tuViMap.keySet());
        File folder = new File(plugin.getDataFolder(), "playerdata");
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".yml")) {
                        try { result.add(UUID.fromString(f.getName().replace(".yml", ""))); }
                        catch (Exception ignored) {}
                    }
                }
            }
        }
        return result;
    }

    public Map<String, Object> readOfflineData(UUID uuid) {
        // Nếu người chơi đang online trên RAM -> lấy trực tiếp số liệu mới nhất trên RAM
        if (tuViMap.containsKey(uuid)) {
            Player online = Bukkit.getPlayer(uuid);
            Map<String, Object> data = new HashMap<>();
            data.put("TenNguoiChoi", online != null ? online.getName() : "???");
            data.put("TuVi", tuViMap.getOrDefault(uuid, 0));
            CanhGioi cg = canhGioiMap.getOrDefault(uuid, CanhGioi.PHAM_NHAN);
            data.put("CanhGioi", cg.name());
            data.put("KhoangThach", khoangThachMap.getOrDefault(uuid, 0L));
            data.put("DaoNiem", daoNiemMap.getOrDefault(uuid, 0L));
            data.put("BeQuanDiem", beQuanDiem.getOrDefault(uuid, 0L));
            return data;
        }

        File file = getPlayerFile(uuid);
        if (!file.exists()) return Collections.emptyMap();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, Object> data = new HashMap<>();
        data.put("TenNguoiChoi", config.getString("TenNguoiChoi", "???"));
        data.put("TuVi", config.getInt("TuVi", 0));
        data.put("CanhGioi", config.getString("CanhGioi", "PHAM_NHAN"));
        data.put("KhoangThach", config.getLong("KhoangThach", 0L));
        data.put("DaoNiem", config.getLong("DaoNiem", 0L));
        data.put("BeQuanDiem", config.getLong("BeQuan.Diem", 0L));
        return data;
    }
}
