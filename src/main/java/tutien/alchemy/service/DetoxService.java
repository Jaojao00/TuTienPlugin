package tutien.alchemy.service;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tutien.alchemy.integration.WorldGuardDetoxHook;
import tutien.alchemy.manager.DanDuocManager;
import tutien.alchemy.model.DanDuoc;
import tutien.alchemy.model.DetoxResult;
import tutien.alchemy.model.DetoxSession;
import tutien.core.ItemKeys;
import tutien.core.MeditationType;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý toàn diện 3 phương pháp Thanh Lọc và Hóa Độc (Phase 3.2).
 */
public class DetoxService {

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final DanDocService danDocService;
    private final DanDuocManager danDuocManager;

    // Quản lý các phiên Bế Quan Hóa Độc đang diễn ra trong RAM
    private final Map<UUID, DetoxSession> activeSessions = new ConcurrentHashMap<>();

    // Cooldown Linh Tuyền (UUID -> timestamp ms)
    private final Map<UUID, Long> springCooldowns = new ConcurrentHashMap<>();

    // Cấu hình từ detox.yml
    private boolean pillEnabled = true;
    private double pillDefaultCleansed = 50.0;

    private boolean springEnabled = true;
    private String springRegionId = "linh-tuyen-tay-tran";
    private double springDetoxPerMinute = 15.0;
    private int springCooldownSeconds = 300;

    private boolean meditationEnabled = true;
    private double meditationDetoxPerMinute = 6.0;
    private double maxTickGapSeconds = 2.0;
    private boolean cancelOnMove = true;
    private boolean cancelOnDamage = true;
    private boolean cancelOnDeath = true;
    private boolean cancelOnTeleport = true;
    private boolean cancelOnQuit = true;

    public DetoxService(TuTienPlugin plugin, PlayerDataManager dataManager,
                        DanDocService danDocService, DanDuocManager danDuocManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.danDocService = danDocService;
        this.danDuocManager = danDuocManager;
        loadConfig();
    }

    public synchronized void loadConfig() {
        if (plugin == null) return;
        File file = new File(plugin.getDataFolder(), "detox.yml");
        if (!file.exists()) {
            plugin.saveResource("detox.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Thanh Đan
        this.pillEnabled = config.getBoolean("detox.thanh-dan.enabled", config.getBoolean("detox.pill.enabled", true));
        this.pillDefaultCleansed = config.getDouble("detox.thanh-dan.default-cleansed", config.getDouble("detox.pill.default-cleansed", 50.0));

        // Linh Tuyền
        this.springEnabled = config.getBoolean("detox.linh-tuyen.enabled", config.getBoolean("detox.spirit-spring.enabled", true));
        this.springRegionId = config.getString("detox.linh-tuyen.region-name", config.getString("detox.spirit-spring.worldguard.region-id", "linh-tuyen-tay-tran"));
        this.springDetoxPerMinute = config.getDouble("detox.linh-tuyen.detox-per-minute", config.getDouble("detox.spirit-spring.detox-per-minute", 15.0));
        this.springCooldownSeconds = config.getInt("detox.linh-tuyen.cooldown-minutes", 5) * 60;

        // Bế Quan Hóa Độc
        this.meditationEnabled = config.getBoolean("detox.be-quan-hoa-doc.enabled", config.getBoolean("detox.meditation.enabled", true));
        this.meditationDetoxPerMinute = config.getDouble("detox.be-quan-hoa-doc.rate-per-minute", config.getDouble("detox.meditation.detox-per-minute", 6.0));
        this.maxTickGapSeconds = config.getDouble("detox.be-quan-hoa-doc.max-lag-buffer-minutes", 2.0) * 60.0;
        if (maxTickGapSeconds <= 0.0 || maxTickGapSeconds > 10.0) {
            maxTickGapSeconds = 2.0; // Mặc định 2 giây chống lag per-tick
        }
        this.cancelOnMove = config.getBoolean("detox.be-quan-hoa-doc.cancel-on-move", config.getBoolean("detox.meditation.cancel-on.move", true));
        this.cancelOnDamage = config.getBoolean("detox.be-quan-hoa-doc.cancel-on-damage", config.getBoolean("detox.meditation.cancel-on.damage", true));
        this.cancelOnDeath = config.getBoolean("detox.meditation.cancel-on.death", true);
        this.cancelOnTeleport = config.getBoolean("detox.meditation.cancel-on.teleport", true);
        this.cancelOnQuit = config.getBoolean("detox.meditation.cancel-on.quit", true);
    }

    // =========================================================
    // 1. THANH ĐAN GIẢI ĐỘC
    // =========================================================

    public synchronized DetoxResult purifyWithPill(Player player, ItemStack item) {
        if (!pillEnabled) {
            return DetoxResult.fail(DetoxResult.Status.DISABLED, danDocService.getDanDoc(player), "Phương pháp Thanh Đan tạm thời bị vô hiệu hóa.");
        }
        if (player == null || item == null) {
            return DetoxResult.fail(DetoxResult.Status.ERROR, 0.0, "Dữ liệu không hợp lệ.");
        }

        String pillId = ItemKeys.getPillId(item);
        if (pillId == null) {
            player.sendMessage(ChatColor.RED + "Vật phẩm này không phải Đan Dược hợp lệ!");
            return DetoxResult.fail(DetoxResult.Status.INVALID_ITEM, danDocService.getDanDoc(player), "Vật phẩm không hợp lệ.");
        }

        DanDuoc pill = danDuocManager != null ? danDuocManager.getDanDuoc(pillId) : null;
        if (pill == null && !pillId.contains("THANH_DAN") && !pillId.contains("HOA_DOC") && !pillId.contains("TAY_TUY")) {
            return DetoxResult.fail(DetoxResult.Status.INVALID_ITEM, danDocService.getDanDoc(player), "Không phải đan giải độc.");
        }

        double currentDanDoc = danDocService.getDanDoc(player);
        if (currentDanDoc <= 0.0) {
            player.sendMessage(ChatColor.GREEN + "Kinh mạch của ngươi đã hoàn toàn thanh khiết, không có đan độc cần hóa giải!");
            return DetoxResult.fail(DetoxResult.Status.NO_TOXICITY, 0.0, "Kinh mạch thanh khiết.");
        }

        // Lượng độc tố được thanh lọc (mặc định hoặc cấu hình)
        double cleansed = pillDefaultCleansed;
        if (pill != null && pill.getDanDocGain() < 0) {
            cleansed = Math.abs(pill.getDanDocGain());
        }

        // Trừ vật phẩm nguyên tử
        int origAmount = item.getAmount();
        if (origAmount > 1) {
            item.setAmount(origAmount - 1);
        } else {
            item.setAmount(0);
        }

        double remaining = danDocService.reduceDanDoc(player, cleansed);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✦ [Thanh Đan Giải Độc] &fĐã tiêu hao &e1x " + (pill != null ? pill.getDisplayName() : pillId) +
                        "&f! Trừ &c-" + String.format("%.1f", cleansed) + " Đan Độc &7(Còn: " + String.format("%.1f", remaining) + "/1000)"));

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.5f);
        } catch (Exception ignored) {}

        return DetoxResult.success(cleansed, remaining, "Giải độc thành công.");
    }

    // =========================================================
    // 2. LINH TUYỀN TẨY TRẦN
    // =========================================================

    public synchronized DetoxResult purifyAtSpring(Player player, String regionId) {
        if (!springEnabled) {
            return DetoxResult.fail(DetoxResult.Status.DISABLED, danDocService.getDanDoc(player), "Linh Tuyền Tẩy Trần chưa được mở.");
        }
        if (player == null) return DetoxResult.fail(DetoxResult.Status.ERROR, 0.0, "Player null");

        String targetRegion = (regionId != null && !regionId.isEmpty()) ? regionId : springRegionId;
        if (!WorldGuardDetoxHook.isInRegion(player, targetRegion)) {
            player.sendMessage(ChatColor.RED + "Ngươi không ở trong khu vực Linh Tuyền Tẩy Trần (" + targetRegion + ")!");
            return DetoxResult.fail(DetoxResult.Status.NOT_IN_SPRING, danDocService.getDanDoc(player), "Không ở trong vùng Linh Tuyền.");
        }

        double currentDanDoc = danDocService.getDanDoc(player);
        if (currentDanDoc <= 0.0) {
            player.sendMessage(ChatColor.GREEN + "Kinh mạch của ngươi đã thanh khiết không còn tạp chất đan độc!");
            return DetoxResult.fail(DetoxResult.Status.NO_TOXICITY, 0.0, "Không có độc.");
        }

        long now = System.currentTimeMillis();
        long lastUse = springCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cdMillis = springCooldownSeconds * 1000L;
        if (now - lastUse < cdMillis) {
            long waitSec = (cdMillis - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.YELLOW + "Linh tuyền cần thời gian tụ khí, hãy đợi thêm " + waitSec + "s nữa!");
            return DetoxResult.fail(DetoxResult.Status.SPRING_COOLDOWN, currentDanDoc, "Đang trong thời gian hồi chiêu.");
        }

        springCooldowns.put(player.getUniqueId(), now);
        double cleansed = springDetoxPerMinute;
        double remaining = danDocService.reduceDanDoc(player, cleansed);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&b✦ [Linh Tuyền Tẩy Trần] &fDòng suối linh khí tịnh hóa kinh mạch! Đã trừ &c-" +
                        String.format("%.1f", cleansed) + " Đan Độc &7(Còn: " + String.format("%.1f", remaining) + "/1000)"));

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.2f);
        } catch (Exception ignored) {}

        return DetoxResult.success(cleansed, remaining, "Tẩy trần thành công.");
    }

    // =========================================================
    // 3. BẾ QUAN HÓA ĐỘC (SESSION RUNTIME ĐỘC LẬP)
    // =========================================================

    public synchronized DetoxResult startDetoxSession(Player player) {
        if (!meditationEnabled) {
            return DetoxResult.fail(DetoxResult.Status.DISABLED, danDocService.getDanDoc(player), "Bế Quan Hóa Độc chưa được mở.");
        }
        if (player == null) return DetoxResult.fail(DetoxResult.Status.ERROR, 0.0, "Player null");

        double currentDanDoc = danDocService.getDanDoc(player);
        if (currentDanDoc <= 0.0) {
            player.sendMessage(ChatColor.GREEN + "Kinh mạch thanh khiết, không có đan độc cần hóa giải!");
            return DetoxResult.fail(DetoxResult.Status.NO_TOXICITY, 0.0, "Kinh mạch không có đan độc.");
        }

        // KIỂM TRA ĐỒNG BỘ: Không đồng thời Tu Luyện hoặc Tham Ngộ
        MeditationType mType = dataManager.getMeditationType(player);
        if (mType == MeditationType.TU_LUYEN) {
            player.sendMessage(ChatColor.RED + "Đang trong trạng thái Tu Luyện Tu Vi, không thể bế quan hóa độc!");
            return DetoxResult.fail(DetoxResult.Status.MEDITATING_OTHER, currentDanDoc, "Đang tu luyện.");
        }
        if (mType == MeditationType.THAM_NGO) {
            player.sendMessage(ChatColor.RED + "Đang trong trạng thái Tham Ngộ Khẩu Quyết, không thể bế quan hóa độc!");
            return DetoxResult.fail(DetoxResult.Status.MEDITATING_OTHER, currentDanDoc, "Đang tham ngộ.");
        }

        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Ngươi đã đang trong phiên Bế Quan Hóa Độc rồi!");
            return DetoxResult.fail(DetoxResult.Status.ALREADY_DETOXING, currentDanDoc, "Đã có phiên hóa độc.");
        }

        // Căn chỉnh tọa độ giữa block để tĩnh tọa
        Location loc = player.getLocation();
        loc.setX(loc.getBlockX() + 0.5);
        loc.setZ(loc.getBlockZ() + 0.5);
        player.teleport(loc);

        DetoxSession session = new DetoxSession(player.getUniqueId(), loc);
        activeSessions.put(player.getUniqueId(), session);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&2&l[BẾ QUAN HÓA ĐỘC] &fNgươi đã tĩnh tọa ngưng thần, bắt đầu vận khí trục xuất độc tố kinh mạch..."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7(Hãy giữ nguyên vị trí, không di chuyển hoặc chịu sát thương để tránh tẩu hỏa gián đoạn)"));

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
        } catch (Exception ignored) {}

        return DetoxResult.success(0.0, currentDanDoc, "Bắt đầu bế quan hóa độc thành công.");
    }

    public synchronized DetoxResult stopDetoxSession(Player player, String reason) {
        if (player == null) return DetoxResult.fail(DetoxResult.Status.ERROR, 0.0, "Player null");
        DetoxSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            return DetoxResult.fail(DetoxResult.Status.ERROR, danDocService.getDanDoc(player), "Không có phiên hóa độc.");
        }

        session.stop();
        double remaining = danDocService.getDanDoc(player);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e[Bế Quan Hóa Độc] &fPhiên hóa độc kết thúc &7(" + reason + ")&f! Tổng độc tố đã giải trừ: &a-" +
                        String.format("%.1f", session.getTotalCleansed()) + " &7(Đan độc hiện tại: " + String.format("%.1f", remaining) + "/1000)"));

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
        } catch (Exception ignored) {}

        return DetoxResult.success(session.getTotalCleansed(), remaining, "Dừng phiên hóa độc: " + reason);
    }

    public boolean isDetoxing(Player player) {
        return player != null && isDetoxing(player.getUniqueId());
    }

    public boolean isDetoxing(UUID uuid) {
        return uuid != null && activeSessions.containsKey(uuid);
    }

    public DetoxSession getSession(Player player) {
        return player != null ? activeSessions.get(player.getUniqueId()) : null;
    }

    public Map<UUID, DetoxSession> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }

    public double getMeditationDetoxPerMinute() {
        return meditationDetoxPerMinute;
    }

    public double getMaxTickGapSeconds() {
        return maxTickGapSeconds;
    }

    public boolean isCancelOnMove() {
        return cancelOnMove;
    }

    public boolean isCancelOnDamage() {
        return cancelOnDamage;
    }

    public boolean isCancelOnDeath() {
        return cancelOnDeath;
    }

    public boolean isCancelOnTeleport() {
        return cancelOnTeleport;
    }

    public boolean isCancelOnQuit() {
        return cancelOnQuit;
    }
}
