package tutien.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.combat.StatsManager;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.LinhCan;
import tutien.core.MeditationType;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.cultivation.service.EpiphanyService;
import tutien.cultivation.service.MasteryService;
import tutien.quest.NhiemVuManager;
import tutien.tutien.gui.GachaGUI;
import tutien.tutien.gui.LuyenDanGUI;

import java.util.Set;

/**
 * Task chạy mỗi 1 giây (20 ticks).
 * Phase 2.4 - Phân nhánh độc lập 2 chế độ Tọa Thiền:
 *   1. TU_LUYEN: Hấp thu linh khí, tăng Tu Vi & Linh Lực (áp dụng QiAbsorbRate, VIP, Song Tu, Linh Thú).
 *   2. THAM_NGO: Tham ngộ khẩu quyết công pháp (ComprehensionService), không tăng Tu Vi, Action Bar riêng biệt.
 *
 * Đồng thời loại bỏ hoàn toàn các phép so sánh ordinal() không an toàn.
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

            // Tích lũy thời gian online hợp lệ cho suy giảm Kháng Thuốc (mỗi 1s)
            try {
                TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
                if (plugin.getDrugResistanceService() != null) {
                    plugin.getDrugResistanceService().tickOnlineDecay(player);
                }
            } catch (Exception ignored) {}

            // === TỰ ĐỘNG LUYỆN ĐAN (mỗi 5 giây) ===
            if (tickCount % 5 == 0 && dataManager.isAutoLuyenDan(player)) {
                String lastPillKey = dataManager.getLastSelectedPill(player);
                LuyenDanGUI.DanDuocData pill = LuyenDanGUI.findPillByKey(lastPillKey);
                if (pill != null) {
                    LuyenDanGUI.craftPillBatch(player, dataManager, pill, 1, true);
                }
            }

            MeditationType meditationType = dataManager.getMeditationType(player);
            if (meditationType == MeditationType.NONE) {
                continue;
            }

            CanhGioi cg = dataManager.getCanhGioi(player);
            if (cg == CanhGioi.PHAM_NHAN) {
                dataManager.setMeditationType(player, MeditationType.NONE);
                player.sendMessage("§c§l[!] §fNgươi là §7Phàm Nhân§f chưa Khai Mạch Dẫn Khí! Hãy mở menu §e/tutien nhapdao §fđể tiến hành Tôi Thể, Tẩy Tủy và Nhập Đạo!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                continue;
            }

            // =========================================================
            // PHÂN NHÁNH 1: TỌA THIỀN THAM NGỘ KHẨU QUYẾT (THAM_NGO)
            // =========================================================
            if (meditationType == MeditationType.THAM_NGO) {
                TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
                if (plugin.getCongPhapManager() == null) {
                    dataManager.setMeditationType(player, MeditationType.NONE);
                    continue;
                }

                PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);
                String activePrimary = cultData.getActivePrimary();
                if (activePrimary == null || activePrimary.equalsIgnoreCase("CHUA_CO")) {
                    dataManager.setMeditationType(player, MeditationType.NONE);
                    player.sendMessage("§c§l[!] §fNgươi chưa kích hoạt Công Pháp Chủ Tu để tham ngộ!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                    continue;
                }

                CongPhap cp = plugin.getCongPhapManager().getCongPhap(activePrimary);
                if (cp == null) {
                    dataManager.setMeditationType(player, MeditationType.NONE);
                    player.sendMessage("§c§l[!] §fCông pháp chủ tu không tồn tại trong danh mục!");
                    continue;
                }

                CongPhapProgress prog = cultData.getOrCreateProgress(activePrimary);
                int targetChapter = prog.getTargetChapter();

                if (targetChapter > cp.getTotalChapters()) {
                    dataManager.setMeditationType(player, MeditationType.NONE);
                    player.sendMessage("§a§l[!] §fCông pháp " + cp.getDisplayName() + " §fđã đạt tới cảnh giới Viên Mãn toàn bộ các tầng!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    continue;
                }

                // Thực hiện 1 nhịp tham ngộ qua ComprehensionService (đồng hồ server, chống lag/AFK)
                plugin.getCongPhapManager().getComprehensionService().processComprehensionTick(player, dataManager, activePrimary);

                // Tích lũy MasteryExp qua tọa thiền tham ngộ (mỗi 10s = 10 ticks)
                if (tickCount % 10 == 0) {
                    plugin.getCongPhapManager().getMasteryService().addMasteryExp(
                            player, dataManager, activePrimary, MasteryService.ActivitySource.MEDITATION);
                }

                // Kiểm tra cơ duyên Đốn Ngộ tại Thiên Địa Linh Địa (mỗi 60s)
                if (tickCount % 60 == 0) {
                    plugin.getCongPhapManager().getEpiphanyService().triggerEpiphanyCheck(
                            player, dataManager, EpiphanyService.EpiphanyEvent.SACRED_SITE);
                }

                // Action Bar tham ngộ chuyên biệt: TUYỆT ĐỐI KHÔNG CỘNG TU VI
                String actionBarMsg = "§b§l✦ THAM NGỘ §f" + cp.getDisplayName()
                        + " §7| Tầng " + targetChapter
                        + " §7| Tiến độ: §e" + String.format("%.1f%%", prog.getComprehensionProgress())
                        + " §7| Đạo Ngộ: §6" + dataManager.getDaoNgoPoints(player) + " điểm";
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMsg));

                // Hiệu ứng linh văn phù chú xoay quanh người
                renderComprehensionEffects(player, cg);
                continue;
            }

            // =========================================================
            // PHÂN NHÁNH 2: TỌA THIỀN TU LUYỆN NĂNG LƯỢNG (TU_LUYEN)
            // =========================================================
            if (meditationType == MeditationType.TU_LUYEN) {

                // === BẾ QUAN ĐIỂM & NHIỆM VỤ: mỗi phút (60 ticks) cộng 1 điểm chỉ khi Tu Luyện ===
                if (tickCount % 60 == 0) {
                    dataManager.addBeQuanDiem(player, 1);
                    TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
                    if (plugin.getNhiemVuManager() != null) {
                        plugin.getNhiemVuManager().addProgress(player, NhiemVuManager.NhiemVu.TU_LUYEN, 1);
                    }
                }

                LinhCan linhCan = dataManager.getLinhCan(player);
                HeTuLuyen he = dataManager.getHeTuLuyen(player);

                // VIP Multiplier
                double vipMultiplier = 1.0;
                String vipPrefix = "";
                if (player.hasPermission("tutien.vip.tiennhan"))      { vipMultiplier = 3.0; vipPrefix = "§d[Tiên Nhân] "; }
                else if (player.hasPermission("tutien.vip.hanhchu")) { vipMultiplier = 2.5; vipPrefix = "§6[Hành Chủ] "; }
                else if (player.hasPermission("tutien.vip.giachu"))  { vipMultiplier = 2.0; vipPrefix = "§e[Gia Chủ] "; }
                else if (player.hasPermission("tutien.vip.hieuchu")) { vipMultiplier = 1.5; vipPrefix = "§a[Hiếu Chủ] "; }

                // Song Tu Bonus
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

                // Đạo Lữ Song Tu (Phase 6)
                try {
                    TuTienPlugin pl = TuTienPlugin.getPlugin(TuTienPlugin.class);
                    if (pl.getDaoLuService() != null) {
                        double dlMult = pl.getDaoLuService().getSongTuTuViMultiplier(player);
                        if (dlMult > 1.0) {
                            songTuBonus = Math.max(songTuBonus, dlMult);
                        }
                    }
                } catch (Throwable ignored) {}

                // Linh Thú & Phụ Kiện Bonus
                double petBonus = 1.0;
                Set<GachaGUI.LinhThu> pets = StatsManager.getActivePets(player);
                if (pets.contains(GachaGUI.LinhThu.THIEN_PHUONG)) {
                    petBonus += 0.50;
                } else if (pets.contains(GachaGUI.LinhThu.THANH_LANG)) {
                    petBonus += 0.10;
                }

                int accessoryTier = StatsManager.getBestAccessoryTier(player);
                double accessoryBonus = 1.0 + (accessoryTier * 0.03);

                // Hệ số cộng hưởng từ Công Pháp Chủ Tu & Phụ Tu (QiAbsorbRate)
                double cpQiBonus = 1.0;
                PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);
                TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
                if (plugin.getCongPhapManager() != null) {
                    CongPhap primaryCp = plugin.getCongPhapManager().getCongPhap(cultData.getActivePrimary());
                    if (primaryCp != null) {
                        cpQiBonus += (primaryCp.getQiAbsorbRate() - 1.0);
                    }
                    for (String auxId : cultData.getActiveAuxiliary()) {
                        CongPhap auxCp = plugin.getCongPhapManager().getCongPhap(auxId);
                        if (auxCp != null) {
                            cpQiBonus += (auxCp.getQiAbsorbRate() - 1.0) * 0.35; // Thừa hưởng 35% từ phụ tu
                        }
                    }
                }
                if (cultData.getAdaptation().isActive()) {
                    cpQiBonus *= (1.0 - cultData.getAdaptation().getPenaltyRatio());
                }
                cpQiBonus = Math.max(0.4, cpQiBonus);

                // Hệ số ảnh hưởng từ Đan Độc (Toxicity Penalty: 1.0 -> 0.95 -> 0.85 -> 0.70 -> 0.50)
                double danDocEfficiency = 1.0;
                if (plugin.getDanDocService() != null) {
                    danDocEfficiency = plugin.getDanDocService().getCultivationEfficiency(player);
                }

                // Hệ số gia tốc từ Động Phủ & Linh Địa (Phase 5)
                double dongPhuTuViBonus = 1.0;
                double dongPhuManaRegen = 0.0;
                if (plugin.getDongPhuManager() != null && plugin.getDongPhuManager().isInsideDongPhu(player)) {
                    dongPhuTuViBonus = plugin.getDongPhuManager().getTuViMultiplier(player);
                    dongPhuManaRegen = plugin.getDongPhuManager().getManaRegenBonus(player);
                }

                // Tính Tu Vi & Linh Lực (đã nhân hệ số suy giảm do Đan Độc & gia tốc Động Phủ)
                int baseTuVi = 2;
                int finalTuVi = Math.max(1, (int) Math.round(
                        baseTuVi * linhCan.getTocDo() * vipMultiplier * songTuBonus * petBonus * accessoryBonus * cpQiBonus * danDocEfficiency * dongPhuTuViBonus));

                int baseLinhLuc = 5;
                int heSoCanhGioi = Math.max(1, cg.getTier() + 1);
                int linhLucNhanDuoc = (int) Math.round((baseLinhLuc * linhCan.getTocDo() * heSoCanhGioi * vipMultiplier) + dongPhuManaRegen);

                int maxLinhLuc = dataManager.getMaxLinhLucEffective(player);

                // Cập nhật
                dataManager.addTuVi(player, finalTuVi);

                int newLinhLuc = Math.min(maxLinhLuc, dataManager.getLinhLuc(player) + linhLucNhanDuoc);
                dataManager.setLinhLuc(player, newLinhLuc);

                // Action Bar
                String songTuIcon = (songTuBonus > 1.0) ? "§d♥ " : "";
                String petIcon = (petBonus > 1.0) ? "§b🐾 " : "";
                String docInfo = (danDocEfficiency < 1.0) ? " §c☠ -" + (int) Math.round((1.0 - danDocEfficiency) * 100) + "%" : "";
                String actionBarMsg = vipPrefix + songTuIcon + petIcon + "§d§l[TỌA THIỀN] §fTu vi: §e+" + finalTuVi
                        + " §f| Linh lực: §b+" + linhLucNhanDuoc
                        + docInfo
                        + " §f| Bế quan: §6" + dataManager.getBeQuanDiem(player);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMsg));

                // Hiệu ứng Tụ Linh Trận
                renderMeditationEffects(player, cg, vipMultiplier > 1.0);
            }
        }
    }

    /**
     * Hiệu ứng Tụ Linh Trận và Linh Khí xoay quanh người chơi khi Tọa Thiền Tu Luyện.
     * Sử dụng isAtLeast() an toàn thay vì ordinal().
     */
    private void renderMeditationEffects(Player player, CanhGioi cg, boolean isVip) {
        Location center = player.getLocation();

        // 1. Tụ Linh Trận (Vòng tròn bát quái xoay tròn dưới chân)
        double radius = 1.3;
        int points = 8;
        double baseAngle = (tickCount * 0.2);

        for (int i = 0; i < points; i++) {
            double angle = baseAngle + (i * (2 * Math.PI / points));
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location ringLoc = center.clone().add(x, 0.1, z);

            if (cg.isAtLeast(CanhGioi.KHUY_NIET)) {
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, ringLoc, 1, 0, 0.05, 0, 0.01);
            } else if (cg.isAtLeast(CanhGioi.KIM_DAN)) {
                player.getWorld().spawnParticle(Particle.END_ROD, ringLoc, 1, 0, 0.05, 0, 0.01);
            } else {
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, ringLoc, 1, 0, 0.03, 0, 0.01);
            }
        }

        // 2. Linh Khí Xoắn Ốc (Helix) cuộn từ chân lên quá đầu
        double height = ((tickCount * 0.4) % 2.4);
        double helixAngle = tickCount * 0.5;
        double hRadius = 0.6;

        Location p1 = center.clone().add(hRadius * Math.cos(helixAngle), height, hRadius * Math.sin(helixAngle));
        Location p2 = center.clone().add(hRadius * Math.cos(helixAngle + Math.PI), height, hRadius * Math.sin(helixAngle + Math.PI));

        player.getWorld().spawnParticle(Particle.ENCHANT, p1, 2, 0.05, 0.05, 0.05, 0.02);
        player.getWorld().spawnParticle(Particle.ENCHANT, p2, 2, 0.05, 0.05, 0.05, 0.02);

        // 3. Dị tượng hào quang theo cảnh giới
        if (cg.isAtLeast(CanhGioi.HOA_THAN)) {
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1.0, 0), 2, 0.2, 0.4, 0.2, 0.01);
        }
        if (cg.isAtLeast(CanhGioi.DAP_THIEN) && tickCount % 4 == 0) {
            player.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 2.2, 0), 1, 0, 0, 0, 0);
        }

        // Tiên âm du dương mỗi 12 giây
        if (tickCount % 12 == 0) {
            player.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.2f);
        }
    }

    /**
     * Hiệu ứng Linh Văn Phù Chú và Thần Niệm khi Tọa Thiền Tham Ngộ Khẩu Quyết.
     */
    private void renderComprehensionEffects(Player player, CanhGioi cg) {
        Location center = player.getLocation();

        // 1. Linh văn phù chú xoay quanh thân thể
        double radius = 1.1;
        int points = 6;
        double angleOffset = (tickCount * 0.15);

        for (int i = 0; i < points; i++) {
            double angle = angleOffset + (i * (2 * Math.PI / points));
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            double yOffset = 0.8 + 0.25 * Math.sin(tickCount * 0.25 + i);
            Location glyphLoc = center.clone().add(x, yOffset, z);
            player.getWorld().spawnParticle(Particle.ENCHANT, glyphLoc, 2, 0.02, 0.02, 0.02, 0.01);
        }

        // 2. Thần niệm nhập định
        if (tickCount % 2 == 0) {
            player.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(0, 1.1, 0), 3, 0.15, 0.25, 0.15, 0.02);
        }

        // 3. Hào quang bản nguyên nếu cảnh giới cao
        if (cg.isAtLeast(CanhGioi.KIM_DAN)) {
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.1, 0), 1, 0.2, 0.05, 0.2, 0.01);
        }

        // Tiên âm thấu triệt mỗi 15 giây
        if (tickCount % 15 == 0) {
            player.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.4f);
        }
    }
}
