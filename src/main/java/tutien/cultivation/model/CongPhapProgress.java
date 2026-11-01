package tutien.cultivation.model;

import java.util.*;

/**
 * Tiến trình riêng biệt của người chơi đối với một công pháp cụ thể:
 * - collectedChapters: Danh sách các tầng/chương đã sưu tầm được tàn quyển/bí tịch (Set<Integer>).
 * - learnedChapters: Danh sách các tầng/chương đã chính thức lĩnh hội thành công (Set<Integer>).
 * - targetChapter: Tầng đang tập trung tham ngộ.
 * - comprehensionProgress: Tiến độ lĩnh hội của tầng hiện tại (0.0% -> 100.0%).
 * - masteryExp: Điểm thành thục tích lũy qua thực chiến và tu luyện (Phase 2.3).
 * - epiphanyPotential: Tiềm năng đốn ngộ tích lũy (Phase 2.3).
 */
public class CongPhapProgress {

    private final String congPhapId;
    private final Set<Integer> collectedChapters = new TreeSet<>();
    private final Set<Integer> learnedChapters = new TreeSet<>();

    private int targetChapter = 1;
    private double comprehensionProgress = 0.0;
    private int masteryExp = 0;
    private double epiphanyPotential = 0.0;

    public CongPhapProgress(String congPhapId) {
        this.congPhapId = congPhapId != null ? congPhapId.trim().toUpperCase() : "";
    }

    public String getCongPhapId() {
        return congPhapId;
    }

    public Set<Integer> getCollectedChapters() {
        return Collections.unmodifiableSet(collectedChapters);
    }

    public Set<Integer> getLearnedChapters() {
        return Collections.unmodifiableSet(learnedChapters);
    }

    public boolean hasCollected(int chapter) {
        return collectedChapters.contains(chapter);
    }

    public boolean hasLearned(int chapter) {
        return learnedChapters.contains(chapter);
    }

    public boolean addCollectedChapter(int chapter) {
        if (chapter <= 0) return false;
        return collectedChapters.add(chapter);
    }

    public boolean addLearnedChapter(int chapter) {
        if (chapter <= 0) return false;
        // Bắt buộc phải có tàn quyển/chương này trước mới được học
        collectedChapters.add(chapter);
        boolean added = learnedChapters.add(chapter);
        if (added && targetChapter <= chapter) {
            targetChapter = chapter + 1;
            comprehensionProgress = 0.0;
        }
        return added;
    }

    /**
     * Kiểm tra xem có đủ điều kiện thứ tự để học chương này không:
     * - Phải sở hữu chương đó trong collectedChapters.
     * - Nếu chapter > 1, bắt buộc phải đã học xong chapter - 1 (không nhảy cóc tầng).
     */
    public boolean canLearnChapter(int chapter) {
        if (chapter <= 0) return false;
        if (!hasCollected(chapter)) return false;
        if (hasLearned(chapter)) return false; // Đã học rồi
        if (chapter == 1) return true;
        return hasLearned(chapter - 1);
    }

    public int getHighestLearnedChapter() {
        if (learnedChapters.isEmpty()) return 0;
        return ((TreeSet<Integer>) learnedChapters).last();
    }

    public int getHighestCollectedChapter() {
        if (collectedChapters.isEmpty()) return 0;
        return ((TreeSet<Integer>) collectedChapters).last();
    }

    public int getTargetChapter() {
        return targetChapter;
    }

    public void setTargetChapter(int targetChapter) {
        this.targetChapter = Math.max(1, targetChapter);
    }

    public double getComprehensionProgress() {
        return comprehensionProgress;
    }

    public void setComprehensionProgress(double progress) {
        if (Double.isNaN(progress)) progress = 0.0;
        this.comprehensionProgress = Math.max(0.0, Math.min(100.0, progress));
    }

    public void addComprehensionProgress(double delta) {
        if (Double.isNaN(delta) || delta <= 0.0) return;
        setComprehensionProgress(this.comprehensionProgress + delta);
    }

    public int getMasteryExp() {
        return masteryExp;
    }

    public void setMasteryExp(int masteryExp) {
        this.masteryExp = Math.max(0, masteryExp);
    }

    public void addMasteryExp(int amount) {
        if (amount <= 0) return;
        this.masteryExp = Math.max(0, this.masteryExp + amount);
    }

    public double getEpiphanyPotential() {
        return epiphanyPotential;
    }

    public void setEpiphanyPotential(double potential) {
        if (Double.isNaN(potential)) potential = 0.0;
        this.epiphanyPotential = Math.max(0.0, Math.min(100.0, potential));
    }

    public void addEpiphanyPotential(double delta) {
        if (Double.isNaN(delta) || delta <= 0.0) return;
        setEpiphanyPotential(this.epiphanyPotential + delta);
    }

    // ==========================================
    // SERIALIZATION / DESERIALIZATION
    // ==========================================

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("collected-chapters", new ArrayList<>(collectedChapters));
        map.put("learned-chapters", new ArrayList<>(learnedChapters));
        map.put("target-chapter", targetChapter);
        map.put("comprehension-progress", comprehensionProgress);
        map.put("mastery-exp", masteryExp);
        map.put("epiphany-potential", epiphanyPotential);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static CongPhapProgress fromMap(String id, Map<String, Object> map) {
        CongPhapProgress progress = new CongPhapProgress(id);
        if (map == null || map.isEmpty()) {
            return progress;
        }

        if (map.containsKey("collected-chapters") && map.get("collected-chapters") instanceof List) {
            List<?> list = (List<?>) map.get("collected-chapters");
            for (Object obj : list) {
                try {
                    int c = Integer.parseInt(String.valueOf(obj));
                    progress.collectedChapters.add(c);
                } catch (Exception ignored) {}
            }
        }

        if (map.containsKey("learned-chapters") && map.get("learned-chapters") instanceof List) {
            List<?> list = (List<?>) map.get("learned-chapters");
            for (Object obj : list) {
                try {
                    int c = Integer.parseInt(String.valueOf(obj));
                    progress.learnedChapters.add(c);
                    progress.collectedChapters.add(c); // Đã học chắc chắn đã từng sở hữu
                } catch (Exception ignored) {}
            }
        }

        if (map.containsKey("target-chapter")) {
            try { progress.targetChapter = Integer.parseInt(String.valueOf(map.get("target-chapter"))); }
            catch (Exception ignored) {}
        }

        if (map.containsKey("comprehension-progress")) {
            try { progress.comprehensionProgress = Double.parseDouble(String.valueOf(map.get("comprehension-progress"))); }
            catch (Exception ignored) {}
        }

        if (map.containsKey("mastery-exp")) {
            try { progress.masteryExp = Integer.parseInt(String.valueOf(map.get("mastery-exp"))); }
            catch (Exception ignored) {}
        }

        if (map.containsKey("epiphany-potential")) {
            try { progress.epiphanyPotential = Double.parseDouble(String.valueOf(map.get("epiphany-potential"))); }
            catch (Exception ignored) {}
        }

        return progress;
    }
}
