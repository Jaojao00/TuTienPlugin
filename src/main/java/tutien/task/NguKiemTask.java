package tutien.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Task quản lý Ngự Kiếm:
 * - Mỗi 2 giây tiêu 50 Linh Lực khi đang bay
 * - Tự động tắt khi hết Linh Lực
 * - Hiệu ứng hạt kiếm xanh xung quanh người
 * SPRINT 2: Nâng cấp Ngự Kiếm từ "bay miễn phí" lên có chi phí.
 */
public class NguKiemTask extends BukkitRunnable {

    private static final int LINH_LUC_COST = 50; // Tiêu mỗi 2 giây
    private static final Set<UUID> flyingPlayers = new HashSet<>();
    private final PlayerDataManager dataManager;

    public NguKiemTask(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public static void startFlight(Player player) {
        flyingPlayers.add(player.getUniqueId());
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§b§l[Ngự Kiếm] §fKhởi động phi kiếm! Tiêu hao §e50 Linh Lực§f/2 giây.");
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
    }

    public static void stopFlight(Player player) {
        flyingPlayers.remove(player.getUniqueId());
        player.setFlying(false);
        player.setAllowFlight(false);
        player.sendMessage("§b[Ngự Kiếm] §fThu hồi phi kiếm.");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.8f);
    }

    public static boolean isFlying(Player player) {
        return flyingPlayers.contains(player.getUniqueId());
    }

    public static void cleanupPlayer(UUID uuid) {
        flyingPlayers.remove(uuid);
    }

    @Override
    public void run() {
        for (UUID uuid : new HashSet<>(flyingPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                flyingPlayers.remove(uuid);
                continue;
            }

            // Kiểm tra cảnh giới (phải Kim Đan+)
            if (dataManager.getCanhGioi(player).ordinal() < CanhGioi.KIM_DAN.ordinal()) {
                stopFlight(player);
                continue;
            }

            int linhLuc = dataManager.getLinhLuc(player);
            if (linhLuc < LINH_LUC_COST) {
                // Hết Linh Lực → tự tắt
                stopFlight(player);
                player.sendMessage("§c§l[!] §fHết Linh Lực! Phi kiếm tự thu hồi!");
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                continue;
            }

            // Tiêu hao Linh Lực
            dataManager.setLinhLuc(player, linhLuc - LINH_LUC_COST);

            // Hiệu ứng hạt xung quanh người khi bay
            Location loc = player.getLocation().add(0, 0.5, 0);
            player.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.4, 0.4, 0.4, 0.1);
            player.getWorld().spawnParticle(Particle.ENCHANT, loc, 5, 0.3, 0.3, 0.3, 0.05);

            // Action bar thông báo Linh Lực còn lại
            int remaining = dataManager.getLinhLuc(player);
            int max = dataManager.getMaxLinhLucEffective(player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("§b⚔ §l[Ngự Kiếm] §fLinh Lực: §e" + remaining + "§f/§e" + max));
        }
    }
}
