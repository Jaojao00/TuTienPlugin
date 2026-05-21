package tutien.combat;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;

/**
 * Lớp quản lý chỉ số thuộc tính (Máu, Sát thương, Tốc độ).
 * ĐÃ SỬA: Đọc MaxMau/BaseDamage từ CanhGioi (config-driven).
 * ĐÃ ĐỒNG BỘ: % buff khớp chính xác với mô tả trong HeTuLuyen.
 */
public class StatsManager {

    public static void applyStats(Player player, PlayerDataManager dataManager) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        HeTuLuyen he = dataManager.getHeTuLuyen(player);

        // 1. Chỉ số cơ bản đọc TRỰC TIẾP từ CanhGioi enum (data từ canhgioi.yml)
        double finalMaxHealth = cg.getMaxMau();
        double finalDamage    = cg.getBaseDamage();
        double finalSpeed     = 0.2;

        // 2. Buff theo Hệ Tu Luyện — ĐÃ ĐỒNG BỘ 100% với mô tả HeTuLuyen.java
        switch (he) {
            case THE_TU:
                // "+40% Máu tối đa"
                finalMaxHealth *= 1.4;
                break;
            case MA_TU:
                // "+30% Sát thương" (CombatListener xử lý thêm -20% phòng thủ khi nhận đòn)
                finalDamage *= 1.3;
                break;
            case YEU_TU:
                // "+15% Tốc độ di chuyển" (hồi máu × 2 handle trong CombatListener)
                finalSpeed *= 1.15;
                break;
            case KIEM_TU:
                // "+20% Sát thương Kiếm" ở base; CombatListener thêm 20% bonus khi cầm kiếm
                finalDamage *= 1.20;
                break;
            case PHAP_TU:
                // "+15% Sát thương phép"; +30% Linh Lực max handled in TuLuyenTask
                finalDamage *= 1.15;
                break;
            case DAN_TU:
                // Bonus đến từ đan dược tích lũy, không buff base stats
                break;
            case SONG_TU:
                // "+15% Tu Vi khi gần đạo lữ" — handled in TuLuyenTask; nhỏ buff base để cân bằng
                finalMaxHealth *= 1.05;
                finalDamage    *= 1.05;
                break;
            default:
                break;
        }

        // 3. Cộng chỉ số vĩnh viễn từ đan dược (Luyện Đan)
        finalDamage    += dataManager.getBonusDamage(player);
        finalMaxHealth += dataManager.getBonusHealth(player);

        // 4. Áp dụng vào Minecraft Attributes
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(finalMaxHealth);
        }
        if (player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(finalDamage);
        }
        if (player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(finalSpeed);
        }

        // 5. Đồng bộ HP hiện tại không vượt quá max mới
        if (player.getHealth() > finalMaxHealth) {
            player.setHealth(finalMaxHealth);
        }
    }
}