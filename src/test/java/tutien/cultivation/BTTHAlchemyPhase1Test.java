package tutien.cultivation;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.alchemy.btth.model.*;
import tutien.alchemy.btth.service.*;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BTTHAlchemyPhase1Test {

    private SoulControlService soulControlService;
    private AlchemistRankService rankService;
    private AlchemyFlameService flameService;
    private AlchemyCauldronService cauldronService;
    private BTTHFormulaManager formulaManager;
    private BTTHProcessController controller;

    @BeforeEach
    public void setup() {
        soulControlService = new SoulControlService(null);
        rankService = new AlchemistRankService(soulControlService);
        flameService = new AlchemyFlameService();
        cauldronService = new AlchemyCauldronService();
        formulaManager = new BTTHFormulaManager();
        controller = new BTTHProcessController(null, soulControlService, rankService, flameService, cauldronService, formulaManager, null);
    }

    private Player createMockPlayer(UUID uuid, String name) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return uuid;
                    if ("getName".equals(method.getName())) return name;
                    return null;
                }
        );
    }

    @Test
    @DisplayName("BTTH 1: Quản lý Linh Hồn Lực và Cảnh Giới Linh Hồn (Phàm Cảnh -> Đế Cảnh)")
    public void testSoulDataAndRealms() {
        UUID uuid = UUID.randomUUID();
        PlayerSoulData soul = soulControlService.getSoulData(uuid);

        assertNotNull(soul);
        assertEquals(SoulRealm.PHAM_CANH, soul.getRealm());
        assertTrue(soul.getSoulPower() >= 50.0);
        assertTrue(soul.getSoulCapacity() >= 100.0);

        // Tiêu hao hồn lực
        assertTrue(soul.consumeSoul(20.0));
        assertEquals(80.0, soul.getCurrentSoul(), 0.01);

        // Không thể tiêu hao vượt quá lượng hiện có
        assertFalse(soul.consumeSoul(150.0));
        assertEquals(80.0, soul.getCurrentSoul(), 0.01);

        // Hồi phục hồn lực
        soul.recoverSoul(50.0);
        assertEquals(100.0, soul.getCurrentSoul(), 0.01); // Capped at capacity

        // Kiểm tra thăng cấp cảnh giới linh hồn theo sức mạnh
        soul.setSoulPower(45000.0);
        assertEquals(SoulRealm.LINH_CANH, soul.getRealm());
        assertTrue(soul.getRealm().canCraftTier(8));

        soul.setSoulPower(120000.0);
        assertEquals(SoulRealm.THIEN_CANH, soul.getRealm());
        assertTrue(soul.getRealm().canCraftTier(9));

        soul.setSoulPower(600000.0);
        assertEquals(SoulRealm.DE_CANH, soul.getRealm());
        assertTrue(soul.getRealm().canCraftTier(10));
    }

    @Test
    @DisplayName("BTTH 2: Phẩm Cấp Luyện Dược Sư và 3 Điều Kiện Thăng Cấp Khảo Hạch")
    public void testAlchemistRankProgression() {
        UUID uuid = UUID.randomUUID();
        assertEquals(AlchemistRank.HOC_DO, rankService.getRank(uuid));

        AlchemistRank next = rankService.getRank(uuid).getNextRank();
        assertEquals(AlchemistRank.NHAT_PHAM, next);
        assertEquals(500, next.getExpRequired());
        assertEquals(100.0, next.getSoulPowerRequired());

        // Kiểm tra chuỗi thăng cấp từ Nhất phẩm đến Cửu phẩm và Đế phẩm
        AlchemistRank cur = AlchemistRank.HOC_DO;
        List<AlchemistRank> chain = new ArrayList<>();
        while (cur != null) {
            chain.add(cur);
            cur = cur.getNextRank();
        }
        assertEquals(11, chain.size());
        assertEquals(AlchemistRank.DE_PHAM, chain.get(chain.size() - 1));
        assertEquals(10, chain.get(chain.size() - 1).getTier());
    }

    @Test
    @DisplayName("BTTH 3: Hệ Thống Hỏa Diễm (Đấu Khí Chi Hỏa & Thú Hỏa)")
    public void testFlamesCatalog() {
        BTTHFlame dauKhi = flameService.getFlame("DAU_KHI_CHI_HOA");
        assertNotNull(dauKhi);
        assertEquals(BTTHFlame.FlameCategory.DAU_KHI, dauKhi.getCategory());
        assertTrue(dauKhi.getHeatControl() >= 0.90);

        BTTHFlame tuTinh = flameService.getFlame("THU_HOA_TU_TINH");
        assertNotNull(tuTinh);
        assertEquals(BTTHFlame.FlameCategory.THU_HOA, tuTinh.getCategory());
        assertTrue(tuTinh.getHeatPower() > dauKhi.getHeatPower());
        assertTrue(tuTinh.getQualityBonus() > 0.10);
    }

    @Test
    @DisplayName("BTTH 4: Dược Đỉnh và Giới Hạn Chịu Nhiệt")
    public void testCauldrons() {
        BTTHCauldron phamDinh = cauldronService.getCauldronPrototype("PHAM_DINH");
        assertNotNull(phamDinh);
        assertEquals(0, phamDinh.getTier());
        assertEquals(180.0, phamDinh.getMaxHeat());

        BTTHCauldron huyenNham = cauldronService.getCauldronPrototype("HUYEN_NHAM_DINH");
        assertNotNull(huyenNham);
        assertEquals(3, huyenNham.getTier());
        assertTrue(huyenNham.getMaxHeat() >= 800.0);

        // Hao mòn độ bền
        phamDinh.reduceDurability(20);
        assertEquals(30, phamDinh.getCurrentDurability());
        assertFalse(phamDinh.isBroken());

        phamDinh.reduceDurability(40);
        assertTrue(phamDinh.isBroken());
    }

    @Test
    @DisplayName("BTTH 5: Nạp Đan Phương Tụ Khí Tán & Trúc Cơ Linh Dịch")
    public void testFormulasCatalog() {
        BTTHFormula trucCo = formulaManager.getFormula("TRUC_CO_LINH_DICH");
        assertNotNull(trucCo);
        assertEquals(2, trucCo.getTier());
        assertEquals(AlchemistRank.NHI_PHAM, trucCo.getRequiredRank());
        assertTrue(trucCo.getIngredients().size() >= 3);
        assertEquals("MOC_HE_MA_HACH_1", trucCo.getIngredients().get(2).getIngredientId());

        BTTHFormula tuKhi = formulaManager.getFormula("TU_KHI_TAN");
        assertNotNull(tuKhi);
        assertEquals(3, tuKhi.getTier());
        assertEquals(AlchemistRank.TAM_PHAM, tuKhi.getRequiredRank());
        assertEquals("MAC_DIEP_LIEN", tuKhi.getIngredients().get(0).getIngredientId());
    }

    @Test
    @DisplayName("BTTH 6: Đánh Giá Phẩm Chất (Hạ, Trung, Thượng, Cực) và Đan Văn")
    public void testResultEvaluator() {
        BTTHFormula formula = formulaManager.getFormula("TU_KHI_TAN");
        BTTHCauldron cauldron = cauldronService.getCauldronPrototype("HUYEN_NHAM_DINH");
        BTTHFlame flame = flameService.getFlame("DAU_KHI_CHI_HOA");

        Player mockPlayer = createMockPlayer(UUID.randomUUID(), "TieuViem");
        BTTHAlchemySession session = new BTTHAlchemySession(mockPlayer, formula, flame, cauldron);
        session.setOverallPurity(0.99);

        BTTHPillQuality quality = BTTHResultEvaluator.evaluate(session, 1.0);
        assertNotNull(quality);
        assertEquals(BTTHPillQuality.PillGrade.CUC_PHAM, quality.getGrade());
        assertEquals(0.0, quality.getDanDoc()); // Cực phẩm không có đan độc
    }
}
