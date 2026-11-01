package tutien.dungeon.bicanh;

import java.util.*;

/**
 * Một phiên vượt Bí Cảnh phân tầng (Phase 5).
 * Theo dõi đóng góp sát thương, đỡ đòn, hạ gục của từng tu sĩ và phân chia phần thưởng theo thứ hạng.
 */
public class BiCanhSession {

    public enum RewardBoxTier {
        RUONG_THAN_THOAI("§e§lRương Thần Thoại", 3.0),
        RUONG_CHI_BAO("§6§lRương Chí Bảo", 2.0),
        RUONG_LINH_BAO("§b§lRương Linh Bảo", 1.2),
        RUONG_PHAM_BAO("§aRương Phàm Bảo", 1.0);

        public final String display;
        public final double bonusMultiplier;

        RewardBoxTier(String display, double bonusMultiplier) {
            this.display = display;
            this.bonusMultiplier = bonusMultiplier;
        }
    }

    private final UUID sessionId;
    private final BiCanhTier tier;
    private final long startedAt;
    private final long maxDurationMs;
    private boolean active = true;
    private boolean rewardDistributed = false;

    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final Map<UUID, Double> damageTanked = new HashMap<>();
    private final Map<UUID, Integer> killCount = new HashMap<>();

    public BiCanhSession(BiCanhTier tier) {
        this(tier, 1800000L); // Mặc định 30 phút
    }

    public BiCanhSession(BiCanhTier tier, long maxDurationMs) {
        this.sessionId = UUID.randomUUID();
        this.tier = (tier != null) ? tier : BiCanhTier.TANG_1;
        this.startedAt = System.currentTimeMillis();
        this.maxDurationMs = maxDurationMs > 0 ? maxDurationMs : 1800000L;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public BiCanhTier getTier() {
        return tier;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startedAt >= maxDurationMs;
    }

    public boolean isRewardDistributed() {
        return rewardDistributed;
    }

    public void setRewardDistributed(boolean rewardDistributed) {
        this.rewardDistributed = rewardDistributed;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public synchronized void recordDamage(UUID pId, double dmg) {
        damageDealt.put(pId, damageDealt.getOrDefault(pId, 0.0) + Math.max(0, dmg));
    }

    public synchronized void recordDamageTanked(UUID pId, double dmg) {
        damageTanked.put(pId, damageTanked.getOrDefault(pId, 0.0) + Math.max(0, dmg));
    }

    public synchronized void recordKill(UUID pId) {
        killCount.put(pId, killCount.getOrDefault(pId, 0) + 1);
    }

    public double getDamage(UUID pId) {
        return damageDealt.getOrDefault(pId, 0.0);
    }

    public double getTanked(UUID pId) {
        return damageTanked.getOrDefault(pId, 0.0);
    }

    public int getKills(UUID pId) {
        return killCount.getOrDefault(pId, 0);
    }

    /**
     * Tính tổng điểm đóng góp (Contribution Score):
     * Điểm = Sát thương gây ra * 1.0 + Đỡ đòn * 0.5 + Số quái hạ * 50.0
     */
    public double getContributionScore(UUID pId) {
        double dmg = getDamage(pId);
        double tank = getTanked(pId);
        int kills = getKills(pId);
        return dmg + (tank * 0.5) + (kills * 50.0);
    }

    /**
     * Bảng xếp hạng đóng góp từ cao xuống thấp
     */
    public List<UUID> getRankedContributors() {
        Set<UUID> allParticipants = new HashSet<>();
        allParticipants.addAll(damageDealt.keySet());
        allParticipants.addAll(damageTanked.keySet());
        allParticipants.addAll(killCount.keySet());

        List<UUID> list = new ArrayList<>(allParticipants);
        list.sort((a, b) -> Double.compare(getContributionScore(b), getContributionScore(a)));
        return list;
    }

    /**
     * Phân định phẩm cấp rương phần thưởng theo thứ hạng đóng góp
     */
    public RewardBoxTier getRewardBoxTier(int rank) {
        if (rank == 1) return RewardBoxTier.RUONG_THAN_THOAI;
        if (rank <= 3) return RewardBoxTier.RUONG_CHI_BAO;
        if (rank <= 6) return RewardBoxTier.RUONG_LINH_BAO;
        return RewardBoxTier.RUONG_PHAM_BAO;
    }
}
