package tutien.event;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.tutien.gui.ChonHeGUI;

import java.util.HashMap;
import java.util.UUID;

/**
 * Lớp lắng nghe sự kiện người chơi.
 * Đã cập nhật: Sửa lỗi sendActionBar và các cảnh báo (Warnings) từ IDE.
 */
public class PlayerListener implements Listener {

    private final PlayerDataManager dataManager;
    private final TuTienPlugin plugin;

    // Bộ nhớ đệm lưu thời gian hồi chiêu của Thuấn Di
    private final HashMap<UUID, Long> thuanDiCooldowns = new HashMap<>();

    public PlayerListener(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 1. Nạp dữ liệu người chơi
        dataManager.loadPlayer(event.getPlayer());

        // 2. Nạp dữ liệu Túi Đồ Hư Không
        if (plugin.getTuiDoManager() != null) {
            plugin.getTuiDoManager().loadPlayer(event.getPlayer());
        }

        // 3. Cập nhật chỉ số
        tutien.combat.StatsManager.applyStats(event.getPlayer(), dataManager);

        // 4. Tự động hiện menu chọn hệ nếu chưa chọn
        if (dataManager.getHeTuLuyen(event.getPlayer()) == HeTuLuyen.CHUA_CHON) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
                    () -> ChonHeGUI.open(event.getPlayer(), dataManager), 40L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Tránh lỗi kẹt trạng thái: Tự động tắt Tu Luyện và gỡ LEVITATION
        if (dataManager.isDangTuLuyen(player)) {
            dataManager.setTuLuyenMode(player, false);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION);
        }

        // Sprint 2: Tắt Ngự Kiếm khi thoát (tránh bay kẹt)
        tutien.task.NguKiemTask.cleanupPlayer(player.getUniqueId());
        player.setFlying(false);
        player.setAllowFlight(false);

        // Dọn dẹp RAM cooldowns
        thuanDiCooldowns.remove(player.getUniqueId());

        // Sprint 3: Giải phóng RAM nhiệm vụ
        if (plugin.getNhiemVuManager() != null) {
            plugin.getNhiemVuManager().cleanupPlayer(player.getUniqueId());
        }

        // Sprint 4: Dọn dẹp Linh Thú Viên (kill boss nếu còn)
        if (plugin.getLinhThuVienManager() != null) {
            plugin.getLinhThuVienManager().cleanupPlayer(player.getUniqueId());
        }

        // Lưu dữ liệu Túi Đồ Hư Không
        if (plugin.getTuiDoManager() != null) {
            plugin.getTuiDoManager().savePlayer(player);
        }

        // Lưu dữ liệu player
        dataManager.savePlayer(player);
    }

    // ==========================================
    // THẦN THÔNG: THUẤN DI (BLINK)
    // Thao tác: Đang chạy nhanh (W W) -> Bấm Shift
    // ==========================================
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Nếu người chơi bấm Shift (Ngồi xuống) TRONG LÚC đang chạy nhanh (Sprinting)
        if (event.isSneaking() && player.isSprinting()) {
            CanhGioi cg = dataManager.getCanhGioi(player);

            // Điều kiện: Chỉ cường giả từ Kim Đan trở lên mới được dùng
            if (cg.ordinal() >= CanhGioi.KIM_DAN.ordinal()) {
                long currentTime = System.currentTimeMillis();
                long lastUsed = thuanDiCooldowns.getOrDefault(player.getUniqueId(), 0L);

                // Kiểm tra hồi chiêu (5 giây = 5000 ms)
                if (currentTime - lastUsed < 5000) {
                    long timeLeft = 5 - ((currentTime - lastUsed) / 1000);
                    // Đã fix lỗi: Sử dụng API của Bungee để gửi ActionBar
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§c[!] §fThuấn Di đang hồi chiêu (" + timeLeft + "s)"));
                    return;
                }

                // Kiểm tra Linh Lực (Tiêu hao 300)
                int linhLuc = dataManager.getLinhLuc(player);
                if (linhLuc < 300) {
                    // Đã fix lỗi: Sử dụng API của Bungee để gửi ActionBar
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§c[!] §fKhông đủ 300 Linh Lực để thi triển Thuấn Di!"));
                    return;
                }

                // Tiêu hao Linh Lực & Cập nhật thời gian hồi chiêu
                dataManager.setLinhLuc(player, linhLuc - 300);
                thuanDiCooldowns.put(player.getUniqueId(), currentTime);

                // --- XỬ LÝ DỊCH CHUYỂN ---
                Location loc = player.getLocation();
                Vector dir = loc.getDirection().normalize();
                Location target = loc.clone();

                // Quét 15 block phía trước để tìm khoảng trống an toàn (tránh kẹt vào tường)
                for (int i = 1; i <= 15; i++) {
                    Location checkLoc = loc.clone().add(dir.clone().multiply(i));
                    // Kiểm tra xem block ở vị trí chân và đầu có phải là khối cứng (solid) không
                    if (checkLoc.getBlock().getType().isSolid() || checkLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                        break; // Gặp tường -> Dừng lại trước mặt tường
                    }
                    target = checkLoc;
                }

                // Giữ nguyên góc nhìn của người chơi sau khi dịch chuyển
                target.setYaw(loc.getYaw());
                target.setPitch(loc.getPitch());

                // Hiệu ứng dịch chuyển tại vị trí cũ
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.5, 1, 0.5, 0.1);

                // Dịch chuyển
                player.teleport(target);

                // Hiệu ứng và âm thanh tại vị trí mới
                player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                player.getWorld().spawnParticle(Particle.PORTAL, target, 50, 0.5, 1, 0.5, 0.2);
                player.sendMessage("§b§l[Thần Thông] §fThuấn Di!");
            }
        }
    }

    // ==========================================
    // KHÓA DI CHUYỂN KHI ĐANG TỌA THIỀN
    // ==========================================
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (dataManager.isDangTuLuyen(player)) {
            Location to = event.getTo();
            // FIX GIẬT: Chỉ chặn di chuyển ngang (X/Z). Cho phép Y thay đổi
            // vì LEVITATION liên tục thay đổi Y — nếu chặn Y sẽ gây giật liên tục.
            if (to != null && (event.getFrom().getX() != to.getX() ||
                    event.getFrom().getZ() != to.getZ())) {
                // Giữ nguyên X/Z từ vị trí cũ, nhưng cho phép Y theo vị trí mới
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
        // Đã fix cảnh báo: Dùng Pattern Variable (Java 16+) thay vì ép kiểu thủ công
        if (event.getEntity() instanceof Player player) {

            if (dataManager.isDangTuLuyen(player)) {
                dataManager.setTuLuyenMode(player, false);
                player.sendMessage("§c§l[!] §fBị tấn công! Quá trình tọa thiền bị tẩu hỏa nhập ma, cưỡng chế dừng lại!");
                player.removePotionEffect(PotionEffectType.LEVITATION);
            }
        }
    }

    // ==========================================
    // NHIỆM VỤ HÀNG NGÀY: XÂY DỰNG (ĐẶT KHỐI)
    // ==========================================
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getNhiemVuManager() != null) {
            // Kiểm tra xem có đang đặt trong Tông Môn không
            if (plugin.getTongMonManager() != null) {
                UUID owner = plugin.getTongMonManager().checkKhongChoPhepTuongTac(event.getBlock().getLocation(), player);
                if (owner == null) {
                    // Cứ đặt khối và không bị chặn -> Tăng tiến độ (có thể đang ở Tông môn của mình)
                    plugin.getNhiemVuManager().addProgress(player, tutien.quest.NhiemVuManager.NhiemVu.XAY_DUNG, 1);
                }
            }
        }
    }

    // ==========================================
    // NHIỆM VỤ HÀNG NGÀY: ĐÁNH QUÁI (GIẾT QUÁI)
    // ==========================================
    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            if (!(event.getEntity() instanceof Player)) {
                if (plugin.getNhiemVuManager() != null) {
                    plugin.getNhiemVuManager().addProgress(player, tutien.quest.NhiemVuManager.NhiemVu.DANH_QUAI, 1);
                }
            }
        }
    }
}