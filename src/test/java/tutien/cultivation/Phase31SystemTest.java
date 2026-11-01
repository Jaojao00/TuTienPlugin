package tutien.cultivation;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.alchemy.model.*;
import tutien.alchemy.service.DanDocService;
import tutien.alchemy.service.DrugResistanceService;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class Phase31SystemTest {

    @Test
    @DisplayName("Kiểm thử 1: Khởi tạo và Serialization của DanDocData")
    public void testDanDocDataSerializationAndDefaults() {
        DanDocData data = new DanDocData();
        assertEquals(0.0, data.getDanDoc(), 0.001);
        assertEquals(ToxicityStatus.THANH_KHIET, data.getToxicityStatus());

        // Kiểm tra 6 nhóm kháng thuốc mặc định đều bằng 0
        for (ResistanceGroup group : ResistanceGroup.values()) {
            assertEquals(0.0, data.getResistance(group.name()), 0.001);
        }

        // Thay đổi chỉ số
        data.setDanDoc(350.0);
        assertEquals(ToxicityStatus.DOC_NHE, data.getToxicityStatus());

        data.addResistance("CULTIVATION", 25.0);
        data.addResistance("RECOVERY", 10.0);
        data.setLastDoseAt("CULTIVATION", 123456789L);
        data.setLastDetoxAt(987654321L);

        // Serialize ra ConfigurationSection
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection sec = config.createSection("DanDocData");
        data.toConfigurationSection(sec);

        // Deserialize trở lại
        DanDocData loaded = DanDocData.fromConfigurationSection(sec);
        assertEquals(350.0, loaded.getDanDoc(), 0.001);
        assertEquals(ToxicityStatus.DOC_NHE, loaded.getToxicityStatus());
        assertEquals(25.0, loaded.getResistance("CULTIVATION"), 0.001);
        assertEquals(10.0, loaded.getResistance("RECOVERY"), 0.001);
        assertEquals(0.0, loaded.getResistance("BODY"), 0.001);
        assertEquals(123456789L, loaded.getLastDoseAt("CULTIVATION"));
        assertEquals(987654321L, loaded.getLastDetoxAt());

        // Clone
        DanDocData clone = loaded.deepClone();
        assertEquals(loaded.getDanDoc(), clone.getDanDoc(), 0.001);
        assertEquals(loaded.getResistance("CULTIVATION"), clone.getResistance("CULTIVATION"), 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 2: Quy trình Migration v4 -> v5 (Tạo .bak5, bảo vệ dữ liệu, stamp v5)")
    public void testMigrationV4ToV5CreatesBackupAndSetsVersion() throws Exception {
        File testDir = new File("target/test-migration");
        testDir.mkdirs();
        File v4File = new File(testDir, "player_test_v4.yml");
        File bakFile = new File(testDir, "player_test_v4.yml.bak5");
        if (v4File.exists()) v4File.delete();
        if (bakFile.exists()) bakFile.delete();

        // Tạo tệp giả lập v4 của người chơi
        YamlConfiguration v4Config = new YamlConfiguration();
        v4Config.set("DataVersion", 4);
        v4Config.set("TuVi", 15420);
        v4Config.set("Tang", 7);
        v4Config.set("CanhGioi", "TRUC_CO");
        v4Config.set("DaoNgoPoints", 450L);
        v4Config.set("DanDoc", 85.5);
        v4Config.set("CongPhapChuTu", "THAI_AT_HOA_CHON_KINH");
        v4Config.save(v4File);

        assertTrue(v4File.exists());
        assertFalse(bakFile.exists());

        // Kích hoạt Migration qua Reflection
        PlayerDataManager pdm = new PlayerDataManager(null);
        Method migrateMethod = PlayerDataManager.class.getDeclaredMethod("migrateV4ToV5",
                org.bukkit.entity.Player.class, File.class, org.bukkit.configuration.file.FileConfiguration.class);
        migrateMethod.setAccessible(true);
        migrateMethod.invoke(pdm, null, v4File, v4Config);

        // 1. Kiểm tra file backup .bak5 đã được tạo
        assertTrue(bakFile.exists(), "File sao lưu .bak5 phải tồn tại!");
        YamlConfiguration bakConfig = YamlConfiguration.loadConfiguration(bakFile);
        assertEquals(4, bakConfig.getInt("DataVersion"), "Backup phải giữ nguyên DataVersion: 4");
        assertEquals(15420, bakConfig.getInt("TuVi"));

        // 2. Kiểm tra file chính đã lên DataVersion: 5
        YamlConfiguration v5Config = YamlConfiguration.loadConfiguration(v4File);
        assertEquals(5, v5Config.getInt("DataVersion"), "DataVersion phải là 5 sau migration");

        // 3. Kiểm tra tính toàn vẹn chỉ số người chơi
        assertEquals(15420, v5Config.getInt("TuVi"));
        assertEquals(7, v5Config.getInt("Tang"));
        assertEquals("TRUC_CO", v5Config.getString("CanhGioi"));
        assertEquals(450L, v5Config.getLong("DaoNgoPoints"));
        assertEquals("THAI_AT_HOA_CHON_KINH", v5Config.getString("CongPhapChuTu"));

        // 4. Kiểm tra cấu trúc DanDocData mới
        assertTrue(v5Config.isConfigurationSection("DanDocData"), "Phải có section DanDocData");
        assertEquals(85.5, v5Config.getDouble("DanDocData.Amount"), 0.001);
        assertEquals(0.0, v5Config.getDouble("DanDocData.Resistance.CULTIVATION"), 0.001);
        assertEquals(0.0, v5Config.getDouble("DanDocData.Resistance.RECOVERY"), 0.001);

        // Dọn dẹp
        v4File.delete();
        bakFile.delete();
    }

    @Test
    @DisplayName("Kiểm thử 3: 5 Mốc Ngưỡng Đan Độc (ToxicityStatus) và Hiệu Suất")
    public void testToxicityStatusThresholdsAndPenalties() {
        assertEquals(ToxicityStatus.THANH_KHIET, ToxicityStatus.fromDanDoc(0.0));
        assertEquals(1.00, ToxicityStatus.THANH_KHIET.getCultivationEfficiency(), 0.001);

        assertEquals(ToxicityStatus.THANH_KHIET, ToxicityStatus.fromDanDoc(199.9));
        assertEquals(ToxicityStatus.DOC_NHE, ToxicityStatus.fromDanDoc(200.0));
        assertEquals(0.95, ToxicityStatus.DOC_NHE.getCultivationEfficiency(), 0.001);

        assertEquals(ToxicityStatus.DOC_TRUNG, ToxicityStatus.fromDanDoc(400.0));
        assertEquals(0.85, ToxicityStatus.DOC_TRUNG.getCultivationEfficiency(), 0.001);

        assertEquals(ToxicityStatus.DOC_NANG, ToxicityStatus.fromDanDoc(600.0));
        assertEquals(0.70, ToxicityStatus.DOC_NANG.getCultivationEfficiency(), 0.001);

        assertEquals(ToxicityStatus.DOC_PHAT, ToxicityStatus.fromDanDoc(800.0));
        assertEquals(0.50, ToxicityStatus.DOC_PHAT.getCultivationEfficiency(), 0.001);
        assertEquals(ToxicityStatus.DOC_PHAT, ToxicityStatus.fromDanDoc(1000.0));
    }

    @Test
    @DisplayName("Kiểm thử 4: Tích lũy Đan Độc, Giới Hạn Clamp 1000 và Phục Hồi")
    public void testDanDocServiceAccumulationAndClamping() {
        PlayerDataManager pdm = new PlayerDataManager(null);
        DanDocService service = new DanDocService(pdm, null);
        UUID uuid = UUID.randomUUID();

        // Ban đầu = 0.0
        assertEquals(0.0, service.getDanDoc(uuid), 0.001);
        assertEquals(ToxicityStatus.THANH_KHIET, service.getStatus(uuid));

        // D_new = min(1000, 0 + 250) = 250 (Độc Nhẹ)
        service.addDanDoc(uuid, 250.0);
        assertEquals(250.0, service.getDanDoc(uuid), 0.001);
        assertEquals(ToxicityStatus.DOC_NHE, service.getStatus(uuid));

        // Cộng thêm 900 -> 1150 -> bị clamp tại 1000.0 (Độc Phát)
        service.addDanDoc(uuid, 900.0);
        assertEquals(1000.0, service.getDanDoc(uuid), 0.001);
        assertEquals(ToxicityStatus.DOC_PHAT, service.getStatus(uuid));

        // Thanh lọc giảm 850.0 -> còn 150.0 (Thanh Khiết)
        service.reduceDanDoc(uuid, 850.0);
        assertEquals(150.0, service.getDanDoc(uuid), 0.001);
        assertEquals(ToxicityStatus.THANH_KHIET, service.getStatus(uuid));

        // Thanh lọc quá mức -> không âm (min 0.0)
        service.reduceDanDoc(uuid, 500.0);
        assertEquals(0.0, service.getDanDoc(uuid), 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 5: Công thức Kháng Thuốc M_R = max(M_min, 1 - (R/100)*C) và Cách Ly Nhóm")
    public void testDrugResistanceFormulaAndGroupIsolation() {
        PlayerDataManager pdm = new PlayerDataManager(null);
        DrugResistanceService service = new DrugResistanceService(pdm, null);
        UUID uuid = UUID.randomUUID();

        // R = 0 -> M_R = 1.0 (100% hiệu quả)
        assertEquals(1.00, service.getEfficiencyMultiplier(uuid, "CULTIVATION"), 0.001);
        assertEquals(1.00, service.getEfficiencyMultiplier(uuid, "RECOVERY"), 0.001);

        // Tăng 50 điểm kháng nhóm CULTIVATION:
        // M_R = 1.0 - (50 / 100) * 0.60 = 1.0 - 0.30 = 0.70 (70%)
        service.addResistance(uuid, "CULTIVATION", 50.0);
        assertEquals(50.0, service.getResistance(uuid, "CULTIVATION"), 0.001);
        assertEquals(0.70, service.getEfficiencyMultiplier(uuid, "CULTIVATION"), 0.001);

        // Tính cách ly nhóm: nhóm RECOVERY và BREAKTHROUGH không hề bị ảnh hưởng!
        assertEquals(0.0, service.getResistance(uuid, "RECOVERY"), 0.001);
        assertEquals(1.00, service.getEfficiencyMultiplier(uuid, "RECOVERY"), 0.001);
        assertEquals(0.0, service.getResistance(uuid, "BREAKTHROUGH"), 0.001);
        assertEquals(1.00, service.getEfficiencyMultiplier(uuid, "BREAKTHROUGH"), 0.001);

        // Đạt 100 điểm kháng CULTIVATION:
        // M_R = 1.0 - (100 / 100) * 0.60 = 0.40 (40%)
        service.addResistance(uuid, "CULTIVATION", 50.0);
        assertEquals(100.0, service.getResistance(uuid, "CULTIVATION"), 0.001);
        assertEquals(0.40, service.getEfficiencyMultiplier(uuid, "CULTIVATION"), 0.001);

        // Suy giảm kháng thuốc khi online
        service.processOnlineDecay(uuid, "CULTIVATION", 10.0);
        assertEquals(90.0, service.getResistance(uuid, "CULTIVATION"), 0.001);
        // M_R = 1.0 - (90 / 100) * 0.60 = 1.0 - 0.54 = 0.46
        assertEquals(0.46, service.getEfficiencyMultiplier(uuid, "CULTIVATION"), 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 6: Tải Catalog 47+ Đan Dược từ file danduoc.yml chuẩn")
    public void testDanDuocCatalogLoadingFromYAML() {
        InputStream stream = getClass().getResourceAsStream("/danduoc.yml");
        assertNotNull(stream, "File /danduoc.yml trong resources phai ton tai!");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        ConfigurationSection sec = config.getConfigurationSection("DanDuoc");
        assertNotNull(sec, "Phải có section DanDuoc");

        int count = sec.getKeys(false).size();
        assertTrue(count >= 47, "Catalog phải có ít nhất 47 loại đan dược, thực tế có: " + count);

        // Kiểm tra các đan tiêu biểu
        assertTrue(sec.contains("TU_KHI_DAN"));
        assertTrue(sec.contains("TRUC_CO_DAN"));
        assertTrue(sec.contains("NGUYEN_ANH_DAN"));
        assertTrue(sec.contains("HOA_THAN_DAN"));
        assertTrue(sec.contains("DO_KIEP_DAN"));

        ConfigurationSection tuKhiSec = sec.getConfigurationSection("TU_KHI_DAN");
        assertNotNull(tuKhiSec);
        assertEquals("GLOWSTONE_DUST", tuKhiSec.getString("Material"));
        assertEquals("PHAM_NHAN", tuKhiSec.getString("CanhGioiMin"));
    }

    @Test
    @DisplayName("Kiểm thử 7: Co giãn hiệu quả dược lực theo hệ số kháng thuốc")
    public void testDanDuocEffectScalingByResistance() {
        DanDuoc pill = new DanDuoc("TEST_PILL");
        DanDuocEffect eff = pill.getEffect();
        eff.setInstantTuVi(1000);
        eff.setBonusDamage(10.0);
        eff.setBonusHealth(50.0);

        // Giả lập 3 mức kháng thuốc: 0% (M_R=1.0), 50% (M_R=0.70), 100% (M_R=0.40)
        double mr1 = 1.0;
        double mr2 = 0.70;
        double mr3 = 0.40;

        assertEquals(1000, (int) Math.round(eff.getInstantTuVi() * mr1));
        assertEquals(10.0, eff.getBonusDamage() * mr1, 0.001);
        assertEquals(50.0, eff.getBonusHealth() * mr1, 0.001);

        assertEquals(700, (int) Math.round(eff.getInstantTuVi() * mr2));
        assertEquals(7.0, eff.getBonusDamage() * mr2, 0.001);
        assertEquals(35.0, eff.getBonusHealth() * mr2, 0.001);

        assertEquals(400, (int) Math.round(eff.getInstantTuVi() * mr3));
        assertEquals(4.0, eff.getBonusDamage() * mr3, 0.001);
        assertEquals(20.0, eff.getBonusHealth() * mr3, 0.001);
    }
}
