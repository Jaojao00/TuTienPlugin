package tutien.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.inventory.TuiDoManager;
import tutien.tongmon.TongMonManager;

/**
 * Task tự động lưu dữ liệu mỗi 5 phút (6000 ticks).
 * Chạy BẤT ĐỒNG BỘ để tránh freeze server khi có nhiều người chơi.
 * SPRINT 1: Thêm tính năng an toàn dữ liệu khi server crash.
 */
public class AutoSaveTask extends BukkitRunnable {

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final TongMonManager tongMonManager;
    private final TuiDoManager tuiDoManager;

    private int saveCount = 0;

    public AutoSaveTask(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
        this.tongMonManager = plugin.getTongMonManager();
        this.tuiDoManager = plugin.getTuiDoManager();
    }

    @Override
    public void run() {
        saveCount++;
        int playerCount = Bukkit.getOnlinePlayers().size();
        if (playerCount == 0) return; // Không có ai online, bỏ qua

        // Thu thập snapshot dữ liệu trên main thread trước
        java.util.List<Player> players = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());

        // Lưu dữ liệu ASYNC (không block main thread)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int saved = 0;
            for (Player player : players) {
                if (!player.isOnline()) continue;
                try {
                    // Lưu từng người một trên async thread
                    savePlayerAsync(player);
                    saved++;
                } catch (Exception e) {
                    plugin.getLogger().warning("[AutoSave] Lỗi khi lưu dữ liệu của " + player.getName() + ": " + e.getMessage());
                }
            }

            // Lưu dữ liệu Tông Môn (không theo từng player)
            try {
                tongMonManager.saveData();
            } catch (Exception e) {
                plugin.getLogger().warning("[AutoSave] Lỗi khi lưu Tông Môn: " + e.getMessage());
            }

            plugin.getLogger().info("[AutoSave #" + saveCount + "] Đã lưu " + saved + "/" + playerCount + " người chơi.");
        });
    }

    /**
     * Lưu dữ liệu một người chơi (gọi trên async thread).
     * Dùng phiên bản saveAsync không dọn RAM để người chơi vẫn tiếp tục chơi.
     */
    private void savePlayerAsync(Player player) {
        // Lưu PlayerData (gọi savePlayerAsync nếu có, hoặc dùng savePlayer thông thường)
        dataManager.savePlayerAsync(player);

        // Lưu Túi Đồ Hư Không
        if (tuiDoManager != null) {
            tuiDoManager.savePlayerAsync(player);
        }
    }
}
