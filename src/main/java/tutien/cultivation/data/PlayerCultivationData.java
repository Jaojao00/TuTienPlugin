package tutien.cultivation.data;

import org.bukkit.configuration.ConfigurationSection;
import tutien.cultivation.model.AdaptationData;
import tutien.cultivation.model.CongPhapProgress;

import java.util.*;

/**
 * Cấu trúc dữ liệu tu luyện công pháp của từng người chơi:
 * - activePrimary: Công pháp chủ tu duy nhất đang vận hành.
 * - activeAuxiliary: Danh sách công pháp phụ tu (Thể tu, Thần thức tu, v.v., tối đa 2-3 bộ).
 * - adaptation: Dữ liệu thích nghi khi chuyển tu.
 * - learnedTechniques: Bản đồ tiến trình riêng biệt cho từng công pháp đã sưu tầm/lĩnh hội.
 */
public class PlayerCultivationData {

    public static final int MAX_AUXILIARY_DEFAULT = 2;

    private String activePrimary;
    private final List<String> activeAuxiliary = new ArrayList<>();
    private AdaptationData adaptation = new AdaptationData();
    private final Map<String, CongPhapProgress> learnedTechniques = new LinkedHashMap<>();

    public PlayerCultivationData() {
        this.activePrimary = null;
    }

    public PlayerCultivationData(String activePrimary) {
        this.activePrimary = activePrimary != null && !activePrimary.trim().isEmpty() ? activePrimary.trim().toUpperCase() : null;
    }

    public String getActivePrimary() {
        return activePrimary;
    }

    public void setActivePrimary(String activePrimary) {
        if (activePrimary == null || activePrimary.trim().isEmpty() || activePrimary.equalsIgnoreCase("CHUA_CO")) {
            this.activePrimary = null;
        } else {
            this.activePrimary = activePrimary.trim().toUpperCase();
        }
    }

    public List<String> getActiveAuxiliary() {
        return Collections.unmodifiableList(activeAuxiliary);
    }

    public boolean addAuxiliary(String congPhapId, int maxSlots) {
        if (congPhapId == null || congPhapId.trim().isEmpty()) return false;
        String clean = congPhapId.trim().toUpperCase();
        if (clean.equals(activePrimary)) return false; // Không thể vừa chủ tu vừa phụ tu
        if (activeAuxiliary.contains(clean)) return true; // Đã có rồi
        if (activeAuxiliary.size() >= Math.max(1, maxSlots)) return false; // Hết slot
        return activeAuxiliary.add(clean);
    }

    public boolean removeAuxiliary(String congPhapId) {
        if (congPhapId == null) return false;
        return activeAuxiliary.remove(congPhapId.trim().toUpperCase());
    }

    public boolean isAuxiliary(String congPhapId) {
        if (congPhapId == null) return false;
        return activeAuxiliary.contains(congPhapId.trim().toUpperCase());
    }

    public AdaptationData getAdaptation() {
        return adaptation;
    }

    public void setAdaptation(AdaptationData adaptation) {
        this.adaptation = adaptation != null ? adaptation : new AdaptationData();
    }

    public Map<String, CongPhapProgress> getLearnedTechniques() {
        return Collections.unmodifiableMap(learnedTechniques);
    }

    public CongPhapProgress getCongPhapProgress(String congPhapId) {
        if (congPhapId == null) return null;
        return learnedTechniques.get(congPhapId.trim().toUpperCase());
    }

    public CongPhapProgress getOrCreateProgress(String congPhapId) {
        if (congPhapId == null || congPhapId.trim().isEmpty()) return null;
        String clean = congPhapId.trim().toUpperCase();
        return learnedTechniques.computeIfAbsent(clean, CongPhapProgress::new);
    }

    public boolean hasLearnedCongPhap(String congPhapId) {
        CongPhapProgress cp = getCongPhapProgress(congPhapId);
        return cp != null && !cp.getLearnedChapters().isEmpty();
    }

    public boolean isLearnedChapter(String congPhapId, int chapter) {
        CongPhapProgress cp = getCongPhapProgress(congPhapId);
        return cp != null && cp.hasLearned(chapter);
    }

    // ==========================================
    // SERIALIZATION / DESERIALIZATION
    // ==========================================

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("active-primary", activePrimary != null ? activePrimary : "CHUA_CO");
        map.put("active-auxiliary", new ArrayList<>(activeAuxiliary));
        map.put("adaptation", adaptation.toMap());

        Map<String, Object> techMap = new LinkedHashMap<>();
        for (Map.Entry<String, CongPhapProgress> entry : learnedTechniques.entrySet()) {
            techMap.put(entry.getKey(), entry.getValue().toMap());
        }
        map.put("learned-techniques", techMap);
        return map;
    }

    public static PlayerCultivationData fromConfigurationSection(ConfigurationSection sec) {
        if (sec == null) {
            return new PlayerCultivationData();
        }
        return fromMap(sec.getValues(false));
    }

    @SuppressWarnings("unchecked")
    public static PlayerCultivationData fromMap(Map<String, Object> map) {
        PlayerCultivationData data = new PlayerCultivationData();
        if (map == null || map.isEmpty()) {
            return data;
        }

        if (map.containsKey("active-primary")) {
            String primary = String.valueOf(map.get("active-primary"));
            if (!primary.equalsIgnoreCase("CHUA_CO") && !primary.equalsIgnoreCase("null")) {
                data.setActivePrimary(primary);
            }
        }

        if (map.containsKey("active-auxiliary") && map.get("active-auxiliary") instanceof List) {
            List<?> list = (List<?>) map.get("active-auxiliary");
            for (Object item : list) {
                if (item != null) {
                    String auxId = String.valueOf(item).trim().toUpperCase();
                    if (!auxId.isEmpty() && !data.activeAuxiliary.contains(auxId)) {
                        data.activeAuxiliary.add(auxId);
                    }
                }
            }
        }

        if (map.containsKey("adaptation")) {
            if (map.get("adaptation") instanceof Map) {
                data.adaptation = AdaptationData.fromMap((Map<String, Object>) map.get("adaptation"));
            } else if (map.get("adaptation") instanceof ConfigurationSection) {
                ConfigurationSection adSec = (ConfigurationSection) map.get("adaptation");
                data.adaptation = AdaptationData.fromMap(adSec.getValues(false));
            }
        }

        Object techObj = map.get("learned-techniques");
        if (techObj == null) {
            techObj = map.get("progress");
        }

        if (techObj != null) {
            if (techObj instanceof Map) {
                Map<?, ?> techMap = (Map<?, ?>) techObj;
                for (Map.Entry<?, ?> entry : techMap.entrySet()) {
                    String techId = String.valueOf(entry.getKey()).trim().toUpperCase();
                    if (entry.getValue() instanceof Map) {
                        CongPhapProgress prog = CongPhapProgress.fromMap(techId, (Map<String, Object>) entry.getValue());
                        data.learnedTechniques.put(techId, prog);
                    } else if (entry.getValue() instanceof ConfigurationSection) {
                        ConfigurationSection sub = (ConfigurationSection) entry.getValue();
                        CongPhapProgress prog = CongPhapProgress.fromMap(techId, sub.getValues(false));
                        data.learnedTechniques.put(techId, prog);
                    }
                }
            } else if (techObj instanceof ConfigurationSection) {
                ConfigurationSection techSec = (ConfigurationSection) techObj;
                for (String techId : techSec.getKeys(false)) {
                    ConfigurationSection sub = techSec.getConfigurationSection(techId);
                    if (sub != null) {
                        CongPhapProgress prog = CongPhapProgress.fromMap(techId.toUpperCase(), sub.getValues(false));
                        data.learnedTechniques.put(techId.toUpperCase(), prog);
                    }
                }
            }
        }

        return data;
    }
}
