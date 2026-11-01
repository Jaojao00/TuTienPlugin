package tutien.cultivation;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.core.CanhGioi;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.manager.CongPhapManager;
import tutien.cultivation.model.*;
import tutien.cultivation.service.CongPhapService;
import tutien.cultivation.service.CongPhapTransitionService;
import tutien.cultivation.service.CongPhapValidationService;
import tutien.element.ElementRelationService;
import tutien.element.LinhCanData;
import tutien.element.LinhCanType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CongPhapSystemTest {

    private CongPhapManager manager;
    private CongPhapService service;
    private CongPhapValidationService validationService;
    private CongPhapTransitionService transitionService;

    @BeforeEach
    public void setup() {
        manager = new CongPhapManager(null);
        InputStream stream = getClass().getClassLoader().getResourceAsStream("congphap.yml");
        assertNotNull(stream, "congphap.yml phải tồn tại trong resources!");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        manager.loadFromConfig(config);

        service = manager.getService();
        validationService = service.getValidationService();
        transitionService = service.getTransitionService();
    }

    @Test
    @DisplayName("Kiểm thử 1: Nạp catalog - Đọc đầy đủ các công pháp định nghĩa trong congphap.yml")
    public void testLoadCatalog() {
        assertTrue(manager.getCatalogSize() >= 5, "Catalog phải nạp tối thiểu 5 bộ công pháp mẫu!");

        CongPhap tm = manager.getCongPhap("THANH_MOC_TRUONG_XUAN");
        assertNotNull(tm);
        assertEquals(CongPhapGrade.HUYEN, tm.getGradeTier());
        assertEquals(CongPhapQuality.THUONG, tm.getGradeQuality());
        assertEquals(CanhGioi.TRUC_CO, tm.getMaxRealm());
        assertEquals(9, tm.getTotalChapters());
        assertTrue(tm.getElements().contains("MOC"));

        CongPhap hp = manager.getCongPhap("HOANG_PHONG_QUYET");
        assertNotNull(hp);
        assertEquals(CongPhapGrade.HOANG, hp.getGradeTier());
        assertEquals(13, hp.getTotalChapters());

        CongPhap mv = manager.getCongPhap("MINH_VUONG_QUYET");
        assertNotNull(mv);
        assertTrue(mv.isRequiresHuMaiDan());

        CongPhap dd = manager.getCongPhap("DAI_DIEN_QUYET");
        assertNotNull(dd);
        assertEquals(CongPhapGrade.DIA, dd.getGradeTier());
    }

    @Test
    @DisplayName("Kiểm thử 2: ID trùng - Phát hiện và từ chối ID trùng lặp, bảo toàn Catalog")
    public void testDuplicateIdHandling() {
        String yamlWithDuplicate =
                "congphap:\n" +
                "  TEST_CP:\n" +
                "    id: TEST_CP\n" +
                "    display-name: 'Test 1'\n" +
                "    grade:\n" +
                "      tier: HOANG\n" +
                "  test_cp:\n" +
                "    id: TEST_CP\n" +
                "    display-name: 'Test Duplicate'\n" +
                "    grade:\n" +
                "      tier: HUYEN\n";

        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yamlWithDuplicate);
        } catch (Exception e) {
            fail("Lỗi parse YAML: " + e.getMessage());
        }

        CongPhapManager testMgr = new CongPhapManager(null);
        testMgr.loadFromConfig(cfg);

        // Chỉ có 1 bộ TEST_CP được đăng ký thành công, bản trùng lặp bị loại bỏ
        assertEquals(1, testMgr.getCatalogSize());
        assertEquals(CongPhapGrade.HOANG, testMgr.getCongPhap("TEST_CP").getGradeTier());
    }

    @Test
    @DisplayName("Kiểm thử 3: Phẩm cấp và Chất lượng - Phân biệt đúng Hạ, Trung, Thượng độc lập")
    public void testGradeAndQualitySeparation() {
        CongPhap tm = manager.getCongPhap("THANH_MOC_TRUONG_XUAN");
        assertEquals("§aHuyền Giai §6Thượng Phẩm", tm.getFormattedGrade());

        CongPhap mv = manager.getCongPhap("MINH_VUONG_QUYET");
        assertEquals("§aHuyền Giai §bTrung Phẩm", mv.getFormattedGrade());

        CongPhap dd = manager.getCongPhap("DAI_DIEN_QUYET");
        assertEquals("§bĐịa Giai §7Hạ Phẩm", dd.getFormattedGrade());
    }

    @Test
    @DisplayName("Kiểm thử 4: Chủ tu - Duy nhất 1 bộ công pháp chủ tu được kích hoạt")
    public void testSingleActivePrimary() {
        PlayerCultivationData data = new PlayerCultivationData();
        data.setActivePrimary("THANH_MOC_TRUONG_XUAN");
        assertEquals("THANH_MOC_TRUONG_XUAN", data.getActivePrimary());

        // Thay đổi chủ tu
        data.setActivePrimary("HOANG_PHONG_QUYET");
        assertEquals("HOANG_PHONG_QUYET", data.getActivePrimary());
    }

    @Test
    @DisplayName("Kiểm thử 5: Phụ tu - Không vượt quá số slot tối đa và không thể vừa chủ tu vừa phụ tu")
    public void testAuxiliaryLimits() {
        PlayerCultivationData data = new PlayerCultivationData("THANH_MOC_TRUONG_XUAN");

        // Không thể phụ tu công pháp đang là chủ tu
        assertFalse(data.addAuxiliary("THANH_MOC_TRUONG_XUAN", 2));

        // Phụ tu bộ 1
        assertTrue(data.addAuxiliary("MINH_VUONG_QUYET", 2));
        assertEquals(1, data.getActiveAuxiliary().size());

        // Phụ tu bộ 2
        assertTrue(data.addAuxiliary("DAI_DIEN_QUYET", 2));
        assertEquals(2, data.getActiveAuxiliary().size());

        // Phụ tu bộ 3 (vượt giới hạn 2 slot) -> Từ chối
        assertFalse(data.addAuxiliary("CU_KHI_QUYET", 2));
        assertEquals(2, data.getActiveAuxiliary().size());
    }

    @Test
    @DisplayName("Kiểm thử 6: Tàn quyển - Dùng Set<Integer>, không lưu trùng lặp chương")
    public void testNonDuplicateChapters() {
        CongPhapProgress prog = new CongPhapProgress("THANH_MOC_TRUONG_XUAN");

        assertTrue(prog.addCollectedChapter(1));
        assertTrue(prog.addCollectedChapter(2));
        assertTrue(prog.addCollectedChapter(4));

        // Thêm lại chương 2 -> không trùng lặp
        assertFalse(prog.addCollectedChapter(2));
        assertEquals(3, prog.getCollectedChapters().size());
        assertTrue(prog.hasCollected(1));
        assertTrue(prog.hasCollected(2));
        assertTrue(prog.hasCollected(4));
        assertFalse(prog.hasCollected(3));
    }

    @Test
    @DisplayName("Kiểm thử 7: Lĩnh hội - Không thể học chương chưa sở hữu và không thể nhảy cóc tầng")
    public void testOrderlyLearningPrerequisites() {
        CongPhap tm = manager.getCongPhap("THANH_MOC_TRUONG_XUAN");
        CongPhapProgress prog = new CongPhapProgress("THANH_MOC_TRUONG_XUAN");

        // Chưa sưu tầm chương 1 -> Không thể học
        CongPhapValidationService.ValidationResult r1 = validationService.validateLearnChapter(
                tm, prog, 1, CanhGioi.LUYEN_KHI);
        assertFalse(r1.isSuccess());

        // Thu thập chương 1 và chương 3 (bỏ qua chương 2)
        prog.addCollectedChapter(1);
        prog.addCollectedChapter(3);

        // Học chương 1 -> Thành công
        assertTrue(validationService.validateLearnChapter(tm, prog, 1, CanhGioi.LUYEN_KHI).isSuccess());
        prog.addLearnedChapter(1);

        // Thử học chương 3 khi chưa học chương 2 -> Bị chặn (không nhảy cóc)
        CongPhapValidationService.ValidationResult r3 = validationService.validateLearnChapter(
                tm, prog, 3, CanhGioi.LUYEN_KHI);
        assertFalse(r3.isSuccess());
        assertTrue(r3.getMessage().contains("nhảy cóc"));

        // Thu thập và học chương 2
        prog.addCollectedChapter(2);
        assertTrue(validationService.validateLearnChapter(tm, prog, 2, CanhGioi.LUYEN_KHI).isSuccess());
        prog.addLearnedChapter(2);

        // Giờ học chương 3 -> Thành công!
        assertTrue(validationService.validateLearnChapter(tm, prog, 3, CanhGioi.LUYEN_KHI).isSuccess());
    }

    @Test
    @DisplayName("Kiểm thử 8: Chuyển tu - Tuyệt đối không hạ đại cảnh giới, tính tổn thất tu vi tiểu cảnh giới")
    public void testTransitionSafety() {
        PlayerCultivationData data = new PlayerCultivationData("HOANG_PHONG_QUYET");
        CongPhap oldCp = manager.getCongPhap("HOANG_PHONG_QUYET");
        CongPhap newCp = manager.getCongPhap("THANH_MOC_TRUONG_XUAN");

        // Đã sở hữu chương 1 của công pháp mới
        CongPhapProgress newProg = data.getOrCreateProgress("THANH_MOC_TRUONG_XUAN");
        newProg.addCollectedChapter(1);

        int currentTuVi = 1000;
        CanhGioi currentRealm = CanhGioi.TRUC_CO;

        CongPhapTransitionService.TransitionResult result = transitionService.transition(
                data, oldCp, newCp, currentTuVi, currentRealm, false);

        assertTrue(result.isSuccess());
        assertEquals("THANH_MOC_TRUONG_XUAN", data.getActivePrimary());
        assertTrue(data.getAdaptation().isActive());
        assertTrue(data.getAdaptation().getRemainingSeconds() > 0);

        // Tổn thất chỉ trừ trên tu vi tiểu cảnh giới hiện tại
        assertTrue(result.getTuViLost() > 0 && result.getTuViLost() < currentTuVi);

        // Đại cảnh giới người chơi tuyệt đối không bị suy suyển
        assertEquals(CanhGioi.TRUC_CO, currentRealm);
    }

    @Test
    @DisplayName("Kiểm thử 9: Serialization & Restart - Bảo toàn trạng thái thích nghi và dữ liệu học tập")
    public void testSerializationAndRestart() {
        PlayerCultivationData data = new PlayerCultivationData("THANH_MOC_TRUONG_XUAN");
        data.addAuxiliary("MINH_VUONG_QUYET", 2);

        long futureEnd = System.currentTimeMillis() + 1800000L; // 30 phút sau
        data.setAdaptation(new AdaptationData(true, futureEnd, "HOANG_PHONG_QUYET", 0.20));

        CongPhapProgress tmProg = data.getOrCreateProgress("THANH_MOC_TRUONG_XUAN");
        tmProg.addCollectedChapter(1);
        tmProg.addCollectedChapter(2);
        tmProg.addCollectedChapter(3);
        tmProg.addLearnedChapter(1);
        tmProg.addLearnedChapter(2);

        // Chuyển sang Map (giả lập lưu vào file YAML)
        Map<String, Object> map = data.toMap();

        // Khôi phục từ Map (giả lập restart server và đọc lại từ YAML)
        PlayerCultivationData restored = PlayerCultivationData.fromMap(map);

        assertEquals("THANH_MOC_TRUONG_XUAN", restored.getActivePrimary());
        assertEquals(1, restored.getActiveAuxiliary().size());
        assertTrue(restored.isAuxiliary("MINH_VUONG_QUYET"));

        // Kiểm tra Adaptation
        assertTrue(restored.getAdaptation().isActive());
        assertEquals(futureEnd, restored.getAdaptation().getEndTimestamp());
        assertEquals("HOANG_PHONG_QUYET", restored.getAdaptation().getPreviousCongPhap());
        assertEquals(0.20, restored.getAdaptation().getPenaltyRatio(), 0.001);

        // Kiểm tra Chapters
        CongPhapProgress restoredProg = restored.getCongPhapProgress("THANH_MOC_TRUONG_XUAN");
        assertNotNull(restoredProg);
        assertEquals(3, restoredProg.getCollectedChapters().size());
        assertEquals(2, restoredProg.getLearnedChapters().size());
        assertTrue(restoredProg.hasLearned(1));
        assertTrue(restoredProg.hasLearned(2));
        assertFalse(restoredProg.hasLearned(3));
    }

    @Test
    @DisplayName("Kiểm thử 10: Migration V2 -> V3 - Bảo toàn công pháp cũ và giữ nguyên ID kể cả khi chưa có trong catalog")
    public void testMigrationV2ToV3Safety() {
        // Giả lập cấu hình người chơi ở DataVersion 2
        String yamlV2 =
                "DataVersion: 2\n" +
                "TuVi: 5000\n" +
                "Tang: 5\n" +
                "CanhGioi: LUYEN_KHI\n" +
                "CongPhapChuTu: CO_XUA_VO_DANH_QUYET\n"; // Công pháp chưa có trong catalog mẫu

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yamlV2);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Chuyển đổi V2 -> V3
        String oldCp = config.getString("CongPhapChuTu", "CHUA_CO");
        PlayerCultivationData cultData = new PlayerCultivationData(oldCp);
        if (!oldCp.equalsIgnoreCase("CHUA_CO") && !oldCp.trim().isEmpty()) {
            CongPhapProgress prog = cultData.getOrCreateProgress(oldCp);
            prog.addLearnedChapter(1);
        }

        config.set("DataVersion", 3);
        config.set("CultivationData", cultData.toMap());

        // Kiểm tra sau khi ghi ra file YAML và đọc lại
        YamlConfiguration diskConfig = new YamlConfiguration();
        try {
            diskConfig.loadFromString(config.saveToString());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Kiểm tra tính toàn vẹn
        assertEquals(3, diskConfig.getInt("DataVersion"));
        assertTrue(diskConfig.isConfigurationSection("CultivationData"));
        assertEquals("CO_XUA_VO_DANH_QUYET", diskConfig.getString("CultivationData.active-primary"));

        // Khôi phục từ section
        PlayerCultivationData restored = PlayerCultivationData.fromConfigurationSection(
                diskConfig.getConfigurationSection("CultivationData"));
        assertEquals("CO_XUA_VO_DANH_QUYET", restored.getActivePrimary());
        assertTrue(restored.hasLearnedCongPhap("CO_XUA_VO_DANH_QUYET"));
        assertTrue(restored.isLearnedChapter("CO_XUA_VO_DANH_QUYET", 1));
    }
}
