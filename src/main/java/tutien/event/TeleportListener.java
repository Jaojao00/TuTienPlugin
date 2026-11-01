package tutien.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;

/**
 * Lớp này xử lý việc chặn người chơi cấp thấp bước vào các vùng đất cao cấp.
 */
public class TeleportListener implements Listener {

    private final PlayerDataManager dataManager;

    public TeleportListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Chỉ kiểm tra khi người chơi thực sự di chuyển sang khối khác để tránh lag
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();

        // 1. Kiểm tra xem người chơi có đang đứng trên khối Beacon (Trận pháp) không
        if (to.getBlock().getType() == Material.BEACON) {

            // 2. Kiểm tra xem có đang ở map Phàm Giới và muốn lên Hạ Tu Tiên không
            if (player.getWorld().getName().equals("world_phamgioi")) {

                CanhGioi cg = dataManager.getCanhGioi(player);

                // YÊU CẦU: Phải đạt Trúc Cơ mới được qua
                if (cg.ordinal() < CanhGioi.TRUC_CO.ordinal()) {

                    // --- XỬ LÝ KHI CHƯA ĐỦ CẢNH GIỚI ---

                    // Giật sét (chỉ tạo hiệu ứng hình ảnh/âm thanh, không gây sát thương thật)
                    player.getWorld().strikeLightningEffect(player.getLocation());

                    // Đẩy lùi người chơi ra xa trận pháp
                    Vector bounce = player.getLocation().getDirection().multiply(-1.5).setY(0.5);
                    player.setVelocity(bounce);

                    // Thông báo
                    player.sendMessage("§c§l[!] §fCảnh giới quá thấp, thân thể không chịu nổi áp lực không gian!");
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

                } else {

                    // --- XỬ LÝ KHI ĐỦ ĐIỀU KIỆN DỊCH CHUYỂN ---

                    player.sendMessage("§b§l[Trận Pháp] §fLinh lực tương thích, bắt đầu dịch chuyển đến Hạ Tu Tiên Giới...");

                    // Tọa độ đích tại world_hatutien (Bạn có thể thay đổi số 0, 100, 0 thành tọa độ bạn muốn)
                    Location destination = new Location(Bukkit.getWorld("world_hatutien"), 0.5, 100, 0.5);

                    // Kiểm tra xem world đã load chưa
                    if (destination.getWorld() != null) {
                        player.teleport(destination);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    } else {
                        player.sendMessage("§c[Lỗi] Thế giới Hạ Tu Tiên chưa khởi tạo!");
                    }
                }
            }
        }
    }
}