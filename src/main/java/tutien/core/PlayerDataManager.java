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
    private final HashMap<UUID, Integer> danLoCapDo = new HashMap<>();       // Cấp độ lò luyện (1, 2, 3...)
    private final HashMap<UUID, Integer> soLanLuyenDan = new HashMap<>();    // Tổng số lần đã luyện
    private final HashMap<UUID, Double> bonusDamage = new HashMap<>();       // Sát thương vĩnh viễn từ đan dược
    private final HashMap<UUID, Double> bonusHealth = new HashMap<>();       // Sinh lực vĩnh viễn từ đan dược
    private final HashMap<UUID, Integer> bonusTuVi = new HashMap<>();        // Tu vi cộng thêm từ đan dược
    private final HashMap<UUID, Boolean> autoLuyenDan = new HashMap<>();     // Tự động luyện đan
    private final HashMap<UUID, Boolean> autoUseDan = new HashMap<>();       // Tự động sử dụng đan dược

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

            LinhCan randomLC = LinhCan.randomLinhCan();
            linhCanMap.put(uuid, randomLC);

            player.sendMessage("§e§l[Thiên Đạo] §fNgươi vừa thức tỉnh được " + randomLC.getTenHienThi() + "§f!");
        }
    }

    public void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("TuVi", getTuVi(player));
        config.set("CanhGioi", getCanhGioi(player).name());
        config.set("LinhCan", getLinhCan(player).name());
        config.set("LinhLuc", getLinhLuc(player));
        config.set("HeTuLuyen", getHeTuLuyen(player).name());

        // Lưu trữ dược lực vào file để không bị mất khi thoát server
        config.set("DigestionBuffer", getDigestionBuffer(player));

        // Lưu dữ liệu Luyện Đan
        config.set("DanLo.CapDo", getDanLoCapDo(player));
        config.set("DanLo.SoLanLuyen", getSoLanLuyenDan(player));
        config.set("DanLo.BonusDamage", getBonusDamage(player));
        config.set("DanLo.BonusHealth", getBonusHealth(player));
        config.set("DanLo.BonusTuVi", getBonusTuVi(player));
        config.set("DanLo.AutoLuyen", isAutoLuyenDan(player));
        config.set("DanLo.AutoUse", isAutoUseDan(player));

        config.set("TenNguoiChoi", player.getName());

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Khong the luu du lieu cho " + player.getName()); }

        // Dọn dẹp RAM
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

    // ĐÃ THÊM: Cấp quyền cho Admin thiết lập Linh Căn
    public void setLinhCan(Player player, LinhCan lc) { linhCanMap.put(player.getUniqueId(), lc); }

    public int getLinhLuc(Player player) { return linhLucMap.getOrDefault(player.getUniqueId(), 100); }
    public void setLinhLuc(Player player, int amount) { linhLucMap.put(player.getUniqueId(), amount); }

    public HeTuLuyen getHeTuLuyen(Player player) { return heTuLuyenMap.getOrDefault(player.getUniqueId(), HeTuLuyen.CHUA_CHON); }
    public void setHeTuLuyen(Player player, HeTuLuyen he) {
        heTuLuyenMap.put(player.getUniqueId(), he);
        tutien.combat.StatsManager.applyStats(player, this);
    }
}