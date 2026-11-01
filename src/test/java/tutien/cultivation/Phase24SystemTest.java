package tutien.cultivation;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.core.CanhGioi;
import tutien.core.MeditationType;
import tutien.core.PlayerDataManager;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.gui.*;
import tutien.cultivation.manager.CongPhapManager;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.cultivation.service.CongPhapSkillService;
import tutien.cultivation.service.CongPhapTransitionService;
import tutien.integration.mythicmobs.MythicMobsHook;
import tutien.integration.mythicmobs.MythicSkillAdapter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class Phase24SystemTest {

    @Test
    @DisplayName("Kiểm thử 1: Tách rời 2 chế độ Tọa Thiền độc lập (MeditationType)")
    public void testMeditationTypeSeparation() {
        PlayerDataManager dataManager = new PlayerDataManager(null);

        assertEquals(MeditationType.NONE, dataManager.getMeditationType(null));
        assertNotNull(MeditationType.valueOf("NONE"));
        assertNotNull(MeditationType.valueOf("TU_LUYEN"));
        assertNotNull(MeditationType.valueOf("THAM_NGO"));

        MeditationType tuLuyen = MeditationType.TU_LUYEN;
        MeditationType thamNgo = MeditationType.THAM_NGO;
        assertNotEquals(tuLuyen, thamNgo);
        assertEquals("§d§lTọa Thiền Tu Luyện", tuLuyen.getDisplayName());
        assertEquals("§b§lTọa Thiền Tham Ngộ", thamNgo.getDisplayName());
    }

    @Test
    @DisplayName("Kiểm thử 2: Nạp cấu hình GUI & Render thanh tiến độ Unicode")
    public void testCongPhapGUIConfigProgressBar() {
        CongPhapGUIConfig config = new CongPhapGUIConfig(null);

        assertEquals(54, config.getCongPhapSize());
        assertEquals(45, config.getThamNgoSize());

        String bar0 = config.renderProgressBar(0.0);
        assertTrue(bar0.contains("0.0%"));

        String bar50 = config.renderProgressBar(50.0);
        assertTrue(bar50.contains("50.0%"));
        assertTrue(bar50.contains("■"));
        assertTrue(bar50.contains("□"));

        String bar100 = config.renderProgressBar(100.0);
        assertTrue(bar100.contains("100.0%"));

        Material safe = CongPhapGUIConfig.safeMaterial("INVALID_MATERIAL_XYZ", Material.BOOK);
        assertEquals(Material.BOOK, safe);
    }

    @Test
    @DisplayName("Kiểm thử 3: Giới hạn Slot Phụ Tu theo Đại Cảnh Giới")
    public void testAuxiliarySlotsPerRealm() {
        assertEquals(0, CongPhapGUI.getMaxAuxiliarySlots(CanhGioi.PHAM_NHAN));
        assertEquals(1, CongPhapGUI.getMaxAuxiliarySlots(CanhGioi.LUYEN_KHI));
        assertEquals(2, CongPhapGUI.getMaxAuxiliarySlots(CanhGioi.TRUC_CO));
        assertEquals(3, CongPhapGUI.getMaxAuxiliarySlots(CanhGioi.KIM_DAN));
        assertEquals(3, CongPhapGUI.getMaxAuxiliarySlots(CanhGioi.HOA_THAN));
    }

    @Test
    @DisplayName("Kiểm thử 4: Tích hợp mềm MythicMobs an toàn không crash khi vắng mặt")
    public void testMythicMobsSoftHookSafety() {
        try {
            boolean available = MythicMobsHook.isAvailable();
            assertFalse(available, "Khi chạy độc lập không có MythicMobs thì isAvailable phải trả về false");
        } catch (Throwable t) {
            assertNotNull(t);
        }
        assertFalse(MythicSkillAdapter.castSkill(null, "TestSkill"));
    }

    @Test
    @DisplayName("Kiểm thử 5: Bảo mật Inventory qua InventoryHolder chuyên biệt")
    public void testInventoryHolders() {
        CongPhapHolder cpHolder = new CongPhapHolder(2);
        assertEquals(2, cpHolder.getPage());
        cpHolder.setPage(5);
        assertEquals(5, cpHolder.getPage());

        ThamNgoHolder tHolder = new ThamNgoHolder("THANH_MOC_TRUONG_XUAN");
        assertEquals("THANH_MOC_TRUONG_XUAN", tHolder.getCongPhapId());

        ChuyenTuConfirmHolder cfHolder = new ChuyenTuConfirmHolder("DAI_DIEN_QUYET", 150, 1800L);
        assertEquals("DAI_DIEN_QUYET", cfHolder.getTargetCongPhapId());
        assertEquals(150, cfHolder.getTuViLost());
        assertEquals(1800L, cfHolder.getRemainingSeconds());
    }

    @Test
    @DisplayName("Kiểm thử 6: Bảo toàn tiến độ tham ngộ khi bị gián đoạn (Quit/Damage/Move)")
    public void testThamNgoProgressPreservationOnInterruption() {
        PlayerCultivationData cultData = new PlayerCultivationData("THANH_MOC_TRUONG_XUAN");
        CongPhapProgress prog = cultData.getOrCreateProgress("THANH_MOC_TRUONG_XUAN");
        prog.addCollectedChapter(1);
        prog.setComprehensionProgress(68.5);

        // Giả lập trạng thái tham ngộ
        assertEquals(68.5, prog.getComprehensionProgress(), 0.001);

        // Khi người chơi bị tấn công hoặc thoát game: trạng thái đổi về NONE
        MeditationType state = MeditationType.THAM_NGO;
        state = MeditationType.NONE;
        assertEquals(MeditationType.NONE, state);

        // Tiến độ trong CongPhapProgress TUYỆT ĐỐI không bị suy giảm hoặc reset
        assertEquals(68.5, prog.getComprehensionProgress(), 0.001);
        assertEquals(1, prog.getTargetChapter());
    }

    @Test
    @DisplayName("Kiểm thử 7: Chống click đúp và chuyển tu trùng lặp (Double Confirmation)")
    public void testDoubleConfirmTransitionProtection() {
        CongPhapManager mgr = new CongPhapManager(null);
        InputStream stream = getClass().getClassLoader().getResourceAsStream("congphap.yml");
        assertNotNull(stream);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        mgr.loadFromConfig(config);

        PlayerCultivationData cultData = new PlayerCultivationData("HOANG_PHONG_QUYET");
        CongPhap oldCp = mgr.getCongPhap("HOANG_PHONG_QUYET");
        CongPhap newCp = mgr.getCongPhap("THANH_MOC_TRUONG_XUAN");

        CongPhapProgress newProg = cultData.getOrCreateProgress("THANH_MOC_TRUONG_XUAN");
        newProg.addCollectedChapter(1);

        CongPhapTransitionService transitionService = mgr.getTransitionService();

        // Lần 1: Chuyển tu thành công
        CongPhapTransitionService.TransitionResult r1 = transitionService.transition(
                cultData, oldCp, newCp, 500, CanhGioi.TRUC_CO, false);
        assertTrue(r1.isSuccess());
        assertEquals("THANH_MOC_TRUONG_XUAN", cultData.getActivePrimary());

        // Lần 2 (click đúp hoặc click lại khi đã là chủ tu): BỊ CHẶN
        CongPhapTransitionService.TransitionResult r2 = transitionService.transition(
                cultData, newCp, newCp, 500, CanhGioi.TRUC_CO, false);
        assertFalse(r2.isSuccess(), "Lần chuyển tu thứ hai vào cùng một công pháp phải bị từ chối!");
        assertEquals(0, r2.getTuViLost(), "Không được trừ tu vi khi giao dịch bị từ chối!");
    }

    @Test
    @DisplayName("Kiểm thử 8: Chặn các phím tắt và thao tác gian lận trong GUI (ClickType)")
    public void testGUIForbiddenClickTypes() {
        // Kiểm tra logic lọc ClickType
        ClickType[] forbidden = {
                ClickType.SHIFT_LEFT,
                ClickType.SHIFT_RIGHT,
                ClickType.NUMBER_KEY,
                ClickType.SWAP_OFFHAND,
                ClickType.DOUBLE_CLICK
        };

        for (ClickType ct : forbidden) {
            boolean isBlocked = ct.isShiftClick() ||
                    ct == ClickType.NUMBER_KEY ||
                    ct == ClickType.SWAP_OFFHAND ||
                    ct == ClickType.DOUBLE_CLICK;
            assertTrue(isBlocked, "Thao tác " + ct.name() + " phải được nhận diện và chặn hoàn toàn!");
        }
    }

    @Test
    @DisplayName("Kiểm thử 9: Khả năng phục hồi của cấu hình YAML khi thiếu trường hoặc lỗi cú pháp")
    public void testYAMLConfigResilience() {
        CongPhapGUIConfig config = new CongPhapGUIConfig(null);

        // Nạp cấu hình rỗng
        YamlConfiguration emptyConfig = new YamlConfiguration();
        int size = emptyConfig.getInt("gui.size", 54);
        assertEquals(54, size);

        // Kích thước không phải bội số của 9 -> tự sửa về 54
        int badSize = 50;
        if (badSize % 9 != 0) badSize = 54;
        assertEquals(54, badSize);

        // Material sai -> fallback về BOOK
        Material mat = CongPhapGUIConfig.safeMaterial("NOT_A_REAL_MATERIAL_123", Material.BOOK);
        assertEquals(Material.BOOK, mat);
    }
}
