package tutien.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.combat.StatsManager;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.LinhCan;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.quest.NhiemVuManager;
import tutien.tutien.gui.GachaGUI;
import tutien.tutien.gui.LuyenDanGUI;

import java.util.Set;

/**
 * Task chạy mỗi 1 giây (20 ticks).
 * v2.2 Skyblock Edition:
 *   - Tích hợp Song Tu (+15% Tu Vi), Pháp Tu (+30% Linh Lực), Điểm Bế Quan (1 phút/điểm).
 *   - Tích hợp Linh Thú AFK Bonus: Thanh Lang Vũ (+10% Tu Vi), Thiên Phượng (+50% Tu Vi).
 *   - Tích hợp Phụ Kiện Hộ Mệnh (+3% Tu Vi mỗi bậc).
 *   - Tích hợp Tự Động Luyện Đan (Auto-Alchemy): Mỗi 5 giây tự động luyện 1 mẻ của loại đan vừa chọn!
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

            // === TỰ ĐỘNG LUYỆN ĐAN (mỗi 5 giây) ===
            if (tickCount % 5 == 0 && dataManager.isAutoLuyenDan(player)) {
                String lastPillKey = dataManager.getLastSelectedPill(player);
                LuyenDanGUI.DanDuocData pill = LuyenDanGUI.findPillByKey(lastPillKey);
                if (pill != null) {
                    LuyenDanGUI.craftPillBatch(player, dataManager, pill, 1, true);
                }
            }

            boolean isMeditating = dataManager.isDangTuLuyen(player);

            // === BẾ QUAN ĐIỂM & NHIỆM VỤ: mỗi phút (60 ticks) cộng 1 điểm nếu đang tọa thiền ===
            if (isMeditating && tickCount % 60 == 0) {
                dataManager.addBeQuanDiem(player, 1);
                TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
                if (plugin.getNhiemVuManager() != null) {
                    plugin.getNhiemVuManager().addProgress(player, NhiemVuManager.NhiemVu.TU_LUYEN, 1);
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
                    .filter(other -> other != player && dataManager.isDangTuLuyen(other))
                    .count();
                if (nearbyMeditating > 0) {
                    songTuBonus = 1.15;
                }
            }

            // =====================
            // LINH THÚ & PHỤ KIỆN BONUS AFK
            // =====================
            double petBonus = 1.0;
            Set<GachaGUI.LinhThu> pets = StatsManager.getActivePets(player);
            if (pets.contains(GachaGUI.LinhThu.THIEN_PHUONG)) {
                petBonus += 0.50;
            } else if (pets.contains(GachaGUI.LinhThu.THANH_LANG)) {
                petBonus += 0.10;
            }

            int accessoryTier = StatsManager.getBestAccessoryTier(player);
            double accessoryBonus = 1.0 + (accessoryTier * 0.03);

            // =====================
            // TÍNH TU VI & LINH LỰC
            // =====================
            int baseTuVi = 2;
            int finalTuVi = Math.max(1, (int) Math.round(
                    baseTuVi * linhCan.getTocDo() * vipMultiplier * songTuBonus * petBonus * accessoryBonus));

            int baseLinhLuc = 5;
            int heSoCanhGioi = cg.ordinal() + 1;
            int linhLucNhanDuoc = (int) Math.round(baseLinhLuc * linhCan.getTocDo() * heSoCanhGioi * vipMultiplier);

            int maxLinhLuc = dataManager.getMaxLinhLucEffective(player);

            // =====================
            // CẬP NHẬT
            // =====================
            dataManager.addTuVi(player, finalTuVi);

            int newLinhLuc = Math.min(maxLinhLuc, dataManager.getLinhLuc(player) + linhLucNhanDuoc);
            dataManager.setLinhLuc(player, newLinhLuc);

            // =====================
            // HIỂN THỊ ACTION BAR
            // =====================
            String songTuIcon = (songTuBonus > 1.0) ? "§d♥ " : "";
            String petIcon = (petBonus > 1.0) ? "§b🐾 " : "";
            String actionBarMsg = vipPrefix + songTuIcon + petIcon + "§d§l[TỌA THIỀN] §fTu vi: §e+" + finalTuVi
                + " §f| Linh lực: §b+" + linhLucNhanDuoc
                + " §f| Bế quan: §6" + dataManager.getBeQuanDiem(player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMsg));

            // Hiệu ứng hạt mỗi 2 giây
            if (tickCount % 2 == 0) {
                Location loc = player.getLocation().add(0, 1, 0);
                int particleCount = (vipMultiplier > 1.0) ? 15 : 6;
                player.getWorld().spawnParticle(Particle.ENCHANT, loc, particleCount, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}
