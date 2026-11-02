package tutien.alchemy.btth.service;

import org.bukkit.entity.Player;
import tutien.alchemy.btth.model.PlayerSoulData;
import tutien.alchemy.btth.model.SoulRealm;
import tutien.core.PlayerDataManager;
import tutien.element.LinhCanData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Linh Hồn Lực và điều kiện tư chất Luyện Dược Sư:
 * - Hỏa linh căn: Điều kiện bắt buộc để điều khiển hỏa diễm.
 * - Mộc linh căn: Thuộc tính phụ trợ tăng tỷ lệ chiết xuất tinh hoa và giảm tiêu hao.
 * - 4 chỉ số linh hồn độc lập (Power, Control, Perception, Capacity).
 */
public class SoulControlService {

    private final PlayerDataManager dataManager;
    private final Map<UUID, PlayerSoulData> soulDataMap = new ConcurrentHashMap<>();

    public SoulControlService(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public PlayerSoulData getSoulData(UUID uuid) {
        return soulDataMap.computeIfAbsent(uuid, k -> {
            // Khởi tạo linh hồn lực ban đầu dựa theo Thần Thức hiện có của tu sĩ
            double thanThuc = dataManager != null ? dataManager.getThanThuc(uuid) : 0.0;
            double basePower = Math.max(50.0, 50.0 + thanThuc * 5.0);
            double baseCap = Math.max(100.0, 100.0 + thanThuc * 10.0);
            return new PlayerSoulData(basePower, 0.70, 0.65, baseCap, baseCap, SoulRealm.PHAM_CANH);
        });
    }

    public PlayerSoulData getSoulData(Player player) {
        if (player == null) return new PlayerSoulData();
        return getSoulData(player.getUniqueId());
    }

    /**
     * Kiểm tra điều kiện tư chất trở thành Luyện Dược Sư:
     * Phải sở hữu Hỏa thuộc tính trong Linh Căn!
     */
    public boolean hasFireAffinity(Player player) {
        if (dataManager == null || player == null) return true;
        LinhCanData lcd = dataManager.getLinhCanData(player);
        if (lcd == null) return false;
        return lcd.hasElement("HOA");
    }

    /**
     * Kiểm tra có Mộc thuộc tính phụ trợ hay không.
     */
    public boolean hasWoodAffinity(Player player) {
        if (dataManager == null || player == null) return false;
        LinhCanData lcd = dataManager.getLinhCanData(player);
        if (lcd == null) return false;
        return lcd.hasElement("MOC");
    }

    /**
     * Tính toán hệ số bonus tinh luyện dựa trên Mộc thuộc tính và độ thuần khiết linh căn.
     */
    public double getWoodBonusMultiplier(Player player) {
        if (!hasWoodAffinity(player)) return 1.0;
        LinhCanData lcd = dataManager.getLinhCanData(player);
        double mocQuality = lcd != null ? lcd.getQuality("MOC") : 10.0;
        // Mộc thuộc tính cho tối đa 25% bonus tinh luyện
        return 1.0 + Math.min(0.25, (mocQuality / 100.0) * 0.25);
    }

    /**
     * Tiêu hao linh hồn lực trong quá trình luyện chế.
     */
    public boolean consumeSoul(Player player, double amount) {
        PlayerSoulData soul = getSoulData(player);
        return soul.consumeSoul(amount);
    }

    /**
     * Tự động hồi phục linh hồn lực theo giây.
     */
    public void recoverSoul(Player player, double amount) {
        PlayerSoulData soul = getSoulData(player);
        soul.recoverSoul(amount);
    }
}
