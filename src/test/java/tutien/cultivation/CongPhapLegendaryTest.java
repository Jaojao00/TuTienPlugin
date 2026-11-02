package tutien.cultivation;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.core.CanhGioi;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.manager.CongPhapManager;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapGrade;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CongPhapLegendaryTest {

    private CongPhapManager manager;

    @BeforeEach
    public void setup() {
        manager = new CongPhapManager(null);
        InputStream stream = getClass().getClassLoader().getResourceAsStream("congphap.yml");
        assertNotNull(stream, "congphap.yml phải tồn tại trong resources!");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        manager.loadFromConfig(config);
    }

    @Test
    @DisplayName("Kiểm thử 1: Nạp đầy đủ 22 công pháp huyền thoại từ 4 đại tác phẩm")
    public void testAllLegendaryManualsLoaded() {
        assertTrue(manager.getCatalogSize() >= 22, "Phải nạp tối thiểu 22 bộ công pháp huyền thoại! Hiện có: " + manager.getCatalogSize());

        // Phàm Nhân Tu Tiên
        CongPhap tx = manager.getCongPhap("TRUONG_XUAN_CONG");
        assertNotNull(tx);
        assertEquals("PHAM_NHAN", tx.getSource());
        assertEquals(CongPhapGrade.HOANG, tx.getGradeTier());

        CongPhap tnkq = manager.getCongPhap("THANH_NGUYEN_KIEM_QUYET");
        assertNotNull(tnkq);
        assertEquals(CongPhapGrade.DIA, tnkq.getGradeTier());
        assertTrue(tnkq.getElements().contains("KIM"));

        CongPhap ptcm = manager.getCongPhap("PHAN_THANH_CHAN_MA_CONG");
        assertNotNull(ptcm);
        assertEquals(CongPhapGrade.THIEN, ptcm.getGradeTier());

        // Tru Tiên
        CongPhap tchtd = manager.getCongPhap("THAI_CUC_HUYEN_THANH_DAO");
        assertNotNull(tchtd);
        assertEquals("TRU_TIEN", tchtd.getSource());
        assertEquals(CongPhapGrade.DIA, tchtd.getGradeTier());
        assertEquals("KyLanThuHo", tchtd.getRequiredMythicBoss());

        CongPhap tt = manager.getCongPhap("THIEN_THU_NGU_QUYEN");
        assertNotNull(tt);
        assertEquals(CongPhapGrade.TIEN, tt.getGradeTier());

        // Tiên Nghịch
        CongPhap ctq = manager.getCongPhap("CO_THAN_QUYET");
        assertNotNull(ctq);
        assertEquals("TIEN_NGHICH", ctq.getSource());
        assertEquals(CongPhapGrade.THIEN, ctq.getGradeTier());
        assertEquals("CoThanDoTu", ctq.getRequiredMythicBoss());

        CongPhap td = manager.getCongPhap("TAN_DA_QUYET");
        assertNotNull(td);
        assertEquals(CongPhapGrade.TIEN, td.getGradeTier());

        // Đấu Phá Thương Khung
        CongPhap pq = manager.getCongPhap("PHAN_QUYET");
        assertNotNull(pq);
        assertEquals("DAU_PHA", pq.getSource());
        assertEquals(CongPhapGrade.THIEN, pq.getGradeTier());
        assertEquals("DiHoaLinhThu", pq.getRequiredMythicBoss());

        CongPhap bcb = manager.getCongPhap("BAT_CUC_BANG");
        assertNotNull(bcb);
        assertEquals(CongPhapGrade.HUYEN, bcb.getGradeTier());
    }

    @Test
    @DisplayName("Kiểm thử 2: Cấp bậc từ thấp lên cao (Hoàng -> Huyền -> Địa -> Thiên -> Tiên)")
    public void testTierProgression() {
        CongPhap hoang = manager.getCongPhap("CU_KHI_QUYET");
        CongPhap huyen = manager.getCongPhap("THANH_MOC_TRUONG_XUAN");
        CongPhap dia = manager.getCongPhap("DAI_DIEN_QUYET");
        CongPhap thien = manager.getCongPhap("CO_THAN_QUYET");
        CongPhap tien = manager.getCongPhap("TAN_DA_QUYET");

        assertTrue(hoang.getGradeTier().ordinal() < huyen.getGradeTier().ordinal());
        assertTrue(huyen.getGradeTier().ordinal() < dia.getGradeTier().ordinal());
        assertTrue(dia.getGradeTier().ordinal() < thien.getGradeTier().ordinal());
        assertTrue(thien.getGradeTier().ordinal() < tien.getGradeTier().ordinal());

        // Tốc độ hấp thu linh khí tăng dần từ thấp lên cao
        assertTrue(hoang.getAbsorptionRate() < huyen.getAbsorptionRate());
        assertTrue(huyen.getAbsorptionRate() < dia.getAbsorptionRate());
        assertTrue(dia.getAbsorptionRate() < thien.getAbsorptionRate());
        assertTrue(thien.getAbsorptionRate() < tien.getAbsorptionRate());
    }

    @Test
    @DisplayName("Kiểm thử 3: Điều kiện MythicMobs Boss Defeat & Serialization trong PlayerCultivationData")
    public void testPlayerBossPersistence() {
        PlayerCultivationData data = new PlayerCultivationData("THANH_NGUYEN_KIEM_QUYET");
        assertFalse(data.hasDefeatedBoss("CoThanDoTu"));

        data.addDefeatedBoss("CoThanDoTu");
        assertTrue(data.hasDefeatedBoss("CoThanDoTu"));
        assertTrue(data.hasDefeatedBoss("cothandotu")); // Case insensitive

        // Test Map Serialization
        Map<String, Object> map = data.toMap();
        assertTrue(map.containsKey("defeated-bosses"));

        PlayerCultivationData restored = PlayerCultivationData.fromMap(map);
        assertTrue(restored.hasDefeatedBoss("CoThanDoTu"));
        assertEquals("THANH_NGUYEN_KIEM_QUYET", restored.getActivePrimary());
    }
}
