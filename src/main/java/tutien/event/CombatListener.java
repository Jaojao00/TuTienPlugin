package tutien.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;

/**
 * Lớp xử lý các hiệu ứng đặc biệt của từng hệ khi đánh nhau.
 * ĐÃ ĐỒNG BỘ: % buff khớp 100% với mô tả trong HeTuLuyen.java.
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

            // THỂ TU: "Giảm 10% sát thương nhận vào" → đúng với mô tả (ghi 10%, code 0.9)
            if (heVictim == HeTuLuyen.THE_TU) {
                event.setDamage(event.getDamage() * 0.9);
            }

            // MA TU: "Giảm 20% khả năng phòng thủ" → nhận thêm 20% sát thương
            if (heVictim == HeTuLuyen.MA_TU) {
                event.setDamage(event.getDamage() * 1.2);
            }
        }

        // --- XỬ LÝ KHI NGƯỜI CHƠI TẤN CÔNG ---
        if (event.getDamager() instanceof Player attacker) {
            HeTuLuyen heAttacker = dataManager.getHeTuLuyen(attacker);
            String mainHandName = attacker.getInventory().getItemInMainHand().getType().name();

            // KIẾM TU: "+20% sát thương Kiếm" khi cầm bất kỳ loại kiếm nào
            if (heAttacker == HeTuLuyen.KIEM_TU && mainHandName.contains("SWORD")) {
                event.setDamage(event.getDamage() * 1.20);
            }

            // YÊU TU: không buff tấn công, ưu thế nằm ở tốc độ + hồi máu
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            // YÊU TU: "Hồi phục tự nhiên" × 2 — đúng với mô tả
            if (dataManager.getHeTuLuyen(player) == HeTuLuyen.YEU_TU) {
                event.setAmount(event.getAmount() * 2.0);
            }
        }
    }
}