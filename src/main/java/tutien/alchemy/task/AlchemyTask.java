package tutien.alchemy.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.alchemy.gui.AlchemyMiniGameGUI;
import tutien.alchemy.model.AlchemyRecipe;
import tutien.alchemy.model.AlchemySession;
import tutien.alchemy.service.AlchemyService;

import java.util.Map;
import java.util.UUID;

/**
 * Task định kỳ 1 giây (20 ticks) cập nhật trạng thái Hỏa Hầu,
 * làm nguội tự nhiên, kiểm tra nổ lò và đồng bộ giao diện Mini-game thời gian thực.
 */
public class AlchemyTask extends BukkitRunnable {

    private final AlchemyService alchemyService;

    public AlchemyTask(AlchemyService alchemyService) {
        this.alchemyService = alchemyService;
    }

    @Override
    public void run() {
        if (alchemyService == null) return;

        Map<UUID, AlchemySession> activeMap = alchemyService.getActiveSessions();
        if (activeMap.isEmpty()) return;

        for (UUID uuid : activeMap.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                // Người chơi offline: Gọi tick để xử lý an toàn
                alchemyService.tickSession(uuid);
                continue;
            }

            AlchemySession session = alchemyService.getSession(uuid);
            if (session == null || session.isFinished() || session.isCancelled()) continue;

            // 1. Tick trạng thái hỏa hầu
            alchemyService.tickSession(uuid);

            // 2. Cập nhật Action Bar thời gian thực
            AlchemyRecipe recipe = session.getRecipe();
            double temp = session.getCurrentTemperature();
            String tempColor = (temp >= recipe.getMinToleranceTemp() && temp <= recipe.getMaxToleranceTemp())
                    ? "§a" : (temp < recipe.getMinToleranceTemp() ? "§b" : "§c");

            String msg = "§6§l[Đan Lò] §fNhiệt độ: " + tempColor + String.format("%.0f", temp) + "°C"
                    + " §8| §fThời gian: §e" + session.getRemainingSeconds() + "s"
                    + " §8| §fỔn định: §a" + String.format("%.1f", session.calculateScore()) + "%";

            try {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            } catch (Exception ignored) {}

            // 3. Nếu người chơi đang mở GUI Lò Luyện -> cập nhật lại các slot
            InventoryView view = player.getOpenInventory();
            if (view != null && view.getTitle().startsWith(AlchemyMiniGameGUI.GUI_TITLE_PREFIX)) {
                AlchemyMiniGameGUI.render(view.getTopInventory(), session);
            }
        }
    }
}
