package tutien.cultivation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.social.DaoLuService;
import tutien.thienkiep.ThienKiepDot;
import tutien.thienkiep.ThienKiepService;
import tutien.tongmon.TangKinhCacService;
import tutien.tongmon.TongMonWarManager;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho Phase 6: Thiên Kiếp & Xã Hội
 * Kiểm chứng:
 * 1. Thiên Kiếp đa đợt (3 đợt lôi kiếp: Sơ Kiếp, Tử Lôi, Tâm Ma Hắc Kiếp)
 * 2. Tương tác phòng ngự của Pháp Bảo & Khí Linh Hộ Chủ (Phase 4)
 * 3. Đột phá đại cảnh giới khi sống sót qua cả 3 đợt lôi kiếp
 * 4. Xử lý thất bại khi máu tụt về 0
 * 5. Kết duyên Đạo Lữ Song Tu (Nhân đôi tốc độ tu vi, buff hợp thể chiến đấu +15% sát thương)
 * 6. Tàng Kinh Các Tông Môn cống hiến bí tịch
 * 7. Chiến Tranh Tông Môn tranh đoạt trận nhãn linh mạch.
 */
public class Phase6ThienKiepTest {

    private PlayerDataManager dataManager;
    private ThienKiepService thienKiepService;
    private DaoLuService daoLuService;
    private TangKinhCacService tangKinhCacService;
    private TongMonWarManager tongMonWarManager;

    @BeforeEach
    public void setUp() {
        new File("target/daolu.yml").delete();
        new File("target/tangkinhcac.yml").delete();
        dataManager = new PlayerDataManager(null);
        thienKiepService = new ThienKiepService(null, dataManager);
        daoLuService = new DaoLuService(null);
        tangKinhCacService = new TangKinhCacService(null);
        tongMonWarManager = new TongMonWarManager();
    }

    private Player createMockPlayer(UUID uuid, CanhGioi rank, double initialHp, double maxHp, Location loc) {
        AtomicReference<Double> hpRef = new AtomicReference<>(initialHp);
        World w = (loc != null) ? loc.getWorld() : createMockWorld("world");
        Location finalLoc = (loc != null) ? loc : new Location(w, 0, 70, 0);

        PlayerInventory inv = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getArmorContents")) return new ItemStack[0];
                    if (method.getName().equals("getContents")) return new ItemStack[0];
                    return null;
                }
        );

        Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getName().equals("getName")) return "TuSi_" + uuid.toString().substring(0, 4);
                    if (method.getName().equals("getHealth")) return hpRef.get();
                    if (method.getName().equals("getMaxHealth")) return maxHp;
                    if (method.getName().equals("setHealth")) {
                        hpRef.set((Double) args[0]);
                        return null;
                    }
                    if (method.getName().equals("getLocation")) return finalLoc;
                    if (method.getName().equals("getWorld")) return finalLoc.getWorld();
                    if (method.getName().equals("getInventory")) return inv;
                    if (method.getName().equals("sendMessage")) return null;
                    if (method.getName().equals("equals")) {
                        if (args.length > 0 && args[0] instanceof Player otherP) {
                            return uuid.equals(otherP.getUniqueId());
                        }
                        return false;
                    }
                    if (method.getName().equals("hashCode")) return uuid.hashCode();
                    return null;
                }
        );
        if (dataManager != null && rank != null) {
            dataManager.setCanhGioi(player, rank);
        }
        return player;
    }

    private World createMockWorld(String name) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return name;
                    if (method.getName().equals("equals")) {
                        if (args.length > 0 && args[0] instanceof World otherW) {
                            return name.equals(otherW.getName());
                        }
                        return false;
                    }
                    if (method.getName().equals("hashCode")) return name.hashCode();
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Kiểm thử 1: 3 Đợt Thiên Kiếp tăng tiến tỷ lệ sát thương theo Max HP")
    public void testThienKiepDotProgression() {
        assertEquals(1, ThienKiepDot.DOT_1.getWaveNumber());
        assertEquals(2, ThienKiepDot.DOT_2.getWaveNumber());
        assertEquals(3, ThienKiepDot.DOT_3.getWaveNumber());

        assertTrue(ThienKiepDot.DOT_1.getDamageRatio() < ThienKiepDot.DOT_2.getDamageRatio());
        assertTrue(ThienKiepDot.DOT_2.getDamageRatio() < ThienKiepDot.DOT_3.getDamageRatio());

        assertFalse(ThienKiepDot.DOT_1.isTamMa());
        assertFalse(ThienKiepDot.DOT_2.isTamMa());
        assertTrue(ThienKiepDot.DOT_3.isTamMa(), "Đợt 3 phải là Tâm Ma Khảo Nghiệm");
    }

    @Test
    @DisplayName("Kiểm thử 2: Sống sót qua 3 đợt lôi kiếp -> Đột phá thăng cảnh giới thành công")
    public void testSurviveThreeWavesSuccess() {
        UUID pId = UUID.randomUUID();
        // Tu sĩ có 200 HP dồi dào
        Player p = createMockPlayer(pId, CanhGioi.KIM_DAN, 200.0, 200.0, null);
        dataManager.setCanhGioi(p, CanhGioi.KIM_DAN);

        // Bắt đầu độ kiếp lên Nguyên Anh
        ThienKiepService.ThienKiepSession session = thienKiepService.startTribulation(p, CanhGioi.NGUYEN_ANH);
        assertNotNull(session);
        assertEquals(CanhGioi.NGUYEN_ANH, session.targetRank);

        // Đợt 1: Sơ Kiếp (35% = 70 dmg) -> HP còn 130
        ThienKiepService.WaveResult w1 = thienKiepService.strikeWave(p, 1);
        assertTrue(w1.playerSurvived);
        assertFalse(w1.isFinalWave);
        assertEquals(130.0, p.getHealth(), 0.001);

        // Đợt 2: Tử Lôi (60% = 120 dmg) -> HP còn 10
        ThienKiepService.WaveResult w2 = thienKiepService.strikeWave(p, 2);
        assertTrue(w2.playerSurvived);
        assertFalse(w2.isFinalWave);
        assertEquals(10.0, p.getHealth(), 0.001);

        // Hồi phục một chút sinh lực trước đợt 3
        p.setHealth(180.0);

        // Đợt 3: Tâm Ma Hắc Kiếp (85% = 170 dmg) -> HP còn 10
        ThienKiepService.WaveResult w3 = thienKiepService.strikeWave(p, 3);
        assertTrue(w3.playerSurvived);
        assertTrue(w3.isFinalWave);
        assertEquals(10.0, p.getHealth(), 0.001);

        // Cảnh giới đã được thăng hoa lên NGUYEN_ANH!
        assertEquals(CanhGioi.NGUYEN_ANH, dataManager.getCanhGioi(p));
        assertNull(thienKiepService.getSession(pId), "Phiên độ kiếp đã hoàn tất và dọn dẹp");
    }

    @Test
    @DisplayName("Kiểm thử 3: Thất bại khi không đủ sinh lực chống chịu lôi kiếp")
    public void testFailureUnderHeavyDamage() {
        UUID pId = UUID.randomUUID();
        // Tu sĩ chỉ có 30 HP
        Player p = createMockPlayer(pId, CanhGioi.KIM_DAN, 30.0, 100.0, null);
        dataManager.setCanhGioi(p, CanhGioi.KIM_DAN);

        thienKiepService.startTribulation(p, CanhGioi.NGUYEN_ANH);

        // Đợt 1 giáng 35 dmg trong khi chỉ có 30 HP -> Thất bại!
        ThienKiepService.WaveResult w1 = thienKiepService.strikeWave(p, 1);
        assertFalse(w1.playerSurvived);

        // Cảnh giới vẫn giữ nguyên là KIM_DAN
        assertEquals(CanhGioi.KIM_DAN, dataManager.getCanhGioi(p));
        assertNull(thienKiepService.getSession(pId));
    }

    @Test
    @DisplayName("Kiểm thử 4: Kết duyên Đạo Lữ, kiểm tra cấm kết duyên trùng lặp và hủy kết duyên")
    public void testDaoLuBondingLifecycle() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();

        // Kết duyên p1 và p2
        assertTrue(daoLuService.pairDaoLu(p1, p2));
        assertTrue(daoLuService.hasPartner(p1));
        assertTrue(daoLuService.hasPartner(p2));
        assertEquals(p2, daoLuService.getBond(p1).getPartner(p1));
        assertEquals(p1, daoLuService.getBond(p2).getPartner(p2));

        // p3 không thể kết duyên với p1 (đã có đạo lữ)
        assertFalse(daoLuService.pairDaoLu(p3, p1));

        // Hủy kết duyên
        assertTrue(daoLuService.dissolveBond(p1));
        assertFalse(daoLuService.hasPartner(p1));
        assertFalse(daoLuService.hasPartner(p2));
    }

    @Test
    @DisplayName("Kiểm thử 5: Song Tu Tọa Thiền (x2.0 Tu Vi) và Buff Hợp Thể khi ở gần nhau")
    public void testSongTuAndProximityBonuses() {
        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();
        daoLuService.pairDaoLu(p1Id, p2Id);

        World world = createMockWorld("world");
        // p1 và p2 ở cách nhau 5 blocks (< 15 blocks)
        Location loc1 = new Location(world, 10, 70, 10);
        Location loc2 = new Location(world, 14, 70, 10);
        Player p1 = createMockPlayer(p1Id, CanhGioi.TRUC_CO, 100.0, 100.0, loc1);
        Player p2 = createMockPlayer(p2Id, CanhGioi.TRUC_CO, 100.0, 100.0, loc2);

        assertTrue(daoLuService.arePartnersNear(p1, p2));
        assertEquals(2.0, daoLuService.getSongTuTuViMultiplier(p1, p2), 0.001); // x2.0 Tu vi
        assertEquals(1.15, daoLuService.getPartnerCombatBonus(p1, p2), 0.001); // +15% sát thương

        // Di chuyển p2 ra xa 50 blocks
        Location farLoc = new Location(world, 60, 70, 10);
        Player p2Far = createMockPlayer(p2Id, CanhGioi.TRUC_CO, 100.0, 100.0, farLoc);

        assertFalse(daoLuService.arePartnersNear(p1, p2Far));
        assertEquals(1.0, daoLuService.getSongTuTuViMultiplier(p1, p2Far), 0.001);
        assertEquals(1.0, daoLuService.getPartnerCombatBonus(p1, p2Far), 0.001);
    }

    @Test
    @DisplayName("Kiểm thử 6: Tàng Kinh Các Tông Môn cống hiến và tra cứu Công Pháp")
    public void testTangKinhCacLibrary() {
        UUID sectId = UUID.randomUUID();

        assertFalse(tangKinhCacService.containsCongPhap(sectId, "THAI_AT_KIEM_QUYET"));

        // Cống hiến công pháp
        assertTrue(tangKinhCacService.contributeCongPhap(sectId, "THAI_AT_KIEM_QUYET"));
        assertTrue(tangKinhCacService.contributeCongPhap(sectId, "CUU_CHUYEN_HUYEN_CONG"));

        // Kiểm tra tồn tại
        assertTrue(tangKinhCacService.containsCongPhap(sectId, "THAI_AT_KIEM_QUYET"));
        assertTrue(tangKinhCacService.containsCongPhap(sectId, "CUU_CHUYEN_HUYEN_CONG"));

        Set<String> list = tangKinhCacService.getCongPhapList(sectId);
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Kiểm thử 7: Tranh Đoạt Linh Mạch Tông Môn và Xác Định Thắng Cuộc (War Concluded)")
    public void testTongMonWarCaptureProgression() {
        UUID sectA = UUID.randomUUID();
        UUID sectB = UUID.randomUUID();

        TongMonWarManager.SectWar war = tongMonWarManager.declareWar(sectA, sectB);
        assertNotNull(war);
        assertFalse(war.concluded);
        assertEquals(0.0, war.attackerControlPercent, 0.001);

        // Phe tấn công chiếm 40%
        war.addControl(40.0);
        assertEquals(40.0, war.attackerControlPercent, 0.001);
        assertFalse(war.concluded);

        // Phe phòng thủ phản công đẩy lui 15%
        war.defendControl(15.0);
        assertEquals(25.0, war.attackerControlPercent, 0.001);

        // Phe tấn công bộc phát chiếm thêm 80% (vượt mốc 100%) -> Thắng cuộc!
        war.addControl(80.0);
        assertEquals(100.0, war.attackerControlPercent, 0.001);
        assertTrue(war.concluded);
        assertEquals(sectA, war.winnerSect);
    }

    @Test
    @DisplayName("Kiểm thử 8: Xác thực Cảnh Giới Đích và Thứ Tự Đợt Lôi Kiếp (Wave Ordering)")
    public void testThienKiepValidationAndWaveOrdering() {
        UUID playerId = UUID.randomUUID();
        Player p = createMockPlayer(playerId, CanhGioi.LUYEN_KHI, 100.0, 100.0, null);

        // 1. Không thể độ kiếp nhảy cóc (Luyện Khí lên Kim Đan là sai quy tắc)
        ThienKiepService.ThienKiepSession invalidSession = thienKiepService.startTribulation(p, CanhGioi.KIM_DAN);
        assertNull(invalidSession, "Phải từ chối độ kiếp nhảy cóc");

        // 2. Độ kiếp hợp lệ: Luyện Khí -> Trúc Cơ
        ThienKiepService.ThienKiepSession validSession = thienKiepService.startTribulation(p, CanhGioi.TRUC_CO);
        assertNotNull(validSession);
        assertEquals(0, validSession.currentWave);

        // 3. Gọi đợt 2 trước đợt 1 -> Phải bị từ chối
        ThienKiepService.WaveResult outOfOrderWave = thienKiepService.strikeWave(p, 2);
        assertNull(outOfOrderWave, "Phải từ chối đợt lôi kiếp sai thứ tự");

        // 4. Gọi đợt 1 hợp lệ
        ThienKiepService.WaveResult wave1 = thienKiepService.strikeWave(p, 1);
        assertNotNull(wave1);
        assertEquals(1, validSession.currentWave);

        // 5. Gọi lại đợt 1 lần nữa -> Phải từ chối
        ThienKiepService.WaveResult repeatWave1 = thienKiepService.strikeWave(p, 1);
        assertNull(repeatWave1, "Phải từ chối đánh lại đợt cũ");

        // 6. Gọi đợt 2 hợp lệ
        ThienKiepService.WaveResult wave2 = thienKiepService.strikeWave(p, 2);
        assertNotNull(wave2);
        assertEquals(2, validSession.currentWave);
    }

    @Test
    @DisplayName("Kiểm thử 9: Cơ chế Yêu Cầu & Chấp Nhận Kết Duyên Đạo Lữ (Request & Accept Flow)")
    public void testDaoLuRequestAcceptFlow() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Player p1 = createMockPlayer(id1, CanhGioi.TRUC_CO, 100.0, 100.0, null);
        Player p2 = createMockPlayer(id2, CanhGioi.TRUC_CO, 100.0, 100.0, null);

        // Gửi lời mời kết duyên
        boolean requested = daoLuService.requestPair(p1, p2);
        assertTrue(requested);
        assertFalse(daoLuService.hasPartner(id1), "Chưa chính thức kết duyên khi đối phương chưa chấp nhận");

        // Đối phương chấp nhận
        boolean accepted = daoLuService.acceptPair(p2);
        assertTrue(accepted);
        assertTrue(daoLuService.hasPartner(id1));
        assertTrue(daoLuService.hasPartner(id2));

        // Không thể chấp nhận lần 2
        assertFalse(daoLuService.acceptPair(p2));
    }

    @Test
    @DisplayName("Kiểm thử 10: Hủy kết duyên Đạo Lữ (Dissolve Bond)")
    public void testDaoLuDissolveBond() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        daoLuService.pairDaoLu(id1, id2);
        assertTrue(daoLuService.hasPartner(id1));
        assertTrue(daoLuService.hasPartner(id2));

        // Hủy kết duyên
        boolean dissolved = daoLuService.dissolveBond(id1);
        assertTrue(dissolved);
        assertFalse(daoLuService.hasPartner(id1));
        assertFalse(daoLuService.hasPartner(id2));

        // Hủy lại khi không có đạo lữ -> false
        assertFalse(daoLuService.dissolveBond(id1));
    }

    @Test
    @DisplayName("Kiểm thử 11: Chiếm đóng Trận Nhãn Tông Môn Chiến (Tran Nhan Capture)")
    public void testTongMonWarNodeCapture() {
        UUID sectA = UUID.randomUUID();
        UUID sectB = UUID.randomUUID();

        TongMonWarManager.SectWar war = tongMonWarManager.declareWar(sectA, sectB);
        assertNotNull(war);
        assertEquals(0.0, war.attackerControlPercent, 0.001);

        // Chiếm đóng Trận Nhãn 1
        boolean cap1 = war.captureNode("TRAN_NHAN_1", sectA, 100.0);
        assertTrue(cap1);
        assertEquals(sectA, war.nodes.get("TRAN_NHAN_1").controllingSect);
        assertTrue(war.attackerControlPercent > 0.0);

        // Phe thủ đẩy lui
        boolean def1 = war.captureNode("TRAN_NHAN_1", sectB, 100.0);
        assertTrue(def1);
        assertEquals(sectB, war.nodes.get("TRAN_NHAN_1").controllingSect);
    }

    @Test
    @DisplayName("Kiểm thử 12: Tàng Kinh Các Lĩnh Ngộ Công Pháp (Comprehend Sect Technique)")
    public void testTangKinhCacComprehension() {
        UUID sectId = UUID.randomUUID();
        String congPhap = "THAI_AT_HOA_KIEM";

        // Ban đầu chưa có
        assertFalse(tangKinhCacService.containsCongPhap(sectId, congPhap));

        // Cống hiến vào Tàng Kinh Các
        assertTrue(tangKinhCacService.contributeCongPhap(sectId, congPhap));
        assertTrue(tangKinhCacService.containsCongPhap(sectId, congPhap));
        assertTrue(tangKinhCacService.getCongPhapList(sectId).contains(congPhap));

        // Cống hiến trùng lặp -> false
        assertFalse(tangKinhCacService.contributeCongPhap(sectId, congPhap));
    }
}
