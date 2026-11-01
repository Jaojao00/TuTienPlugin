package tutien.tongmon;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Tranh Đoạt Linh Mạch & Chiến Tranh Tông Môn (Phase 6).
 */
public class TongMonWarManager {

    public static class TranNhanNode {
        public String nodeId;
        public String nodeName;
        public UUID controllingSect;
        public double captureProgress; // 0.0 -> 100.0
        public org.bukkit.Location location;

        public TranNhanNode(String nodeId, String nodeName) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.controllingSect = null;
            this.captureProgress = 0.0;
        }
    }

    public static class SectWar {
        public UUID warId;
        public UUID attackerSect;
        public UUID defenderSect;
        public long startTime;
        public double attackerControlPercent = 0.0;
        public boolean concluded = false;
        public UUID winnerSect = null;
        public final Map<String, TranNhanNode> nodes = new ConcurrentHashMap<>();

        public SectWar(UUID attackerSect, UUID defenderSect) {
            this(UUID.randomUUID(), attackerSect, defenderSect, System.currentTimeMillis(), 0.0, false, null);
        }

        public SectWar(UUID warId, UUID attackerSect, UUID defenderSect, long startTime,
                       double attackerControlPercent, boolean concluded, UUID winnerSect) {
            this.warId = warId;
            this.attackerSect = attackerSect;
            this.defenderSect = defenderSect;
            this.startTime = startTime;
            this.attackerControlPercent = attackerControlPercent;
            this.concluded = concluded;
            this.winnerSect = winnerSect;

            TranNhanNode n1 = new TranNhanNode("TRAN_NHAN_1", "Thái Cực Linh Trận");
            TranNhanNode n2 = new TranNhanNode("TRAN_NHAN_2", "Hư Không Phong Ma Trận");
            TranNhanNode n3 = new TranNhanNode("TRAN_NHAN_3", "Ngũ Hành Hộ Tông Trận");
            if (org.bukkit.Bukkit.getServer() != null) {
                try {
                    List<org.bukkit.World> worlds = org.bukkit.Bukkit.getWorlds();
                    if (worlds != null && !worlds.isEmpty()) {
                        org.bukkit.World w = worlds.get(0);
                        n1.location = new org.bukkit.Location(w, 100.0, 70.0, 100.0);
                        n2.location = new org.bukkit.Location(w, 200.0, 70.0, 200.0);
                        n3.location = new org.bukkit.Location(w, 300.0, 70.0, 300.0);
                    }
                } catch (Throwable ignored) {}
            }
            nodes.put("TRAN_NHAN_1", n1);
            nodes.put("TRAN_NHAN_2", n2);
            nodes.put("TRAN_NHAN_3", n3);
        }

        public void addControl(double amount) {
            if (concluded) return;
            attackerControlPercent = Math.min(100.0, Math.max(0.0, attackerControlPercent + amount));
            if (attackerControlPercent >= 100.0) {
                concluded = true;
                winnerSect = attackerSect;
            }
        }

        public void defendControl(double amount) {
            if (concluded) return;
            attackerControlPercent = Math.max(0.0, attackerControlPercent - amount);
        }

        public boolean captureNode(String nodeId, UUID sectId, double progress) {
            if (concluded) return false;
            TranNhanNode node = nodes.get(nodeId);
            if (node == null) return false;

            if (sectId.equals(attackerSect)) {
                node.captureProgress = Math.min(100.0, node.captureProgress + progress);
                if (node.captureProgress >= 100.0) {
                    node.controllingSect = attackerSect;
                }
                addControl(progress * 0.33);
            } else if (sectId.equals(defenderSect)) {
                node.captureProgress = Math.max(0.0, node.captureProgress - progress);
                if (node.captureProgress <= 0.0) {
                    node.controllingSect = defenderSect;
                }
                defendControl(progress * 0.33);
            }
            return true;
        }
    }

    private final TuTienPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, SectWar> activeWars = new ConcurrentHashMap<>();

    public TongMonWarManager() {
        this(null);
    }

    public TongMonWarManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.file = (plugin != null) ? new File(plugin.getDataFolder(), "wars.yml") : new File("target/wars.yml");
        loadData();
    }

    public void loadData() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        activeWars.clear();

        if (config.contains("Wars")) {
            for (String key : config.getConfigurationSection("Wars").getKeys(false)) {
                try {
                    UUID wId = UUID.fromString(key);
                    String path = "Wars." + key;
                    UUID att = UUID.fromString(config.getString(path + ".Attacker"));
                    UUID def = UUID.fromString(config.getString(path + ".Defender"));
                    long start = config.getLong(path + ".StartTime");
                    double ctrl = config.getDouble(path + ".Control", 0.0);
                    boolean conc = config.getBoolean(path + ".Concluded", false);
                    String winStr = config.getString(path + ".Winner");
                    UUID win = (winStr != null && !winStr.isBlank()) ? UUID.fromString(winStr) : null;

                    SectWar war = new SectWar(wId, att, def, start, ctrl, conc, win);
                    if (config.contains(path + ".Nodes")) {
                        for (String nKey : config.getConfigurationSection(path + ".Nodes").getKeys(false)) {
                            String nPath = path + ".Nodes." + nKey;
                            String nName = config.getString(nPath + ".Name", nKey);
                            TranNhanNode node = war.nodes.computeIfAbsent(nKey, k -> new TranNhanNode(k, nName));
                            node.nodeName = nName;
                            node.captureProgress = config.getDouble(nPath + ".Progress", 0.0);
                            String ctrlStr = config.getString(nPath + ".Controller");
                            if (ctrlStr != null && !ctrlStr.isBlank()) {
                                node.controllingSect = UUID.fromString(ctrlStr);
                            }
                            if (config.contains(nPath + ".World")) {
                                String wName = config.getString(nPath + ".World");
                                org.bukkit.World w = (org.bukkit.Bukkit.getServer() != null) ? org.bukkit.Bukkit.getWorld(wName) : null;
                                if (w != null) {
                                    double x = config.getDouble(nPath + ".X");
                                    double y = config.getDouble(nPath + ".Y");
                                    double z = config.getDouble(nPath + ".Z");
                                    node.location = new org.bukkit.Location(w, x, y, z);
                                }
                            }
                        }
                    }
                    activeWars.put(wId, war);
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveData() {
        if (config == null) return;
        config.set("Wars", null);
        for (Map.Entry<UUID, SectWar> entry : activeWars.entrySet()) {
            String path = "Wars." + entry.getKey().toString();
            SectWar war = entry.getValue();
            config.set(path + ".Attacker", war.attackerSect.toString());
            config.set(path + ".Defender", war.defenderSect.toString());
            config.set(path + ".StartTime", war.startTime);
            config.set(path + ".Control", war.attackerControlPercent);
            config.set(path + ".Concluded", war.concluded);
            if (war.winnerSect != null) config.set(path + ".Winner", war.winnerSect.toString());
            for (Map.Entry<String, TranNhanNode> nEntry : war.nodes.entrySet()) {
                String nPath = path + ".Nodes." + nEntry.getKey();
                config.set(nPath + ".Name", nEntry.getValue().nodeName);
                config.set(nPath + ".Progress", nEntry.getValue().captureProgress);
                if (nEntry.getValue().controllingSect != null) {
                    config.set(nPath + ".Controller", nEntry.getValue().controllingSect.toString());
                }
                if (nEntry.getValue().location != null && nEntry.getValue().location.getWorld() != null) {
                    config.set(nPath + ".World", nEntry.getValue().location.getWorld().getName());
                    config.set(nPath + ".X", nEntry.getValue().location.getX());
                    config.set(nPath + ".Y", nEntry.getValue().location.getY());
                    config.set(nPath + ".Z", nEntry.getValue().location.getZ());
                }
            }
        }
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    private final Map<UUID, Long> captureCooldowns = new ConcurrentHashMap<>();

    public long getCaptureCooldownRemainingMs(UUID playerId) {
        if (playerId == null) return 0;
        long last = captureCooldowns.getOrDefault(playerId, 0L);
        long diff = System.currentTimeMillis() - last;
        return (diff < 10000L) ? (10000L - diff) : 0L;
    }

    public void setCaptureCooldown(UUID playerId) {
        if (playerId != null) {
            captureCooldowns.put(playerId, System.currentTimeMillis());
        }
    }

    public TranNhanNode getNode(String nodeId) {
        if (nodeId == null) return null;
        for (SectWar war : activeWars.values()) {
            if (!war.concluded && war.nodes.containsKey(nodeId)) {
                return war.nodes.get(nodeId);
            }
        }
        return null;
    }

    public boolean occupyNode(UUID sectId, String nodeId, double progress) {
        if (sectId == null || nodeId == null) return false;
        for (SectWar war : activeWars.values()) {
            if (!war.concluded && (sectId.equals(war.attackerSect) || sectId.equals(war.defenderSect))) {
                boolean ok = war.captureNode(nodeId, sectId, progress);
                if (ok) {
                    saveData();
                    return true;
                }
            }
        }
        return false;
    }

    public SectWar declareWar(UUID attackerSect, UUID defenderSect) {
        if (attackerSect == null || defenderSect == null || attackerSect.equals(defenderSect)) {
            return null;
        }
        SectWar war = new SectWar(attackerSect, defenderSect);
        activeWars.put(war.warId, war);
        saveData();
        return war;
    }

    public SectWar getWar(UUID warId) {
        return activeWars.get(warId);
    }

    public boolean endWar(UUID warId) {
        SectWar war = activeWars.remove(warId);
        if (war != null) {
            war.concluded = true;
            saveData();
            return true;
        }
        return false;
    }

    public Map<UUID, SectWar> getActiveWars() {
        return Collections.unmodifiableMap(activeWars);
    }
}
