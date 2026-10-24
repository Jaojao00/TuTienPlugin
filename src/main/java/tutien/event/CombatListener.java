package tutien.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import tutien.combat.StatsManager;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;
import tutien.tutien.gui.GachaGUI;

import java.util.Set;

/**
 * Lớp xử lý các hiệu ứng đặc biệt của từng hệ và Linh Thú khi giao chiến.
 * v2.2 Skyblock Edition:
 *   - Đồng bộ % buff của Hệ Tu Luyện (Thể Tu, Ma Tu, Kiếm Tu, Yêu Tu).
 *   - Bổ sung hiệu ứng trợ chiến của Linh Thú: Huyết Phù Quy (-15% sát thương nhận), Thiên Mãu Hổ (+20% sát thương gây ra).
 */
public class CombatListener implements Listener {

    private final PlayerDataManager dataManager;

    public CombatListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {

        // --- XỬ LÝ KHI NGƯỜI CHƠI BỊ TẤN CÔNG ---
        if (event.getEntity() instanceof Player victim) {
            HeTuLuyen heVictim = dataManager.getHeTuLuyen(victim);
            Set<GachaGUI.LinhThu> victimPets = StatsManager.getActivePets(victim);

            double dmgMultiplier = 1.0;

            // THỂ TU: Giảm 10% sát thương nhận vào
            if (heVictim == HeTuLuyen.THE_TU) {
                dmgMultiplier *= 0.90;
            }

            // MA TU: Nhận thêm 20% sát thương (đổi lại +30% công kích)
            if (heVictim == HeTuLuyen.MA_TU) {
                dmgMultiplier *= 1.20;
            }

            // LINH THÚ HUYẾT PHÙ QUY: +15% Phòng thủ (Giảm 15% sát thương nhận vào)
            if (victimPets.contains(GachaGUI.LinhThu.HUYET_PHU_QUY)) {
                dmgMultiplier *= 0.85;
            }

            event.setDamage(event.getDamage() * dmgMultiplier);
        }

        // --- XỬ LÝ KHI NGƯỜI CHƠI TẤN CÔNG ---
        if (event.getDamager() instanceof Player attacker) {
            HeTuLuyen heAttacker = dataManager.getHeTuLuyen(attacker);
            Set<GachaGUI.LinhThu> attackerPets = StatsManager.getActivePets(attacker);
            String mainHandName = attacker.getInventory().getItemInMainHand().getType().name();

            double atkMultiplier = 1.0;

            // KIẾM TU: +20% sát thương khi cầm Kiếm
            if (heAttacker == HeTuLuyen.KIEM_TU && mainHandName.contains("SWORD")) {
                atkMultiplier *= 1.20;
            }

            // LINH THÚ THIÊN MÃU HỔ: +20% sát thương cận chiến
            if (attackerPets.contains(GachaGUI.LinhThu.THIEN_MAU_HO)) {
                atkMultiplier *= 1.20;
            }

            event.setDamage(event.getDamage() * atkMultiplier);
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            // YÊU TU: Hồi phục tự nhiên × 2
            if (dataManager.getHeTuLuyen(player) == HeTuLuyen.YEU_TU) {
                event.setAmount(event.getAmount() * 2.0);
            }
        }
    }
}
