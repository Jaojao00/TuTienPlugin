package tutien.alchemy.btth.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tutien.alchemy.btth.model.BTTHCauldron;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Dược Đỉnh trong Đấu Phá Thương Khung:
 * - Phàm Đỉnh
 * - Dược đỉnh Nhất Phẩm đến Cửu Phẩm
 * - Độ bền, độ ổn định và sức chịu Đan Lôi
 */
public class AlchemyCauldronService {

    private final Map<String, BTTHCauldron> cauldronCatalog = new ConcurrentHashMap<>();
    private final Map<UUID, BTTHCauldron> activeCauldronMap = new ConcurrentHashMap<>();

    public AlchemyCauldronService() {
        loadDefaults();
    }

    public void loadFromConfig(FileConfiguration config) {
        if (config == null || !config.isConfigurationSection("cauldrons")) return;
        ConfigurationSection root = config.getConfigurationSection("cauldrons");
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            String name = sec.getString("name", id);
            String display = sec.getString("display", name);
            int tier = sec.getInt("tier", 0);
            double maxHeat = sec.getDouble("max_heat", 200.0);
            double stability = sec.getDouble("stability", 0.70);
            int maxSlots = sec.getInt("max_slots", 2);
            int maxDurability = sec.getInt("max_durability", 100);
            double tribulationResist = sec.getDouble("tribulation_resist", 0.0);
            String desc = sec.getString("description", "");

            cauldronCatalog.put(id, new BTTHCauldron(id, name, display, tier, maxHeat,
                    stability, maxSlots, maxDurability, maxDurability, tribulationResist, desc));
        }
    }

    private void loadDefaults() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("alchemy/cauldrons.yml");
        if (stream != null) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            loadFromConfig(cfg);
        }
        if (cauldronCatalog.isEmpty()) {
            cauldronCatalog.put("PHAM_DINH", new BTTHCauldron(
                    "PHAM_DINH", "Phàm Đỉnh", "§7Phàm Đỉnh Rèn Thô", 0,
                    180.0, 0.70, 2, 50, 50, 0.0, "Lò thuốc thông thường người phàm."
            ));
        }
    }

    public BTTHCauldron getCauldronPrototype(String id) {
        return cauldronCatalog.get(id);
    }

    public BTTHCauldron getActiveCauldron(Player player) {
        if (player == null) return getCauldronPrototype("PHAM_DINH");
        return activeCauldronMap.computeIfAbsent(player.getUniqueId(), k -> {
            BTTHCauldron proto = getCauldronPrototype("PHAM_DINH");
            return new BTTHCauldron(proto.getId(), proto.getName(), proto.getDisplayName(),
                    proto.getTier(), proto.getMaxHeat(), proto.getStability(),
                    proto.getMaxSlots(), proto.getMaxDurability(), proto.getCurrentDurability(),
                    proto.getTribulationResist(), proto.getDescription());
        });
    }

    public void setActiveCauldron(Player player, String cauldronId) {
        if (player == null || cauldronId == null) return;
        BTTHCauldron proto = getCauldronPrototype(cauldronId);
        if (proto != null) {
            activeCauldronMap.put(player.getUniqueId(), new BTTHCauldron(
                    proto.getId(), proto.getName(), proto.getDisplayName(),
                    proto.getTier(), proto.getMaxHeat(), proto.getStability(),
                    proto.getMaxSlots(), proto.getMaxDurability(), proto.getMaxDurability(),
                    proto.getTribulationResist(), proto.getDescription()
            ));
        }
    }

    public void reduceDurability(Player player, int amount) {
        BTTHCauldron cauldron = getActiveCauldron(player);
        if (cauldron != null) {
            cauldron.reduceDurability(amount);
        }
    }
}
