package tutien.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Task quản lý Ngự Kiếm:
 * v2.2 Skyblock Edition:
 *   - Hệ Kiếm Tu được giảm 50% tiêu hao Linh Lực (25 Linh Lực / 2 giây thay vì 50) đúng theo mô tả HeTuLuyen.
 *   - Khi hết Linh Lực trên không trung, tự động ban hiệu ứng Rơi Chậm (Slow Falling 10 giây) để tránh rơi thẳng xuống vực Void của Skyblock.
 */
public class NguKiemTask extends BukkitRunnable {

    private static final int BASE_LINH_LUC_COST = 50; // Tiêu mỗi 2 giây
    private static final Set<UUID> flyingPlayers = new HashSet<>();
    private final PlayerDataManager dataManager;

    public NguKiemTask(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public static int getCost(Player player, PlayerDataManager dataManager) {
        if (dataManager.getHeTuLuyen(player) == HeTuLuyen.KIEM_TU) {
            return BASE_LINH_LUC_COST / 2; // Kiếm Tu: 25 Linh Lực / 2s
        }
        return BASE_LINH_LUC_COST;
    }

    public static void startFlight(Player player, PlayerDataManager dataManager) {
        int cost = getCost(player, dataManager);
        flyingPlayers.add(player.getUniqueId());
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§b§l[Ngự Kiếm] §fKhởi động phi kiếm! Tiêu hao §e" + cost + " Linh Lực§f/2 giây.");
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
    }

    // Giữ overload cũ để tương thích
    public static void startFlight(Player player) {
        flyingPlayers.add(player.getUniqueId());
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§b§l[Ngự Kiếm] §fKhởi động phi kiếm!");
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
    }

    public static void stopFlight(Player player) {
        flyingPlayers.remove(player.getUniqueId());
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        // Ban hiệu ứng Rơi Chậm 10s để bảo vệ người chơi trên đảo Skyblock khỏi chết oan vì sát thương rơi hoặc lọt xuống Void
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0, true, false));
        player.sendMessage("§b[Ngự Kiếm] §fThu hồi phi kiếm. §7(Được Hộ Phong Rơi Chậm 10s)");
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

            if (dataManager.getCanhGioi(player).ordinal() < CanhGioi.KIM_DAN.ordinal()) {
                stopFlight(player);
                continue;
            }

            int cost = getCost(player, dataManager);
            int linhLuc = dataManager.getLinhLuc(player);
            if (linhLuc < cost) {
                stopFlight(player);
                player.sendMessage("§c§l[!] §fHết Linh Lực! Phi kiếm tự thu hồi, kích hoạt Hộ Phong hạ cánh!");
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                continue;
            }

            dataManager.setLinhLuc(player, linhLuc - cost);

            Location loc = player.getLocation().add(0, 0.5, 0);
            player.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.4, 0.4, 0.4, 0.1);
            player.getWorld().spawnParticle(Particle.ENCHANT, loc, 5, 0.3, 0.3, 0.3, 0.05);

            int remaining = dataManager.getLinhLuc(player);
            int max = dataManager.getMaxLinhLucEffective(player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("§b⚔ §l[Ngự Kiếm] §fLinh Lực: §e" + remaining + "§f/§e" + max + " §7(-" + cost + "/2s)"));
        }
    }
}
