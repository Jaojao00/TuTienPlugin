package tutien.alchemy.btth.service;

import org.bukkit.entity.Player;
import tutien.alchemy.btth.model.AlchemistRank;
import tutien.alchemy.btth.model.BTTHPillQuality;
import tutien.alchemy.btth.model.PlayerSoulData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Phẩm Cấp Luyện Dược Sư và Khảo Hạch Thăng Cấp (Đan Tháp).
 */
public class AlchemistRankService {

    private final SoulControlService soulControlService;
    private final Map<UUID, AlchemistRank> rankMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> expMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> highGradeCraftCountMap = new ConcurrentHashMap<>();

    public AlchemistRankService(SoulControlService soulControlService) {
        this.soulControlService = soulControlService;
    }

    public AlchemistRank getRank(UUID uuid) {
        return rankMap.getOrDefault(uuid, AlchemistRank.HOC_DO);
    }

    public AlchemistRank getRank(Player player) {
        if (player == null) return AlchemistRank.HOC_DO;
        return getRank(player.getUniqueId());
    }

    public void setRank(Player player, AlchemistRank rank) {
        if (player == null || rank == null) return;
        rankMap.put(player.getUniqueId(), rank);
    }

    public long getExp(UUID uuid) {
        return expMap.getOrDefault(uuid, 0L);
    }

    public long getExp(Player player) {
        if (player == null) return 0L;
        return getExp(player.getUniqueId());
    }

    public void addExp(Player player, long amount) {
        if (player == null || amount <= 0) return;
        UUID uuid = player.getUniqueId();
        expMap.put(uuid, getExp(uuid) + amount);
    }

    public int getHighGradeCrafts(Player player) {
        if (player == null) return 0;
        return highGradeCraftCountMap.getOrDefault(player.getUniqueId(), 0);
    }

    public void recordSuccessfulCraft(Player player, int tier, BTTHPillQuality.PillGrade grade) {
        if (player == null) return;
        AlchemistRank currentRank = getRank(player);
        if (tier >= currentRank.getTier() && (grade == BTTHPillQuality.PillGrade.THUONG_PHAM || grade == BTTHPillQuality.PillGrade.CUC_PHAM)) {
            UUID uuid = player.getUniqueId();
            highGradeCraftCountMap.put(uuid, getHighGradeCrafts(player) + 1);
        }
        // Thưởng điểm kinh nghiệm luyện dược
        addExp(player, Math.max(10, tier * 25L));
    }

    /**
     * Kiểm tra 3 điều kiện thăng cấp Luyện Dược Sư:
     * 1. Đạt đủ EXP tích lũy.
     * 2. Luyện thành công đủ số lượng đan dược phẩm chất cao (Thượng/Cực phẩm) của cấp hiện tại.
     * 3. Linh hồn lực đạt chuẩn yêu cầu của cấp tiếp theo.
     */
    public boolean canPromote(Player player) {
        if (player == null) return false;
        AlchemistRank current = getRank(player);
        AlchemistRank next = current.getNextRank();
        if (next == null) return false; // Đã đạt Đế Phẩm

        // 1. Kiểm tra EXP
        if (getExp(player) < next.getExpRequired()) return false;

        // 2. Kiểm tra số lần luyện thành phẩm chất cao
        int crafts = getHighGradeCrafts(player);
        if (crafts < current.getTier() * 2) return false;

        // 3. Kiểm tra Linh Hồn Lực
        if (soulControlService != null) {
            PlayerSoulData soul = soulControlService.getSoulData(player);
            if (soul.getSoulPower() < next.getSoulPowerRequired()) return false;
            if (!soul.getRealm().canCraftTier(next.getMaxPillTier())) return false;
        }

        return true;
    }

    public boolean promote(Player player) {
        if (!canPromote(player)) return false;
        AlchemistRank next = getRank(player).getNextRank();
        if (next != null) {
            setRank(player, next);
            return true;
        }
        return false;
    }
}
