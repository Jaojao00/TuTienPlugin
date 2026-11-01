package tutien.cultivation;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.core.CanhGioi;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.manager.CongPhapManager;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.cultivation.service.ComprehensionService;
import tutien.cultivation.service.ElementCompatibilityService;
import tutien.cultivation.service.EpiphanyService;
import tutien.cultivation.service.MasteryService;
import tutien.element.ElementRelationService;
import tutien.element.LinhCanData;
import tutien.element.LinhCanType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Phase23SystemTest {

    private CongPhapManager manager;
    private ElementRelationService relationService;
    private ElementCompatibilityService compatibilityService;
    private ComprehensionService comprehensionService;
    private MasteryService masteryService;
    private EpiphanyService epiphanyService;

    @BeforeEach
    public void setup() {
        manager = new CongPhapManager(null);
        InputStream stream = getClass().getClassLoader().getResourceAsStream("congphap.yml");
        assertNotNull(stream, "congphap.yml phải tồn tại trong resources!");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        manager.loadFromConfig(config);

        relationService = manager.getRelationService();
        compatibilityService = manager.getCompatibilityService();
        comprehensionService = manager.getComprehensionService();
        masteryService = manager.getMasteryService();
        epiphanyService = manager.getEpiphanyService();
    }

    @Test
    @DisplayName("Kiểm thử 1: Tương thích Phi - Đơn linh căn Đồng hệ, Sinh, Khắc")
    public void testCompatibilitySingleElement() {
        CongPhap tm = manager.getCongPhap("THANH_MOC_TRUONG_XUAN"); // Yêu cầu MOC
        assertNotNull(tm);

        // 1. Thuần Mộc (Mộc 100) -> Đồng hệ 1.50
        LinhCanData mocLinhCan = new LinhCanData(LinhCanType.THIEN_LINH_CAN, LinhCanType.THIEN_LINH_CAN, 1.00);
        mocLinhCan.setQuality("MOC", 100.0);
        double phiMoc = compatibilityService.calculateCompatibility(mocLinhCan, tm);
        assertEquals(1.50, phiMoc, 0.001, "Thuần Mộc tu luyện công pháp Mộc phải đạt Phi = 1.50 (Đồng hệ)");

        // 2. Thuần Thủy (Thủy 100) -> Thủy sinh Mộc 1.20
        LinhCanData thuyLinhCan = new LinhCanData(LinhCanType.THIEN_LINH_CAN, LinhCanType.THIEN_LINH_CAN, 1.00);
        thuyLinhCan.setQuality("THUY", 100.0);
        double phiThuy = compatibilityService.calculateCompatibility(thuyLinhCan, tm);
        assertEquals(1.20, phiThuy, 0.001, "Thuần Thủy tu luyện công pháp Mộc phải đạt Phi = 1.20 (Tương sinh)");

        // 3. Thuần Kim (Kim 100) -> Kim khắc Mộc 0.60
        LinhCanData kimLinhCan = new LinhCanData(LinhCanType.THIEN_LINH_CAN, LinhCanType.THIEN_LINH_CAN, 1.00);
        kimLinhCan.setQuality("KIM", 100.0);
        double phiKim = compatibilityService.calculateCompatibility(kimLinhCan, tm);
        assertEquals(0.60, phiKim, 0.001, "Thuần Kim tu luyện công pháp Mộc phải chịu Phi = 0.60 (Tương khắc)");
    }

    @Test
    @DisplayName("Kiểm thử 2: Tương thích Phi - Đa linh căn & Chuẩn hóa không nhân đôi Purity")
    public void testCompatibilityMultiElementAndPurityIndependence() {
        CongPhap tm = manager.getCongPhap("THANH_MOC_TRUONG_XUAN"); // Yêu cầu MOC

        // Song Linh Căn: Thủy (80) và Hỏa (80)
        // Với req=MOC: Thủy sinh Mộc (1.20 * 0.8 = 0.96), Hỏa không sinh/khắc Mộc (1.00 * 0.8 = 0.80)
        // max Ae = 0.96 -> Phi = 0.96
        LinhCanData songLinhCanPurity50 = new LinhCanData(LinhCanType.CHAN_LINH_CAN, LinhCanType.CHAN_LINH_CAN, 0.50);
        songLinhCanPurity50.setQuality("THUY", 80.0);
        songLinhCanPurity50.setQuality("HOA", 80.0);

        LinhCanData songLinhCanPurity90 = new LinhCanData(LinhCanType.CHAN_LINH_CAN, LinhCanType.CHAN_LINH_CAN, 0.90);
        songLinhCanPurity90.setQuality("THUY", 80.0);
        songLinhCanPurity90.setQuality("HOA", 80.0);

        double phi1 = compatibilityService.calculateCompatibility(songLinhCanPurity50, tm);
        double phi2 = compatibilityService.calculateCompatibility(songLinhCanPurity90, tm);

        assertEquals(0.96, phi1, 0.001);
        assertEquals(0.96, phi2, 0.001);
        assertEquals(phi1, phi2, "Purity tuyệt đối KHÔNG được nhân đôi vào hệ số tương thích Phi!");

        // Giới hạn clamp [0.40, 1.60]
        LinhCanData badLinhCan = new LinhCanData(LinhCanType.NGUY_LINH_CAN, LinhCanType.NGUY_LINH_CAN, 0.50);
        badLinhCan.setQuality("KIM", 10.0); // Kim khắc Mộc (0.60 * 0.1 = 0.06) -> clamp min 0.40
        double phiClamp = compatibilityService.calculateCompatibility(badLinhCan, tm);
        assertEquals(ElementCompatibilityService.MIN_PHI, phiClamp, 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 3: Công thức Tham Ngộ Delta P - Phản ánh Ngộ Tính, Phi và Độ khó tầng")
    public void testComprehensionFormula() {
        // Delta P = B * Delta t * N * Phi * D
        // B = 0.50%/min, Delta t = 1.0 min, NgoTinh = 10.0 (N = 1.0), Phi = 1.0, chapter = 1 (D = 1.0)
        double delta1 = comprehensionService.calculateDeltaProgress(1.0, 10.0, 1.0, 1);
        assertEquals(0.50, delta1, 0.001);

        // Tầng cao hơn (chapter 5) -> D = 1.0 - 4*0.05 = 0.80 -> Delta P = 0.5 * 1.0 * 1.0 * 1.0 * 0.8 = 0.40
        double deltaHighChapter = comprehensionService.calculateDeltaProgress(1.0, 10.0, 1.0, 5);
        assertEquals(0.40, deltaHighChapter, 0.001);

        // Ngộ tính cao (NgoTinh = 50.0) -> N = 1.0 + 40/40 = 2.0 (Max N) -> Delta P = 0.5 * 1.0 * 2.0 * 1.0 * 1.0 = 1.00
        double deltaHighNgoTinh = comprehensionService.calculateDeltaProgress(1.0, 50.0, 1.0, 1);
        assertEquals(1.00, deltaHighNgoTinh, 0.001);

        // Giới hạn chống AFK/lag: deltaMinutes bị clamp tối đa 2.0 phút
        double deltaLag = comprehensionService.calculateDeltaProgress(100.0, 10.0, 1.0, 1);
        double deltaMaxExpected = comprehensionService.calculateDeltaProgress(2.0, 10.0, 1.0, 1);
        assertEquals(deltaMaxExpected, deltaLag, 0.001, "Delta t khi lag/AFK phải bị giới hạn an toàn tối đa 2.0 phút!");
    }

    @Test
    @DisplayName("Kiểm thử 4: Tiến trình Tham Ngộ - Tự động đột phá chương khi đạt 100%")
    public void testComprehensionProgressionAndChapterAdvance() {
        CongPhapProgress prog = new CongPhapProgress("THANH_MOC_TRUONG_XUAN");
        prog.addCollectedChapter(1);
        prog.addCollectedChapter(2);
        prog.setTargetChapter(1);

        assertEquals(0.0, prog.getComprehensionProgress());
        prog.addComprehensionProgress(45.5);
        assertEquals(45.5, prog.getComprehensionProgress(), 0.001);

        // Thêm tiếp để đạt 100%
        prog.addComprehensionProgress(54.5);
        assertEquals(100.0, prog.getComprehensionProgress(), 0.001);

        // Lĩnh hội thành công chương 1
        prog.addLearnedChapter(1);
        // addLearnedChapter tự động chuyển targetChapter sang 2 và reset progress
        assertEquals(2, prog.getTargetChapter());
        assertEquals(0.0, prog.getComprehensionProgress());
        assertTrue(prog.hasLearned(1));
    }

    @Test
    @DisplayName("Kiểm thử 5: Độ Thành Thục Mastery - 4 Cấp bậc và Giảm tiêu hao Linh Lực")
    public void testMasteryTiersAndManaCostReduction() {
        assertEquals(MasteryService.MasteryTier.SO_KHOI, MasteryService.MasteryTier.fromExp(0));
        assertEquals(MasteryService.MasteryTier.SO_KHOI, MasteryService.MasteryTier.fromExp(999));
        assertEquals(0.00, MasteryService.MasteryTier.SO_KHOI.getManaCostReduction());

        assertEquals(MasteryService.MasteryTier.TIEU_THANH, MasteryService.MasteryTier.fromExp(1000));
        assertEquals(MasteryService.MasteryTier.TIEU_THANH, MasteryService.MasteryTier.fromExp(3999));
        assertEquals(0.05, MasteryService.MasteryTier.TIEU_THANH.getManaCostReduction());

        assertEquals(MasteryService.MasteryTier.DAI_THANH, MasteryService.MasteryTier.fromExp(4000));
        assertEquals(MasteryService.MasteryTier.DAI_THANH, MasteryService.MasteryTier.fromExp(9999));
        assertEquals(0.10, MasteryService.MasteryTier.DAI_THANH.getManaCostReduction());

        assertEquals(MasteryService.MasteryTier.VIEN_MAN, MasteryService.MasteryTier.fromExp(10000));
        assertEquals(MasteryService.MasteryTier.VIEN_MAN, MasteryService.MasteryTier.fromExp(50000));
        assertEquals(0.15, MasteryService.MasteryTier.VIEN_MAN.getManaCostReduction());

        // Kiểm tra PlayerCultivationData tích hợp MasteryExp
        PlayerCultivationData data = new PlayerCultivationData("THANH_MOC_TRUONG_XUAN");
        CongPhapProgress prog = data.getOrCreateProgress("THANH_MOC_TRUONG_XUAN");
        prog.setMasteryExp(4500);

        MasteryService.MasteryTier tier = masteryService.getMasteryTier(data, "THANH_MOC_TRUONG_XUAN");
        assertEquals(MasteryService.MasteryTier.DAI_THANH, tier);
        assertEquals(0.10, masteryService.getManaCostReduction(data, "THANH_MOC_TRUONG_XUAN"));
    }

    @Test
    @DisplayName("Kiểm thử 6: Cơ Duyên Đốn Ngộ - Cooldown 15 phút và Tích lũy tiềm năng")
    public void testEpiphanyCooldownAndPotential() {
        CongPhapProgress prog = new CongPhapProgress("THANH_MOC_TRUONG_XUAN");
        prog.setEpiphanyPotential(15.0);
        assertEquals(15.0, prog.getEpiphanyPotential());

        prog.addEpiphanyPotential(10.5);
        assertEquals(25.5, prog.getEpiphanyPotential(), 0.001);

        // Clamp tối đa 100.0
        prog.addEpiphanyPotential(200.0);
        assertEquals(100.0, prog.getEpiphanyPotential());

        // Reset tiềm năng sau khi hoàn tất
        prog.setEpiphanyPotential(0.0);
        assertEquals(0.0, prog.getEpiphanyPotential());

        // Xác nhận cooldown cấu hình đúng 15 phút (900000 ms)
        assertEquals(15 * 60 * 1000L, EpiphanyService.EPIPHANY_COOLDOWN_MS);
    }

    @Test
    @DisplayName("Kiểm thử 7: Migration V3 -> V4 - Bảo toàn toàn bộ dữ liệu v3 và bổ sung DaoNgoPoints")
    public void testMigrationV3ToV4Integrity() {
        String yamlV3 =
                "DataVersion: 3\n" +
                "TuVi: 12500\n" +
                "Tang: 7\n" +
                "CanhGioi: TRUC_CO\n" +
                "CongPhapChuTu: THANH_MOC_TRUONG_XUAN\n" +
                "CultivationData:\n" +
                "  active-primary: THANH_MOC_TRUONG_XUAN\n" +
                "  active-auxiliary: [MINH_VUONG_QUYET]\n" +
                "  progress:\n" +
                "    THANH_MOC_TRUONG_XUAN:\n" +
                "      collected-chapters: [1, 2, 3]\n" +
                "      learned-chapters: [1, 2]\n" +
                "      target-chapter: 3\n" +
                "      comprehension-progress: 42.5\n" +
                "      mastery-exp: 2500\n" +
                "      epiphany-potential: 18.0\n";

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yamlV3);
        } catch (Exception e) {
            fail("Lỗi load YAML: " + e.getMessage());
        }

        // Giả lập quy trình migrateV3ToV4
        long currentDaoNgoPoints = config.getLong("DaoNgoPoints", 0L);
        config.set("DataVersion", 4);
        config.set("DaoNgoPoints", currentDaoNgoPoints);

        // Roundtrip qua chuỗi serialization
        YamlConfiguration diskConfig = new YamlConfiguration();
        try {
            diskConfig.loadFromString(config.saveToString());
        } catch (Exception e) {
            fail("Lỗi serialize YAML: " + e.getMessage());
        }

        // Kiểm tra sau khi migrate
        assertEquals(4, diskConfig.getInt("DataVersion"));
        assertEquals(0L, diskConfig.getLong("DaoNgoPoints"));
        assertEquals("THANH_MOC_TRUONG_XUAN", diskConfig.getString("CultivationData.active-primary"));
        assertEquals(42.5, diskConfig.getDouble("CultivationData.progress.THANH_MOC_TRUONG_XUAN.comprehension-progress"), 0.001);
        assertEquals(2500, diskConfig.getInt("CultivationData.progress.THANH_MOC_TRUONG_XUAN.mastery-exp"));
        assertEquals(18.0, diskConfig.getDouble("CultivationData.progress.THANH_MOC_TRUONG_XUAN.epiphany-potential"), 0.001);

        // Đọc lại vào model
        PlayerCultivationData restored = PlayerCultivationData.fromConfigurationSection(
                diskConfig.getConfigurationSection("CultivationData"));
        assertNotNull(restored);
        assertEquals("THANH_MOC_TRUONG_XUAN", restored.getActivePrimary());
        assertTrue(restored.isAuxiliary("MINH_VUONG_QUYET"));

        CongPhapProgress prog = restored.getCongPhapProgress("THANH_MOC_TRUONG_XUAN");
        assertNotNull(prog);
        assertEquals(3, prog.getTargetChapter());
        assertEquals(42.5, prog.getComprehensionProgress(), 0.001);
        assertEquals(2500, prog.getMasteryExp());
        assertEquals(18.0, prog.getEpiphanyPotential(), 0.001);
        assertEquals(MasteryService.MasteryTier.TIEU_THANH, MasteryService.MasteryTier.fromExp(prog.getMasteryExp()));
    }
}
