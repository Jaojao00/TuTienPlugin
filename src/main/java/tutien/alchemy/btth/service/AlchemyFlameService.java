package tutien.alchemy.btth.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tutien.alchemy.btth.model.BTTHFlame;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý danh mục Hỏa Diễm Đấu Phá Thương Khung:
 * - Đấu Khí Chi Hỏa (mặc định mở khóa khi có Hỏa Linh Căn)
 * - Thú Hỏa
 * - Dị Hỏa
 */
public class AlchemyFlameService {

    private final Map<String, BTTHFlame> flameCatalog = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeFlameMap = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> unlockedFlamesMap = new ConcurrentHashMap<>();

    public AlchemyFlameService() {
        loadDefaults();
    }

    public void loadFromConfig(FileConfiguration config) {
        if (config == null || !config.isConfigurationSection("flames")) return;
        ConfigurationSection root = config.getConfigurationSection("flames");
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            String name = sec.getString("name", id);
            String display = sec.getString("display", name);
            String typeStr = sec.getString("type", "DAU_KHI");
            BTTHFlame.FlameCategory cat = BTTHFlame.FlameCategory.DAU_KHI;
            try { cat = BTTHFlame.FlameCategory.valueOf(typeStr); } catch (Exception ignored) {}

            double heatPower = sec.getDouble("heat_power", 60.0);
            double heatControl = sec.getDouble("heat_control", 0.90);
            double heatPrecision = sec.getDouble("heat_precision", 0.85);
            double stability = sec.getDouble("flame_stability", 0.90);
            double soulDrain = sec.getDouble("soul_consumption", 3.0);
            double qualityBonus = sec.getDouble("quality_bonus", 0.0);
            String special = sec.getString("special_effect", "NONE");
            String desc = sec.getString("description", "");

            flameCatalog.put(id, new BTTHFlame(id, name, display, cat, heatPower, heatControl,
                    heatPrecision, stability, soulDrain, qualityBonus, special, desc));
        }
    }

    private void loadDefaults() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("alchemy/flames.yml");
        if (stream != null) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            loadFromConfig(cfg);
        }
        if (flameCatalog.isEmpty()) {
            // Fallback default flame
            flameCatalog.put("DAU_KHI_CHI_HOA", new BTTHFlame(
                    "DAU_KHI_CHI_HOA", "Đấu Khí Chi Hỏa", "§eĐấu Khí Chi Hỏa",
                    BTTHFlame.FlameCategory.DAU_KHI, 60.0, 0.95, 0.90, 0.95, 3.0, 0.0, "NONE",
                    "Hỏa diễm do người tu luyện ngưng tụ."
            ));
        }
    }

    public BTTHFlame getFlame(String id) {
        return flameCatalog.get(id);
    }

    public Collection<BTTHFlame> getAllFlames() {
        return Collections.unmodifiableCollection(flameCatalog.values());
    }

    public BTTHFlame getActiveFlame(Player player) {
        if (player == null) return getFlame("DAU_KHI_CHI_HOA");
        String flameId = activeFlameMap.getOrDefault(player.getUniqueId(), "DAU_KHI_CHI_HOA");
        BTTHFlame flame = getFlame(flameId);
        return flame != null ? flame : getFlame("DAU_KHI_CHI_HOA");
    }

    public void setActiveFlame(Player player, String flameId) {
        if (player == null || flameId == null) return;
        if (flameCatalog.containsKey(flameId)) {
            activeFlameMap.put(player.getUniqueId(), flameId);
        }
    }

    public boolean hasUnlockedFlame(Player player, String flameId) {
        if (player == null) return false;
        if ("DAU_KHI_CHI_HOA".equals(flameId)) return true;
        Set<String> unlocked = unlockedFlamesMap.get(player.getUniqueId());
        return unlocked != null && unlocked.contains(flameId);
    }

    public void unlockFlame(Player player, String flameId) {
        if (player == null || flameId == null) return;
        unlockedFlamesMap.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(flameId);
    }
}
