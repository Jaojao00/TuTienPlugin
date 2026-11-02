package tutien.alchemy.btth.service;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tutien.alchemy.btth.model.AlchemistRank;
import tutien.alchemy.btth.model.BTTHFormula;
import tutien.alchemy.btth.model.BTTHIngredient;
import tutien.alchemy.btth.model.SoulRealm;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Danh mục Dược Liệu & Đan Phương Đấu Phá Thương Khung.
 */
public class BTTHFormulaManager {

    private final Map<String, BTTHIngredient> ingredientCatalog = new ConcurrentHashMap<>();
    private final Map<String, BTTHFormula> formulaCatalog = new ConcurrentHashMap<>();

    public BTTHFormulaManager() {
        loadDefaults();
    }

    public void loadDefaults() {
        // 1. Nạp ingredients
        InputStream inStream = getClass().getClassLoader().getResourceAsStream("alchemy/ingredients.yml");
        if (inStream != null) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(inStream, StandardCharsets.UTF_8));
            loadIngredients(cfg);
        }

        // 2. Nạp formulas
        InputStream fStream = getClass().getClassLoader().getResourceAsStream("alchemy/formulas.yml");
        if (fStream != null) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(fStream, StandardCharsets.UTF_8));
            loadFormulas(cfg);
        }
    }

    public void loadIngredients(FileConfiguration config) {
        if (config == null || !config.isConfigurationSection("ingredients")) return;
        ConfigurationSection root = config.getConfigurationSection("ingredients");
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            String name = sec.getString("name", id);
            String display = sec.getString("display", name);
            String typeStr = sec.getString("type", "HERB");
            BTTHIngredient.IngredientType type = BTTHIngredient.IngredientType.HERB;
            try { type = BTTHIngredient.IngredientType.valueOf(typeStr); } catch (Exception ignored) {}

            String element = sec.getString("element", "NONE");
            double minHeat = sec.getDouble("optimal_heat_min", 50.0);
            double maxHeat = sec.getDouble("optimal_heat_max", 80.0);
            int ticks = sec.getInt("extract_time_ticks", 60);
            double impurity = sec.getDouble("base_impurity", 0.20);
            String matStr = sec.getString("material", "PAPER");
            Material mat = Material.matchMaterial(matStr);
            if (mat == null) mat = Material.PAPER;
            String desc = sec.getString("description", "");

            ingredientCatalog.put(id, new BTTHIngredient(id, name, display, type, element,
                    minHeat, maxHeat, ticks, impurity, mat, desc));
        }
    }

    public void loadFormulas(FileConfiguration config) {
        if (config == null || !config.isConfigurationSection("formulas")) return;
        ConfigurationSection root = config.getConfigurationSection("formulas");
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            String display = sec.getString("display_name", id);
            String chinese = sec.getString("chinese_name", "");
            int tier = sec.getInt("tier", 1);
            String rankStr = sec.getString("required_rank", "HOC_DO");
            AlchemistRank rank = AlchemistRank.HOC_DO;
            try { rank = AlchemistRank.valueOf(rankStr); } catch (Exception ignored) {}

            double soulPower = sec.getDouble("required_soul_power", 50.0);
            String soulRealmStr = sec.getString("required_soul_realm", "PHAM_CANH");
            SoulRealm realm = SoulRealm.PHAM_CANH;
            try { realm = SoulRealm.valueOf(soulRealmStr); } catch (Exception ignored) {}

            int cauldronTier = sec.getInt("cauldron_tier_min", 0);
            double targetHeat = sec.getDouble("target_heat", 60.0);

            List<BTTHFormula.IngredientRequirement> ingredients = new ArrayList<>();
            if (sec.isList("ingredients")) {
                List<Map<?, ?>> list = sec.getMapList("ingredients");
                for (Map<?, ?> map : list) {
                    String ingId = String.valueOf(map.get("id"));
                    int amount = map.containsKey("amount") ? Integer.parseInt(String.valueOf(map.get("amount"))) : 1;
                    String role = map.containsKey("role") ? String.valueOf(map.get("role")) : "PRIMARY";
                    ingredients.add(new BTTHFormula.IngredientRequirement(ingId, amount, role));
                }
            }

            List<String> fusionOrder = sec.getStringList("fusion_order");
            boolean switchReq = sec.getBoolean("flame_switching_required", false);
            int switchCount = sec.getInt("flame_switch_count", 0);
            int maxLines = sec.getInt("max_lines", 0);
            boolean trib = sec.getBoolean("tribulation_enabled", false);

            String effType = sec.getString("effects.type", "NONE");
            double effVal = sec.getDouble("effects.value", 0.0);
            String effLore = sec.getString("effects.lore", "");

            formulaCatalog.put(id, new BTTHFormula(id, display, chinese, tier, rank, soulPower,
                    realm, cauldronTier, targetHeat, ingredients, fusionOrder, switchReq,
                    switchCount, maxLines, trib, effType, effVal, effLore));
        }
    }

    public BTTHIngredient getIngredient(String id) {
        return ingredientCatalog.get(id);
    }

    public BTTHFormula getFormula(String id) {
        return formulaCatalog.get(id);
    }

    public Collection<BTTHFormula> getAllFormulas() {
        return Collections.unmodifiableCollection(formulaCatalog.values());
    }
}
