package tutien.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tutien.alchemy.model.DanDocData;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.CongPhapProgress;
import tutien.element.LinhCanData;
import tutien.element.LinhCanType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp quản lý toàn bộ dữ liệu cốt lõi:
 * Hệ thống 7 Chỉ Số Tu Sĩ Độc Lập:
 *   1. CanhGioi (Đại cảnh giới) & Tang (Tiểu cảnh giới: 0 cho Phàm Nhân, 1..13 cho Luyện Khí)
 *   2. TuVi (Tiến trình tu luyện)
 *   3. LinhLuc & MaxLinhLuc (Tài nguyên tiêu hao nội tại)
 *   4. CanCo (Chất lượng nền móng tu luyện)
 *   5. NgoTinh (Khả năng lĩnh hội công pháp, ý cảnh)
 *   6. ThanThuc (Sức mạnh tâm hồn và nhận thức)
 *   7. TheChat (Thể phách, máu, khí huyết)
 *   8. DaoNgo (Tiến trình ngộ đạo)
 *   9. DanDoc (Đan Độc tích tụ) & CongPhapChuTu (Công pháp đang vận hành)
 */
public class PlayerDataManager {

    private final TuTienPlugin plugin;

    // 1. Tiến trình & Cảnh giới
    private final Map<UUID, CanhGioi> canhGioiMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tangMap = new ConcurrentHashMap<>(); // Tiểu cảnh giới (0..13)
    private final Map<UUID, Integer> tuViMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> linhLucMap = new ConcurrentHashMap<>();
    private final Map<UUID, LinhCan> linhCanMap = new ConcurrentHashMap<>();
    private final Map<UUID, LinhCanData> linhCanDataMap = new ConcurrentHashMap<>();
    private final Map<UUID, HeTuLuyen> heTuLuyenMap = new ConcurrentHashMap<>();

    // 2. Thuộc tính tu sĩ độc lập (7 chỉ số)
    private final Map<UUID, Double> canCoMap = new ConcurrentHashMap<>();     // Căn Cơ (0..100)
    private final Map<UUID, Double> ngoTinhMap = new ConcurrentHashMap<>();   // Ngộ Tính (0..100)
    private final Map<UUID, Double> thanThucMap = new ConcurrentHashMap<>(); // Thần Thức
    private final Map<UUID, Double> theChatMap = new ConcurrentHashMap<>();   // Thể Chất
    private final Map<UUID, Long> daoNgoMap = new ConcurrentHashMap<>();      // Đạo Ngộ (Chỉ số lĩnh ngộ vĩnh viễn)
    private final Map<UUID, Long> daoNgoPointsMap = new ConcurrentHashMap<>();// Điểm Đạo Ngộ tiêu hao (DaoNgoPoints Phase 2.3)
    private final Map<UUID, Double> danDocMap = new ConcurrentHashMap<>();    // Đan Độc tích tụ (0..100)
    private final Map<UUID, DanDocData> danDocDataMap = new ConcurrentHashMap<>(); // Đan Độc & Kháng Thuốc (Phase 3.1)
    private final Map<UUID, String> congPhapChuTuMap = new ConcurrentHashMap<>(); // Công pháp chủ tu (Legacy cache)
    private final Map<UUID, PlayerCultivationData> cultivationDataMap = new ConcurrentHashMap<>(); // Hệ thống Công Pháp Phase 2.2

    // 3. Tiến trình Nhập Đạo (Phàm Nhân Khai Mạch)
    private final Map<UUID, Double> toiTheProgressMap = new ConcurrentHashMap<>(); // Tiến độ Tôi Thể (0.0..100.0%)
    private final Map<UUID, Set<String>> khaiMachMap = new ConcurrentHashMap<>();

    // Quản lý trạng thái Tọa Thiền và Kho dược lực (Digestion Buffer)
    private final Map<UUID, Boolean> tuLuyenMode = new ConcurrentHashMap<>();
    private final Map<UUID, MeditationType> meditationTypeMap = new ConcurrentHashMap<>();
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
            int dataVersion = config.getInt("DataVersion", 0);

            // MIGRATION ENGINE (v0 -> v1) có sao lưu, kiểm tra và thay thế nguyên tử
            if (dataVersion < 1) {
                migrateV0ToV1(player, file, config);
                config = YamlConfiguration.loadConfiguration(file);
                dataVersion = config.getInt("DataVersion", 1);
            }

            // MIGRATION ENGINE (v1 -> v2) chuẩn hóa Linh Căn & Ngũ Hành Phase 2.1
            if (dataVersion < 2) {
                migrateV1ToV2(player, file, config);
                config = YamlConfiguration.loadConfiguration(file);
                dataVersion = config.getInt("DataVersion", 2);
            }

            // MIGRATION ENGINE (v2 -> v3) kiến trúc Công Pháp độc lập Phase 2.2
            if (dataVersion < 3) {
                migrateV2ToV3(player, file, config);
                config = YamlConfiguration.loadConfiguration(file);
                dataVersion = config.getInt("DataVersion", 3);
            }

            // MIGRATION ENGINE (v3 -> v4) bổ sung DaoNgoPoints tiêu hao Phase 2.3
            if (dataVersion < 4) {
                migrateV3ToV4(player, file, config);
                config = YamlConfiguration.loadConfiguration(file);
                dataVersion = config.getInt("DataVersion", 4);
            }

            // MIGRATION ENGINE (v4 -> v5) bổ sung DanDocData & Kháng Thuốc Phase 3.1
            if (dataVersion < 5) {
                migrateV4ToV5(player, file, config);
                config = YamlConfiguration.loadConfiguration(file);
            }

            int loadedTuVi = config.getInt("TuVi", 0);
            int loadedTang = config.getInt("Tang", 1);

            CanhGioi cg = CanhGioi.PHAM_NHAN;
            try { cg = CanhGioi.valueOf(config.getString("CanhGioi", CanhGioi.PHAM_NHAN.name())); }
            catch (IllegalArgumentException e) { cg = CanhGioi.PHAM_NHAN; }
            canhGioiMap.put(uuid, cg);

            // Cân bằng chặt chẽ: Tu Vi không được vượt quá bình cảnh cảnh giới
            int maxTuVi = cg.getTuViYeuCau();
            if (loadedTuVi > maxTuVi) {
                loadedTuVi = maxTuVi;
            }
            tuViMap.put(uuid, loadedTuVi);

            // Tầng tiểu cảnh giới luôn đi đôi và được đồng bộ chuẩn xác với Tu Vi
            int syncedTang = CanhGioi.calculateTang(cg, loadedTuVi);
            tangMap.put(uuid, syncedTang);

            linhLucMap.put(uuid, config.getInt("LinhLuc", 0));
            digestionBuffer.put(uuid, config.getInt("DigestionBuffer", 0));

            // Tải Linh Căn Data (v2)
            LinhCanData lcData;
            if (config.isConfigurationSection("LinhCanData")) {
                lcData = LinhCanData.fromConfigurationSection(config.getConfigurationSection("LinhCanData"));
            } else if (config.contains("LinhCan")) {
                lcData = plugin.getLinhCanManager().createFromLegacyEnum(config.getString("LinhCan"));
            } else {
                lcData = plugin.getLinhCanManager().generateRandomLinhCan();
            }
            linhCanDataMap.put(uuid, lcData);
            linhCanMap.put(uuid, mapToLegacyLinhCan(lcData.getCurrentType()));

            try { heTuLuyenMap.put(uuid, HeTuLuyen.valueOf(config.getString("HeTuLuyen", HeTuLuyen.CHUA_CHON.name()))); }
            catch (IllegalArgumentException e) { heTuLuyenMap.put(uuid, HeTuLuyen.CHUA_CHON); }

            // Tải 7 thuộc tính tu sĩ
            canCoMap.put(uuid, config.getDouble("CanCo", 10.0));
            ngoTinhMap.put(uuid, config.getDouble("NgoTinh", 10.0));
            thanThucMap.put(uuid, config.getDouble("ThanThuc", 0.0));
            theChatMap.put(uuid, config.getDouble("TheChat", 20.0));
            daoNgoMap.put(uuid, config.getLong("DaoNgo", 0L));
            daoNgoPointsMap.put(uuid, config.getLong("DaoNgoPoints", 0L));

            // Tải DanDocData & Kháng Thuốc (v5)
            DanDocData ddd;
            if (config.isConfigurationSection("DanDocData")) {
                ddd = DanDocData.fromConfigurationSection(config.getConfigurationSection("DanDocData"));
            } else if (config.isConfigurationSection("DanDoc")) {
                ddd = DanDocData.fromConfigurationSection(config.getConfigurationSection("DanDoc"));
            } else {
                double oldDanDoc = config.getDouble("DanDoc", 0.0);
                ddd = new DanDocData(oldDanDoc);
            }
            danDocDataMap.put(uuid, ddd);
            danDocMap.put(uuid, ddd.getDanDoc());

            // Tải Hệ Thống Công Pháp (v3)
            PlayerCultivationData cultData;
            if (config.isConfigurationSection("CultivationData")) {
                cultData = PlayerCultivationData.fromConfigurationSection(config.getConfigurationSection("CultivationData"));
            } else {
                String oldCp = config.getString("CongPhapChuTu", "CHUA_CO");
                cultData = new PlayerCultivationData(oldCp);
                if (!oldCp.equalsIgnoreCase("CHUA_CO")) {
                    CongPhapProgress prog = cultData.getOrCreateProgress(oldCp);
                    prog.addLearnedChapter(1);
                }
            }
            cultivationDataMap.put(uuid, cultData);
            congPhapChuTuMap.put(uuid, cultData.getActivePrimary() != null ? cultData.getActivePrimary() : "CHUA_CO");

            // Tải tiến trình Tôi Thể (0.0..100.0%)
            toiTheProgressMap.put(uuid, config.getDouble("ToiTheProgress", 0.0));

            // Tải tiến trình khai mạch
            Set<String> kmSet = new HashSet<>(config.getStringList("KhaiMach"));
            khaiMachMap.put(uuid, kmSet);

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
            // Khởi tạo cho người chơi mới: PHÀM NHÂN HOÀN TOÀN
            tuViMap.put(uuid, 0);
            canhGioiMap.put(uuid, CanhGioi.PHAM_NHAN);
            tangMap.put(uuid, 0); // Phàm thai
            linhLucMap.put(uuid, 0); // Phàm nhân chưa có Linh Lực!
            heTuLuyenMap.put(uuid, HeTuLuyen.CHUA_CHON);
            digestionBuffer.put(uuid, 0);

            canCoMap.put(uuid, 10.0);
            ngoTinhMap.put(uuid, 10.0);
            thanThucMap.put(uuid, 0.0);
            theChatMap.put(uuid, 20.0);
            daoNgoMap.put(uuid, 0L);
            daoNgoPointsMap.put(uuid, 0L);
            danDocMap.put(uuid, 0.0);
            danDocDataMap.put(uuid, new DanDocData(0.0));
            congPhapChuTuMap.put(uuid, "CHUA_CO");
            toiTheProgressMap.put(uuid, 0.0);
            khaiMachMap.put(uuid, new HashSet<>());

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
            daoNgoPointsMap.put(uuid, 0L);

            LinhCanData randomLC = plugin.getLinhCanManager().generateRandomLinhCan();
            linhCanDataMap.put(uuid, randomLC);
            linhCanMap.put(uuid, mapToLegacyLinhCan(randomLC.getCurrentType()));

            PlayerCultivationData newCult = new PlayerCultivationData();
            cultivationDataMap.put(uuid, newCult);
            congPhapChuTuMap.put(uuid, "CHUA_CO");

            player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§e§l[Thiên Đạo] §fNgươi sinh ra mang thân thể §7Phàm Nhân§f!");
            player.sendMessage("§fTư chất linh căn ẩn giấu: " + randomLC.getFormattedDisplayName());
            player.sendMessage("§fHãy bắt đầu rèn luyện thể phách, tẩy tủy và dẫn khí nhập đạo (§e/tutien nhapdao§f)!");
            player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    /**
     * Quy trình Migration dữ liệu người chơi cũ (v0 -> v1) an toàn tuyệt đối:
     * - Tạo bản sao lưu .bak trước khi xử lý.
     * - Chuyển đổi trong RAM và xác minh tính toàn vẹn.
     * - Ghi ra file tạm .tmp rồi thay thế nguyên tử.
     * - Có tính chất Idempotent: chạy nhiều lần không làm sai lệch dữ liệu.
     */
    private void migrateV0ToV1(Player player, File file, FileConfiguration config) {
        plugin.getLogger().info("[Migration] Phat hien du lieu cu (v0) cua " + player.getName() + ", bat dau quy trinh sao luu va chuyen doi...");

        // 1. Sao lưu file gốc (.bak)
        File bakFile = new File(file.getParentFile(), file.getName() + ".bak");
        try {
            Files.copy(file.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the tao file backup cho " + player.getName() + "! Dung migration de bao ve du lieu.");
            return;
        }

        // 2. Chuyển đổi dữ liệu trong bộ nhớ
        String cgStr = config.getString("CanhGioi", "PHAM_NHAN");
        CanhGioi cg;
        try { cg = CanhGioi.valueOf(cgStr); }
        catch (IllegalArgumentException e) { cg = CanhGioi.PHAM_NHAN; }

        int oldTang = config.getInt("Tang", cg == CanhGioi.PHAM_NHAN ? 0 : 1);
        double oldCanCo = config.getDouble("CanCo", 10.0);
        int oldLinhLuc = config.getInt("LinhLuc", 0);
        int oldMaxQi = config.getInt("MaxLinhLuc", -1);
        List<String> oldKmList = config.getStringList("KhaiMach");
        Set<String> newKmSet = new HashSet<>(oldKmList);

        double newToiTheProgress;
        double newCanCo;

        if (cg != CanhGioi.PHAM_NHAN) {
            // Đã lên Luyện Khí hoặc cảnh giới cao hơn:
            newToiTheProgress = 100.0;
            newCanCo = oldCanCo; // Bảo toàn Căn Cơ cũ!
            newKmSet.add("TOI_THE");
            newKmSet.add("TAY_TUY");
            newKmSet.add("DAN_KHI");
        } else {
            // Còn là Phàm Nhân:
            if (newKmSet.contains("TOI_THE")) {
                newToiTheProgress = 100.0;
                newCanCo = 10.0;
            } else {
                // Chuyển Căn Cơ đã farm cũ (10.0 -> 20.0) thành tiến độ Tôi Thể (0% -> 100%)
                newToiTheProgress = Math.min(100.0, Math.max(0.0, (oldCanCo - 10.0) * 10.0));
                newCanCo = 10.0;
            }
        }

        // 3. Tính Linh Lực an toàn theo thang mới
        int newMaxQi;
        int newLinhLuc;

        if (cg == CanhGioi.PHAM_NHAN) {
            newMaxQi = 0;
            newLinhLuc = 0;
        } else if (cg == CanhGioi.LUYEN_KHI) {
            int t = Math.max(1, Math.min(13, oldTang));
            newMaxQi = 100 + 25 * (t - 1);
            if (oldMaxQi > 0 && oldLinhLuc >= oldMaxQi) {
                newLinhLuc = newMaxQi; // Bảo toàn trạng thái đầy bình
            } else if (oldLinhLuc >= 150) {
                newLinhLuc = newMaxQi;
            } else {
                newLinhLuc = Math.min(newMaxQi, oldLinhLuc);
            }
        } else {
            // Trúc Cơ trở lên
            newMaxQi = cg.getMaxLinhLuc();
            newLinhLuc = Math.min(newMaxQi, oldLinhLuc);
        }

        // 4. Kiểm tra tính hợp lệ dữ liệu trước khi ghi
        if (!Double.isFinite(newToiTheProgress) || newToiTheProgress < 0.0 || newToiTheProgress > 100.0) {
            newToiTheProgress = 0.0;
        }
        if (!Double.isFinite(newCanCo) || newCanCo < 0.0) {
            newCanCo = 10.0;
        }

        // 5. Cập nhật dữ liệu vào config (bảo toàn toàn bộ các trường khác!)
        config.set("DataVersion", 1);
        config.set("ToiTheProgress", newToiTheProgress);
        config.set("CanCo", newCanCo);
        config.set("LinhLuc", newLinhLuc);
        config.set("KhaiMach", new ArrayList<>(newKmSet));

        // 6. Ghi ra file tạm (.tmp)
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(tmpFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the ghi file tam .tmp cho " + player.getName() + "! Huy bo migration.");
            return;
        }

        // 7. Thay thế nguyên tử file chính
        try {
            try {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicEx) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            plugin.getLogger().info("[Migration] Migration DataVersion: 1 hoan tat thanh cong cho " + player.getName() + "!");
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the thay the file chinh tu file tam cho " + player.getName() + "!");
        }
    }

    /**
     * Quy trình Migration dữ liệu người chơi từ v1 -> v2 an toàn tuyệt đối:
     * - Tạo bản sao lưu .bak2 trước khi xử lý.
     * - Đọc LinhCan cũ và khởi tạo LinhCanData đa thuộc tính chuẩn Phase 2.1.
     * - Ghi ra file tạm .tmp rồi thay thế nguyên tử.
     * - Stamp DataVersion: 2.
     * - Idempotent: chạy nhiều lần không làm sai lệch dữ liệu.
     */
    private void migrateV1ToV2(Player player, File file, FileConfiguration config) {
        plugin.getLogger().info("[Migration] Phat hien du lieu v1 cua " + player.getName() + ", bat dau nang cap sang DataVersion 2 (Linh Can & Ngu Hanh)...");

        // 1. Sao lưu file gốc (.bak2)
        File bakFile = new File(file.getParentFile(), file.getName() + ".bak2");
        try {
            Files.copy(file.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the tao file backup bak2 cho " + player.getName() + "! Dung migration de bao ve du lieu.");
            return;
        }

        // 2. Chuyển đổi dữ liệu trong bộ nhớ
        LinhCanData lcData;
        if (config.isConfigurationSection("LinhCanData")) {
            lcData = LinhCanData.fromConfigurationSection(config.getConfigurationSection("LinhCanData"));
        } else {
            String legacyEnum = config.getString("LinhCan", "PHE_LINH_CAN");
            lcData = plugin.getLinhCanManager().createFromLegacyEnum(legacyEnum);
        }

        // 3. Chuẩn bị file tạm và ghi dữ liệu
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.set("DataVersion", 2);
            config.set("LinhCanData", lcData.toMap());
            config.set("LinhCan", lcData.getCurrentType().name());
            config.save(tmpFile);

            // 4. Kiểm tra tính toàn vẹn của file tạm trước khi thay thế
            if (!tmpFile.exists() || tmpFile.length() == 0) {
                plugin.getLogger().severe("[Migration] File tam bi rong hoac loi cho " + player.getName() + "! Huy bo migration.");
                if (tmpFile.exists()) tmpFile.delete();
                return;
            }

            // 5. Di chuyển nguyên tử
            try {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicEx) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            plugin.getLogger().info("[Migration] Migration DataVersion: 2 hoan tat thanh cong cho " + player.getName() + "!");
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the thay the file chinh tu file tam cho " + player.getName() + "!");
        }
    }

    /**
     * Quy trình Migration dữ liệu người chơi từ v2 -> v3 an toàn tuyệt đối:
     * - Tạo bản sao lưu .bak3 trước khi xử lý.
     * - Chuyển đổi CongPhapChuTu cũ sang PlayerCultivationData hoàn chỉnh (Phase 2.2).
     * - Bảo tồn tuyệt đối ID công pháp cũ kể cả khi chưa có trong catalog mới.
     * - Ghi ra file tạm .tmp rồi thay thế nguyên tử.
     * - Stamp DataVersion: 3.
     */
    private void migrateV2ToV3(Player player, File file, FileConfiguration config) {
        plugin.getLogger().info("[Migration] Phat hien du lieu v2 cua " + player.getName() + ", bat dau nang cap sang DataVersion 3 (He Thong Cong Phap)...");

        // 1. Sao lưu file gốc (.bak3)
        File bakFile = new File(file.getParentFile(), file.getName() + ".bak3");
        try {
            Files.copy(file.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the tao file backup bak3 cho " + player.getName() + "! Dung migration de bao ve du lieu.");
            return;
        }

        // 2. Chuyển đổi dữ liệu trong bộ nhớ
        PlayerCultivationData cultData;
        if (config.isConfigurationSection("CultivationData")) {
            cultData = PlayerCultivationData.fromConfigurationSection(config.getConfigurationSection("CultivationData"));
        } else {
            String oldCp = config.getString("CongPhapChuTu", "CHUA_CO");
            cultData = new PlayerCultivationData(oldCp);
            if (!oldCp.equalsIgnoreCase("CHUA_CO") && !oldCp.trim().isEmpty()) {
                CongPhapProgress prog = cultData.getOrCreateProgress(oldCp);
                prog.addLearnedChapter(1);
            }
        }

        // 3. Chuẩn bị file tạm và ghi dữ liệu
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.set("DataVersion", 3);
            config.set("CultivationData", cultData.toMap());
            config.set("CongPhapChuTu", cultData.getActivePrimary() != null ? cultData.getActivePrimary() : "CHUA_CO");
            config.save(tmpFile);

            // 4. Kiểm tra tính toàn vẹn của file tạm trước khi thay thế
            if (!tmpFile.exists() || tmpFile.length() == 0) {
                plugin.getLogger().severe("[Migration] File tam bi rong hoac loi cho " + player.getName() + "! Huy bo migration.");
                if (tmpFile.exists()) tmpFile.delete();
                return;
            }

            // 5. Di chuyển nguyên tử
            try {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicEx) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            plugin.getLogger().info("[Migration] Migration DataVersion: 3 hoan tat thanh cong cho " + player.getName() + "!");
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the thay the file chinh tu file tam cho " + player.getName() + "!");
        }
    }

    /**
     * Quy trình Migration dữ liệu người chơi từ v3 -> v4 an toàn tuyệt đối:
     * - Tạo bản sao lưu .bak4 trước khi xử lý.
     * - Bổ sung trường DaoNgoPoints tiêu hao (Phase 2.3).
     * - Bảo toàn toàn bộ dữ liệu Linh Căn và Công Pháp.
     * - Ghi ra file tạm .tmp rồi thay thế nguyên tử.
     * - Stamp DataVersion: 4.
     */
    private void migrateV3ToV4(Player player, File file, FileConfiguration config) {
        plugin.getLogger().info("[Migration] Phat hien du lieu v3 cua " + player.getName() + ", bat dau nang cap sang DataVersion 4 (DaoNgoPoints & Phase 2.3)...");

        // 1. Sao lưu file gốc (.bak4)
        File bakFile = new File(file.getParentFile(), file.getName() + ".bak4");
        try {
            Files.copy(file.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the tao file backup bak4 cho " + player.getName() + "! Dung migration de bao ve du lieu.");
            return;
        }

        // 2. Chuyển đổi dữ liệu trong bộ nhớ
        long currentDaoNgoPoints = config.getLong("DaoNgoPoints", 0L);

        // 3. Chuẩn bị file tạm và ghi dữ liệu
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.set("DataVersion", 4);
            config.set("DaoNgoPoints", currentDaoNgoPoints);
            config.save(tmpFile);

            // 4. Kiểm tra tính toàn vẹn của file tạm trước khi thay thế
            if (!tmpFile.exists() || tmpFile.length() == 0) {
                plugin.getLogger().severe("[Migration] File tam bi rong hoac loi cho " + player.getName() + "! Huy bo migration.");
                if (tmpFile.exists()) tmpFile.delete();
                return;
            }

            // 5. Di chuyển nguyên tử
            try {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicEx) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            plugin.getLogger().info("[Migration] Migration DataVersion: 4 hoan tat thanh cong cho " + player.getName() + "!");
        } catch (IOException e) {
            plugin.getLogger().severe("[Migration] Khong the thay the file chinh tu file tam cho " + player.getName() + "!");
        }
    }

    /**
     * Quy trình Migration dữ liệu người chơi từ v4 -> v5 an toàn tuyệt đối:
     * - Tạo bản sao lưu .bak5 trước khi xử lý.
     * - Khởi tạo DanDocData với Amount, Resistance, LastDoseAt, LastDetoxAt (Phase 3.1).
     * - Bảo toàn toàn bộ dữ liệu Tu Vi, Linh Lực, Linh Căn và Công Pháp.
     * - Ghi ra file tạm .tmp rồi thay thế nguyên tử.
     * - Stamp DataVersion: 5.
     * - Idempotent: chạy nhiều lần không làm sai lệch dữ liệu.
     */
    private void migrateV4ToV5(Player player, File file, FileConfiguration config) {
        String pName = player != null ? player.getName() : (file != null ? file.getName() : "Unknown");
        if (plugin != null && plugin.getLogger() != null) {
            plugin.getLogger().info("[Migration] Phat hien du lieu v4 cua " + pName + ", bat dau nang cap sang DataVersion 5 (DanDoc & Resistance Phase 3.1)...");
        }

        // 1. Sao lưu file gốc (.bak5)
        File bakFile = new File(file.getParentFile(), file.getName() + ".bak5");
        try {
            Files.copy(file.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (plugin != null && plugin.getLogger() != null) {
                plugin.getLogger().severe("[Migration] Khong the tao file backup bak5 cho " + pName + "! Dung migration de bao ve du lieu.");
            }
            return;
        }

        // 2. Chuyển đổi dữ liệu trong bộ nhớ
        double currentDanDoc = config.getDouble("DanDoc", 0.0);
        DanDocData ddd = new DanDocData(currentDanDoc);

        // 3. Chuẩn bị file tạm và ghi dữ liệu
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.set("DataVersion", 5);
            org.bukkit.configuration.ConfigurationSection dddSec = config.createSection("DanDocData");
            ddd.toConfigurationSection(dddSec);
            config.set("DanDoc", currentDanDoc);
            config.save(tmpFile);

            // 4. Kiểm tra tính toàn vẹn của file tạm trước khi thay thế
            if (!tmpFile.exists() || tmpFile.length() == 0) {
                if (plugin != null && plugin.getLogger() != null) {
                    plugin.getLogger().severe("[Migration] File tam bi rong hoac loi cho " + pName + "! Huy bo migration.");
                }
                if (tmpFile.exists()) tmpFile.delete();
                return;
            }

            // 5. Di chuyển nguyên tử
            try {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicEx) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (plugin != null && plugin.getLogger() != null) {
                plugin.getLogger().info("[Migration] Migration DataVersion: 5 hoan tat thanh cong cho " + pName + "!");
            }
        } catch (IOException e) {
            if (plugin != null && plugin.getLogger() != null) {
                plugin.getLogger().severe("[Migration] Khong the thay the file chinh tu file tam cho " + pName + "!");
            }
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
        config.set("DataVersion", 5);
        config.set("TuVi", getTuVi(player));
        config.set("Tang", getTang(player));
        config.set("CanhGioi", getCanhGioi(player).name());
        LinhCanData lcData = getLinhCanData(player);
        config.set("LinhCanData", lcData.toMap());
        config.set("LinhCan", lcData.getCurrentType().name());

        PlayerCultivationData cultData = getPlayerCultivationData(player);
        config.set("CultivationData", cultData.toMap());
        config.set("CongPhapChuTu", cultData.getActivePrimary() != null ? cultData.getActivePrimary() : "CHUA_CO");
        config.set("LinhLuc", getLinhLuc(player));
        config.set("HeTuLuyen", getHeTuLuyen(player).name());

        config.set("CanCo", getCanCo(player));
        config.set("NgoTinh", getNgoTinh(player));
        config.set("ThanThuc", getThanThuc(player));
        config.set("TheChat", getTheChat(player));
        config.set("DaoNgo", getDaoNgo(player));
        config.set("DaoNgoPoints", getDaoNgoPoints(player));
        
        DanDocData ddd = getDanDocData(player);
        if (ddd != null) {
            org.bukkit.configuration.ConfigurationSection dddSec = config.createSection("DanDocData");
            ddd.toConfigurationSection(dddSec);
        }
        config.set("DanDoc", getDanDoc(player));
        config.set("CongPhapChuTu", getCongPhapChuTu(player));
        config.set("ToiTheProgress", getToiTheProgress(player));
        config.set("KhaiMach", new ArrayList<>(getKhaiMach(player)));

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
        tangMap.remove(uuid);
        canhGioiMap.remove(uuid);
        linhCanMap.remove(uuid);
        linhCanDataMap.remove(uuid);
        linhLucMap.remove(uuid);
        heTuLuyenMap.remove(uuid);

        canCoMap.remove(uuid);
        ngoTinhMap.remove(uuid);
        thanThucMap.remove(uuid);
        theChatMap.remove(uuid);
        daoNgoMap.remove(uuid);
        daoNgoPointsMap.remove(uuid);
        danDocMap.remove(uuid);
        danDocDataMap.remove(uuid);
        congPhapChuTuMap.remove(uuid);
        cultivationDataMap.remove(uuid);
        toiTheProgressMap.remove(uuid);
        khaiMachMap.remove(uuid);

        tuLuyenMode.remove(uuid);
        meditationTypeMap.remove(uuid);
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
        CanhGioi hienTai = getCanhGioi(player);
        int maxTuVi = hienTai.getTuViYeuCau();
        int prevTuVi = getTuVi(player);

        // Nếu đã ở bình cảnh tối đa và amount > 0, không thể tăng thêm tu vi nữa
        if (amount > 0 && prevTuVi >= maxTuVi) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§c§l[Bình Cảnh] §fTu vi đã đạt cực hạn (§e" + maxTuVi + "§f/§e" + maxTuVi + "§f), hãy đột phá cảnh giới!"));
            return;
        }

        int currentTuVi = Math.max(0, Math.min(maxTuVi, prevTuVi + amount));
        tuViMap.put(uuid, currentTuVi);

        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent("§a" + (amount >= 0 ? "+" : "") + amount + " Tu Vi §8| §eTổng: " + currentTuVi + "/" + maxTuVi));

        kiemTraSanSangDoKiep(player, prevTuVi, currentTuVi);
    }

    private void kiemTraSanSangDoKiep(Player player, int prevTuVi, int currentTuVi) {
        CanhGioi hienTai = getCanhGioi(player);
        int oldTang = getTang(player);
        int newTang = CanhGioi.calculateTang(hienTai, currentTuVi);

        if (newTang > oldTang) {
            setTang(player, newTang);
            player.sendMessage("§a§l[Đột Phá Tiểu Cảnh Giới] §fNgươi đã đả thông kinh lạc, bước lên §e" + hienTai.getTieuCanhGioi(newTang) + "§f!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            tutien.combat.StatsManager.applyStats(player, this);
        } else if (newTang < oldTang) {
            setTang(player, newTang);
            tutien.combat.StatsManager.applyStats(player, this);
        }

        int maxTuVi = hienTai.getTuViYeuCau();
        if (currentTuVi >= maxTuVi && prevTuVi < maxTuVi) {
            if (hienTai == CanhGioi.LUYEN_KHI) {
                player.sendMessage("§e§l[Thiên Đạo] §fTu vi Luyện Khí Tầng 13 đã đạt §cĐại Viên Mãn (" + maxTuVi + "/" + maxTuVi + ")§f! Hãy chuẩn bị §bTrúc Cơ Đan §fvà rèn Căn Cơ để Trúc Cơ (/tutien dotpha)!");
            } else if (hienTai == CanhGioi.PHAM_NHAN) {
                player.sendMessage("§e§l[Thiên Đạo] §fNgươi đã tích lũy đủ căn cơ phàm nhân! Hãy dùng §b/tutien nhapdao §fđể tiến hành Khai Mạch Nhập Đạo!");
            } else {
                player.sendMessage("§e§l[Thiên Đạo] §fTu vi cảnh giới §b" + hienTai.getTenHienThi() + " §fđã đạt bình cảnh §c(" + maxTuVi + "/" + maxTuVi + ")§f! Hãy chuẩn bị đan dược và dùng §c/tutien dokiep §fhoặc §c/tutien dotpha §fđể đột phá!");
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
        }
    }

    // --- CÁC PHƯƠNG THỨC TỌA THIỀN & TIÊU HÓA (PHASE 2.4) ---

    public MeditationType getMeditationType(Player p) {
        if (p == null) return MeditationType.NONE;
        return meditationTypeMap.getOrDefault(p.getUniqueId(), MeditationType.NONE);
    }

    public void setMeditationType(Player p, MeditationType type) {
        if (p == null) return;
        MeditationType target = type != null ? type : MeditationType.NONE;
        if (target == MeditationType.NONE) {
            meditationTypeMap.remove(p.getUniqueId());
            tuLuyenMode.remove(p.getUniqueId());
        } else {
            meditationTypeMap.put(p.getUniqueId(), target);
            tuLuyenMode.put(p.getUniqueId(), target == MeditationType.TU_LUYEN);
        }
    }

    public boolean isMeditating(Player p) {
        return getMeditationType(p) != MeditationType.NONE;
    }

    public boolean isDangTuLuyen(Player p) {
        return getMeditationType(p) == MeditationType.TU_LUYEN;
    }

    public boolean isDangThamNgo(Player p) {
        return getMeditationType(p) == MeditationType.THAM_NGO;
    }

    public void setTuLuyenMode(Player p, boolean status) {
        setMeditationType(p, status ? MeditationType.TU_LUYEN : MeditationType.NONE);
    }

    public void setDangTuLuyen(Player p, boolean status) {
        setTuLuyenMode(p, status);
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
    // GETTER / SETTER 7 THUỘC TÍNH TU SĨ
    // =============================================

    public int getTang(Player p) { return tangMap.getOrDefault(p.getUniqueId(), 1); }
    public void setTang(Player p, int tang) { tangMap.put(p.getUniqueId(), Math.max(0, tang)); }

    public double getCanCo(Player p) { return canCoMap.getOrDefault(p.getUniqueId(), 10.0); }
    public void setCanCo(Player p, double v) { canCoMap.put(p.getUniqueId(), Math.max(0.0, Math.min(100.0, v))); }
    public void addCanCo(Player p, double delta) { setCanCo(p, getCanCo(p) + delta); }

    public double getNgoTinh(Player p) { return ngoTinhMap.getOrDefault(p.getUniqueId(), 10.0); }
    public void setNgoTinh(Player p, double v) { ngoTinhMap.put(p.getUniqueId(), Math.max(0.0, v)); }

    public double getThanThuc(Player p) { return thanThucMap.getOrDefault(p.getUniqueId(), 0.0); }
    public double getThanThuc(UUID uuid) { return thanThucMap.getOrDefault(uuid, 0.0); }
    public void setThanThuc(Player p, double v) { thanThucMap.put(p.getUniqueId(), Math.max(0.0, v)); }

    public double getTheChat(Player p) { return theChatMap.getOrDefault(p.getUniqueId(), 20.0); }
    public void setTheChat(Player p, double v) { theChatMap.put(p.getUniqueId(), Math.max(1.0, v)); }

    public long getDaoNgo(Player p) { return daoNgoMap.getOrDefault(p.getUniqueId(), 0L); }
    public void addDaoNgo(Player p, long amount) { daoNgoMap.put(p.getUniqueId(), getDaoNgo(p) + amount); }

    public long getDaoNgoPoints(Player p) { return daoNgoPointsMap.getOrDefault(p.getUniqueId(), 0L); }
    public void setDaoNgoPoints(Player p, long amount) { daoNgoPointsMap.put(p.getUniqueId(), Math.max(0L, amount)); }
    public void addDaoNgoPoints(Player p, long amount) { setDaoNgoPoints(p, getDaoNgoPoints(p) + amount); }
    public boolean hasDaoNgoPoints(Player p, long amount) { return getDaoNgoPoints(p) >= amount; }
    public boolean takeDaoNgoPoints(Player p, long amount) {
        if (!hasDaoNgoPoints(p, amount)) return false;
        setDaoNgoPoints(p, getDaoNgoPoints(p) - amount);
        return true;
    }

    public DanDocData getDanDocData(Player p) {
        if (p == null) return null;
        return getDanDocData(p.getUniqueId());
    }

    public DanDocData getDanDocData(UUID uuid) {
        if (uuid == null) return null;
        return danDocDataMap.computeIfAbsent(uuid, k -> new DanDocData(danDocMap.getOrDefault(k, 0.0)));
    }

    public void setDanDocData(Player p, DanDocData data) {
        if (p == null || data == null) return;
        danDocDataMap.put(p.getUniqueId(), data);
        danDocMap.put(p.getUniqueId(), data.getDanDoc());
    }

    public double getDanDoc(Player p) {
        DanDocData data = getDanDocData(p);
        return data != null ? data.getDanDoc() : danDocMap.getOrDefault(p.getUniqueId(), 0.0);
    }

    public void setDanDoc(Player p, double v) {
        DanDocData data = getDanDocData(p);
        if (data != null) {
            data.setDanDoc(v);
        }
        danDocMap.put(p.getUniqueId(), Math.max(0.0, Math.min(1000.0, v)));
    }

    public void addDanDoc(Player p, double delta) {
        setDanDoc(p, getDanDoc(p) + delta);
    }

    public PlayerCultivationData getPlayerCultivationData(Player p) {
        return cultivationDataMap.computeIfAbsent(p.getUniqueId(), k -> new PlayerCultivationData());
    }

    public void setPlayerCultivationData(Player p, PlayerCultivationData data) {
        if (data != null) {
            cultivationDataMap.put(p.getUniqueId(), data);
            congPhapChuTuMap.put(p.getUniqueId(), data.getActivePrimary() != null ? data.getActivePrimary() : "CHUA_CO");
        }
    }

    public String getCongPhapChuTu(Player p) {
        PlayerCultivationData cd = cultivationDataMap.get(p.getUniqueId());
        if (cd != null && cd.getActivePrimary() != null) {
            return cd.getActivePrimary();
        }
        return congPhapChuTuMap.getOrDefault(p.getUniqueId(), "CHUA_CO");
    }

    public void setCongPhapChuTu(Player p, String cp) {
        if (cp != null) {
            congPhapChuTuMap.put(p.getUniqueId(), cp);
            PlayerCultivationData cd = getPlayerCultivationData(p);
            cd.setActivePrimary(cp);
        }
    }

    public Set<String> getKhaiMach(Player p) { return khaiMachMap.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()); }
    public boolean hasKhaiMachStep(Player p, String step) { return getKhaiMach(p).contains(step); }
    public void addKhaiMachStep(Player p, String step) { getKhaiMach(p).add(step); }

    // =============================================
    // TIẾN TRÌNH TÔI THỂ (PHÀM NHÂN)
    // =============================================

    public double getToiTheProgress(Player p) {
        return toiTheProgressMap.getOrDefault(p.getUniqueId(), 0.0);
    }

    public void setToiTheProgress(Player p, double progress) {
        if (!Double.isFinite(progress)) return;
        double value = Math.max(0.0, Math.min(100.0, progress));
        toiTheProgressMap.put(p.getUniqueId(), value);
    }

    public void addToiTheProgress(Player p, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return;
        if (getCanhGioi(p) != CanhGioi.PHAM_NHAN) return;
        double current = getToiTheProgress(p);
        if (current >= 100.0) return;
        double updated = Math.min(100.0, current + amount);
        setToiTheProgress(p, updated);
        if (updated >= 100.0) {
            addKhaiMachStep(p, "TOI_THE");
            p.sendMessage("§a§l✦ [Tôi Thể Hoàn Tất] §fThể phách rắn rỏi, gân cốt kiên cường! Tiến độ Tôi Thể đã đạt 100%!");
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }
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
        int curTuVi = getTuVi(player);
        int maxTuVi = cg != null ? cg.getTuViYeuCau() : Integer.MAX_VALUE;
        if (curTuVi > maxTuVi) {
            curTuVi = maxTuVi;
            tuViMap.put(player.getUniqueId(), curTuVi);
        }
        int expectedTang = CanhGioi.calculateTang(cg, curTuVi);
        tangMap.put(player.getUniqueId(), expectedTang);
        tutien.combat.StatsManager.applyStats(player, this);
    }

    public int getTuVi(Player player) { return tuViMap.getOrDefault(player.getUniqueId(), 0); }
    public void setTuVi(Player player, int amount) {
        CanhGioi cg = getCanhGioi(player);
        int maxTuVi = cg != null ? cg.getTuViYeuCau() : Integer.MAX_VALUE;
        int clamped = Math.max(0, Math.min(maxTuVi, amount));
        tuViMap.put(player.getUniqueId(), clamped);
        int expectedTang = CanhGioi.calculateTang(cg, clamped);
        tangMap.put(player.getUniqueId(), expectedTang);
    }

    public LinhCan getLinhCan(Player player) {
        LinhCanData lcd = linhCanDataMap.get(player.getUniqueId());
        if (lcd != null) {
            return mapToLegacyLinhCan(lcd.getCurrentType());
        }
        return linhCanMap.getOrDefault(player.getUniqueId(), LinhCan.TAP_LINH_CAN);
    }

    public void setLinhCan(Player player, LinhCan lc) {
        linhCanMap.put(player.getUniqueId(), lc);
    }

    public LinhCanData getLinhCanData(Player player) {
        return linhCanDataMap.computeIfAbsent(player.getUniqueId(),
                k -> plugin.getLinhCanManager().generateRandomLinhCan());
    }

    public void setLinhCanData(Player player, LinhCanData data) {
        if (data != null) {
            linhCanDataMap.put(player.getUniqueId(), data);
            linhCanMap.put(player.getUniqueId(), mapToLegacyLinhCan(data.getCurrentType()));
        }
    }

    public LinhCan mapToLegacyLinhCan(LinhCanType type) {
        if (type == null) return LinhCan.TAP_LINH_CAN;
        switch (type) {
            case THIEN_LINH_CAN: return LinhCan.THIEN_LINH_CAN;
            case CHAN_LINH_CAN: return LinhCan.CHAN_LINH_CAN;
            case DI_LINH_CAN: return LinhCan.HON_DON_LINH_CAN;
            case NGUY_LINH_CAN:
            default: return LinhCan.TAP_LINH_CAN;
        }
    }

    public int getLinhLuc(Player player) { return linhLucMap.getOrDefault(player.getUniqueId(), 0); }
    public void setLinhLuc(Player player, int amount) { linhLucMap.put(player.getUniqueId(), Math.max(0, amount)); }
    public void addLinhLuc(Player player, int amount) {
        int max = getMaxLinhLucEffective(player);
        setLinhLuc(player, Math.min(max, getLinhLuc(player) + amount));
    }
    public void takeLinhLuc(Player player, int amount) {
        if (player == null || amount <= 0) return;
        setLinhLuc(player, Math.max(0, getLinhLuc(player) - amount));
    }

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
        CanhGioi cg = getCanhGioi(p);
        if (cg == CanhGioi.PHAM_NHAN) return 0; // Phàm nhân = 0 Linh Lực!

        int base;
        // Luyện Khí 13 tầng: MaxQi = 100 + 25 * (tang - 1)
        if (cg == CanhGioi.LUYEN_KHI) {
            int tang = Math.max(1, Math.min(13, getTang(p)));
            base = 100 + 25 * (tang - 1);
        } else {
            // Trúc Cơ trở lên: đọc trực tiếp từ cấu hình đại cảnh giới
            base = cg.getMaxLinhLuc();
        }

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

    public long getDaoNiem(Player p) { return p != null ? getDaoNiem(p.getUniqueId()) : 0L; }
    public long getDaoNiem(UUID uuid) { return uuid != null ? daoNiemMap.getOrDefault(uuid, 0L) : 0L; }
    public void addDaoNiem(Player p, long amount) { if (p != null) addDaoNiem(p.getUniqueId(), amount); }
    public void addDaoNiem(UUID uuid, long amount) { if (uuid != null) daoNiemMap.put(uuid, Math.max(0, getDaoNiem(uuid) + amount)); }
    public void setDaoNiem(Player p, long amount) { if (p != null) setDaoNiem(p.getUniqueId(), amount); }
    public void setDaoNiem(UUID uuid, long amount) { if (uuid != null) daoNiemMap.put(uuid, Math.max(0, amount)); }

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
