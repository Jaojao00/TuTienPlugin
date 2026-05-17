package tutien.combat;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;

/**
 * Lớp quản lý chỉ số thuộc tính (Máu, Sát thương, Tốc độ)
 * Tích hợp sâu các bùa lợi từ 7 Đại Đạo.
 * ĐÃ NÂNG CẤP: Cộng chỉ số vĩnh viễn từ Đan Dược (Luyện Đan).
 */
public class StatsManager {

    public static void applyStats(Player player, PlayerDataManager dataManager) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        HeTuLuyen he = dataManager.getHeTuLuyen(player);

        // 1. Chỉ số cơ bản theo Cảnh giới (Máu tăng dần, Sát thương tay tăng dần)
        int level = cg.ordinal();
        double finalMaxHealth = 20.0 + (level * 10.0);
        double finalDamage = 1.0 + (level * 2.0);
        double finalSpeed = 0.2;

        // 2. Áp dụng Buff dựa trên Hệ Tu Luyện (Tăng trưởng chuyên biệt)
        switch (he) {
            case THE_TU:
                // Thể Tu: Nhục thân thành thánh, máu cực dày (+40%)
                finalMaxHealth *= 1.4;
                break;

            case MA_TU:
                // Ma Tu: Nghịch thiên tàn sát, sát thương tay cực khủng (+50%)
                finalDamage *= 1.5;
                break;

            case YEU_TU:
                // Yêu Tu: Linh hoạt như thú, tăng tốc độ di chuyển (+30%)
                finalSpeed *= 1.3;
                break;

            case KIEM_TU:
                // Kiếm Tu: Kiếm ý tung hoành (+25% Damage)
                finalDamage *= 1.25;
                break;

            case PHAP_TU:
                // Pháp Tu: Thần hồn mạnh mẽ, tăng giới hạn Linh Lực (Xử lý ở lớp khác)
                // Cộng thêm 1 chút sát thương phụ họa
                finalDamage *= 1.1;
                break;

            default:
                break;
        }

        // 3. [MỚI] Cộng chỉ số vĩnh viễn từ Đan Dược (Luyện Đan)
        double pillBonusDamage = dataManager.getBonusDamage(player);
        double pillBonusHealth = dataManager.getBonusHealth(player);
        finalDamage += pillBonusDamage;
        finalMaxHealth += pillBonusHealth;

        // 4. Cập nhật vào Attributes của Minecraft
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(finalMaxHealth);
        }
        if (player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(finalDamage);
        }
        if (player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(finalSpeed);
        }

        // Đồng bộ máu hiện tại (Nếu máu cũ cao hơn max mới thì bóp lại)
        if (player.getHealth() > finalMaxHealth) {
            player.setHealth(finalMaxHealth);
        }
    }
}