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
 */
public class CombatListener implements Listener {

    private final PlayerDataManager dataManager;

    public CombatListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        // --- XỬ LÝ KHI NGƯỜI CHƠI BỊ TẤN CÔNG ---
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            HeTuLuyen he = dataManager.getHeTuLuyen(victim);

            // THỂ TU: Giảm 20% mọi sát thương nhận vào
            if (he == HeTuLuyen.THE_TU) {
                event.setDamage(event.getDamage() * 0.8);
            }

            // MA TU: Nhận thêm 25% sát thương (Hình phạt cho việc quá mạnh sát thương)
            if (he == HeTuLuyen.MA_TU) {
                event.setDamage(event.getDamage() * 1.25);
            }
        }

        // --- XỬ LÝ KHI NGƯỜI CHƠI TẤN CÔNG ---
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            HeTuLuyen he = dataManager.getHeTuLuyen(attacker);

            // KIẾM TU: Nếu dùng kiếm gỗ/sắt/kim cương... thì +15% sát thương kiếm ý
            if (he == HeTuLuyen.KIEM_TU && attacker.getInventory().getItemInMainHand().getType().name().contains("SWORD")) {
                event.setDamage(event.getDamage() * 1.15);
            }
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // YÊU TU: Hồi máu tự nhiên nhanh gấp đôi
            if (dataManager.getHeTuLuyen(player) == HeTuLuyen.YEU_TU) {
                event.setAmount(event.getAmount() * 2.0);
            }
        }
    }
}