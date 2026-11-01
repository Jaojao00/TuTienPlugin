package tutien.alchemy.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.alchemy.model.DetoxSession;
import tutien.alchemy.service.DanDocService;
import tutien.alchemy.service.DetoxService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Task định kỳ (1 giây/lần) xử lý tiến độ Bế Quan Hóa Độc per-tick và hiển thị Action Bar.
 */
public class DetoxTask extends BukkitRunnable {

    private final DetoxService detoxService;
    private final DanDocService danDocService;

    public DetoxTask(DetoxService detoxService, DanDocService danDocService) {
        this.detoxService = detoxService;
        this.danDocService = danDocService;
    }

    @Override
    public void run() {
        if (detoxService == null || danDocService == null) return;

        List<UUID> sessionIds = new ArrayList<>(detoxService.getActiveSessions().keySet());
        for (UUID uuid : sessionIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                detoxService.stopDetoxSession(player, "Thoát game");
                continue;
            }

            if (player.isDead()) {
                detoxService.stopDetoxSession(player, "Trọng thương tử vong");
                continue;
            }

            DetoxSession session = detoxService.getSession(player);
            if (session == null || !session.isActive()) continue;

            // Kiểm tra dịch chuyển vị trí
            if (session.getStartLocation() != null) {
                if (!player.getWorld().equals(session.getStartLocation().getWorld())
                        || player.getLocation().distanceSquared(session.getStartLocation()) > 0.5) {
                    detoxService.stopDetoxSession(player, "Di chuyển khỏi vị trí tĩnh tọa");
                    continue;
                }
            }

            // Thực hiện 1 nhịp thanh lọc an toàn (đã kẹp delta clamp chống lag)
            double ratePerMin = detoxService.getMeditationDetoxPerMinute();
            double maxGap = detoxService.getMaxTickGapSeconds();
            double cleansedThisTick = session.tick(ratePerMin, maxGap);

            if (cleansedThisTick > 0) {
                // Trừ Đan Độc ngay lập tức theo chu kỳ nhỏ (Zero Loss, không đợi hết session)
                double remaining = danDocService.reduceDanDoc(player, cleansedThisTick);

                // Action Bar phản hồi trực quan thời gian thực
                String msg = "§2§l✦ HÓA ĐỘC §fĐan độc: §c" + String.format("%.1f", remaining) + "/1000"
                        + " §7| Tẩy trừ: §a-" + String.format("%.1f", session.getTotalCleansed())
                        + " §7| §eTốc độ: " + String.format("%.2f", ratePerMin / 60.0) + "/s";
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));

                // Khi đan độc về 0 -> tự động hoàn tất session
                if (remaining <= 0.0) {
                    detoxService.stopDetoxSession(player, "Kinh mạch đã hoàn toàn tịnh hóa");
                    continue;
                }

                // Hiệu ứng hạt thanh lọc khí độc (Spell Witch / Happy Villager)
                renderDetoxParticles(player);
            }
        }
    }

    private void renderDetoxParticles(Player player) {
        try {
            org.bukkit.Location loc = player.getLocation().add(0, 0.8, 0);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 3, 0.3, 0.4, 0.3, 0.05);
            player.getWorld().spawnParticle(Particle.ENCHANT, loc, 2, 0.2, 0.3, 0.2, 0.1);
        } catch (Throwable ignored) {}
    }
}
