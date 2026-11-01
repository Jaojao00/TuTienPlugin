package tutien.alchemy.service;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import tutien.alchemy.manager.DanDuocManager;
import tutien.alchemy.model.DanDocData;
import tutien.alchemy.model.ToxicityStatus;
import tutien.core.PlayerDataManager;

/**
 * Service quản lý độc tính Đan Độc tích lũy của tu sĩ.
 */
public class DanDocService {

    private final PlayerDataManager dataManager;
    private final DanDuocManager danDuocManager;

    public DanDocService(PlayerDataManager dataManager, DanDuocManager danDuocManager) {
        this.dataManager = dataManager;
        this.danDuocManager = danDuocManager;
    }

    public double getDanDoc(Player player) {
        if (player == null) return 0.0;
        return getDanDoc(player.getUniqueId());
    }

    public double getDanDoc(java.util.UUID uuid) {
        if (uuid == null) return 0.0;
        DanDocData data = dataManager.getDanDocData(uuid);
        return data != null ? data.getDanDoc() : 0.0;
    }

    public ToxicityStatus getStatus(Player player) {
        if (player == null) return ToxicityStatus.THANH_KHIET;
        return getStatus(player.getUniqueId());
    }

    public ToxicityStatus getStatus(java.util.UUID uuid) {
        return ToxicityStatus.fromDanDoc(getDanDoc(uuid));
    }

    public double addDanDoc(java.util.UUID uuid, double gain) {
        if (uuid == null || gain <= 0) return getDanDoc(uuid);

        DanDocData data = dataManager.getDanDocData(uuid);
        if (data == null) return 0.0;

        double oldVal = data.getDanDoc();
        double maxToxicity = danDuocManager != null ? danDuocManager.getMaxToxicity() : 1000.0;
        double newVal = Math.min(maxToxicity, oldVal + gain);
        data.setDanDoc(newVal);
        return newVal;
    }

    public double reduceDanDoc(java.util.UUID uuid, double amount) {
        if (uuid == null || amount <= 0) return getDanDoc(uuid);

        DanDocData data = dataManager.getDanDocData(uuid);
        if (data == null) return 0.0;

        double oldVal = data.getDanDoc();
        double newVal = Math.max(0.0, oldVal - amount);
        data.setDanDoc(newVal);
        return newVal;
    }

    /**
     * Tích lũy Đan Độc: D_new = min(D_max, D_current + D_gain).
     * Phát hiện bước qua ngưỡng độc để gửi cảnh báo 1 lần duy nhất (không spam theo tick).
     */
    public double addDanDoc(Player player, double gain) {
        if (player == null || gain <= 0) return getDanDoc(player);

        DanDocData data = dataManager.getDanDocData(player);
        if (data == null) return 0.0;

        double oldVal = data.getDanDoc();
        ToxicityStatus oldStatus = ToxicityStatus.fromDanDoc(oldVal);

        double maxToxicity = danDuocManager != null ? danDuocManager.getMaxToxicity() : 1000.0;
        double newVal = Math.min(maxToxicity, oldVal + gain);
        data.setDanDoc(newVal);

        ToxicityStatus newStatus = ToxicityStatus.fromDanDoc(newVal);

        // Kích hoạt thông báo khi bước qua ngưỡng độc mới nặng hơn
        if (newStatus.ordinal() > oldStatus.ordinal()) {
            notifyThresholdCrossed(player, oldStatus, newStatus);
        }

        return newVal;
    }

    /**
     * Giảm Đan Độc khi thanh lọc: D_new = max(0, D_current - amount).
     */
    public double reduceDanDoc(Player player, double amount) {
        if (player == null || amount <= 0) return getDanDoc(player);

        DanDocData data = dataManager.getDanDocData(player);
        if (data == null) return 0.0;

        double oldVal = data.getDanDoc();
        ToxicityStatus oldStatus = ToxicityStatus.fromDanDoc(oldVal);

        double newVal = Math.max(0.0, oldVal - amount);
        data.setDanDoc(newVal);

        ToxicityStatus newStatus = ToxicityStatus.fromDanDoc(newVal);

        // Thông báo phục hồi kinh mạch
        if (newStatus.ordinal() < oldStatus.ordinal()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a✦ [Thanh Lọc] Kinh mạch được tịnh hóa! Đan độc giảm về trạng thái: " + newStatus.getDisplayName()));
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            } catch (Exception ignored) {}
        }

        return newVal;
    }

    /**
     * Lấy hệ số hiệu suất tu luyện bị suy giảm bởi Đan Độc.
     */
    public double getCultivationEfficiency(Player player) {
        return getStatus(player).getCultivationEfficiency();
    }

    private void notifyThresholdCrossed(Player player, ToxicityStatus oldStatus, ToxicityStatus newStatus) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c⚠ &l[ĐAN ĐỘC CẢNH BÁO] &fKinh mạch của ngươi đã chuyển sang: " + newStatus.getDisplayName()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7" + newStatus.getDescription()));

        try {
            if (newStatus == ToxicityStatus.DOC_PHAT) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
            } else if (newStatus == ToxicityStatus.DOC_NANG) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        } catch (Exception ignored) {}
    }
}
