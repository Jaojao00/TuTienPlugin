package tutien.cultivation.service;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import tutien.core.PlayerDataManager;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.CongPhapProgress;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dịch vụ quản lý Độ Thành Thục (Mastery) của Công Pháp (Phase 2.3):
 *
 * 4 Cấp bậc thành thục:
 * - Sơ Khởi (0 EXP): Vừa lĩnh hội, kích hoạt tác dụng cơ bản.
 * - Tiểu Thành (1,000 EXP): Giảm 5% tiêu hao linh lực, tăng độ ổn định.
 * - Đại Thành (4,000 EXP): Mở rộng phạm vi và uy lực thần thông.
 * - Viên Mãn (10,000 EXP): Mở khóa nội tại bản nguyên, xuất hiện cơ duyên Đốn Ngộ riêng.
 *
 * Tích lũy qua hoạt động thực chiến, có cơ chế chống farm/spam kỹ năng.
 */
public class MasteryService {

    public enum MasteryTier {
        SO_KHOI("Sơ Khởi", "§7Sơ Khởi", 0, 0.00),
        TIEU_THANH("Tiểu Thành", "§aTiểu Thành", 1000, 0.05),
        DAI_THANH("Đại Thành", "§bĐại Thành", 4000, 0.10),
        VIEN_MAN("Viên Mãn", "§6§lViên Mãn", 10000, 0.15);

        private final String name;
        private final String displayName;
        private final int requiredExp;
        private final double manaCostReduction;

        MasteryTier(String name, String displayName, int requiredExp, double manaCostReduction) {
            this.name = name;
            this.displayName = displayName;
            this.requiredExp = requiredExp;
            this.manaCostReduction = manaCostReduction;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getRequiredExp() {
            return requiredExp;
        }

        public double getManaCostReduction() {
            return manaCostReduction;
        }

        public static MasteryTier fromExp(int exp) {
            if (exp >= VIEN_MAN.requiredExp) return VIEN_MAN;
            if (exp >= DAI_THANH.requiredExp) return DAI_THANH;
            if (exp >= TIEU_THANH.requiredExp) return TIEU_THANH;
            return SO_KHOI;
        }
    }

    public enum ActivitySource {
        MEDITATION(1, 0),         // Tọa thiền
        COMBAT(5, 1000),          // Chiến đấu diệt quái phù hợp (cooldown 1s)
        ABILITY_USE(10, 3000),    // Sử dụng kỹ năng hợp lệ (cooldown 3s)
        CHALLENGE(250, 0);        // Thử thách / đột phá

        private final int baseExp;
        private final long cooldownMs;

        ActivitySource(int baseExp, long cooldownMs) {
            this.baseExp = baseExp;
            this.cooldownMs = cooldownMs;
        }

        public int getBaseExp() {
            return baseExp;
        }

        public long getCooldownMs() {
            return cooldownMs;
        }
    }

    // Chống spam kỹ năng tích MasteryExp
    private final Map<String, Long> antiSpamMap = new ConcurrentHashMap<>();

    /**
     * Lấy cấp độ thành thục hiện tại của công pháp.
     */
    public MasteryTier getMasteryTier(PlayerCultivationData data, String congPhapId) {
        if (data == null || congPhapId == null) return MasteryTier.SO_KHOI;
        CongPhapProgress prog = data.getCongPhapProgress(congPhapId);
        if (prog == null) return MasteryTier.SO_KHOI;
        return MasteryTier.fromExp(prog.getMasteryExp());
    }

    /**
     * Lấy tỷ lệ giảm tiêu hao Linh Lực dựa theo cấp bậc thành thục.
     */
    public double getManaCostReduction(PlayerCultivationData data, String congPhapId) {
        return getMasteryTier(data, congPhapId).getManaCostReduction();
    }

    /**
     * Cộng điểm thành thục qua hoạt động hợp lệ với kiểm tra chống spam.
     */
    public boolean addMasteryExp(Player player, PlayerDataManager dataManager, String congPhapId, ActivitySource source) {
        if (player == null || dataManager == null || congPhapId == null || source == null) return false;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        String spamKey = uuid.toString() + ":" + congPhapId + ":" + source.name();

        if (source.getCooldownMs() > 0) {
            long lastTime = antiSpamMap.getOrDefault(spamKey, 0L);
            if (now - lastTime < source.getCooldownMs()) {
                return false; // Chưa hết cooldown chống spam
            }
        }
        antiSpamMap.put(spamKey, now);

        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        CongPhapProgress prog = data.getOrCreateProgress(congPhapId);

        MasteryTier oldTier = MasteryTier.fromExp(prog.getMasteryExp());
        prog.addMasteryExp(source.getBaseExp());
        MasteryTier newTier = MasteryTier.fromExp(prog.getMasteryExp());

        // Nếu thăng tầng thành thục -> phát hiệu ứng và thông báo
        if (newTier.ordinal() > oldTier.ordinal()) {
            player.sendMessage("§6§l✦ CÔNG PHÁP THĂNG HOA ✦ §fCông pháp §e" + congPhapId
                    + " §fđã đạt tới cảnh giới: " + newTier.getDisplayName() + "§f!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }

        return true;
    }
}
