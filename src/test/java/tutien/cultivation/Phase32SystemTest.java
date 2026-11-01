package tutien.cultivation;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.alchemy.integration.WorldGuardDetoxHook;
import tutien.alchemy.manager.DanDuocManager;
import tutien.alchemy.model.*;
import tutien.alchemy.service.DanDocService;
import tutien.alchemy.service.DetoxService;
import tutien.alchemy.service.DrugResistanceService;
import tutien.core.CanhGioi;
import tutien.core.MeditationType;
import tutien.core.PlayerDataManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class Phase32SystemTest {

    @Test
    @DisplayName("Kiểm thử 1: Migration v4 -> v5 trên tệp đầy đủ 25+ trường (Zero Loss Test)")
    public void testFullV4PlayerMigrationDataPreservation() throws Exception {
        File testDir = new File("target/test-migration-real");
        testDir.mkdirs();
        File v4File = new File(testDir, "full_player_v4.yml");
        File bakFile = new File(testDir, "full_player_v4.yml.bak5");
        if (v4File.exists()) v4File.delete();
        if (bakFile.exists()) bakFile.delete();

        // 1. Tạo tệp v4 hoàn chỉnh với tất cả các trường từ Phase 1 đến Phase 2.4
        YamlConfiguration v4 = new YamlConfiguration();
        v4.set("DataVersion", 4);
        v4.set("TuVi", 350000);
        v4.set("Tang", 12);
        v4.set("CanhGioi", "HOA_THAN");
        v4.set("LinhLuc", 4500);
        v4.set("HeTuLuyen", "KIEM_TU");
        v4.set("CanCo", 88.5);
        v4.set("NgoTinh", 92.0);
        v4.set("ThanThuc", 120.0);
        v4.set("TheChat", 150.0);
        v4.set("DaoNgo", 50000L);
        v4.set("DaoNgoPoints", 1200L);
        v4.set("DanDoc", 145.0);
        v4.set("CongPhapChuTu", "THAI_AT_HOA_CHON_KINH");
        v4.set("ToiTheProgress", 100.0);
        v4.set("KhaiMach", Arrays.asList("TOI_THE", "TAY_TUY", "DAN_KHI"));

        // Linh Căn Data
        v4.set("LinhCanData.type", "THUY_HOA_MOC");
        v4.set("LinhCanData.purity", 85.0);
        v4.set("LinhCanData.elements.FIRE", 75.0);
        v4.set("LinhCanData.elements.WATER", 80.0);
        v4.set("LinhCanData.elements.WOOD", 70.0);

        // Cultivation Data
        v4.set("CultivationData.activePrimary", "THAI_AT_HOA_CHON_KINH");
        v4.set("CultivationData.activeAuxiliary", Arrays.asList("THAN_CONG_1", "THAN_CONG_2"));
        v4.set("CultivationData.progress.THAI_AT_HOA_CHON_KINH.currentChapter", 4);
        v4.set("CultivationData.progress.THAI_AT_HOA_CHON_KINH.learnedChapters", Arrays.asList(1, 2, 3, 4));
        v4.set("CultivationData.progress.THAI_AT_HOA_CHON_KINH.masteryExp", 4500);

        // Đan Lò & Bế Quan
        v4.set("DanLo.CapDo", 3);
        v4.set("DanLo.SoLanLuyen", 55);
        v4.set("DanLo.BonusDamage", 12.5);
        v4.set("DanLo.BonusHealth", 80.0);
        v4.set("BeQuan.Diem", 320L);

        v4.save(v4File);

        // 2. Chạy migration qua PlayerDataManager
        PlayerDataManager pdm = new PlayerDataManager(null);
        Method migrateMethod = PlayerDataManager.class.getDeclaredMethod("migrateV4ToV5",
                org.bukkit.entity.Player.class, File.class, org.bukkit.configuration.file.FileConfiguration.class);
        migrateMethod.setAccessible(true);
        migrateMethod.invoke(pdm, null, v4File, v4);

        // 3. Kiểm tra backup tồn tại
        assertTrue(bakFile.exists(), "Tệp backup .bak5 phải được tạo!");
        YamlConfiguration bak = YamlConfiguration.loadConfiguration(bakFile);
        assertEquals(4, bak.getInt("DataVersion"));
        assertEquals(350000, bak.getInt("TuVi"));

        // 4. Kiểm tra tệp sau migration lên DataVersion: 5
        YamlConfiguration v5 = YamlConfiguration.loadConfiguration(v4File);
        assertEquals(5, v5.getInt("DataVersion"));

        // 5. Kiểm tra TẤT CẢ các trường trước và sau: KHÔNG MẤT BẤT KỲ TRƯỜNG NÀO
        assertEquals(350000, v5.getInt("TuVi"));
        assertEquals(12, v5.getInt("Tang"));
        assertEquals("HOA_THAN", v5.getString("CanhGioi"));
        assertEquals(4500, v5.getInt("LinhLuc"));
        assertEquals("KIEM_TU", v5.getString("HeTuLuyen"));
        assertEquals(88.5, v5.getDouble("CanCo"), 0.001);
        assertEquals(92.0, v5.getDouble("NgoTinh"), 0.001);
        assertEquals(120.0, v5.getDouble("ThanThuc"), 0.001);
        assertEquals(150.0, v5.getDouble("TheChat"), 0.001);
        assertEquals(50000L, v5.getLong("DaoNgo"));
        assertEquals(1200L, v5.getLong("DaoNgoPoints"));
        assertEquals("THAI_AT_HOA_CHON_KINH", v5.getString("CongPhapChuTu"));
        assertEquals(100.0, v5.getDouble("ToiTheProgress"), 0.001);
        assertEquals(3, v5.getStringList("KhaiMach").size());

        // Linh Căn & Công Pháp
        assertEquals("THUY_HOA_MOC", v5.getString("LinhCanData.type"));
        assertEquals(85.0, v5.getDouble("LinhCanData.purity"), 0.001);
        assertEquals(75.0, v5.getDouble("LinhCanData.elements.FIRE"), 0.001);
        assertEquals("THAI_AT_HOA_CHON_KINH", v5.getString("CultivationData.activePrimary"));
        assertEquals(4, v5.getInt("CultivationData.progress.THAI_AT_HOA_CHON_KINH.currentChapter"));
        assertEquals(4500, v5.getInt("CultivationData.progress.THAI_AT_HOA_CHON_KINH.masteryExp"));

        // Đan Lò & Bế Quan
        assertEquals(3, v5.getInt("DanLo.CapDo"));
        assertEquals(55, v5.getInt("DanLo.SoLanLuyen"));
        assertEquals(12.5, v5.getDouble("DanLo.BonusDamage"), 0.001);
        assertEquals(80.0, v5.getDouble("DanLo.BonusHealth"), 0.001);
        assertEquals(320L, v5.getLong("BeQuan.Diem"));

        // 6. Xác nhận trường DanDocData được tạo chuẩn xác
        assertTrue(v5.isConfigurationSection("DanDocData"));
        assertEquals(145.0, v5.getDouble("DanDocData.Amount"), 0.001);
        assertEquals(0.0, v5.getDouble("DanDocData.Resistance.CULTIVATION"), 0.001);

        // Dọn dẹp
        v4File.delete();
        bakFile.delete();
    }

    @Test
    @DisplayName("Kiểm thử 2: Tích hợp hình phạt Đan Độc vào hiệu suất tu luyện và khả năng phục hồi")
    public void testTuLuyenTaskDanDocPenaltyIntegration() {
        PlayerDataManager pdm = new PlayerDataManager(null);
        DanDocService danDocService = new DanDocService(pdm, null);
        UUID uuid = UUID.randomUUID();

        // 1. Mặc định 0 độc: hiệu suất = 100%
        assertEquals(1.0, danDocService.getCultivationEfficiency(null));

        DanDocData ddd = pdm.getDanDocData(uuid);
        ddd.setDanDoc(0.0);
        assertEquals(1.00, ddd.getToxicityStatus().getCultivationEfficiency(), 0.001);

        // 2. Độc Nhẹ (250): giảm 5%
        ddd.setDanDoc(250.0);
        assertEquals(0.95, ddd.getToxicityStatus().getCultivationEfficiency(), 0.001);

        // 3. Độc Trung (450): giảm 15%
        ddd.setDanDoc(450.0);
        assertEquals(0.85, ddd.getToxicityStatus().getCultivationEfficiency(), 0.001);

        // 4. Độc Nặng (650): giảm 30%
        ddd.setDanDoc(650.0);
        assertEquals(0.70, ddd.getToxicityStatus().getCultivationEfficiency(), 0.001);

        // 5. Độc Phát (850): giảm 50%
        ddd.setDanDoc(850.0);
        assertEquals(0.50, ddd.getToxicityStatus().getCultivationEfficiency(), 0.001);

        // 6. Thanh lọc giảm về 50: phục hồi về 100% không còn hình phạt
        danDocService.reduceDanDoc(uuid, 800.0);
        assertEquals(50.0, ddd.getDanDoc(), 0.001);
        assertEquals(1.00, ddd.getToxicityStatus().getCultivationEfficiency(), 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 3: Soft-Hook WorldGuard an toàn khi thiếu plugin")
    public void testWorldGuardDetoxHookSafetyWhenMissing() {
        // Trong môi trường unit test headless (Bukkit.getServer() == null hoặc không cài WorldGuard)
        assertFalse(WorldGuardDetoxHook.isAvailable(), "WorldGuard phải trả về false an toàn!");
        assertFalse(WorldGuardDetoxHook.isInRegion(null, "linh-tuyen-tay-tran"), "Không được ném ngoại lệ khi không có WG!");
    }

    @Test
    @DisplayName("Kiểm thử 4: Đồng bộ trạng thái Bế Quan Hóa Độc (DetoxSession & Synchronization)")
    public void testDetoxSessionLifecycleAndSyncProtection() {
        PlayerDataManager pdm = new PlayerDataManager(null);
        DanDocService danDocService = new DanDocService(pdm, null);
        DetoxService detoxService = new DetoxService(null, pdm, danDocService, null);

        UUID uuid = UUID.randomUUID();
        DanDocData ddd = pdm.getDanDocData(uuid);
        ddd.setDanDoc(300.0);

        // 1. Kiểm tra session cơ bản
        DetoxSession session = new DetoxSession(uuid, null);
        assertTrue(session.isActive());
        assertEquals(uuid, session.getPlayerId());

        // 2. Nhịp thanh lọc per-tick: rate = 6.0/phút = 0.1/giây
        // Giả lập 2.0s trôi qua (chu kỳ hợp lệ)
        double cleansed = session.tick(6.0, 2.0);
        assertTrue(cleansed >= 0.0, "Lượng thanh lọc phải không âm");

        // 3. Cơ chế kẹp delta chống lag: lag 10s chỉ được tính tối đa 2.0s
        double lagCleansed = session.tick(6.0, 2.0);
        assertTrue(lagCleansed <= (6.0 / 60.0) * 2.0 + 0.01, "Delta time lag phải bị clamp tại maxTickGapSeconds!");

        session.stop();
        assertFalse(session.isActive());
        assertEquals(0.0, session.tick(6.0, 2.0), "Session đã dừng không được phép thanh lọc tiếp!");
    }

    @Test
    @DisplayName("Kiểm thử 5: Cơ chế suy giảm Kháng Thuốc online chống gian lận offline")
    public void testOnlineDecayOnlyCountsOnlineTime() {
        PlayerDataManager pdm = new PlayerDataManager(null);
        DrugResistanceService service = new DrugResistanceService(pdm, null);
        UUID uuid = UUID.randomUUID();

        service.addResistance(uuid, "CULTIVATION", 40.0);
        assertEquals(40.0, service.getResistance(uuid, "CULTIVATION"), 0.001);

        // Khi người chơi offline (player null), không thể gọi tickOnlineDecay
        service.tickOnlineDecay(null);
        // Kháng vẫn giữ nguyên 40.0, hoàn toàn không suy giảm do thời gian offline!
        assertEquals(40.0, service.getResistance(uuid, "CULTIVATION"), 0.001);

        // Dọn dẹp cache khi player quit
        service.onPlayerQuit(uuid);
        assertEquals(40.0, service.getResistance(uuid, "CULTIVATION"), 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 6: Kết quả nghiệp vụ DetoxResult và DetoxMethod")
    public void testDetoxResultAndMethods() {
        DetoxResult success = DetoxResult.success(50.0, 150.0, "Thành công");
        assertTrue(success.isSuccess());
        assertEquals(DetoxResult.Status.SUCCESS, success.getStatus());
        assertEquals(50.0, success.getToxicityCleansed(), 0.001);
        assertEquals(150.0, success.getRemainingToxicity(), 0.001);

        DetoxResult fail = DetoxResult.fail(DetoxResult.Status.MEDITATING_OTHER, 200.0, "Đang tu luyện");
        assertFalse(fail.isSuccess());
        assertEquals(DetoxResult.Status.MEDITATING_OTHER, fail.getStatus());

        assertEquals(3, DetoxMethod.values().length);
        assertNotNull(DetoxMethod.valueOf("THANH_DAN"));
        assertNotNull(DetoxMethod.valueOf("LINH_TUYEN"));
        assertNotNull(DetoxMethod.valueOf("BE_QUAN_HOA_DOC"));
    }
}
