package tutien.event;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import tutien.inventory.TuiDoManager;

/**
 * Lắng nghe sự kiện nhặt đồ.
 * ĐÃ NÂNG CẤP: Xử lý trường hợp túi đầy (300/ô), cảnh báo người chơi.
 */
public class AutoPickupListener implements Listener {

    private final TuiDoManager tuiDoManager;

    public AutoPickupListener(TuiDoManager tuiDoManager) {
        this.tuiDoManager = tuiDoManager;
    }

    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        if (tuiDoManager.isAutoPickup(player)) {
            ItemStack item = event.getItem().getItemStack();

            // Điều kiện để phân biệt Đồ Tu Tiên và Đồ Rác Vani (Đất, Đá):
            // Đồ tu tiên chắc chắn sẽ có Tên Hiển Thị (DisplayName) hoặc Custom Model Data
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                int added = tuiDoManager.addItem(player, item, item.getAmount());

                if (added > 0) {
                    event.setCancelled(true);

                    // Nếu chỉ cất được một phần (ô đầy)
                    if (added < item.getAmount()) {
                        int leftover = item.getAmount() - added;
                        ItemStack remaining = item.clone();
                        remaining.setAmount(leftover);
                        event.getItem().setItemStack(remaining);
                        player.sendMessage(
                                "§c§l[Túi Đồ] §fÔ chứa đã đầy (300/300)! Còn §e" + leftover + " §fvật phẩm rơi ngoài.");
                    } else {
                        // Cất hết thành công -> xóa entity
                        event.getItem().remove();
                    }

                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, 1.5f);
                } else {
                    // Không cất được gì (ô đầy hoàn toàn)
                    player.sendMessage(
                            "§c§l[Túi Đồ] §fÔ chứa đã đầy (300/300)! Vật phẩm rơi ra ngoài, hãy kiểm tra túi đồ!");
                }
            }
        }
    }
}