package tutien.alchemy.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tutien.alchemy.model.AlchemyRecipe;
import tutien.core.CanhGioi;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Quản lý công thức luyện đan nạp từ luyen_dan.yml với cơ chế hoán đổi nguyên tử (atomic swap).
 */
public class AlchemyManager {

    private final TuTienPlugin plugin;
    private final Logger logger;
    private final Map<String, AlchemyRecipe> recipeMap = new ConcurrentHashMap<>();

    private double defaultCoolingRate = 12.0;
    private int defaultDuration = 15;

    public AlchemyManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("AlchemyManager");
        reload();
    }

    public synchronized void reload() {
        File file = plugin != null ? new File(plugin.getDataFolder(), "luyen_dan.yml") : new File("src/main/resources/luyen_dan.yml");
        FileConfiguration config;

        if (file != null && file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            File resFile = new File("src/main/resources/luyen_dan.yml");
            if (resFile.exists()) {
                config = YamlConfiguration.loadConfiguration(resFile);
            } else {
                config = new YamlConfiguration();
                if (plugin != null) {
                    try (InputStream in = plugin.getResource("luyen_dan.yml")) {
                        if (in != null) {
                            config.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        Map<String, AlchemyRecipe> newMap = new HashMap<>();

        defaultCoolingRate = config.getDouble("alchemy.furnace.temperature.default-cooling-rate", 12.0);
        defaultDuration = config.getInt("alchemy.furnace.base-duration-seconds", 15);

        ConfigurationSection sec = config.getConfigurationSection("recipes");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection rSec = sec.getConfigurationSection(key);
                if (rSec == null) continue;

                String danId = rSec.getString("dan-id", key);
                String name = rSec.getString("name", key);
                String realmStr = rSec.getString("min-realm", "PHAM_NHAN");
                CanhGioi minRealm;
                try {
                    minRealm = CanhGioi.valueOf(realmStr);
                } catch (Exception e) {
                    minRealm = CanhGioi.PHAM_NHAN;
                }

                int duration = rSec.getInt("duration-seconds", defaultDuration);
                double idealTemp = rSec.getDouble("ideal-temp", 500.0);
                double minTol = rSec.getDouble("min-tolerance", idealTemp - 70.0);
                double maxTol = rSec.getDouble("max-tolerance", idealTemp + 70.0);
                double cooling = rSec.getDouble("cooling-rate", defaultCoolingRate);
                double baseSuccess = rSec.getDouble("base-success", 0.70);
                double explosion = rSec.getDouble("explosion-temp", 900.0);

                List<AlchemyRecipe.Ingredient> ingredients = new ArrayList<>();
                List<String> rawIngs = rSec.getStringList("ingredients");
                for (String raw : rawIngs) {
                    String[] parts = raw.split(":");
                    if (parts.length >= 2) {
                        Material mat = Material.matchMaterial(parts[0]);
                        if (mat != null) {
                            int amt = 1;
                            try {
                                amt = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException ignored) {}
                            String viName = parts.length >= 3 ? parts[2] : mat.name();
                            ingredients.add(new AlchemyRecipe.Ingredient(mat, amt, viName));
                        }
                    }
                }

                AlchemyRecipe recipe = new AlchemyRecipe(key, danId, name, minRealm, ingredients,
                        idealTemp, minTol, maxTol, duration, cooling, baseSuccess, explosion);
                newMap.put(key.toUpperCase(), recipe);
                newMap.put(danId.toUpperCase(), recipe);
            }
        }

        // Hoán đổi an toàn
        recipeMap.clear();
        recipeMap.putAll(newMap);

        if (logger != null) {
            logger.info("[AlchemyManager] Đã nạp " + newMap.size() + " công thức hỏa hầu luyện đan.");
        }
    }

    public AlchemyRecipe getRecipe(String recipeIdOrDanId) {
        if (recipeIdOrDanId == null) return null;
        return recipeMap.get(recipeIdOrDanId.toUpperCase());
    }

    public Collection<AlchemyRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipeMap.values());
    }

    /**
     * Tự động tổng hợp công thức động nếu đan dược có trong danduoc.yml nhưng chưa được thêm vào luyen_dan.yml
     */
    public AlchemyRecipe getOrCreateDynamicRecipe(String pillId, CanhGioi minRealm, List<AlchemyRecipe.Ingredient> ingredients) {
        AlchemyRecipe existing = getRecipe(pillId);
        if (existing != null) return existing;

        int realmOrdinal = minRealm != null ? minRealm.ordinal() : 0;
        double ideal = 350.0 + (realmOrdinal * 50.0);
        double minTol = ideal - (60.0 - Math.min(30.0, realmOrdinal * 3.0));
        double maxTol = ideal + (60.0 - Math.min(30.0, realmOrdinal * 3.0));
        int duration = Math.min(30, 12 + (realmOrdinal * 2));
        double cooling = 10.0 + (realmOrdinal * 1.2);
        double baseSuccess = Math.max(0.4, 0.85 - (realmOrdinal * 0.04));
        double explosion = Math.min(1150.0, 750.0 + (realmOrdinal * 40.0));

        AlchemyRecipe dynamic = new AlchemyRecipe(pillId, pillId, pillId, minRealm,
                ingredients, ideal, minTol, maxTol, duration, cooling, baseSuccess, explosion);
        recipeMap.put(pillId.toUpperCase(), dynamic);
        return dynamic;
    }
}
