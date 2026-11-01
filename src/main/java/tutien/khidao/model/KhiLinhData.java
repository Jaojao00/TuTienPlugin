package tutien.khidao.model;

import java.util.Objects;

/**
 * Đại diện cho một Khí Linh độc lập uẩn dưỡng bên trong Pháp Bảo (Phase 4).
 */
public class KhiLinhData {

    private String name;
    private String element; // KIM, MOC, THUY, HOA, THO, LOI, etc.
    private int level; // 1 -> 10
    private int soul; // Linh tính 0 -> 100
    private KhiLinhSkill skill;

    public KhiLinhData(String name, String element, int level, int soul, KhiLinhSkill skill) {
        this.name = (name == null || name.isBlank()) ? "Khí Linh Bản Mệnh" : name;
        this.element = (element == null) ? "KIM" : element.toUpperCase();
        this.level = Math.max(1, Math.min(10, level));
        this.soul = Math.max(0, Math.min(100, soul));
        this.skill = (skill == null) ? KhiLinhSkill.KIEM_KHI_TRAC_VIET : skill;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(10, level));
    }

    public int getSoul() {
        return soul;
    }

    public void setSoul(int soul) {
        this.soul = Math.max(0, Math.min(100, soul));
    }

    public KhiLinhSkill getSkill() {
        return skill;
    }

    public void setSkill(KhiLinhSkill skill) {
        this.skill = skill;
    }

    /**
     * Tăng điểm Linh Tính (Soul). Nếu đạt 100 điểm thì thăng cấp Khí Linh.
     * @return true nếu Khí Linh được thăng cấp.
     */
    public boolean addSoul(int amount) {
        if (level >= 10) {
            soul = 100;
            return false;
        }
        soul += amount;
        if (soul >= 100) {
            soul -= 100;
            level++;
            return true;
        }
        return false;
    }

    /**
     * Serialize to string: name;element;level;soul;skill
     */
    public String serialize() {
        return name + ";" + element + ";" + level + ";" + soul + ";" + skill.name();
    }

    /**
     * Deserialize from string
     */
    public static KhiLinhData deserialize(String str) {
        if (str == null || str.isBlank()) return null;
        String[] parts = str.split(";");
        if (parts.length < 5) return null;
        try {
            String name = parts[0];
            String element = parts[1];
            int level = Integer.parseInt(parts[2]);
            int soul = Integer.parseInt(parts[3]);
            KhiLinhSkill skill = KhiLinhSkill.fromName(parts[4]);
            return new KhiLinhData(name, element, level, soul, skill);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KhiLinhData that)) return false;
        return level == that.level && soul == that.soul &&
                Objects.equals(name, that.name) &&
                Objects.equals(element, that.element) &&
                skill == that.skill;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, element, level, soul, skill);
    }
}
