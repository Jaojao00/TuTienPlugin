package tutien.event;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import tutien.tongmon.TongMonManager;

import java.util.UUID;

/**
 * Lớp này lắng nghe sự kiện đập/đặt block để bảo vệ Lãnh Địa Tông Môn.
 */
public class TongMonListener implements Listener {

    private final TongMonManager tongMonManager;

    public TongMonListener(TongMonManager tongMonManager) {
        this.tongMonManager = tongMonManager;
    }

    // Bắt sự kiện khi có người đập block
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleProtection(event, event.getPlayer(), event.getBlock().getLocation());
    }

    // Bắt sự kiện khi có người đặt block
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        handleProtection(event, event.getPlayer(), event.getBlock().getLocation());
    }

    // Hàm dùng chung để xử lý logic bảo vệ đất
    private void handleProtection(org.bukkit.event.Cancellable event, Player player, Location loc) {
        // Kiểm tra xem vị trí này có thuộc về Tông Môn nào khác không
        UUID ownerId = tongMonManager.checkKhongChoPhepTuongTac(loc, player);

        // Nếu kết quả trả về KHÁC null, nghĩa là người chơi ĐANG XÂM PHẠM đất của Tông Môn khác
        if (ownerId != null) {
            event.setCancelled(true); // Hủy ngay hành động đập/đặt block

            String tenTongMon = tongMonManager.getTenTongMon(ownerId);
            player.sendMessage("§c§l[Cảnh Báo] §fĐây là sơn môn của §e" + tenTongMon + "§f! Kẻ ngoại đạo không có quyền phận sự ở đây.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}