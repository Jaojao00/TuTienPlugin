package tutien.element;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Cấu trúc dữ liệu toàn diện của Linh Căn một tu sĩ:
 *
 * 1. Phân loại (originType, currentType): Thiên, Chân, Ngụy, Dị.
 * 2. LinhCanPurity (purity): Độ thuần khiết bẩm sinh (0.70 <= purity <= 1.00),
 *    ảnh hưởng ổn định luyện hóa và hạn chế tạp khí tích tụ.
 * 3. Quality từng thuộc tính (elementQualityMap): Độ cảm ứng của từng nguyên tố (1.0 <= quality <= 100.0).
 * 4. PrimaryElement: Thuộc tính chủ đạo mạnh nhất.
 * 5. Mutations: Nhật ký biến dị, cơ duyên tẩy tủy, thăng phẩm linh căn.
 */
public class LinhCanData {

    private LinhCanType originType;
    private LinhCanType currentType;
    private double purity; // LinhCanPurity: 0.70 -> 1.00
    private String primaryElement; // Thuộc tính có Quality cao nhất
    private final Map<String, Double> elementQualityMap = new LinkedHashMap<>();
    private final List<MutationRecord> mutations = new ArrayList<>();

    public LinhCanData() {
        this.originType = LinhCanType.NGUY_LINH_CAN;
        this.currentType = LinhCanType.NGUY_LINH_CAN;
        this.purity = 0.75;
        this.primaryElement = null;
    }

    public LinhCanData(LinhCanType originType, LinhCanType currentType, double purity) {
        this.originType = originType != null ? originType : LinhCanType.NGUY_LINH_CAN;
        this.currentType = currentType != null ? currentType : this.originType;
        this.purity = clamp(purity, 0.50, 1.00);
    }

    public LinhCanType getOriginType() {
        return originType;
    }

    public void setOriginType(LinhCanType originType) {
        this.originType = originType;
    }

    public LinhCanType getCurrentType() {
        return currentType;
    }

    public void setCurrentType(LinhCanType currentType) {
        this.currentType = currentType;
    }

    public double getPurity() {
        return purity;
    }

    public void setPurity(double purity) {
        this.purity = clamp(purity, 0.50, 1.00);
    }

    public String getPrimaryElement() {
        return primaryElement;
    }

    public void setPrimaryElement(String primaryElement) {
        this.primaryElement = primaryElement;
    }

    public Map<String, Double> getElementQualityMap() {
        return Collections.unmodifiableMap(elementQualityMap);
    }

    public Set<String> getElements() {
        return Collections.unmodifiableSet(elementQualityMap.keySet());
    }

    public int getElementCount() {
        return elementQualityMap.size();
    }

    public boolean hasElement(String element) {
        if (element == null) return false;
        return elementQualityMap.containsKey(element.trim().toUpperCase());
    }

    public double getQuality(String element) {
        if (element == null) return 0.0;
        return elementQualityMap.getOrDefault(element.trim().toUpperCase(), 0.0);
    }

    public void setQuality(String element, double quality) {
        if (element == null || element.trim().isEmpty()) return;
        String clean = element.trim().toUpperCase();
        double clamped = clamp(quality, 0.0, 100.0);
        if (clamped <= 0.0) {
            elementQualityMap.remove(clean);
        } else {
            elementQualityMap.put(clean, clamped);
        }
        recalculatePrimaryElement();
    }

    public void removeElement(String element) {
        if (element == null) return;
        elementQualityMap.remove(element.trim().toUpperCase());
        recalculatePrimaryElement();
    }

    public void recalculatePrimaryElement() {
        if (elementQualityMap.isEmpty()) {
            primaryElement = null;
            return;
        }
        String bestElement = null;
        double maxQ = -1.0;
        for (Map.Entry<String, Double> entry : elementQualityMap.entrySet()) {
            if (entry.getValue() > maxQ) {
                maxQ = entry.getValue();
                bestElement = entry.getKey();
            }
        }
        this.primaryElement = bestElement;
    }

    public List<MutationRecord> getMutations() {
        return Collections.unmodifiableList(mutations);
    }

    public void addMutation(String reason, String description) {
        mutations.add(new MutationRecord(System.currentTimeMillis(), reason, description));
    }

    /**
     * Tên hiển thị đầy đủ (dùng cho Chat và Placeholder)
     */
    /**
     * Tên hiển thị rút gọn tối ưu cho TAB list, Scoreboard và Chat (chống tràn màn hình).
     * Ví dụ: "§6Thiên Linh Căn §7(§cHỏa§7)" hoặc "§eChân Linh Căn"
     */
    public String getCompactDisplayName() {
        if (primaryElement != null && !primaryElement.isEmpty()) {
            return currentType.getTenHienThi() + " §7(" + getElementDisplayTag(primaryElement) + "§7)";
        }
        return currentType.getTenHienThi();
    }

    /**
     * Tên hiển thị đầy đủ (dùng cho Chat và Placeholder)
     */
    public String getFormattedDisplayName() {
        StringBuilder sb = new StringBuilder();
        sb.append(currentType.getTenHienThi());
        if (!elementQualityMap.isEmpty()) {
            sb.append(" §f[");
            int i = 0;
            for (Map.Entry<String, Double> entry : elementQualityMap.entrySet()) {
                if (i > 0) sb.append("§7, ");
                sb.append(getElementDisplayTag(entry.getKey())).append(" §e").append(Math.round(entry.getValue()));
                i++;
            }
            sb.append("§f]");
        }
        return sb.toString();
    }

    /**
     * Thẻ hiển thị màu của một nguyên tố
     */
    public static String getElementDisplayTag(String element) {
        if (element == null) return "§7Vô";
        Optional<NguHanh> nh = NguHanh.fromString(element);
        if (nh.isPresent()) {
            return nh.get().getTenHienThi();
        }
        Optional<DiLinhCan> di = DiLinhCan.fromString(element);
        if (di.isPresent()) {
            return di.get().getTenHienThi();
        }
        return "§e" + element;
    }

    /**
     * Danh sách dòng chi tiết dùng cho GUI Lore
     */
    public List<String> getDetailedLore() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Phẩm loại: " + currentType.getTenHienThi());
        lore.add("§7Thuần Khiết Bản Nguyên: §b" + String.format("%.1f%%", purity * 100.0));
        if (primaryElement != null) {
            lore.add("§7Thuộc tính chủ đạo: " + getElementDisplayTag(primaryElement));
        }
        lore.add("§7Chi tiết thuộc tính cảm ứng:");
        for (Map.Entry<String, Double> entry : elementQualityMap.entrySet()) {
            lore.add("  §8• " + getElementDisplayTag(entry.getKey()) + ": §f" + String.format("%.1f", entry.getValue()) + "/100");
        }
        if (!mutations.isEmpty()) {
            lore.add("§7Cơ duyên biến dị: §d" + mutations.size() + " lần");
        }
        return lore;
    }

    // ==========================================
    // SERIALIZATION / DESERIALIZATION
    // ==========================================

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("originType", originType.name());
        map.put("currentType", currentType.name());
        map.put("purity", purity);
        if (primaryElement != null) {
            map.put("primaryElement", primaryElement);
        }
        Map<String, Double> elemMap = new LinkedHashMap<>(elementQualityMap);
        map.put("elements", elemMap);

        if (!mutations.isEmpty()) {
            List<Map<String, Object>> mutList = new ArrayList<>();
            for (MutationRecord r : mutations) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("time", r.timestamp);
                m.put("reason", r.reason);
                m.put("desc", r.description);
                mutList.add(m);
            }
            map.put("mutations", mutList);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static LinhCanData fromConfigurationSection(ConfigurationSection section) {
        if (section == null) {
            return new LinhCanData();
        }
        return fromMap(section.getValues(false));
    }

    public static LinhCanData fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new LinhCanData();
        }

        LinhCanType origin = LinhCanType.NGUY_LINH_CAN;
        if (map.containsKey("originType")) {
            try { origin = LinhCanType.valueOf(String.valueOf(map.get("originType"))); }
            catch (Exception ignored) {}
        }

        LinhCanType current = origin;
        if (map.containsKey("currentType")) {
            try { current = LinhCanType.valueOf(String.valueOf(map.get("currentType"))); }
            catch (Exception ignored) {}
        }

        double purity = 0.75;
        if (map.containsKey("purity")) {
            try { purity = Double.parseDouble(String.valueOf(map.get("purity"))); }
            catch (Exception ignored) {}
        }

        LinhCanData data = new LinhCanData(origin, current, purity);
        if (map.containsKey("primaryElement")) {
            data.setPrimaryElement(String.valueOf(map.get("primaryElement")));
        }

        if (map.containsKey("elements") && map.get("elements") instanceof Map) {
            Map<?, ?> rawElem = (Map<?, ?>) map.get("elements");
            for (Map.Entry<?, ?> e : rawElem.entrySet()) {
                String elem = String.valueOf(e.getKey());
                double q = 50.0;
                try {
                    // Xử lý cả dạng number hoặc nested map (quality: 80.0)
                    if (e.getValue() instanceof Map) {
                        Map<?, ?> sub = (Map<?, ?>) e.getValue();
                        Object qVal = sub.get("quality");
                        q = qVal != null ? Double.parseDouble(String.valueOf(qVal)) : 50.0;
                    } else {
                        q = Double.parseDouble(String.valueOf(e.getValue()));
                    }
                } catch (Exception ignored) {}
                data.setQuality(elem, q);
            }
        }

        if (map.containsKey("mutations") && map.get("mutations") instanceof List) {
            List<?> list = (List<?>) map.get("mutations");
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    long t = 0L;
                    try {
                        Object timeVal = m.get("time");
                        if (timeVal != null) t = Long.parseLong(String.valueOf(timeVal));
                    } catch (Exception ignored) {}
                    Object reasonVal = m.get("reason");
                    String reason = reasonVal != null ? String.valueOf(reasonVal) : "";
                    Object descVal = m.get("desc");
                    String desc = descVal != null ? String.valueOf(descVal) : "";
                    data.mutations.add(new MutationRecord(t, reason, desc));
                }
            }
        }

        data.recalculatePrimaryElement();
        return data;
    }

    private static double clamp(double val, double min, double max) {
        if (Double.isNaN(val)) return min;
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Nhật ký biến dị linh căn
     */
    public static class MutationRecord {
        private final long timestamp;
        private final String reason;
        private final String description;

        public MutationRecord(long timestamp, String reason, String description) {
            this.timestamp = timestamp;
            this.reason = reason;
            this.description = description;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getReason() {
            return reason;
        }

        public String getDescription() {
            return description;
        }
    }
}
