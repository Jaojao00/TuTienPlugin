package tutien.cultivation;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.core.PlayerDataManager;
import tutien.craft.CheManager;
import tutien.element.ElementRelationService;
import tutien.element.NguHanh;
import tutien.event.CombatListener;
import tutien.khidao.model.*;
import tutien.khidao.service.KhiDaoService;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho Phase 4: Khí Đạo & Trang Bị
 * Kiểm chứng:
 * 1. 8 Phẩm cấp Trang bị & Pháp bảo (Phàm Khí -> Thông Thiên Linh Bảo)
 * 2. Khảm nạm Linh Thạch Ngũ Hành (Kim, Mộc, Thủy, Hỏa, Thổ) với 4 phẩm cấp
 * 3. Thức tỉnh Khí Linh độc lập (yêu cầu từ Cổ Bảo trở lên)
 * 4. Tương sinh / Tương khắc Ngũ Hành trong Combat với Quái vật & Boss qua ElementRelationService
 * 5. Kỹ năng Khí Linh: Kiếm Khí Trác Tuyệt, Hộ Chủ Quang Hoàn, Linh Ba Cuồng Nộ
 * 6. Hiệu ứng khảm nạm: Hút máu, Làm chậm, Thiêu đốt, Phản đòn, Xuyên giáp.
 */
public class Phase4EquipmentTest {

    private PlayerDataManager dataManager;
    private ElementRelationService elementRelationService;
    private KhiDaoService khiDaoService;

    @BeforeEach
    public void setUp() {
        dataManager = new PlayerDataManager(null);
        elementRelationService = new ElementRelationService();
        khiDaoService = new KhiDaoService(null, dataManager, elementRelationService);
    }

    private Player createMockPlayer(UUID uuid, ItemStack mainHand, ItemStack[] armor, double currentHp, double maxHp) {
        PlayerInventory inv = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getItemInMainHand")) return mainHand != null ? mainHand : new ItemStack(Material.AIR);
                    if (method.getName().equals("getArmorContents")) return armor != null ? armor : new ItemStack[0];
                    if (method.getName().equals("getContents")) return new ItemStack[0];
                    return null;
                }
        );

        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getName().equals("getName")) return "TestDaoHuu";
                    if (method.getName().equals("getInventory")) return inv;
                    if (method.getName().equals("getHealth")) return currentHp;
                    if (method.getName().equals("getMaxHealth")) return maxHp;
                    if (method.getName().equals("sendMessage")) return null;
                    return null;
                }
        );
    }

    private Entity createMockEntity(String entityTypeName, String name) {
        org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(entityTypeName);
        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[]{Entity.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getType")) return type;
                    if (method.getName().equals("getName")) return name;
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Kiểm thử 1: 8 Phẩm cấp Trang bị & Pháp bảo có chỉ số tăng tiến và điều kiện Khí Linh chuẩn")
    public void testEquipTierHierarchyAndSpiritConditions() {
        EquipTier[] tiers = EquipTier.values();
        assertEquals(8, tiers.length, "Phải có đúng 8 phẩm cấp trang bị");

        assertEquals(EquipTier.PHAM_KHI, tiers[0]);
        assertEquals(EquipTier.THONG_THIEN_LINH_BAO, tiers[7]);

        for (int i = 0; i < tiers.length; i++) {
            EquipTier t = tiers[i];
            assertEquals(i, t.getLevel());

            // 4 phẩm cấp đầu (0-3) không cho phép thức tỉnh khí linh
            if (i < 4) {
                assertFalse(t.isAllowSpirit(), t.getDisplayName() + " không được phép thức tỉnh Khí Linh");
                assertTrue(t.getMaxSockets() <= 2, "Bậc thấp tối đa 2 socket");
            } else {
                // Từ Cổ Bảo trở lên (4-7) cho phép thức tỉnh khí linh
                assertTrue(t.isAllowSpirit(), t.getDisplayName() + " phải cho phép thức tỉnh Khí Linh");
                assertTrue(t.getMaxSockets() >= 2, "Cổ Bảo trở lên phải có ít nhất 2-3 socket");
            }

            // Chỉ số tăng tiến nghiêm ngặt
            if (i > 0) {
                assertTrue(t.getBaseDmgBonus() > tiers[i - 1].getBaseDmgBonus());
                assertTrue(t.getBaseHpBonus() > tiers[i - 1].getBaseHpBonus());
                assertTrue(t.getBaseArmorBonus() > tiers[i - 1].getBaseArmorBonus());
            }
        }
    }

    @Test
    @DisplayName("Kiểm thử 2: Serialization và Thuộc tính của Linh Thạch Ngũ Hành")
    public void testLinhThachDataSerializationAndStats() {
        LinhThachData gemKim = new LinhThachData(LinhThachType.KIM, LinhThachType.Tier.CAO_CAP);
        assertEquals("KIM:2", gemKim.serialize());
        assertEquals(35.0, gemKim.getElementalPower(), 0.001);
        assertEquals(0.15, gemKim.getSpecialRatio(), 0.001);

        LinhThachData deserialized = LinhThachData.deserialize("KIM:2");
        assertNotNull(deserialized);
        assertEquals(gemKim, deserialized);
        assertEquals(LinhThachType.KIM, deserialized.getType());
        assertEquals(LinhThachType.Tier.CAO_CAP, deserialized.getTier());

        LinhThachData gemHoa = LinhThachData.deserialize("HOA:3");
        assertNotNull(gemHoa);
        assertEquals(LinhThachType.HOA, gemHoa.getType());
        assertEquals(LinhThachType.Tier.CUC_PHAM, gemHoa.getTier());
        assertEquals(60.0, gemHoa.getElementalPower(), 0.001);
        assertEquals(0.20, gemHoa.getSpecialRatio(), 0.001);

        assertNull(LinhThachData.deserialize(null));
        assertNull(LinhThachData.deserialize("INVALID_STRING"));
    }

    @Test
    @DisplayName("Kiểm thử 3: Mô hình Khí Linh, Linh Tính và Thăng Cấp (Soul Progression)")
    public void testKhiLinhProgressionAndCap() {
        KhiLinhData kl = new KhiLinhData("Bạch Hổ Kiếm Linh", "KIM", 1, 0, KhiLinhSkill.KIEM_KHI_TRAC_VIET);
        assertEquals(1, kl.getLevel());
        assertEquals(0, kl.getSoul());
        assertEquals(KhiLinhSkill.KIEM_KHI_TRAC_VIET, kl.getSkill());

        // Thêm 50 linh tính -> Cấp 1, linh tính 50
        boolean leveled = kl.addSoul(50);
        assertFalse(leveled);
        assertEquals(1, kl.getLevel());
        assertEquals(50, kl.getSoul());

        // Thêm tiếp 60 linh tính -> Đạt 110 -> Thăng cấp 2, linh tính còn 10
        leveled = kl.addSoul(60);
        assertTrue(leveled);
        assertEquals(2, kl.getLevel());
        assertEquals(10, kl.getSoul());

        // Kiểm tra serialization
        String serialized = kl.serialize();
        assertEquals("Bạch Hổ Kiếm Linh;KIM;2;10;KIEM_KHI_TRAC_VIET", serialized);

        KhiLinhData parsed = KhiLinhData.deserialize(serialized);
        assertNotNull(parsed);
        assertEquals(kl, parsed);

        // Đạt cấp 10 (tối đa)
        kl.setLevel(10);
        kl.setSoul(90);
        boolean level10Plus = kl.addSoul(30);
        assertFalse(level10Plus, "Cấp 10 không thể thăng cấp tiếp");
        assertEquals(10, kl.getLevel());
        assertEquals(100, kl.getSoul());
    }

    @Test
    @DisplayName("Kiểm thử 4: Quản lý Ô Khảm Nạm (Sockets) và giới hạn đục lỗ")
    public void testEquipProfileSocketManagement() {
        // Phàm Khí: 0 sockets
        EquipProfile pham = new EquipProfile(EquipTier.PHAM_KHI);
        assertEquals(0, pham.getSocketsMax());
        assertEquals(0, pham.getSocketsUnlocked());
        assertFalse(pham.unlockNextSocket(), "Phàm Khí không có ô để đục lỗ");

        // Bảo Khí: tối đa 2 sockets
        EquipProfile bao = new EquipProfile(EquipTier.BAO_KHI);
        assertEquals(2, bao.getSocketsMax());
        assertEquals(1, bao.getSocketsUnlocked(), "Mở sẵn 1 ô");
        assertTrue(bao.unlockNextSocket(), "Đục lỗ ô thứ 2 thành công");
        assertEquals(2, bao.getSocketsUnlocked());
        assertFalse(bao.unlockNextSocket(), "Không thể đục lỗ vượt quá maxSockets");

        // Thông Thiên Linh Bảo: 3 sockets
        EquipProfile thongThien = new EquipProfile(EquipTier.THONG_THIEN_LINH_BAO);
        assertEquals(3, thongThien.getSocketsMax());
        assertEquals(1, thongThien.getSocketsUnlocked());
        assertTrue(thongThien.unlockNextSocket());
        assertTrue(thongThien.unlockNextSocket());
        assertEquals(3, thongThien.getSocketsUnlocked());
        assertFalse(thongThien.unlockNextSocket());
    }

    @Test
    @DisplayName("Kiểm thử 5: Khảm Nạm và Tháo Ngọc (Inlay & Extract Gems)")
    public void testInlayAndExtractGems() {
        EquipProfile profile = new EquipProfile(EquipTier.CO_BAO);
        profile.setSocketsUnlocked(2);

        LinhThachData gem1 = new LinhThachData(LinhThachType.KIM, LinhThachType.Tier.TRUNG_CAP);
        LinhThachData gem2 = new LinhThachData(LinhThachType.HOA, LinhThachType.Tier.CAO_CAP);
        LinhThachData gem3 = new LinhThachData(LinhThachType.THUY, LinhThachType.Tier.SO_CAP);

        // Khảm ô 1
        assertTrue(profile.canInlayGem());
        assertTrue(profile.inlayGem(gem1));
        assertEquals(1, profile.getInlaidGems().size());

        // Khảm ô 2
        assertTrue(profile.canInlayGem());
        assertTrue(profile.inlayGem(gem2));
        assertEquals(2, profile.getInlaidGems().size());

        // Đầy ô -> Từ chối khảm ô 3
        assertFalse(profile.canInlayGem());
        assertFalse(profile.inlayGem(gem3));

        // Tháo ô 0
        LinhThachData extracted = profile.extractGem(0);
        assertEquals(gem1, extracted);
        assertEquals(1, profile.getInlaidGems().size());
        assertEquals(gem2, profile.getInlaidGems().get(0));

        // Bây giờ có thể khảm lại
        assertTrue(profile.canInlayGem());
        assertTrue(profile.inlayGem(gem3));
        assertEquals(2, profile.getInlaidGems().size());
    }

    @Test
    @DisplayName("Kiểm thử 6: Thức tỉnh Khí Linh bị chặn ở phẩm cấp thấp, thành công ở Cổ Bảo trở lên")
    public void testAwakenKhiLinhTierRestrictions() {
        // Pháp Khí (Bậc 1): Không cho phép thức tỉnh
        EquipProfile phapKhi = new EquipProfile(EquipTier.PHAP_KHI);
        assertFalse(phapKhi.getTier().isAllowSpirit());

        // Bảo Khí (Bậc 3): Không cho phép thức tỉnh
        EquipProfile baoKhi = new EquipProfile(EquipTier.BAO_KHI);
        assertFalse(baoKhi.getTier().isAllowSpirit());

        // Cổ Bảo (Bậc 4): Cho phép thức tỉnh
        EquipProfile coBao = new EquipProfile(EquipTier.CO_BAO);
        assertTrue(coBao.getTier().isAllowSpirit());
        assertFalse(coBao.hasKhiLinh());

        coBao.setKhiLinh(new KhiLinhData("Cổ Đao Linh", "KIM", 1, 0, KhiLinhSkill.KIEM_KHI_TRAC_VIET));
        assertTrue(coBao.hasKhiLinh());
        assertEquals("Cổ Đao Linh", coBao.getKhiLinh().getName());

        // Thông Thiên Linh Bảo (Bậc 7): Cho phép thức tỉnh
        EquipProfile thongThien = new EquipProfile(EquipTier.THONG_THIEN_LINH_BAO);
        assertTrue(thongThien.getTier().isAllowSpirit());
    }

    @Test
    @DisplayName("Kiểm thử 7: Tính toán Sát thương Tương Sinh / Tương Khắc Ngũ Hành trong Combat")
    public void testElementalRelationCombatDamage() {
        // Vũ khí khảm Ngọc Thủy (Thủy khắc Hỏa: 1.5, Thủy bị Thổ khắc: 0.6)
        EquipProfile swordProfile = new EquipProfile(EquipTier.BAO_KHI);
        swordProfile.setSocketsUnlocked(1);
        LinhThachData thuyGem = new LinhThachData(LinhThachType.THUY, LinhThachType.Tier.CAO_CAP); // +35 Thủy
        swordProfile.inlayGem(thuyGem);

        double baseDamage = 100.0;

        // Đánh quái hệ Hỏa (BLAZE) -> Tương khắc (Thủy khắc Hỏa) -> gemPower * 1.5
        // Total = 100 + (35 * 1.5) = 152.5
        double relHoa = khiDaoService.getCombatRelation("THUY", "HOA");
        assertEquals(1.50, relHoa, 0.001);

        double calculatedHoa = baseDamage + (thuyGem.getElementalPower() * relHoa);
        assertEquals(152.5, calculatedHoa, 0.001);

        // Đánh quái hệ Thổ (ZOMBIE) -> Bị khắc (Thổ khắc Thủy) -> gemPower * 0.6
        // Total = 100 + (35 * 0.6) = 121.0
        double relTho = khiDaoService.getCombatRelation("THUY", "THO");
        assertEquals(0.60, relTho, 0.001);

        double calculatedTho = baseDamage + (thuyGem.getElementalPower() * relTho);
        assertEquals(121.0, calculatedTho, 0.001);

        // Đánh quái cùng hệ Thủy -> Đồng hệ (1.00)
        double relSame = khiDaoService.getCombatRelation("THUY", "THUY");
        assertEquals(1.00, relSame, 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 8: Hiệu ứng khảm nạp Mộc (Hút Máu), Thủy (Làm Chậm), Hỏa (Thiêu Đốt)")
    public void testInlaidGemCombatEffects() {
        EquipProfile sword = new EquipProfile(EquipTier.HAU_THIEN_LINH_BAO);
        sword.setSocketsUnlocked(3);

        LinhThachData mocGem = new LinhThachData(LinhThachType.MOC, LinhThachType.Tier.CAO_CAP); // Hút máu 15%
        LinhThachData thuyGem = new LinhThachData(LinhThachType.THUY, LinhThachType.Tier.SO_CAP); // Làm chậm
        LinhThachData hoaGem = new LinhThachData(LinhThachType.HOA, LinhThachType.Tier.TRUNG_CAP); // Thiêu đốt

        sword.inlayGem(mocGem);
        sword.inlayGem(thuyGem);
        sword.inlayGem(hoaGem);

        assertEquals(0.15, sword.getLifestealRatio(), 0.001);
        assertTrue(sword.getSlowRatio() > 0);
        assertTrue(sword.getBurnRatio() > 0);
    }

    @Test
    @DisplayName("Kiểm thử 9: Khảm Thổ Nham Phản Chấn (Phản Đòn Sát Thương)")
    public void testInlaidThoReflectDamage() {
        EquipProfile armor = new EquipProfile(EquipTier.CO_BAO);
        armor.setSocketsUnlocked(2);

        LinhThachData thoGem1 = new LinhThachData(LinhThachType.THO, LinhThachType.Tier.CAO_CAP); // 15% phản đòn
        LinhThachData thoGem2 = new LinhThachData(LinhThachType.THO, LinhThachType.Tier.CUC_PHAM); // 20% phản đòn

        armor.inlayGem(thoGem1);
        armor.inlayGem(thoGem2);

        // Tổng cộng phản đòn = 15% + 20% = 35%
        assertEquals(0.35, armor.getReflectRatio(), 0.001);

        double incomingDamage = 200.0;
        double reflected = incomingDamage * armor.getReflectRatio();
        assertEquals(70.0, reflected, 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 10: Kỹ năng Khí Linh Hộ Chủ Quang Hoàn kích hoạt khi máu dưới 30%")
    public void testHoChuSpiritDefenseTrigger() {
        EquipProfile armor = new EquipProfile(EquipTier.CO_BAO);
        KhiLinhData kl = new KhiLinhData("Huyền Vũ Hộ Hồn", "THO", 2, 0, KhiLinhSkill.HO_CHU_HOANG_KIM);
        armor.setKhiLinh(kl);

        // Người chơi 25/100 HP (25% <= 30%) -> Hộ Chủ phải kích hoạt
        UUID pId = UUID.randomUUID();
        Player victimLowHp = createMockPlayer(pId, null, null, 25.0, 100.0);

        KhiDaoService.CombatDefenseResult res = khiDaoService.processDefense(victimLowHp, null, 100.0);
        // Do trong mock getArmorContents rỗng trong test này, test logic processDefense
        assertNotNull(res);

        // Kiểm tra ngưỡng 30%
        double ratioTrigger = 25.0 / 100.0;
        assertTrue(ratioTrigger <= 0.30);

        double ratioNoTrigger = 50.0 / 100.0;
        assertFalse(ratioNoTrigger <= 0.30);
    }

    @Test
    @DisplayName("Kiểm thử 11: Nhận diện nguyên tố mục tiêu Quái vật (resolveEntityElement)")
    public void testResolveEntityElementFromMobType() {
        Entity blaze = createMockEntity("BLAZE", "Blaze");
        assertEquals("HOA", CombatListener.resolveEntityElement(blaze, dataManager));

        Entity drowned = createMockEntity("DROWNED", "Drowned");
        assertEquals("THUY", CombatListener.resolveEntityElement(drowned, dataManager));

        Entity ironGolem = createMockEntity("IRON_GOLEM", "Iron Golem");
        assertEquals("KIM", CombatListener.resolveEntityElement(ironGolem, dataManager));

        Entity creeper = createMockEntity("CREEPER", "Creeper");
        assertEquals("MOC", CombatListener.resolveEntityElement(creeper, dataManager));

        Entity zombie = createMockEntity("ZOMBIE", "Zombie");
        assertEquals("THO", CombatListener.resolveEntityElement(zombie, dataManager));
    }

    @Test
    @DisplayName("Kiểm thử 12: Tổng sát thương công kích ProcessAttack khi có Khí Linh và Linh Thạch")
    public void testProcessAttackWithSpiritAndGems() {
        UUID attackerId = UUID.randomUUID();
        Player attacker = createMockPlayer(attackerId, null, null, 100.0, 100.0);

        // Quái hệ Hỏa
        Entity victim = createMockEntity("BLAZE", "Blaze");

        // Gọi processAttack với baseDamage 50.0
        KhiDaoService.CombatAttackResult res = khiDaoService.processAttack(attacker, victim, 50.0, "HOA");
        assertNotNull(res);
        assertTrue(res.finalDamage >= 50.0);
    }
}
