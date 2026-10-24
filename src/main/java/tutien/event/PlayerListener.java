package tutien.event;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tutien.combat.StatsManager;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.quest.NhiemVuManager;
import tutien.task.NguKiemTask;
import tutien.tutien.gui.ChonHeGUI;

import java.util.HashMap;
import java.util.UUID;

/**
 * Lớp lắng nghe sự kiện người chơi.
 * v2.2 Skyblock Edition:
 *   - Sửa Thuấn Di (Blink): Chỉ kích hoạt khi người chơi cầm Kiếm VÀ nhìn chếch lên trên (pitch < -15 độ).
 *     Tuyệt đối không còn bị bay văng xuống vực Void khi vừa chạy vừa bấm Shift bắc cầu (Speed-bridging) trên đảo Skyblock!
 *   - Khi thoát game chỉ tắt Fly nếu đang dùng Ngự Kiếm (tôn trọng quyền /is fly của plugin Skyblock).
 */
public class PlayerListener implements Listener {

    private final PlayerDataManager dataManager;
    private final TuTienPlugin plugin;

    private final HashMap<UUID, Long> thuanDiCooldowns = new HashMap<>();

    public PlayerListener(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        dataManager.loadPlayer(player);

        if (plugin.getTuiDoManager() != null) {
            plugin.getTuiDoManager().loadPlayer(player);
        }

        StatsManager.applyStats(player, dataManager);

        if (dataManager.getHeTuLuyen(player) == HeTuLuyen.CHUA_CHON) {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> ChonHeGUI.open(player, dataManager), 40L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (dataManager.isDangTuLuyen(player)) {
            dataManager.setTuLuyenMode(player, false);
            player.removePotionEffect(PotionEffectType.LEVITATION);
        }

        // Chỉ tắt Fly khi thoát nếu người chơi đang bật Ngự Kiếm (tránh mất /is fly của Skyblock)
        if (NguKiemTask.isFlying(player)) {
            NguKiemTask.cleanupPlayer(player.getUniqueId());
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }

        thuanDiCooldowns.remove(player.getUniqueId());

        if (plugin.getNhiemVuManager() != null) {
            plugin.getNhiemVuManager().cleanupPlayer(player.getUniqueId());
        }

        if (plugin.getLinhThuVienManager() != null) {
            plugin.getLinhThuVienManager().cleanupPlayer(player.getUniqueId());
        }

        if (plugin.getTuiDoManager() != null) {
            plugin.getTuiDoManager().savePlayer(player);
        }

        dataManager.savePlayer(player);
    }

    // ==========================================
    // THẦN THÔNG: THUẤN DI (BLINK AN TOÀN CHO SKYBLOCK)
    // Thao tác: Cầm Kiếm + Nhìn lên trên (pitch < -15) + Đang chạy nhanh (Sprint) -> Bấm Shift
    // ==========================================
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking() && player.isSprinting()) {
            // Điều kiện an toàn Skyblock: Phải cầm Kiếm và nhìn chếch lên trên để không kích hoạt nhầm khi đang bắc cầu (bridging)
            String handType = player.getInventory().getItemInMainHand().getType().name();
            if (!handType.contains("SWORD") || player.getLocation().getPitch() > -15.0f) {
                return;
            }

            CanhGioi cg = dataManager.getCanhGioi(player);
            if (cg.ordinal() >= CanhGioi.KIM_DAN.ordinal()) {
                long currentTime = System.currentTimeMillis();
                long lastUsed = thuanDiCooldowns.getOrDefault(player.getUniqueId(), 0L);

                if (currentTime - lastUsed < 5000) {
                    long timeLeft = 5 - ((currentTime - lastUsed) / 1000);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§c[!] §fThuấn Di đang hồi chiêu (" + timeLeft + "s)"));
                    return;
                }

                int linhLuc = dataManager.getLinhLuc(player);
                if (linhLuc < 300) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§c[!] §fKhông đủ 300 Linh Lực để thi triển Thuấn Di!"));
                    return;
                }

                dataManager.setLinhLuc(player, linhLuc - 300);
                thuanDiCooldowns.put(player.getUniqueId(), currentTime);

                Location loc = player.getLocation();
                Vector dir = loc.getDirection().normalize();
                Location target = loc.clone();

                for (int i = 1; i <= 12; i++) {
                    Location checkLoc = loc.clone().add(dir.clone().multiply(i));
                    if (checkLoc.getBlock().getType().isSolid() || checkLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                        break;
                    }
                    target = checkLoc;
                }

                target.setYaw(loc.getYaw());
                target.setPitch(loc.getPitch());

                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.5, 1, 0.5, 0.1);
                player.teleport(target);
                player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                player.getWorld().spawnParticle(Particle.PORTAL, target, 50, 0.5, 1, 0.5, 0.2);
                player.sendMessage("§b§l[Thần Thông] §fThuấn Di!");
            }
        }
    }

    // ==========================================
    // KHÓA DI CHUYỂN NGANG KHI ĐANG TỌA THIỀN
    // ==========================================
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (dataManager.isDangTuLuyen(player)) {
            Location to = event.getTo();
            if (to != null && (event.getFrom().getX() != to.getX() ||
                    event.getFrom().getZ() != to.getZ())) {
                Location fixed = event.getFrom().clone();
                fixed.setY(to.getY());
                fixed.setPitch(to.getPitch());
                fixed.setYaw(to.getYaw());
                event.setTo(fixed);
            }
        }
    }

    // ==========================================
    // HỦY TRẠNG THÁI TỌA THIỀN KHI BỊ TẤN CÔNG
    // ==========================================
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (dataManager.isDangTuLuyen(player)) {
                dataManager.setTuLuyenMode(player, false);
                player.sendMessage("§c§l[!] §fBị tấn công! Quá trình tọa thiền bị gián đoạn!");
                player.removePotionEffect(PotionEffectType.LEVITATION);
            }
        }
    }

    // ==========================================
    // NHIỆM VỤ HÀNG NGÀY: KIẾN THIẾT ĐẢO TIÊN (ĐẶT KHỐI)
    // ==========================================
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getNhiemVuManager() != null) {
            if (plugin.getTongMonManager() != null) {
                UUID owner = plugin.getTongMonManager().checkKhongChoPhepTuongTac(event.getBlock().getLocation(), player);
                if (owner == null) {
                    plugin.getNhiemVuManager().addProgress(player, NhiemVuManager.NhiemVu.XAY_DUNG, 1);
                }
            }
        }
    }

    // ==========================================
    // NHIỆM VỤ HÀNG NGÀY: ĐÁNH QUÁI (GIẾT QUÁI)
    // ==========================================
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            if (!(event.getEntity() instanceof Player)) {
                if (plugin.getNhiemVuManager() != null) {
                    plugin.getNhiemVuManager().addProgress(player, NhiemVuManager.NhiemVu.DANH_QUAI, 1);
                }
            }
        }
    }
}
