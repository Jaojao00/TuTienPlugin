package tutien.cultivation.model;

import org.bukkit.configuration.ConfigurationSection;
import tutien.core.CanhGioi;

import java.util.*;

/**
 * Định nghĩa template công pháp nạp từ congphap.yml:
 * Quản lý phẩm cấp, thuộc tính ngũ hành, giới hạn truyền thừa,
 * tốc độ hấp thu, hiệu suất luyện hóa, điều kiện nhập môn và bí thuật mở khóa.
 * Tích hợp nguồn gốc tiểu thuyết (Phàm Nhân, Tru Tiên, Tiên Nghịch, Đấu Phá) và điều kiện MythicMobs.
 */
public class CongPhap {

    private final String id;
    private final String displayName;
    private final CongPhapGrade gradeTier;
    private final CongPhapQuality gradeQuality;
    private final List<String> elements = new ArrayList<>();

    // Giới hạn truyền thừa
    private final CanhGioi maxRealm;
    private final CongPhapStage maxStage;
    private final int totalChapters;
    private final Map<Integer, CanhGioi> chapterRequirements = new LinkedHashMap<>();

    // Chỉ số tu luyện
    private final double absorptionRate;
    private final double refinementEfficiency;
    private final double stabilityMultiplier;

    // Điều kiện nhập môn
    private final CanhGioi minRealm;
    private final double minNgoTinh;
    private final List<String> requiredElements = new ArrayList<>();

    // Thần thông / Bí thuật mở khóa theo chương
    private final List<AbilityUnlock> abilities = new ArrayList<>();

    // Quy tắc chuyển tu
    private final boolean canTransition;
    private final boolean requiresHuMaiDan;
    private final double transitionPenaltyRatio;

    // Nguồn gốc & Cốt truyện
    private final String source;
    private final String description;

    // Môi trường & Điều kiện MythicMobs
    private final String requiredMythicBoss;
    private final String dropMythicMob;
    private final double dropRate;

    public CongPhap(String id, String displayName, CongPhapGrade gradeTier, CongPhapQuality gradeQuality,
                    List<String> elements, CanhGioi maxRealm, CongPhapStage maxStage, int totalChapters,
                    Map<Integer, CanhGioi> chapterRequirements, double absorptionRate, double refinementEfficiency,
                    double stabilityMultiplier, CanhGioi minRealm, double minNgoTinh, List<String> requiredElements,
                    List<AbilityUnlock> abilities, boolean canTransition, boolean requiresHuMaiDan, double transitionPenaltyRatio) {
        this(id, displayName, gradeTier, gradeQuality, elements, maxRealm, maxStage, totalChapters,
                chapterRequirements, absorptionRate, refinementEfficiency, stabilityMultiplier,
                minRealm, minNgoTinh, requiredElements, abilities, canTransition, requiresHuMaiDan,
                transitionPenaltyRatio, "TU_TIEN_GIOI", "", "", "", 0.0);
    }

    public CongPhap(String id, String displayName, CongPhapGrade gradeTier, CongPhapQuality gradeQuality,
                    List<String> elements, CanhGioi maxRealm, CongPhapStage maxStage, int totalChapters,
                    Map<Integer, CanhGioi> chapterRequirements, double absorptionRate, double refinementEfficiency,
                    double stabilityMultiplier, CanhGioi minRealm, double minNgoTinh, List<String> requiredElements,
                    List<AbilityUnlock> abilities, boolean canTransition, boolean requiresHuMaiDan, double transitionPenaltyRatio,
                    String source, String description, String requiredMythicBoss, String dropMythicMob, double dropRate) {
        this.id = id != null ? id.trim().toUpperCase() : "UNKNOWN";
        this.displayName = displayName != null ? displayName : id;
        this.gradeTier = gradeTier != null ? gradeTier : CongPhapGrade.HOANG;
        this.gradeQuality = gradeQuality != null ? gradeQuality : CongPhapQuality.TRUNG;
        if (elements != null) this.elements.addAll(elements);

        this.maxRealm = maxRealm != null ? maxRealm : this.gradeTier.getDefaultMaxRealm();
        this.maxStage = maxStage != null ? maxStage : CongPhapStage.DAI_VIEN_MAN;
        this.totalChapters = Math.max(1, totalChapters);
        if (chapterRequirements != null) this.chapterRequirements.putAll(chapterRequirements);

        this.absorptionRate = Math.max(0.1, absorptionRate);
        this.refinementEfficiency = Math.max(0.1, Math.min(2.0, refinementEfficiency));
        this.stabilityMultiplier = Math.max(0.1, stabilityMultiplier);

        this.minRealm = minRealm != null ? minRealm : CanhGioi.LUYEN_KHI;
        this.minNgoTinh = Math.max(0.0, minNgoTinh);
        if (requiredElements != null) this.requiredElements.addAll(requiredElements);
        if (abilities != null) this.abilities.addAll(abilities);

        this.canTransition = canTransition;
        this.requiresHuMaiDan = requiresHuMaiDan;
        this.transitionPenaltyRatio = Math.max(0.0, Math.min(1.0, transitionPenaltyRatio));

        this.source = source != null && !source.trim().isEmpty() ? source.trim().toUpperCase() : "TU_TIEN_GIOI";
        this.description = description != null ? description : "";
        this.requiredMythicBoss = requiredMythicBoss != null ? requiredMythicBoss.trim() : "";
        this.dropMythicMob = dropMythicMob != null ? dropMythicMob.trim() : "";
        this.dropRate = Math.max(0.0, Math.min(1.0, dropRate));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName.replace('&', '§');
    }

    public CongPhapGrade getGradeTier() {
        return gradeTier;
    }

    public CongPhapQuality getGradeQuality() {
        return gradeQuality;
    }

    public List<String> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public CanhGioi getMaxRealm() {
        return maxRealm;
    }

    public CongPhapStage getMaxStage() {
        return maxStage;
    }

    public int getTotalChapters() {
        return totalChapters;
    }

    public Map<Integer, CanhGioi> getChapterRequirements() {
        return Collections.unmodifiableMap(chapterRequirements);
    }

    public CanhGioi getChapterRequiredRealm(int chapter) {
        return chapterRequirements.getOrDefault(chapter, minRealm);
    }

    public double getAbsorptionRate() {
        return absorptionRate;
    }

    public double getQiAbsorbRate() {
        return getAbsorptionRate();
    }

    public double getRefinementEfficiency() {
        return refinementEfficiency;
    }

    public double getStabilityMultiplier() {
        return stabilityMultiplier;
    }

    public CanhGioi getMinRealm() {
        return minRealm;
    }

    public double getMinNgoTinh() {
        return minNgoTinh;
    }

    public List<String> getRequiredElements() {
        return Collections.unmodifiableList(requiredElements);
    }

    public List<AbilityUnlock> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    public List<String> getAbilitiesUnlockedAt(int chapter) {
        List<String> list = new ArrayList<>();
        for (AbilityUnlock au : abilities) {
            if (au.unlockChapter <= chapter) {
                list.add(au.id);
            }
        }
        return list;
    }

    public boolean canTransition() {
        return canTransition;
    }

    public boolean isRequiresHuMaiDan() {
        return requiresHuMaiDan;
    }

    public double getTransitionPenaltyRatio() {
        return transitionPenaltyRatio;
    }

    public String getSource() {
        return source;
    }

    public String getSourceDisplayName() {
        return switch (source) {
            case "PHAM_NHAN", "PHAM_NHAN_TU_TIEN" -> "§aPhàm Nhân Tu Tiên";
            case "TRU_TIEN" -> "§bTru Tiên";
            case "TIEN_NGHICH" -> "§cTiên Nghịch";
            case "DAU_PHA", "DAU_PHA_THUONG_KHUNG" -> "§eĐấu Phá Thương Khung";
            default -> "§7Tu Tiên Giới";
        };
    }

    public String getDescription() {
        return description.replace('&', '§');
    }

    public String getRequiredMythicBoss() {
        return requiredMythicBoss;
    }

    public String getDropMythicMob() {
        return dropMythicMob;
    }

    public double getDropRate() {
        return dropRate;
    }

    public String getFormattedGrade() {
        return gradeTier.getColor() + gradeTier.getName() + " " + gradeQuality.getDisplayName();
    }

    public boolean isRealmExceeded(CanhGioi playerRealm) {
        return playerRealm.ordinal() > maxRealm.ordinal();
    }

    // ==========================================
    // PARSER TIỆN ÍCH AN TOÀN CHO CẢNH GIỚI
    // ==========================================

    public static CanhGioi parseCanhGioi(String str, CanhGioi defaultVal) {
        if (str == null || str.trim().isEmpty()) return defaultVal;
        String clean = str.trim().toUpperCase();
        try {
            return CanhGioi.valueOf(clean);
        } catch (IllegalArgumentException e) {
            // Map các danh xưng tu tiên tương đương với hệ thống Cảnh Giới
            return switch (clean) {
                case "LUYEN_HU" -> CanhGioi.ANH_BIEN;
                case "HOP_THE" -> CanhGioi.VAN_DINH;
                case "DAI_THUA" -> CanhGioi.DUONG_THUC;
                case "DO_KIEP" -> CanhGioi.TOAI_NIET;
                case "TIEN_NHAN", "TIEN_DE" -> CanhGioi.DAP_THIEN;
                default -> defaultVal;
            };
        }
    }

    // ==========================================
    // FACTORY LOADER TỪ YAML / MAP
    // ==========================================

    @SuppressWarnings("unchecked")
    public static CongPhap fromConfigurationSection(String id, ConfigurationSection sec) {
        if (sec == null) {
            return null;
        }

        String displayName = sec.getString("display-name", id);

        CongPhapGrade tier = CongPhapGrade.fromString(sec.getString("grade.tier", "HOANG"));
        CongPhapQuality quality = CongPhapQuality.fromString(sec.getString("grade.quality", "TRUNG"));

        List<String> elements = sec.getStringList("elements");

        // Transmission
        CanhGioi maxRealm = parseCanhGioi(sec.getString("transmission.max-realm"), tier.getDefaultMaxRealm());
        CongPhapStage maxStage = CongPhapStage.fromString(sec.getString("transmission.max-stage", "DAI_VIEN_MAN"));
        int totalChapters = sec.getInt("transmission.total-chapters", 9);

        Map<Integer, CanhGioi> chapterMap = new LinkedHashMap<>();
        if (sec.isConfigurationSection("transmission.chapters")) {
            ConfigurationSection chSec = sec.getConfigurationSection("transmission.chapters");
            for (String chKey : chSec.getKeys(false)) {
                try {
                    int chNum = Integer.parseInt(chKey);
                    String rStr = chSec.getString(chKey + ".required-realm", "LUYEN_KHI");
                    chapterMap.put(chNum, parseCanhGioi(rStr, CanhGioi.LUYEN_KHI));
                } catch (Exception ignored) {}
            }
        }

        // Cultivation stats
        double absorption = sec.getDouble("cultivation.absorption-rate", 1.0);
        double refinement = sec.getDouble("cultivation.refinement-efficiency", 1.0);
        double stability = sec.getDouble("cultivation.stability-multiplier", 1.0);

        // Requirements
        CanhGioi minRealm = parseCanhGioi(sec.getString("requirements.min-realm"), CanhGioi.LUYEN_KHI);
        double minNgoTinh = sec.getDouble("requirements.min-ngo-tinh", 10.0);
        List<String> reqElements = sec.getStringList("requirements.required-elements");

        // Abilities
        List<AbilityUnlock> abilities = new ArrayList<>();
        if (sec.isList("abilities")) {
            List<?> abList = sec.getList("abilities");
            for (Object item : abList) {
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    Object idObj = m.get("id");
                    String abId = idObj != null ? String.valueOf(idObj) : "";
                    int unCh = 1;
                    try {
                        Object unChObj = m.get("unlock-chapter");
                        if (unChObj != null) unCh = Integer.parseInt(String.valueOf(unChObj));
                    } catch (Exception ignored) {}
                    if (!abId.isEmpty()) {
                        abilities.add(new AbilityUnlock(abId, unCh));
                    }
                }
            }
        }

        // Transition
        boolean canTrans = sec.getBoolean("transition.can-transition", true);
        boolean reqHuMai = sec.getBoolean("transition.requires-hu-mai-dan", false);
        double penRatio = sec.getDouble("transition.penalty-ratio", 0.30);

        // Nguồn gốc & Cốt truyện
        String source = sec.getString("source", "TU_TIEN_GIOI");
        String desc = sec.getString("description", "");

        // Mythic conditions
        String reqBoss = sec.getString("mythic-conditions.required-boss", "");
        String dropMob = sec.getString("mythic-conditions.drop-from-mob", "");
        double dropRate = sec.getDouble("mythic-conditions.drop-rate", 0.0);

        return new CongPhap(id, displayName, tier, quality, elements, maxRealm, maxStage, totalChapters,
                chapterMap, absorption, refinement, stability, minRealm, minNgoTinh, reqElements, abilities,
                canTrans, reqHuMai, penRatio, source, desc, reqBoss, dropMob, dropRate);
    }

    /**
     * Khai báo bí thuật / thần thông mở khóa theo chương
     */
    public static class AbilityUnlock {
        private final String id;
        private final int unlockChapter;

        public AbilityUnlock(String id, int unlockChapter) {
            this.id = id;
            this.unlockChapter = Math.max(1, unlockChapter);
        }

        public String getId() {
            return id;
        }

        public int getUnlockChapter() {
            return unlockChapter;
        }
    }
}
