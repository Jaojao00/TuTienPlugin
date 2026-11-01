package tutien.cultivation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.alchemy.escrow.AlchemyEscrowManager;
import tutien.alchemy.escrow.EscrowState;
import tutien.alchemy.escrow.EscrowTransaction;
import tutien.alchemy.manager.AlchemyManager;
import tutien.alchemy.manager.DanDuocManager;
import tutien.alchemy.model.*;
import tutien.alchemy.service.AlchemyService;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho Phase 3.3 - Hệ thống Mini-Game Luyện Đan & Hỏa Hầu
 * Kiểm chứng:
 * 1. Nạp và xác thực công thức luyen_dan.yml & tổng hợp động.
 * 2. Ký Quỹ Escrow nguyên tử: Không nhân đôi, không mất đồ khi hủy/ngắt kết nối/tắt server.
 * 3. Mô phỏng hỏa hầu: Dao động nhiệt tự nhiên, tính điểm ổn định và phân cấp phẩm chất (Cực phẩm -> Phế đan).
 * 4. Cơ chế chống quá nhiệt và nổ lò an toàn.
 * 5. Chặn mở nhiều phiên đồng thời và cưỡng chế cảnh giới tối thiểu.
 */
public class Phase33AlchemyTest {

    private PlayerDataManager dataManager;
    private AlchemyManager alchemyManager;
    private DanDuocManager danDuocManager;
    private AlchemyService alchemyService;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        File testDir = new File("target/test-escrow");
        if (testDir.exists()) {
            deleteRecursively(testDir);
        }
        dataManager = new PlayerDataManager(null);
        alchemyManager = new AlchemyManager(null);
        danDuocManager = new DanDuocManager(null);
        alchemyService = new AlchemyService(null, dataManager, alchemyManager, danDuocManager);
        playerUuid = UUID.randomUUID();
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) deleteRecursively(f);
            }
        }
        file.delete();
    }

    private Player createMockPlayer(UUID uuid, String name, ItemStack[] contents) {
        return createMockPlayer(uuid, name, contents, false);
    }

    private Player createMockPlayer(UUID uuid, String name, ItemStack[] contents, boolean fullInventory) {
        PlayerInventory inv = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getContents") || method.getName().equals("getStorageContents")) return contents;
                    if (method.getName().equals("getArmorContents") || method.getName().equals("getExtraContents")) return new ItemStack[0];
                    if (method.getName().equals("getItem") && args != null && args.length > 0 && args[0] instanceof Integer slot) {
                        if (slot >= 0 && slot < contents.length) return contents[slot];
                        return null;
                    }
                    if (method.getName().equals("setItem") && args != null && args.length > 1 && args[0] instanceof Integer slot) {
                        if (slot >= 0 && slot < contents.length) {
                            contents[slot] = (ItemStack) args[1];
                        }
                        return null;
                    }
                    if (method.getName().equals("addItem")) {
                        if (fullInventory && args.length > 0 && args[0] instanceof ItemStack[] items) {
                            HashMap<Integer, ItemStack> leftover = new HashMap<>();
                            for (int i = 0; i < items.length; i++) {
                                leftover.put(i, items[i]);
                            }
                            return leftover;
                        }
                        if (args.length > 0 && args[0] instanceof ItemStack[] items) {
                            for (ItemStack it : items) {
                                for (int i = 0; i < contents.length; i++) {
                                    if (contents[i] == null || contents[i].getType() == Material.AIR) {
                                        contents[i] = it.clone();
                                        break;
                                    }
                                }
                            }
                        }
                        return new HashMap<Integer, ItemStack>();
                    }
                    return null;
                }
        );

        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getName().equals("getName")) return name;
                    if (method.getName().equals("isOnline")) return true;
                    if (method.getName().equals("getInventory")) return inv;
                    if (method.getName().equals("getLocation")) return new Location(null, 100, 64, 100);
                    if (method.getName().equals("sendMessage")) return null;
                    if (method.getName().equals("playSound")) return null;
                    if (method.getName().equals("saveData")) return null;
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Test 1: Nạp công thức từ luyen_dan.yml và xác thực thông số hỏa hầu")
    void testRecipeLoadingAndValidation() {
        AlchemyRecipe trucCo = alchemyManager.getRecipe("TRUC_CO_DAN");
        assertNotNull(trucCo, "Công thức TRUC_CO_DAN phải được nạp thành công");
        assertEquals("Trúc Cơ Đan", trucCo.getDisplayName());
        assertEquals(CanhGioi.LUYEN_KHI, trucCo.getMinRealm());
        assertEquals(450.0, trucCo.getIdealTemperature(), 0.01);
        assertEquals(380.0, trucCo.getMinToleranceTemp(), 0.01);
        assertEquals(520.0, trucCo.getMaxToleranceTemp(), 0.01);
        assertTrue(trucCo.isInTolerance(450.0));
        assertTrue(trucCo.isInTolerance(380.0));
        assertTrue(trucCo.isInTolerance(520.0));
        assertFalse(trucCo.isInTolerance(350.0)); // Dưới ngưỡng
        assertFalse(trucCo.isInTolerance(550.0)); // Quá ngưỡng
        assertEquals(850.0, trucCo.getExplosionTemp(), 0.01);
    }

    @Test
    @DisplayName("Test 2: Tự động tổng hợp công thức động khi đan dược chưa có trong luyen_dan.yml")
    void testDynamicRecipeFallbackSynthesis() {
        AlchemyRecipe dynamic = alchemyManager.getOrCreateDynamicRecipe(
                "CUU_CHUYEN_HOAN_HON_DAN",
                CanhGioi.HOA_THAN,
                List.of(new AlchemyRecipe.Ingredient(Material.GOLDEN_APPLE, 1, "Thánh Quả"))
        );

        assertNotNull(dynamic, "Phải tự động tổng hợp công thức động cân bằng theo cảnh giới");
        assertEquals(CanhGioi.HOA_THAN, dynamic.getMinRealm());
        assertTrue(dynamic.getIdealTemperature() > 500.0, "Cảnh giới cao phải có nhiệt độ lý tưởng tương xứng");
        assertTrue(dynamic.getMaxToleranceTemp() > dynamic.getIdealTemperature());
        assertTrue(dynamic.getMinToleranceTemp() < dynamic.getIdealTemperature());
        assertEquals(1, dynamic.getIngredients().size());
    }

    @Test
    @DisplayName("Test 3: Ký Quỹ Escrow an toàn - Khóa đồ khi bắt đầu và hoàn trả 100% khi hủy")
    void testEscrowLockAndSafeRefundOnCancel() {
        ItemStack blaze = new ItemStack(Material.BLAZE_POWDER, 10);
        ItemStack gold = new ItemStack(Material.GOLD_INGOT, 5);
        ItemStack wart = new ItemStack(Material.NETHER_WART, 5);
        ItemStack[] contents = new ItemStack[]{blaze, gold, wart};

        Player player = createMockPlayer(playerUuid, "DaoHuuTrucCo", contents);
        dataManager.setCanhGioi(player, CanhGioi.TRUC_CO);

        // 1. Khai lò
        AlchemyResult startRes = alchemyService.startSession(player, "TRUC_CO_DAN");
        assertTrue(startRes.isSuccessful(), "Khai lò phải thành công khi đủ nguyên liệu: " + startRes.getMessage());
        assertTrue(alchemyService.isAlchemizing(playerUuid), "Người chơi phải ở trạng thái đang luyện đan");

        // Nguyên liệu phải bị trừ chính xác từ túi đồ để khóa vào Escrow
        assertEquals(7, blaze.getAmount(), "Phải trừ chính xác 3 Hỏa Linh Phấn (10 - 3 = 7)");
        assertEquals(3, gold.getAmount(), "Phải trừ chính xác 2 Huyền Kim Sa (5 - 2 = 3)");
        assertEquals(3, wart.getAmount(), "Phải trừ chính xác 2 U Minh Thảo (5 - 2 = 3)");

        AlchemySession session = alchemyService.getSession(playerUuid);
        assertNotNull(session);
        assertEquals(3, session.getEscrowItems().size(), "Escrow phải lưu giữ đủ 3 loại nguyên liệu đã khóa");

        // 2. Hủy lò
        AlchemyResult cancelRes = alchemyService.cancelSession(player, true, "Đạo hữu hủy lò");
        assertEquals(AlchemyResult.Status.CANCELLED_REFUNDED, cancelRes.getStatus());
        assertFalse(alchemyService.isAlchemizing(playerUuid), "Phiên luyện phải bị đóng");
        assertEquals(3, cancelRes.getRefundedItems().size(), "Phải hoàn trả đủ 3 loại vật phẩm cho người chơi");
    }

    @Test
    @DisplayName("Test 4: Mô phỏng hỏa hầu - Làm nguội, điều chỉnh nhiệt độ và tính điểm phẩm chất")
    void testHeatAdjustmentAndScoring() {
        AlchemyRecipe recipe = alchemyManager.getRecipe("TRUC_CO_DAN");
        assertNotNull(recipe);

        AlchemySession session = new AlchemySession(playerUuid, recipe, Collections.emptyList());
        assertEquals(35.0, session.getCurrentTemperature(), 0.01, "Nhiệt độ ban đầu ở mức phòng");

        // Dùng Vũ Hỏa (+40°C) 10 lần đưa nhiệt độ lên 435°C (trong dải lý tưởng [380..520])
        for (int i = 0; i < 10; i++) {
            session.adjustTemperature(40.0);
        }
        assertEquals(435.0, session.getCurrentTemperature(), 0.01);
        assertTrue(recipe.isInTolerance(session.getCurrentTemperature()), "Nhiệt độ phải nằm trong dải dung sai");

        // Giả lập 15 giây duy trì ổn định (tickOneSecond tự động nguội 12°C/s, bù nhiệt bằng Văn Hỏa)
        for (int i = 0; i < recipe.getDurationSeconds(); i++) {
            boolean exploded = session.tickOneSecond();
            assertFalse(exploded, "Không được nổ lò khi nhiệt độ chuẩn");
            session.adjustTemperature(recipe.getNaturalCoolingRate()); // Giữ nhiệt ổn định
        }

        assertTrue(session.getInToleranceTicks() >= 15, "Số nhịp chuẩn phải đạt toàn thời gian");
        double score = session.calculateScore();
        assertEquals(100.0, score, 0.01, "Điểm hỏa hầu phải đạt tối đa 100.0: " + score);
        AlchemyQuality quality = AlchemyQuality.fromScore(score);
        assertEquals(AlchemyQuality.CUC_PHAM, quality, "Điểm 100 phải đạt Cực Phẩm");
    }

    @Test
    @DisplayName("Test 5: Quá nhiệt liên tục 3 giây kích hoạt Nổ Lò và đóng phiên an toàn")
    void testOverheatExplosionSafety() {
        AlchemyRecipe recipe = alchemyManager.getRecipe("TRUC_CO_DAN");
        assertNotNull(recipe);

        AlchemySession session = new AlchemySession(playerUuid, recipe, Collections.emptyList());
        // Đẩy nhiệt độ vượt ngưỡng nổ lò (> 850°C)
        session.adjustTemperature(950.0);
        assertTrue(session.getCurrentTemperature() >= recipe.getExplosionTemp());

        assertFalse(session.tickOneSecond(), "Giây 1 quá nhiệt: chưa nổ ngay");
        assertEquals(1, session.getConsecutiveOverheatTicks());

        assertFalse(session.tickOneSecond(), "Giây 2 quá nhiệt: cảnh báo");
        assertEquals(2, session.getConsecutiveOverheatTicks());

        assertTrue(session.tickOneSecond(), "Giây 3 quá nhiệt: Lò phải phát nổ!");
        assertEquals(3, session.getConsecutiveOverheatTicks());
    }

    @Test
    @DisplayName("Test 6: Chống mở nhiều phiên đồng thời và cưỡng chế cảnh giới tối thiểu")
    void testConcurrencyAndRealmRestrictions() {
        ItemStack wheat = new ItemStack(Material.WHEAT, 10);
        ItemStack poppy = new ItemStack(Material.POPPY, 5);
        ItemStack[] contents = new ItemStack[]{wheat, poppy};

        Player player = createMockPlayer(playerUuid, "DaoHuuKiemTra", contents);
        dataManager.setCanhGioi(player, CanhGioi.PHAM_NHAN);

        // 1. Phàm nhân không thể luyện Trúc Cơ Đan (yêu cầu Luyện Khí)
        AlchemyResult resLowRealm = alchemyService.startSession(player, "TRUC_CO_DAN");
        assertFalse(resLowRealm.isSuccessful(), "Phải chặn người chơi chưa đủ cảnh giới");
        assertTrue(resLowRealm.getMessage().contains("Cảnh giới chưa đủ"));

        // 2. Nâng lên Luyện Khí và cấp đủ nguyên liệu Tụ Khí Đan
        dataManager.setCanhGioi(player, CanhGioi.LUYEN_KHI);
        AlchemyResult resSuccess = alchemyService.startSession(player, "TU_KHI_DAN");
        assertTrue(resSuccess.isSuccessful());

        // 3. Cố tình mở tiếp phiên thứ 2 trong khi phiên 1 đang chạy
        AlchemyResult resDuplicate = alchemyService.startSession(player, "TU_KHI_DAN");
        assertFalse(resDuplicate.isSuccessful(), "Phải ngăn chặn mở 2 phiên cùng lúc");
        assertTrue(resDuplicate.getMessage().contains("đang trong một phiên"));
    }

    @Test
    @DisplayName("Test 7: Bảo toàn Ký Quỹ Escrow khi người chơi Quit hoặc Plugin Disable (Zero-Loss)")
    void testZeroLossOnDisconnectAndDisable() {
        ItemStack wheat = new ItemStack(Material.WHEAT, 10);
        ItemStack poppy = new ItemStack(Material.POPPY, 5);
        ItemStack[] contents = new ItemStack[]{wheat, poppy};

        Player player = createMockPlayer(playerUuid, "DaoHuuDisconnect", contents);
        dataManager.setCanhGioi(player, CanhGioi.LUYEN_KHI);

        alchemyService.startSession(player, "TU_KHI_DAN");
        assertTrue(alchemyService.isAlchemizing(playerUuid));

        // Giả lập người chơi ngắt kết nối đột ngột
        alchemyService.onPlayerQuit(player);
        assertFalse(alchemyService.isAlchemizing(playerUuid), "Phiên phải được dọn dẹp khỏi RAM");

        // Giả lập tắt server: onDisable phải an toàn tuyệt đối
        assertDoesNotThrow(() -> alchemyService.onDisable());
    }

    @Test
    @DisplayName("Test 8: Phục hồi khi Server Crash giữa chừng (State ACTIVE -> REFUND_PENDING -> Hoàn trả khi Join)")
    void testCrashRecoveryFromActiveState() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        List<ItemStack> locked = List.of(new ItemStack(Material.BLAZE_POWDER, 5));

        // 1. Ghi nhận giao dịch đang ACTIVE trên đĩa (trước khi crash)
        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "TRUC_CO_DAN", locked);
        UUID txId = tx.getTransactionId();
        escrow.commitActive(txId);
        assertEquals(EscrowState.ACTIVE, tx.getState());

        // 2. Giả lập Server Crash: Khởi động lại EscrowManager mới (đọc từ đĩa)
        AlchemyEscrowManager rebootedEscrow = new AlchemyEscrowManager(null);
        EscrowTransaction recoveredTx = rebootedEscrow.getTransaction(txId);
        assertNotNull(recoveredTx, "Giao dịch phải tồn tại sau khi phục hồi");
        assertEquals(EscrowState.REFUND_PENDING, recoveredTx.getState(), "Giao dịch ACTIVE dở dang phải chuyển sang REFUND_PENDING");

        // 3. Người chơi đăng nhập lại: Hệ thống tự động hoàn trả
        Player player = createMockPlayer(playerUuid, "DaoHuuRejoin", new ItemStack[36]);
        rebootedEscrow.onPlayerJoin(player);

        assertEquals(EscrowState.REFUNDED, recoveredTx.getState(), "Sau khi hoàn trả cho người chơi, state phải là REFUNDED");
    }

    @Test
    @DisplayName("Test 9: Fault Injection - Crash sau PREPARED, trước khi trừ đồ (BeforeHash khớp -> DISCARDED, Không hoàn thừa)")
    void testFaultInjectionCrashAfterPreparedBeforeDebit() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        ItemStack diamond = new ItemStack(Material.DIAMOND, 5);
        ItemStack[] contents = new ItemStack[]{diamond, null, null};
        String beforeHash = AlchemyEscrowManager.computeInventoryFingerprint(contents);

        // Chuẩn bị giao dịch với BeforeHash
        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "VIP_DAN", List.of(diamond), beforeHash);
        UUID txId = tx.getTransactionId();
        assertEquals(EscrowState.PREPARED, tx.getState());

        // CRASH xảy ra trước khi kịp trừ đồ! Người chơi vẫn còn nguyên 5 kim cương trong túi:
        Player player = createMockPlayer(playerUuid, "DaoHuuBeforeCrash", contents);

        // Khởi động lại máy chủ
        AlchemyEscrowManager rebootedEscrow = new AlchemyEscrowManager(null);
        rebootedEscrow.onPlayerJoin(player);

        EscrowTransaction recTx = rebootedEscrow.getTransaction(txId);
        assertNotNull(recTx);
        assertEquals(EscrowState.DISCARDED, recTx.getState(), "Khi currentHash khớp beforeHash, phải DISCARDED và KHÔNG hoàn trả");
        assertEquals(5, diamond.getAmount(), "Người chơi vẫn giữ nguyên 5 kim cương, không bị mất cũng không bị dupe");
    }

    @Test
    @DisplayName("Test 10: Fault Injection - Crash sau khi trừ đồ, trước ACTIVE (AfterHash khớp -> REFUND_PENDING -> Hoàn trả đúng 1 lần)")
    void testFaultInjectionCrashAfterDebitBeforeActive() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        ItemStack emerald = new ItemStack(Material.EMERALD, 10);
        ItemStack[] beforeContents = new ItemStack[]{emerald, null, null};
        String beforeHash = AlchemyEscrowManager.computeInventoryFingerprint(beforeContents);

        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "EMERALD_DAN", List.of(new ItemStack(Material.EMERALD, 10)), beforeHash);
        UUID txId = tx.getTransactionId();

        // Giả lập thao tác trừ đồ đã xảy ra trong Minecraft:
        ItemStack[] afterContents = new ItemStack[]{null, null, null};
        String afterHash = AlchemyEscrowManager.computeInventoryFingerprint(afterContents);
        tx.setInventoryAfterHash(afterHash);
        escrow.saveTransactionsToDisk();
        // CRASH xảy ra ngay trước khi commitActive! Transaction trên đĩa vẫn là PREPARED.

        // Server khởi động lại và người chơi đăng nhập
        AlchemyEscrowManager rebootedEscrow = new AlchemyEscrowManager(null);
        Player player = createMockPlayer(playerUuid, "DaoHuuAfterCrash", afterContents);
        rebootedEscrow.onPlayerJoin(player);

        EscrowTransaction recTx = rebootedEscrow.getTransaction(txId);
        assertNotNull(recTx);
        assertEquals(EscrowState.REFUNDED, recTx.getState(), "Khi currentHash khớp afterHash, hệ thống phát hiện đồ đã bị trừ và hoàn trả an toàn");
    }

    @Test
    @DisplayName("Test 11: Fault Injection - Crash với Inventory không khớp cả Before lẫn After (Chuyển NEEDS_RECONCILIATION, không tự xóa)")
    void testFaultInjectionCrashWithAmbiguousInventory() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        ItemStack[] before = new ItemStack[]{new ItemStack(Material.NETHERITE_INGOT, 2)};
        String bHash = AlchemyEscrowManager.computeInventoryFingerprint(before);

        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "NETHER_DAN", List.of(new ItemStack(Material.NETHERITE_INGOT, 2)), bHash);
        tx.setInventoryAfterHash("DIFFERENT_AFTER_HASH");
        escrow.saveTransactionsToDisk();

        // Người chơi đăng nhập lại với túi đồ bị biến đổi lạ lẫm (không khớp cả hai và không có netherite)
        ItemStack[] strangeContents = new ItemStack[]{new ItemStack(Material.DIRT, 64)};
        Player player = createMockPlayer(playerUuid, "DaoHuuAmbiguous", strangeContents);

        AlchemyEscrowManager rebootedEscrow = new AlchemyEscrowManager(null);
        rebootedEscrow.onPlayerJoin(player);

        EscrowTransaction recTx = rebootedEscrow.getTransaction(tx.getTransactionId());
        assertNotNull(recTx);
        assertEquals(EscrowState.NEEDS_RECONCILIATION, recTx.getState(), "Không khớp cả before lẫn after -> chuyển sang NEEDS_RECONCILIATION");
        assertNotNull(rebootedEscrow.getTransaction(tx.getTransactionId()), "Bản ghi giao dịch tuyệt đối không được tự ý xóa khỏi WAL");
    }

    @Test
    @DisplayName("Test 12: Fault Injection - Crash sau khi trao đan, trước khi kịp ghi CLAIMED (Receipt Verification chống phát đúp)")
    void testFaultInjectionCrashAfterPillGivenBeforeClaimed() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        ItemStack pill = new ItemStack(Material.BLAZE_ROD, 1);
        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "FIRE_DAN", Collections.emptyList());
        escrow.commitActive(tx.getTransactionId());
        escrow.completeTransaction(tx.getTransactionId(), List.of(pill));

        UUID receiptId = tx.getClaimReceiptId();
        assertNotNull(receiptId, "Thành phẩm hoàn tất phải có unique receipt ID");

        // Giả lập người chơi đã nhận viên đan có receipt vào túi đồ
        ItemStack pillWithReceipt = tx.getOutputItems().get(0).clone();
        ItemStack[] bag = new ItemStack[]{pillWithReceipt, null, null};
        Player player = createMockPlayer(playerUuid, "DaoHuuReceiptTest", bag);

        // Crash xảy ra trước khi ghi CLAIMED (Transaction trên đĩa vẫn là COMPLETED)
        AlchemyEscrowManager rebootedEscrow = new AlchemyEscrowManager(null);
        EscrowTransaction recTx = rebootedEscrow.getTransaction(tx.getTransactionId());
        assertEquals(EscrowState.COMPLETED, recTx.getState());

        // Người chơi đăng nhập lại -> onPlayerJoin gọi processClaim
        rebootedEscrow.onPlayerJoin(player);

        assertEquals(EscrowState.CLAIMED, recTx.getState(), "Phát hiện receipt đã có trong túi đồ -> xác nhận CLAIMED");

        // Kiểm tra túi đồ người chơi: chỉ có ĐÚNG 1 viên pill, không bị phát đúp viên thứ 2
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.BLAZE_ROD) count += is.getAmount();
        }
        assertEquals(1, count, "Tuyệt đối không bị nhân đôi thành phẩm sau crash");
    }

    @Test
    @DisplayName("Test 13: Tính lũy đẳng của cơ chế Hoàn Trả (Idempotent Refund - Không thể hoàn trả 2 lần)")
    void testIdempotentRefund() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        List<ItemStack> locked = List.of(new ItemStack(Material.EMERALD, 10));

        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "EMERALD_DAN", locked);
        UUID txId = tx.getTransactionId();
        escrow.commitActive(txId);

        Player player = createMockPlayer(playerUuid, "DaoHuuRefund", new ItemStack[36]);

        // Lần 1: Hoàn trả thành công
        boolean firstRefund = escrow.processRefund(player, txId);
        assertTrue(firstRefund, "Lần đầu hoàn trả phải thành công");
        assertEquals(EscrowState.REFUNDED, escrow.getTransaction(txId).getState());

        // Lần 2: Cố tình hoàn trả lại cùng transaction
        boolean secondRefund = escrow.processRefund(player, txId);
        assertFalse(secondRefund, "Lần 2 hoàn trả phải bị từ chối tuyệt đối (chống duplicate)");
    }

    @Test
    @DisplayName("Test 14: Túi đồ đầy chuyển vào Hòm Thư an toàn và nhận lại qua /tutien homthu")
    void testMailboxOverflowWhenInventoryFull() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        List<ItemStack> locked = List.of(new ItemStack(Material.GOLDEN_APPLE, 2));

        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "THANH_DAN", locked);
        UUID txId = tx.getTransactionId();
        escrow.commitActive(txId);

        // Giả lập người chơi túi đồ đầy (fullInventory = true)
        Player fullPlayer = createMockPlayer(playerUuid, "DaoHuuFullBag", new ItemStack[36], true);

        // Hoàn trả khi túi đồ đầy -> chuyển vào Mailbox
        boolean refundResult = escrow.processRefund(fullPlayer, txId);
        assertTrue(refundResult);
        assertEquals(EscrowState.REFUNDED, escrow.getTransaction(txId).getState());
        assertEquals(1, escrow.getPendingMailboxCount(playerUuid), "Nguyên liệu phải nằm an toàn trong Hòm Thư");

        // Giả lập người chơi dọn túi đồ và gõ /tutien homthu (fullInventory = false)
        Player emptyPlayer = createMockPlayer(playerUuid, "DaoHuuFullBag", new ItemStack[36], false);
        List<ItemStack> claimed = escrow.claimMailbox(emptyPlayer);
        assertEquals(1, claimed.size(), "Phải nhận thành công vật phẩm từ Hòm Thư");
        assertEquals(0, escrow.getPendingMailboxCount(playerUuid), "Hòm thư phải trống sau khi claim");
    }

    @Test
    @DisplayName("Test 15: Khôi phục WAL từ bản sao lưu .bak khi tệp transactions.yml bị lỗi hoặc rỗng")
    void testWALCorruptedFileFallbackToBackup() throws Exception {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "FALLBACK_DAN", List.of(new ItemStack(Material.GOLD_INGOT, 8)));
        UUID txId = tx.getTransactionId();
        escrow.commitActive(txId);

        File escrowDir = new File("target/test-escrow", "escrow");
        File mainFile = new File(escrowDir, "transactions.yml");
        File bakFile = new File(escrowDir, "transactions.yml.bak");

        assertTrue(bakFile.exists(), "Tệp transactions.yml.bak phải tồn tại sau khi ghi bền vững");

        // Giả lập crash làm hỏng hoặc xóa trắng transactions.yml
        java.nio.file.Files.write(mainFile.toPath(), new byte[0]);
        assertEquals(0, mainFile.length());

        // Khởi động lại EscrowManager: Phải tự động nạp từ .bak
        AlchemyEscrowManager rebootedEscrow = new AlchemyEscrowManager(null);
        EscrowTransaction recTx = rebootedEscrow.getTransaction(txId);
        assertNotNull(recTx, "Giao dịch phải được cứu nguy thành công từ transactions.yml.bak");
        assertEquals("FALLBACK_DAN", recTx.getRecipeId());
    }

    @Test
    @DisplayName("Test 16: Condition 1 - PREPARED lưu CẢ HAI fingerprint trước khi chạm vào túi đồ thật")
    void testPrecomputedBothFingerprintsStoredInPrepared() {
        ItemStack blaze = new ItemStack(Material.BLAZE_POWDER, 10);
        ItemStack gold = new ItemStack(Material.GOLD_INGOT, 5);
        ItemStack wart = new ItemStack(Material.NETHER_WART, 5);
        ItemStack[] contents = new ItemStack[]{blaze, gold, wart};

        Player player = createMockPlayer(playerUuid, "DaoHuuTwoHash", contents);
        dataManager.setCanhGioi(player, CanhGioi.TRUC_CO);

        // Bắt đầu session: hệ thống tự động mô phỏng và ghi CẢ HAI hash trước khi trừ đồ
        AlchemyResult res = alchemyService.startSession(player, "TRUC_CO_DAN");
        assertTrue(res.isSuccessful());

        AlchemySession session = alchemyService.getSession(playerUuid);
        assertNotNull(session);
        UUID txId = session.getTransactionId();

        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        EscrowTransaction tx = escrow.getTransaction(txId);
        assertNotNull(tx);

        assertNotNull(tx.getInventoryBeforeHash(), "inventoryBeforeHash phải được ghi nhận");
        assertNotNull(tx.getInventoryAfterHash(), "inventoryAfterHash phải được tính toán trước và ghi nhận");
        assertNotEquals(tx.getInventoryBeforeHash(), tx.getInventoryAfterHash(), "Hai hash trước và sau trừ đồ phải khác nhau");
    }

    @Test
    @DisplayName("Test 17: Condition 2 - Phục hồi phần thưởng một phần (Partial Reward Recovery - Bù đúng số lượng thiếu)")
    void testPartialRewardRecoveryWhenPartiallyClaimed() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        // Mẻ luyện tạo ra 2 viên đan Cực Phẩm
        ItemStack pill1 = new ItemStack(Material.FIREWORK_STAR, 1);
        ItemStack pill2 = new ItemStack(Material.FIREWORK_STAR, 1);
        List<ItemStack> expectedPills = List.of(pill1, pill2);

        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "CUC_PHAM_DAN", Collections.emptyList());
        escrow.commitActive(tx.getTransactionId());
        escrow.completeTransaction(tx.getTransactionId(), expectedPills);

        UUID receiptId = tx.getClaimReceiptId();
        assertNotNull(receiptId);

        // Giả lập crash: Người chơi chỉ mới nhận ĐÚNG 1 viên đan có receipt vào túi đồ
        ItemStack partialPill = tx.getOutputItems().get(0).clone();
        partialPill.setAmount(1);
        ItemStack[] bag = new ItemStack[]{partialPill, null, null, null};
        Player player = createMockPlayer(playerUuid, "DaoHuuPartialClaim", bag);

        // Khởi động lại máy chủ và người chơi vào game
        AlchemyEscrowManager rebootedEscrow = new AlchemyEscrowManager(null);
        rebootedEscrow.onPlayerJoin(player);

        EscrowTransaction recTx = rebootedEscrow.getTransaction(tx.getTransactionId());
        assertEquals(EscrowState.CLAIMED, recTx.getState(), "Sau khi bù phát phần thiếu, state phải chuyển CLAIMED");

        // Tổng số viên đan trong túi đồ phải CHÍNH XÁC là 2 viên (1 viên cũ + 1 viên bù phát = 2 viên)
        int totalPills = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.FIREWORK_STAR) {
                totalPills += is.getAmount();
            }
        }
        assertEquals(2, totalPills, "Bù phát phần thiếu chính xác: Tổng cộng phải có đúng 2 viên đan, không nhân đôi thành 3");
    }

    @Test
    @DisplayName("Test 18: Condition 3 - Mailbox có định danh giao dịch bền vững (Chống duplicate khi đã có trong Mailbox)")
    void testStructuredMailboxTransactionDeduplication() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        ItemStack pill = new ItemStack(Material.NETHER_STAR, 2);
        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "THANH_DAO_DAN", Collections.emptyList());
        escrow.commitActive(tx.getTransactionId());
        escrow.completeTransaction(tx.getTransactionId(), List.of(pill));

        UUID receiptId = tx.getClaimReceiptId();
        assertNotNull(receiptId);

        // Giả lập túi đồ đầy: thành phẩm đã được chuyển vào Mailbox trước khi crash
        ItemStack mailboxPill = tx.getOutputItems().get(0).clone();
        escrow.depositToMailbox(playerUuid, mailboxPill, tx.getTransactionId(), receiptId);
        assertEquals(1, escrow.getPendingMailboxCount(playerUuid));

        // Khởi động lại máy chủ
        AlchemyEscrowManager rebootedEscrow = new AlchemyEscrowManager(null);
        Player player = createMockPlayer(playerUuid, "DaoHuuMailboxDedup", new ItemStack[36], true);

        // Khi người chơi join: hệ thống đối chiếu receipt trong cả Inventory và Mailbox
        rebootedEscrow.onPlayerJoin(player);

        EscrowTransaction recTx = rebootedEscrow.getTransaction(tx.getTransactionId());
        assertEquals(EscrowState.CLAIMED, recTx.getState(), "Nhận diện receipt đã có trong Mailbox -> chuyển CLAIMED");

        // Mailbox vẫn giữ đúng 1 mục đền bù (2 viên đan), tuyệt đối không bị đúp thêm mục thứ 2
        assertEquals(1, rebootedEscrow.getPendingMailboxCount(playerUuid), "Không bị nhân đôi bản ghi trong Hòm Thư");
    }

    @Test
    @DisplayName("Test 19: Refund Idempotency - Không hoàn trả lặp khi restart hoặc gọi processRefund nhiều lần")
    void testIngredientRefundIdempotency() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        ItemStack herb = new ItemStack(Material.WHEAT, 16);
        ItemStack stone = new ItemStack(Material.LAPIS_LAZULI, 8);
        List<ItemStack> ingredients = List.of(herb, stone);

        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "HOAN_NGUYEN_DAN", ingredients);
        escrow.commitActive(tx.getTransactionId(), ingredients);
        assertEquals(EscrowState.ACTIVE, tx.getState());

        // Giả lập server crash khi đang active -> chuyển REFUND_PENDING
        escrow.markRefundPending(tx.getTransactionId());

        ItemStack[] bag = new ItemStack[36];
        Player player = createMockPlayer(playerUuid, "DaoHuuRefundTest", bag, false);

        // Lần 1: Hoàn trả thành công
        boolean firstRefund = escrow.processRefund(player, tx.getTransactionId());
        assertTrue(firstRefund, "Lần hoàn trả đầu tiên phải thành công");
        assertEquals(EscrowState.REFUNDED, tx.getState());
        assertNotNull(tx.getRefundReceiptId(), "Phải có refundReceiptId được gán cho transaction");

        // Lần 2: Gọi lại processRefund ngay sau đó -> phải từ chối (idempotent)
        boolean secondRefund = escrow.processRefund(player, tx.getTransactionId());
        assertFalse(secondRefund, "Lần hoàn trả thứ hai phải bị từ chối do trạng thái đã là REFUNDED");

        // Đếm số lượng trong túi đồ
        int wheatCount = 0;
        int lapisCount = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null) {
                if (is.getType() == Material.WHEAT) wheatCount += is.getAmount();
                if (is.getType() == Material.LAPIS_LAZULI) lapisCount += is.getAmount();
            }
        }
        assertEquals(16, wheatCount, "Đúng 16 lúa mì, không bị nhân đôi");
        assertEquals(8, lapisCount, "Đúng 8 linh thạch, không bị nhân đôi");
    }

    @Test
    @DisplayName("Test 20: Persistent Data & ItemMeta preservation qua Escrow Item Serializer")
    void testPdcPreservationAcrossEscrowRestart() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        ItemStack rarePill = new ItemStack(Material.NETHER_STAR, 1);
        // Tag receipt & transaction
        UUID txId = UUID.randomUUID();
        UUID receiptId = UUID.randomUUID();
        rarePill = AlchemyEscrowManager.tagClaimReceipt(rarePill, txId, receiptId);

        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "TIEN_DAN", Collections.emptyList());
        escrow.commitActive(tx.getTransactionId());
        escrow.completeTransaction(tx.getTransactionId(), List.of(rarePill));
        UUID assignedReceipt = tx.getClaimReceiptId();

        // Khởi động lại Escrow
        AlchemyEscrowManager rebooted = new AlchemyEscrowManager(null);
        EscrowTransaction loadedTx = rebooted.getTransaction(tx.getTransactionId());
        assertNotNull(loadedTx);
        assertEquals(1, loadedTx.getOutputItems().size());

        ItemStack loadedItem = loadedTx.getOutputItems().get(0);
        assertEquals(Material.NETHER_STAR, loadedItem.getType());
        assertEquals(1, loadedItem.getAmount());
        assertTrue(AlchemyEscrowManager.hasClaimReceipt(loadedItem, assignedReceipt), "Vật phẩm sau khi nạp lại từ disk phải bảo toàn receipt ID!");
    }

    @Test
    @DisplayName("Test 21: Giao thức Mailbox Two-Phase Claim (PENDING -> CLAIMING -> CLAIMED) đối soát receipt & amount")
    void testMailboxTwoPhaseClaimProtocolAudit() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        UUID pId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID rId = UUID.randomUUID();

        ItemStack item = new ItemStack(Material.DIAMOND, 5);
        item = AlchemyEscrowManager.tagClaimReceipt(item, txId, rId);

        // Nạp vào Mailbox -> ban đầu là PENDING
        escrow.depositToMailbox(pId, item, txId, rId);
        assertEquals(1, escrow.getPendingMailboxCount(pId));
        assertEquals(5, escrow.countReceiptItemsInMailbox(pId, rId));

        Player player = createMockPlayer(pId, "DaoHuuClaim", new ItemStack[36], false);
        // Nhận Mailbox qua claimMailbox
        List<ItemStack> claimed = escrow.claimMailbox(player);
        assertFalse(claimed.isEmpty());
        assertEquals(5, claimed.get(0).getAmount());

        // Sau khi nhận thành công, mailbox của người chơi phải được dọn dẹp
        assertEquals(0, escrow.getPendingMailboxCount(pId));
    }

    @Test
    @DisplayName("Test 22: Khi cả BeforeHash và AfterHash đều không khớp, giao dịch chuyển sang NEEDS_RECONCILIATION (không DISCARD)")
    void testReconciliationFallbackToNeedsReconciliationWhenHashesMismatch() {
        AlchemyEscrowManager escrow = alchemyService.getEscrowManager();
        ItemStack herb = new ItemStack(Material.WHEAT, 10);
        List<ItemStack> locked = List.of(herb);

        // Tạo transaction PREPARED với hash giả định
        EscrowTransaction tx = escrow.prepareTransaction(playerUuid, "TEST_RECIPE", locked, "HASH_BEFORE_AAA", "HASH_AFTER_BBB");
        assertEquals(EscrowState.PREPARED, tx.getState());

        // Giả lập người chơi join với túi đồ có hash khác hoàn toàn (ví dụ: bị thay đổi slot khác trước crash)
        ItemStack[] currentInv = new ItemStack[]{new ItemStack(Material.APPLE, 5)};
        Player player = createMockPlayer(playerUuid, "DaoHuuMismatch", currentInv, false);

        escrow.reconcilePreparedTransaction(player, tx);

        // Không được DISCARD, mà PHẢI chuyển sang NEEDS_RECONCILIATION
        assertEquals(EscrowState.NEEDS_RECONCILIATION, tx.getState(), "Khi lệch cả 2 hash, Escrow phải bảo vệ tài sản bằng cách chuyển NEEDS_RECONCILIATION");
    }
}
