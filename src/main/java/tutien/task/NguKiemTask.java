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
 * Task quản lý Ngự Không (Đạp Mây Phi Hành):
 * v2.3 Skyblock Edition:
 *   - Đổi tên từ Ngự Kiếm thành "Ngự Không" (vẫn hỗ trợ alias cũ).
 *   - Hiệu ứng tùy chỉnh tuyệt đẹp dưới chân: Tường Vân (đám mây tiên bồng bềnh), Phong Độn khí lưu và tiên quang biến đổi theo Cảnh Giới.
 *   - Hệ Kiếm Tu được giảm 50% tiêu hao Linh Lực (25 Linh Lực / 2 giây thay vì 50).
 *   - Khi hết Linh Lực trên không trung, tự động ban hiệu ứng Rơi Chậm (Slow Falling 10 giây) để tránh rơi vào Void Skyblock.
 */
public class NguKiemTask extends BukkitRunnable {

    private static final int BASE_LINH_LUC_COST = 50; // Tiêu mỗi 2 giây
    private static final Set<UUID> flyingPlayers = new HashSet<>();
    private final PlayerDataManager dataManager;
    private int step = 0;

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
        player.sendMessage("§b§l[Ngự Không] §fKhởi động Độn Thuật Ngự Không! Đạp mây phi hành hư không! §7(-" + cost + " Linh Lực/2s)");
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
    }

    // Overload cũ để tương thích
    public static void startFlight(Player player) {
        flyingPlayers.add(player.getUniqueId());
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§b§l[Ngự Không] §fKhởi động Độn Thuật Ngự Không!");
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
    }

    public static void stopFlight(Player player) {
        flyingPlayers.remove(player.getUniqueId());
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        // Ban hiệu ứng Rơi Chậm 10s để bảo vệ người chơi trên đảo Skyblock khỏi rơi xuống Void
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0, true, false));
        player.sendMessage("§b[Ngự Không] §fThu hồi độn thuật. §7(Được Hộ Phong Rơi Chậm 10s)");
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
        step++;
        for (UUID uuid : new HashSet<>(flyingPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                flyingPlayers.remove(uuid);
                continue;
            }

            CanhGioi cg = dataManager.getCanhGioi(player);
            if (cg.ordinal() < CanhGioi.KIM_DAN.ordinal()) {
                stopFlight(player);
                continue;
            }

            int cost = getCost(player, dataManager);
            int linhLuc = dataManager.getLinhLuc(player);
            if (linhLuc < cost) {
                stopFlight(player);
                player.sendMessage("§c§l[!] §fHết Linh Lực! Độn thuật tự thu hồi, kích hoạt Hộ Phong hạ cánh an toàn!");
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                continue;
            }

            dataManager.setLinhLuc(player, linhLuc - cost);

            // ==========================================
            // HIỆU ỨNG TÙY CHỈNH DƯỚI CHÂN KHI NGỰ KHÔNG
            // ==========================================
            renderFlightEffects(player, cg);

            int remaining = dataManager.getLinhLuc(player);
            int max = dataManager.getMaxLinhLucEffective(player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("§b☁ §l[Ngự Không] §fLinh Lực: §e" + remaining + "§f/§e" + max + " §7(-" + cost + "/2s)"));
        }
    }

    /**
     * Tạo hiệu ứng Tường Vân (đám mây tiên) và khí lưu xoay quanh gót chân.
     */
    private void renderFlightEffects(Player player, CanhGioi cg) {
        Location feet = player.getLocation().clone().subtract(0, 0.15, 0);

        // 1. Tường Vân (Đám mây tiên bồng bềnh dưới 2 chân)
        player.getWorld().spawnParticle(Particle.CLOUD, feet, 4, 0.25, 0.05, 0.25, 0.02);

        // 2. Vòng xoáy phong độn dưới gót chân (bán kính 0.5)
        for (int i = 0; i < 4; i++) {
            double angle = (step * 0.4) + (i * Math.PI / 2);
            double x = 0.5 * Math.cos(angle);
            double z = 0.5 * Math.sin(angle);
            Location ringPoint = feet.clone().add(x, 0, z);

            if (cg.ordinal() >= CanhGioi.KHUY_NIET.ordinal()) {
                // Cửu Sắc Tiên Vân cho Tiên Nhân
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, ringPoint, 1, 0, 0, 0, 0.01);
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, ringPoint, 1, 0, 0, 0, 0.01);
            } else if (cg.ordinal() >= CanhGioi.HOA_THAN.ordinal()) {
                // Tử Điện Tường Vân cho Hóa Thần trở lên
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ringPoint, 1, 0, 0, 0, 0.01);
                player.getWorld().spawnParticle(Particle.END_ROD, ringPoint, 1, 0, 0, 0, 0.01);
            } else {
                // Tiên quang thanh nhã cho Kim Đan & Nguyên Anh
                player.getWorld().spawnParticle(Particle.END_ROD, ringPoint, 1, 0, 0, 0, 0.01);
            }
        }
    }
}
