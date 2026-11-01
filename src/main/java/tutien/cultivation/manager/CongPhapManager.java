package tutien.cultivation.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tutien.core.TuTienPlugin;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.service.CongPhapService;
import tutien.element.ElementRelationService;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý toàn bộ danh mục công pháp trong hệ thống:
 * - Đọc và kiểm tra cấu trúc hợp lệ từ congphap.yml.
 * - Phát hiện và từ chối ID trùng lặp, bảo toàn tính toàn vẹn của catalog.
 * - Cung cấp truy xuất tốc độ cao (O(1)) cho các service và người chơi.
 */
public class CongPhapManager {

    private final TuTienPlugin plugin;
    private final ElementRelationService relationService;
    private final CongPhapService service;
    private final tutien.cultivation.service.ElementCompatibilityService compatibilityService;
    private final tutien.cultivation.service.ComprehensionService comprehensionService;
    private final tutien.cultivation.service.MasteryService masteryService;
    private final tutien.cultivation.service.EpiphanyService epiphanyService;
    private final Map<String, CongPhap> congPhapCatalog = new ConcurrentHashMap<>();

    private File configFile;
    private FileConfiguration config;

    public CongPhapManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.relationService = plugin != null && plugin.getLinhCanManager() != null
                ? plugin.getLinhCanManager().getRelationService()
                : new ElementRelationService();
        this.service = new CongPhapService(this);
        this.compatibilityService = new tutien.cultivation.service.ElementCompatibilityService(this.relationService);
        this.comprehensionService = new tutien.cultivation.service.ComprehensionService(this);
        this.masteryService = new tutien.cultivation.service.MasteryService();
        this.epiphanyService = new tutien.cultivation.service.EpiphanyService(this);
    }

    public ElementRelationService getRelationService() {
        return relationService;
    }

    public CongPhapService getService() {
        return service;
    }

    public tutien.cultivation.service.CongPhapTransitionService getTransitionService() {
        return service.getTransitionService();
    }

    public tutien.cultivation.service.ElementCompatibilityService getCompatibilityService() {
        return compatibilityService;
    }

    public tutien.cultivation.service.ComprehensionService getComprehensionService() {
        return comprehensionService;
    }

    public tutien.cultivation.service.MasteryService getMasteryService() {
        return masteryService;
    }

    public tutien.cultivation.service.EpiphanyService getEpiphanyService() {
        return epiphanyService;
    }

    public void load() {
        congPhapCatalog.clear();
        if (plugin == null) return;

        configFile = new File(plugin.getDataFolder(), "congphap.yml");
        if (!configFile.exists()) {
            plugin.saveResource("congphap.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Nạp và bổ sung các giá trị mặc định từ resource trong jar nếu có
        InputStream defStream = plugin.getResource("congphap.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }

        loadFromConfig(config);
    }

    /**
     * Nạp danh mục từ ConfigurationSection (hỗ trợ cả chạy Unit Test độc lập)
     */
    public void loadFromConfig(FileConfiguration cfg) {
        congPhapCatalog.clear();
        if (cfg == null) return;

        ConfigurationSection sec = cfg.getConfigurationSection("congphap");
        if (sec == null) {
            if (plugin != null) plugin.getLogger().warning("[CongPhap] Không tìm thấy mục 'congphap' trong congphap.yml!");
            return;
        }

        Set<String> seenIds = new HashSet<>();
        int loadedCount = 0;

        for (String key : sec.getKeys(false)) {
            String cleanKey = key.trim().toUpperCase();
            if (seenIds.contains(cleanKey)) {
                if (plugin != null) plugin.getLogger().severe("[CongPhap] Phát hiện ID trùng lặp: '" + cleanKey + "'! Từ chối đăng ký mục này.");
                continue;
            }

            ConfigurationSection cpSec = sec.getConfigurationSection(key);
            if (cpSec == null) continue;

            String declaredId = cpSec.getString("id", cleanKey).trim().toUpperCase();
            if (!declaredId.equalsIgnoreCase(cleanKey)) {
                if (plugin != null) plugin.getLogger().warning("[CongPhap] Khóa '" + cleanKey + "' và trường id '" + declaredId + "' không khớp nhau! Sử dụng khóa '" + cleanKey + "'.");
            }

            try {
                CongPhap cp = CongPhap.fromConfigurationSection(cleanKey, cpSec);
                if (cp != null) {
                    congPhapCatalog.put(cleanKey, cp);
                    seenIds.add(cleanKey);
                    loadedCount++;
                }
            } catch (Exception e) {
                if (plugin != null) plugin.getLogger().severe("[CongPhap] Lỗi khi nạp công pháp '" + cleanKey + "': " + e.getMessage());
            }
        }

        if (plugin != null) {
            plugin.getLogger().info("[CongPhap] Đã nạp thành công " + loadedCount + " bộ công pháp vào Catalog.");
        }
    }

    public CongPhap getCongPhap(String id) {
        if (id == null) return null;
        return congPhapCatalog.get(id.trim().toUpperCase());
    }

    public boolean hasCongPhap(String id) {
        if (id == null) return false;
        return congPhapCatalog.containsKey(id.trim().toUpperCase());
    }

    public Collection<CongPhap> getAllCongPhap() {
        return Collections.unmodifiableCollection(congPhapCatalog.values());
    }

    public int getCatalogSize() {
        return congPhapCatalog.size();
    }

    public void registerCongPhap(CongPhap cp) {
        if (cp == null) return;
        congPhapCatalog.put(cp.getId(), cp);
    }
}
