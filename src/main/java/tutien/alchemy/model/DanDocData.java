package tutien.alchemy.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model lưu trữ bền vững Đan Độc và Kháng Thuốc của từng tu sĩ (DataVersion: 5).
 */
public class DanDocData {

    private double danDoc = 0.0;
    private final Map<String, Double> resistance = new ConcurrentHashMap<>();
    private final Map<String, Long> lastDoseAt = new ConcurrentHashMap<>();
    private long lastDetoxAt = 0L;

    public DanDocData() {
        initDefaultResistances();
    }

    public DanDocData(double initialDanDoc) {
        this.danDoc = Math.max(0.0, Math.min(1000.0, initialDanDoc));
        initDefaultResistances();
    }

    private void initDefaultResistances() {
        for (ResistanceGroup group : ResistanceGroup.values()) {
            resistance.putIfAbsent(group.name(), 0.0);
        }
    }

    public double getDanDoc() {
        return danDoc;
    }

    public void setDanDoc(double danDoc) {
        this.danDoc = Math.max(0.0, Math.min(1000.0, danDoc));
    }

    public void addDanDoc(double delta) {
        if (delta > 0) {
            setDanDoc(this.danDoc + delta);
        }
    }

    public void reduceDanDoc(double delta) {
        if (delta > 0) {
            setDanDoc(this.danDoc - delta);
        }
    }

    public double getResistance(String group) {
        if (group == null) return 0.0;
        return resistance.getOrDefault(group.toUpperCase().trim(), 0.0);
    }

    public void setResistance(String group, double value) {
        if (group == null) return;
        resistance.put(group.toUpperCase().trim(), Math.max(0.0, Math.min(100.0, value)));
    }

    public void addResistance(String group, double delta) {
        if (group == null || delta <= 0) return;
        double current = getResistance(group);
        setResistance(group, current + delta);
    }

    public void reduceResistance(String group, double delta) {
        if (group == null || delta <= 0) return;
        double current = getResistance(group);
        setResistance(group, current - delta);
    }

    public Map<String, Double> getAllResistances() {
        return Collections.unmodifiableMap(resistance);
    }

    public long getLastDoseAt(String group) {
        if (group == null) return 0L;
        return lastDoseAt.getOrDefault(group.toUpperCase().trim(), 0L);
    }

    public void setLastDoseAt(String group, long timestamp) {
        if (group == null) return;
        lastDoseAt.put(group.toUpperCase().trim(), timestamp);
    }

    public Map<String, Long> getAllLastDoseAt() {
        return Collections.unmodifiableMap(lastDoseAt);
    }

    public long getLastDetoxAt() {
        return lastDetoxAt;
    }

    public void setLastDetoxAt(long lastDetoxAt) {
        this.lastDetoxAt = lastDetoxAt;
    }

    public ToxicityStatus getToxicityStatus() {
        return ToxicityStatus.fromDanDoc(danDoc);
    }

    /**
     * Xuất dữ liệu ra ConfigurationSection của YAML.
     */
    public void toConfigurationSection(ConfigurationSection section) {
        if (section == null) return;
        section.set("Amount", danDoc);

        ConfigurationSection resSec = section.createSection("Resistance");
        for (Map.Entry<String, Double> entry : resistance.entrySet()) {
            resSec.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection doseSec = section.createSection("LastDoseAt");
        for (Map.Entry<String, Long> entry : lastDoseAt.entrySet()) {
            doseSec.set(entry.getKey(), entry.getValue());
        }

        section.set("LastDetoxAt", lastDetoxAt);
    }

    /**
     * Tải dữ liệu từ ConfigurationSection của YAML.
     */
    public static DanDocData fromConfigurationSection(ConfigurationSection section) {
        if (section == null) {
            return new DanDocData();
        }

        double amount = section.getDouble("Amount", 0.0);
        DanDocData data = new DanDocData(amount);

        if (section.isConfigurationSection("Resistance")) {
            ConfigurationSection resSec = section.getConfigurationSection("Resistance");
            for (String key : resSec.getKeys(false)) {
                data.setResistance(key, resSec.getDouble(key, 0.0));
            }
        }

        if (section.isConfigurationSection("LastDoseAt")) {
            ConfigurationSection doseSec = section.getConfigurationSection("LastDoseAt");
            for (String key : doseSec.getKeys(false)) {
                data.setLastDoseAt(key, doseSec.getLong(key, 0L));
            }
        }

        data.setLastDetoxAt(section.getLong("LastDetoxAt", 0L));
        return data;
    }

    public DanDocData deepClone() {
        DanDocData clone = new DanDocData(this.danDoc);
        for (Map.Entry<String, Double> e : this.resistance.entrySet()) {
            clone.setResistance(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Long> e : this.lastDoseAt.entrySet()) {
            clone.setLastDoseAt(e.getKey(), e.getValue());
        }
        clone.setLastDetoxAt(this.lastDetoxAt);
        return clone;
    }
}
