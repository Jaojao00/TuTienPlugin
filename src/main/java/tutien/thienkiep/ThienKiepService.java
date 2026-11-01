package tutien.thienkiep;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.khidao.model.EquipProfile;
import tutien.khidao.model.KhiLinhSkill;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Độ Kiếp Đa Đợt (Phase 6):
 * Lôi kiếp chân thực giáng thế (3 đợt: Sơ Kiếp, Tử Lôi, Tâm Ma Hắc Kiếp).
 * Tương tác với Pháp Bảo phòng ngự và Khí Linh hộ chủ (Phase 4).
 */
public class ThienKiepService {

    public static class ThienKiepSession {
        public UUID playerId;
        public CanhGioi currentRank;
        public CanhGioi targetRank;
        public int currentWave = 0;
        public boolean completed = false;
        public boolean failed = false;

        public ThienKiepSession(UUID playerId, CanhGioi currentRank, CanhGioi targetRank) {
            this.playerId = playerId;
            this.currentRank = currentRank;
            this.targetRank = targetRank;
        }
    }

    public static class WaveResult {
        public int waveNumber;
        public double rawDamage;
        public double mitigatedDamage;
        public boolean hoChuTriggered;
        public boolean playerSurvived;
        public boolean isFinalWave;

        public WaveResult(int waveNumber, double rawDamage, double mitigatedDamage,
                          boolean hoChuTriggered, boolean playerSurvived, boolean isFinalWave) {
            this.waveNumber = waveNumber;
            this.rawDamage = rawDamage;
            this.mitigatedDamage = mitigatedDamage;
            this.hoChuTriggered = hoChuTriggered;
            this.playerSurvived = playerSurvived;
            this.isFinalWave = isFinalWave;
        }
    }

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final Map<UUID, ThienKiepSession> activeSessions = new ConcurrentHashMap<>();

    public ThienKiepService(TuTienPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public ThienKiepSession startTribulation(Player p, CanhGioi targetRank) {
        if (p == null || targetRank == null) return null;
        CanhGioi current = (dataManager != null) ? dataManager.getCanhGioi(p) : CanhGioi.LUYEN_KHI;
        // Kiểm tra breakthrough rank verification: Chỉ được độ kiếp lên tầng kế tiếp theo getCanhGioiTiepTheo()
        if (current.getCanhGioiTiepTheo() != targetRank) {
            p.sendMessage("§c§l[Thiên Kiếp] §fCảnh giới đích không hợp lệ! Chỉ có thể độ kiếp lên tầng kế tiếp.");
            return null;
        }
        ThienKiepSession session = new ThienKiepSession(p.getUniqueId(), current, targetRank);
        activeSessions.put(p.getUniqueId(), session);
        return session;
    }

    public ThienKiepSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }

    /**
     * Thực hiện một đợt lôi kiếp giáng thế
     */
    public WaveResult strikeWave(Player p, int waveNumber) {
        ThienKiepSession session = activeSessions.get(p.getUniqueId());
        if (session == null || session.completed || session.failed) return null;

        // Wave ordering check: Phải đúng thứ tự đợt currentWave + 1
        if (waveNumber != session.currentWave + 1) {
            p.sendMessage("§c§l[Thiên Kiếp] §fThứ tự đợt lôi kiếp không hợp lệ!");
            return null;
        }

        ThienKiepDot dot = switch (waveNumber) {
            case 1 -> ThienKiepDot.DOT_1;
            case 2 -> ThienKiepDot.DOT_2;
            default -> ThienKiepDot.DOT_3;
        };

        session.currentWave = waveNumber;

        double maxHp = p.getMaxHealth();
        double currentHp = p.getHealth();
        double rawDamage = maxHp * dot.getDamageRatio();

        // Kiểm tra phòng ngự pháp bảo & Khí Linh (Phase 4)
        double mitigation = 0.0;
        boolean hoChu = false;

        if (p.getInventory() != null) {
            ItemStack[] armors = p.getInventory().getArmorContents();
            if (armors != null) {
                for (ItemStack armor : armors) {
                    if (armor == null) continue;
                    EquipProfile profile = EquipProfile.fromItemStack(armor);
                    if (profile == null) continue;

                    // Giáp bậc cao giảm sát thương lôi kiếp
                    mitigation += profile.getTier().getLevel() * 0.03; // +3% mỗi bậc

                    // Khí Linh Hộ Chủ kích hoạt
                    if (profile.hasKhiLinh() && profile.getKhiLinh().getSkill() == KhiLinhSkill.HO_CHU_HOANG_KIM) {
                        hoChu = true;
                        mitigation += 0.40; // Hộ chủ giảm 40% sát thương
                    }
                }
            }
        }

        mitigation = Math.min(0.80, mitigation); // Giảm tối đa 80%
        double mitigatedDamage = rawDamage * (1.0 - mitigation);

        double newHp = currentHp - mitigatedDamage;
        boolean survived = newHp > 0.0;

        if (survived) {
            p.setHealth(Math.max(1.0, newHp));
            if (waveNumber >= 3) {
                // Hoàn tất cả 3 đợt lôi kiếp -> Đột phá đại thành công!
                session.completed = true;
                activeSessions.remove(p.getUniqueId());
                if (dataManager != null) {
                    dataManager.setCanhGioi(p, session.targetRank);
                }
            }
        } else {
            // Thất bại
            session.failed = true;
            activeSessions.remove(p.getUniqueId());
            p.setHealth(Math.min(maxHp, 5.0)); // Giữ lại chút tàn mệnh
        }

        return new WaveResult(waveNumber, rawDamage, mitigatedDamage, hoChu, survived, waveNumber >= 3);
    }
}
