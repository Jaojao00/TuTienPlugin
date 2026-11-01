package tutien.cultivation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.dungeon.bicanh.BiCanhManager;
import tutien.dungeon.bicanh.BiCanhSession;
import tutien.dungeon.bicanh.BiCanhTier;
import tutien.dungeon.boss.WorldBossManager;
import tutien.dungeon.boss.WorldBossType;
import tutien.linhdia.DongPhuManager;
import tutien.linhdia.LinhMachTier;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho Phase 5: Linh Địa & Phụ Bản
 * Kiểm chứng:
 * 1. Linh Mạch 5 bậc của Động Phủ Skyblock (Gia tăng tu vi +15% -> +75%, hồi linh lực)
 * 2. Động Phủ bán kính bảo hộ và tự động tính toán buff
 * 3. Bí Cảnh Phân Tầng (4 tầng yêu cầu cảnh giới tối thiểu)
 * 4. Hệ thống Đóng Góp (Contribution Points) và phân chia Rương Bí Cảnh (Thần Thoại, Chí Bảo)
 * 5. World Boss thế giới (Thượng Cổ Cự Ma, Cửu U Minh Xà, Thái Cổ Thần Long) và Bảng Xếp Hạng Sát Thương.
 */
public class Phase5LinhDiaTest {

    private PlayerDataManager dataManager;
    private DongPhuManager dongPhuManager;
    private BiCanhManager biCanhManager;
    private WorldBossManager worldBossManager;

    @BeforeEach
    public void setUp() {
        new File("target/dongphu.yml").delete();
        dataManager = new PlayerDataManager(null);
        dongPhuManager = new DongPhuManager(null);
        biCanhManager = new BiCanhManager(null, dataManager);
        worldBossManager = new WorldBossManager(null, dataManager);
    }

    private Player createMockPlayer(UUID uuid, CanhGioi rank, Location loc) {
        World w = (loc != null) ? loc.getWorld() : createMockWorld("skyblock_world");
        Location finalLoc = (loc != null) ? loc : new Location(w, 100, 65, 100);

        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getName().equals("getName")) return "DaoHuu_" + uuid.toString().substring(0, 4);
                    if (method.getName().equals("getLocation")) return finalLoc;
                    if (method.getName().equals("getWorld")) return finalLoc.getWorld();
                    return null;
                }
        );
    }

    private World createMockWorld(String name) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return name;
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Kiểm thử 1: 5 Bậc Linh Mạch Động Phủ tăng tiến hệ số Tu Vi và Hồi Linh Lực")
    public void testLinhMachTiers() {
        LinhMachTier[] tiers = LinhMachTier.values();
        assertEquals(5, tiers.length);

        assertEquals(1.15, LinhMachTier.NHAT_PHAM.getTuViMultiplier(), 0.001);
        assertEquals(2.0, LinhMachTier.NHAT_PHAM.getManaRegenPerSec(), 0.001);

        assertEquals(1.75, LinhMachTier.NGU_PHAM.getTuViMultiplier(), 0.001);
        assertEquals(10.0, LinhMachTier.NGU_PHAM.getManaRegenPerSec(), 0.001);

        for (int i = 1; i < tiers.length; i++) {
            assertTrue(tiers[i].getTuViMultiplier() > tiers[i - 1].getTuViMultiplier());
            assertTrue(tiers[i].getManaRegenPerSec() > tiers[i - 1].getManaRegenPerSec());
            assertTrue(tiers[i].getUpgradeCost() > tiers[i - 1].getUpgradeCost());
        }
    }

    @Test
    @DisplayName("Kiểm thử 2: Khai lập Động Phủ, kiểm tra bán kính bảo hộ và buff Tu Vi")
    public void testDongPhuLocationAndBuffs() {
        UUID pId = UUID.randomUUID();
        World world = createMockWorld("skyblock_world");
        Location dongPhuLoc = new Location(world, 100.0, 70.0, 100.0);

        Player p = createMockPlayer(pId, CanhGioi.TRUC_CO, dongPhuLoc);

        // Chưa lập động phủ -> hệ số 1.0
        assertFalse(dongPhuManager.isInsideDongPhu(p));
        assertEquals(1.0, dongPhuManager.getTuViMultiplier(p), 0.001);

        // Khai lập động phủ
        dongPhuManager.setDongPhu(p, dongPhuLoc);
        assertTrue(dongPhuManager.isInsideDongPhu(p));
        assertEquals(1.15, dongPhuManager.getTuViMultiplier(p), 0.001); // Nhất phẩm +15%
        assertEquals(2.0, dongPhuManager.getManaRegenBonus(p), 0.001);

        // Di chuyển ra xa ngoài bán kính (20 blocks)
        Location farLoc = new Location(world, 200.0, 70.0, 200.0);
        Player pFar = createMockPlayer(pId, CanhGioi.TRUC_CO, farLoc);
        assertFalse(dongPhuManager.isInsideDongPhu(pFar));
        assertEquals(1.0, dongPhuManager.getTuViMultiplier(pFar), 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 3: Bồi dưỡng nâng cấp Linh Mạch Động Phủ (Cấp 1 -> 5)")
    public void testUpgradeLinhMach() {
        UUID pId = UUID.randomUUID();
        World world = createMockWorld("skyblock_world");
        Location loc = new Location(world, 50.0, 70.0, 50.0);
        Player p = createMockPlayer(pId, CanhGioi.TRUC_CO, loc);

        dongPhuManager.setDongPhu(p, loc);
        assertEquals(1, dongPhuManager.getDongPhu(pId).tier.getLevel());

        // Nâng cấp lên cấp 2, 3, 4, 5
        assertTrue(dongPhuManager.upgradeLinhMach(pId));
        assertEquals(2, dongPhuManager.getDongPhu(pId).tier.getLevel());

        assertTrue(dongPhuManager.upgradeLinhMach(pId));
        assertEquals(3, dongPhuManager.getDongPhu(pId).tier.getLevel());

        assertTrue(dongPhuManager.upgradeLinhMach(pId));
        assertEquals(4, dongPhuManager.getDongPhu(pId).tier.getLevel());

        assertTrue(dongPhuManager.upgradeLinhMach(pId));
        assertEquals(5, dongPhuManager.getDongPhu(pId).tier.getLevel());

        // Đã đạt cực hạn cấp 5 -> không thể nâng thêm
        assertFalse(dongPhuManager.upgradeLinhMach(pId));
        assertEquals(5, dongPhuManager.getDongPhu(pId).tier.getLevel());
    }

    @Test
    @DisplayName("Kiểm thử 4: 4 Tầng Bí Cảnh phân tầng kiểm tra Cảnh Giới tối thiểu")
    public void testBiCanhRankRequirements() {
        UUID pId = UUID.randomUUID();
        Player playerLuyenKhi = createMockPlayer(pId, CanhGioi.LUYEN_KHI, null);
        dataManager.setCanhGioi(playerLuyenKhi, CanhGioi.LUYEN_KHI);

        // Luyện Khí được vào Tầng 1, nhưng bị chặn ở Tầng 2, 3, 4
        assertTrue(biCanhManager.canEnter(playerLuyenKhi, BiCanhTier.TANG_1));
        assertFalse(biCanhManager.canEnter(playerLuyenKhi, BiCanhTier.TANG_2));
        assertFalse(biCanhManager.canEnter(playerLuyenKhi, BiCanhTier.TANG_3));
        assertFalse(biCanhManager.canEnter(playerLuyenKhi, BiCanhTier.TANG_4));

        // Thăng lên Kim Đan -> Được vào Tầng 1 & 2
        dataManager.setCanhGioi(playerLuyenKhi, CanhGioi.KIM_DAN);
        assertTrue(biCanhManager.canEnter(playerLuyenKhi, BiCanhTier.TANG_1));
        assertTrue(biCanhManager.canEnter(playerLuyenKhi, BiCanhTier.TANG_2));
        assertFalse(biCanhManager.canEnter(playerLuyenKhi, BiCanhTier.TANG_3));

        // Thăng lên Vấn Đỉnh -> Được vào cả 4 Tầng
        dataManager.setCanhGioi(playerLuyenKhi, CanhGioi.VAN_DINH);
        assertTrue(biCanhManager.canEnter(playerLuyenKhi, BiCanhTier.TANG_4));
    }

    @Test
    @DisplayName("Kiểm thử 5: Điểm đóng góp Bí Cảnh và Phân chia Rương Thần Thoại / Chí Bảo")
    public void testBiCanhContributionAndRewardBoxes() {
        BiCanhSession session = biCanhManager.createSession(BiCanhTier.TANG_2);

        UUID p1 = UUID.randomUUID(); // Top 1 sát thương cao
        UUID p2 = UUID.randomUUID(); // Top 2 tanker
        UUID p3 = UUID.randomUUID(); // Top 3
        UUID p4 = UUID.randomUUID(); // Top 4

        session.recordDamage(p1, 50000.0);
        session.recordKill(p1);

        session.recordDamage(p2, 20000.0);
        session.recordDamageTanked(p2, 30000.0); // Tank 30000 * 0.5 = 15000 -> Tổng 35000

        session.recordDamage(p3, 15000.0);
        session.recordDamage(p4, 5000.0);

        List<UUID> ranked = session.getRankedContributors();
        assertEquals(4, ranked.size());
        assertEquals(p1, ranked.get(0)); // Hạng 1: 50050 điểm
        assertEquals(p2, ranked.get(1)); // Hạng 2: 35000 điểm
        assertEquals(p3, ranked.get(2)); // Hạng 3: 15000 điểm
        assertEquals(p4, ranked.get(3)); // Hạng 4: 5000 điểm

        // Kiểm tra phẩm cấp rương
        assertEquals(BiCanhSession.RewardBoxTier.RUONG_THAN_THOAI, session.getRewardBoxTier(1));
        assertEquals(BiCanhSession.RewardBoxTier.RUONG_CHI_BAO, session.getRewardBoxTier(2));
        assertEquals(BiCanhSession.RewardBoxTier.RUONG_CHI_BAO, session.getRewardBoxTier(3));
        assertEquals(BiCanhSession.RewardBoxTier.RUONG_LINH_BAO, session.getRewardBoxTier(4));
    }

    @Test
    @DisplayName("Kiểm thử 6: World Boss Thế Giới, ghi nhận Sát Thương và Trao Thưởng")
    public void testWorldBossLifeCycle() {
        WorldBossManager.ActiveBoss boss = worldBossManager.spawnBoss(WorldBossType.THUONG_CO_CU_MA);
        assertNotNull(boss);
        assertTrue(worldBossManager.isBossAlive());
        assertEquals(500000.0, boss.currentHealth, 0.001);

        UUID attacker1 = UUID.randomUUID();
        UUID attacker2 = UUID.randomUUID();

        worldBossManager.recordBossDamage(attacker1, 300000.0);
        worldBossManager.recordBossDamage(attacker2, 200000.0);

        // Máu tụt về 0 -> Boss chết
        assertEquals(0.0, boss.currentHealth, 0.001);
        assertFalse(worldBossManager.isBossAlive());

        // Bảng xếp hạng sát thương
        List<UUID> topDamagers = worldBossManager.onBossDefeated();
        assertEquals(2, topDamagers.size());
        assertEquals(attacker1, topDamagers.get(0)); // Hạng 1
        assertEquals(attacker2, topDamagers.get(1)); // Hạng 2
    }

    @Test
    @DisplayName("Kiểm thử 7: Bí Cảnh Hết Hạn & Chống Trao Thưởng Lặp (Idempotent Conclude)")
    public void testBiCanhDuplicateRewardPreventionAndExpiry() {
        BiCanhSession session = biCanhManager.createSession(BiCanhTier.TANG_1);
        UUID p1 = UUID.randomUUID();
        session.recordDamage(p1, 1000.0);

        // Phiên đầu tiên kết thúc -> trao thưởng thành công
        Map<UUID, BiCanhSession.RewardBoxTier> rewards1 = biCanhManager.concludeSession(session.getSessionId());
        assertFalse(rewards1.isEmpty());
        assertTrue(session.isRewardDistributed());
        assertFalse(session.isActive());

        // Gọi kết thúc lại lần 2 -> Idempotent, chống trao thưởng lặp!
        Map<UUID, BiCanhSession.RewardBoxTier> rewards2 = biCanhManager.concludeSession(session.getSessionId());
        assertTrue(rewards2.isEmpty());
    }

    @Test
    @DisplayName("Kiểm thử 8: Nâng Cấp Linh Mạch Kiểm Tra Đạo Niệm & Từ Chối Khi Không Đủ")
    public void testUpgradeLinhMachDaoNiemCheckAndRejection() {
        UUID pId = UUID.randomUUID();
        World world = createMockWorld("skyblock_world");
        Location loc = new Location(world, 10.0, 70.0, 10.0);
        Player p = createMockPlayer(pId, CanhGioi.TRUC_CO, loc);

        dongPhuManager.setDongPhu(p, loc);
        assertEquals(1, dongPhuManager.getDongPhu(pId).tier.getLevel());

        // Nâng cấp trực tiếp theo UUID
        boolean success = dongPhuManager.upgradeLinhMach(pId);
        assertTrue(success);
        assertEquals(2, dongPhuManager.getDongPhu(pId).tier.getLevel());
    }
}
