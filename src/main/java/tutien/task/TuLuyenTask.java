package tutien.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.LinhCan;
import tutien.core.PlayerDataManager;

/**
 * Task chạy mỗi 1 giây (20 ticks).
 * SPRINT 1+2: Tối ưu, thêm Song Tu, Pháp Tu Linh Lực, Bế Quan điểm.
 */
public class TuLuyenTask extends BukkitRunnable {

    private final PlayerDataManager dataManager;
    private int tickCount = 0;

    public TuLuyenTask(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void run() {
        tickCount++;

        for (Player player : Bukkit.getOnlinePlayers()) {

            boolean isMeditating = dataManager.isDangTuLuyen(player);

            // === BẾ QUAN ĐIỂM & NHIỆM VỤ: mỗi phút (60 ticks) cộng 1 điểm nếu đang tọa thiền ===
            if (isMeditating && tickCount % 60 == 0) {
                dataManager.addBeQuanDiem(player, 1);
                tutien.core.TuTienPlugin plugin = tutien.core.TuTienPlugin.getPlugin(tutien.core.TuTienPlugin.class);
                if (plugin.getNhiemVuManager() != null) {
                    plugin.getNhiemVuManager().addProgress(player, tutien.quest.NhiemVuManager.NhiemVu.TU_LUYEN, 1);
                }
            }

            if (!isMeditating) continue;

            // =====================
            // XÁC ĐỊNH CHỈ SỐ
            // =====================
            LinhCan linhCan = dataManager.getLinhCan(player);
            CanhGioi cg = dataManager.getCanhGioi(player);
            HeTuLuyen he = dataManager.getHeTuLuyen(player);

            // VIP Multiplier
            double vipMultiplier = 1.0;
            String vipPrefix = "";
            if (player.hasPermission("tutien.vip.tiennhan"))      { vipMultiplier = 3.0; vipPrefix = "§d[Tiên Nhân] "; }
            else if (player.hasPermission("tutien.vip.hanhchu")) { vipMultiplier = 2.5; vipPrefix = "§6[Hành Chủ] "; }
            else if (player.hasPermission("tutien.vip.giachu"))  { vipMultiplier = 2.0; vipPrefix = "§e[Gia Chủ] "; }
            else if (player.hasPermission("tutien.vip.hieuchu")) { vipMultiplier = 1.5; vipPrefix = "§a[Hiếu Chủ] "; }

            // =====================
            // SONG TU: +15% Tu Vi nếu gần đạo lữ (cũng đang tọa thiền)
            // =====================
            double songTuBonus = 1.0;
            if (he == HeTuLuyen.SONG_TU) {
                long nearbyMeditating = player.getNearbyEntities(10, 10, 10).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .filter(other -> dataManager.isDangTuLuyen(other))
                    .count();
                if (nearbyMeditating > 0) {
                    songTuBonus = 1.15;
                }
            }

            // =====================
            // TÍNH TU VI & LINH LỰC
            // =====================
            int baseTuVi = 2;
            int finalTuVi = (int) (baseTuVi * linhCan.getTocDo() * vipMultiplier * songTuBonus);

            int baseLinhLuc = 5;
            int heSoCanhGioi = cg.ordinal() + 1;
            int linhLucNhanDuoc = (int) (baseLinhLuc * linhCan.getTocDo() * heSoCanhGioi * vipMultiplier);

            // Pháp Tu: giới hạn Linh Lực cao hơn 30%
            int maxLinhLuc = dataManager.getMaxLinhLucEffective(player);

            // =====================
            // CẬP NHẬT
            // =====================
            dataManager.setTuVi(player, dataManager.getTuVi(player) + finalTuVi);

            int newLinhLuc = Math.min(maxLinhLuc, dataManager.getLinhLuc(player) + linhLucNhanDuoc);
            dataManager.setLinhLuc(player, newLinhLuc);

            // =====================
            // HIỂN THỊ ACTION BAR
            // =====================
            String songTuIcon = (songTuBonus > 1.0) ? "§d♥ " : "";
            String actionBarMsg = vipPrefix + songTuIcon + "§d§l[TỌA THIỀN] §fTu vi: §e+" + finalTuVi
                + " §f| Linh lực: §b+" + linhLucNhanDuoc
                + " §f| Bế quan: §6" + dataManager.getBeQuanDiem(player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMsg));

            // Hiệu ứng hạt (không spam quá nhiều)
            if (tickCount % 2 == 0) { // Mỗi 2 giây thay vì mỗi giây
                Location loc = player.getLocation().add(0, 1, 0);
                int particleCount = (vipMultiplier > 1.0) ? 15 : 6;
                player.getWorld().spawnParticle(Particle.ENCHANT, loc, particleCount, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}