package tutien.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.core.CanhGioi;
import tutien.core.LinhCan;
import tutien.core.PlayerDataManager;

/**
 * Task này chạy mỗi 1 giây (20 ticks).
 * Nhiệm vụ: Tăng Tu Vi và Linh Lực cho những ai đang Tọa Thiền.
 * Đã cập nhật: Tích hợp Đặc Quyền Địa Vị (VIP) giúp x1.5 -> x3 tốc độ tu luyện.
 */
public class TuLuyenTask extends BukkitRunnable {

    private final PlayerDataManager dataManager;

    public TuLuyenTask(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void run() {
        // Lặp qua tất cả người chơi đang online trên server
        for (Player player : Bukkit.getOnlinePlayers()) {

            // Kiểm tra xem người chơi này có đang bật chế độ Tọa Thiền hay không
            if (dataManager.isDangTuLuyen(player)) {

                // ===================================================
                // BƯỚC 1: XÁC ĐỊNH CHỈ SỐ CỦA NGƯỜI CHƠI VÀ ĐẶC QUYỀN VIP
                // ===================================================

                LinhCan linhCan = dataManager.getLinhCan(player);
                CanhGioi cg = dataManager.getCanhGioi(player);

                // Kiểm tra Địa Vị (VIP) qua hệ thống quyền (Permissions của LuckPerms)
                double vipMultiplier = 1.0;
                String vipPrefix = "";

                if (player.hasPermission("tutien.vip.tiennhan")) {
                    vipMultiplier = 3.0; // Tiên Nhân x3 Tốc độ
                    vipPrefix = "§d[Tiên Nhân] ";
                } else if (player.hasPermission("tutien.vip.hanhchu")) {
                    vipMultiplier = 2.5; // Hành Chủ x2.5 Tốc độ
                    vipPrefix = "§6[Hành Chủ] ";
                } else if (player.hasPermission("tutien.vip.giachu")) {
                    vipMultiplier = 2.0; // Gia Chủ x2 Tốc độ
                    vipPrefix = "§e[Gia Chủ] ";
                } else if (player.hasPermission("tutien.vip.hieuchu")) {
                    vipMultiplier = 1.5; // Hiếu Chủ x1.5 Tốc độ
                    vipPrefix = "§a[Hiếu Chủ] ";
                }

                // ===================================================
                // BƯỚC 2: TÍNH TOÁN TU VI VÀ LINH LỰC NHẬN ĐƯỢC
                // ===================================================

                // 1. Tính Tu Vi: Cơ bản 2 * Hệ số Linh Căn * Hệ số VIP
                int baseTuVi = 2;
                int finalTuVi = (int) (baseTuVi * linhCan.getTocDo() * vipMultiplier);

                // 2. Tính Linh Lực: Cơ bản 5 * Hệ số Linh Căn * Bậc Cảnh Giới * Hệ số VIP
                int baseLinhLuc = 5;
                int heSoCanhGioi = cg.ordinal() + 1;
                int linhLucNhanDuoc = (int) (baseLinhLuc * linhCan.getTocDo() * heSoCanhGioi * vipMultiplier);

                // ===================================================
                // BƯỚC 3: CẬP NHẬT CHỈ SỐ VÀO HỆ THỐNG
                // ===================================================

                int currentTuVi = dataManager.getTuVi(player);
                int currentLinhLuc = dataManager.getLinhLuc(player);
                int maxLinhLuc = cg.getMaxLinhLuc();

                // Lưu Tu Vi mới
                dataManager.setTuVi(player, currentTuVi + finalTuVi);

                // Lưu Linh Lực mới (Đảm bảo không vượt quá Max)
                int newLinhLuc = currentLinhLuc + linhLucNhanDuoc;
                if (newLinhLuc > maxLinhLuc) {
                    newLinhLuc = maxLinhLuc;
                }
                dataManager.setLinhLuc(player, newLinhLuc);

                // ===================================================
                // BƯỚC 4: HIỂN THỊ THÔNG BÁO VÀ HIỆU ỨNG
                // ===================================================

                // Gửi thông báo chữ nhảy liên tục (Thêm Tiền tố VIP nếu có)
                String actionBarMsg = vipPrefix + "§d§l[TỌA THIỀN] §fTu vi: §e+" + finalTuVi + " §f| Linh lực: §b+" + linhLucNhanDuoc;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMsg));

                // Triệu hồi các hạt linh khí (Thêm nhiều hạt hơn nếu là VIP)
                Location loc = player.getLocation().add(0, 1, 0);
                int particleCount = (vipMultiplier > 1.0) ? 25 : 10;
                player.getWorld().spawnParticle(Particle.ENCHANT, loc, particleCount, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}