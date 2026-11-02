package tutien.alchemy.btth.model;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Lưu trữ 4 chỉ số Linh Hồn Lực độc lập của người chơi:
 * 1. soulPower: Cường độ linh hồn.
 * 2. soulControl: Khả năng khống chế hỏa hầu & dược dịch (0.0 - 1.0).
 * 3. soulPerception: Khả năng cảm nhận tạp chất & thời điểm thu đan (0.0 - 1.0).
 * 4. soulCapacity / currentSoul: Bể chứa linh hồn lực tiêu hao theo thời gian thực.
 */
public class PlayerSoulData {

    private double soulPower;
    private double soulControl;
    private double soulPerception;
    private double soulCapacity;
    private double currentSoul;
    private SoulRealm realm;

    public PlayerSoulData() {
        this(50.0, 0.70, 0.65, 100.0, 100.0, SoulRealm.PHAM_CANH);
    }

    public PlayerSoulData(double soulPower, double soulControl, double soulPerception,
                          double soulCapacity, double currentSoul, SoulRealm realm) {
        this.soulPower = Math.max(10.0, soulPower);
        this.soulControl = Math.max(0.1, Math.min(1.0, soulControl));
        this.soulPerception = Math.max(0.1, Math.min(1.0, soulPerception));
        this.soulCapacity = Math.max(50.0, soulCapacity);
        this.currentSoul = Math.max(0.0, Math.min(this.soulCapacity, currentSoul));
        this.realm = realm != null ? realm : SoulRealm.PHAM_CANH;
    }

    public double getSoulPower() { return soulPower; }
    public void setSoulPower(double soulPower) {
        this.soulPower = Math.max(10.0, soulPower);
        updateRealm();
    }
    public void addSoulPower(double delta) { setSoulPower(this.soulPower + delta); }

    public double getSoulControl() { return soulControl; }
    public void setSoulControl(double soulControl) { this.soulControl = Math.max(0.1, Math.min(1.0, soulControl)); }
    public void addSoulControl(double delta) { setSoulControl(this.soulControl + delta); }

    public double getSoulPerception() { return soulPerception; }
    public void setSoulPerception(double soulPerception) { this.soulPerception = Math.max(0.1, Math.min(1.0, soulPerception)); }
    public void addSoulPerception(double delta) { setSoulPerception(this.soulPerception + delta); }

    public double getSoulCapacity() { return soulCapacity; }
    public void setSoulCapacity(double soulCapacity) {
        this.soulCapacity = Math.max(50.0, soulCapacity);
        if (this.currentSoul > this.soulCapacity) this.currentSoul = this.soulCapacity;
    }

    public double getCurrentSoul() { return currentSoul; }
    public void setCurrentSoul(double currentSoul) {
        this.currentSoul = Math.max(0.0, Math.min(this.soulCapacity, currentSoul));
    }

    public boolean consumeSoul(double amount) {
        if (currentSoul < amount) return false;
        currentSoul -= amount;
        return true;
    }

    public void recoverSoul(double amount) {
        this.currentSoul = Math.min(this.soulCapacity, this.currentSoul + amount);
    }

    public SoulRealm getRealm() { return realm; }
    public void setRealm(SoulRealm realm) { this.realm = realm != null ? realm : SoulRealm.PHAM_CANH; }

    public void updateRealm() {
        if (this.soulPower >= 500000.0) {
            this.realm = SoulRealm.DE_CANH;
        } else if (this.soulPower >= 100000.0) {
            this.realm = SoulRealm.THIEN_CANH;
        } else if (this.soulPower >= 40000.0) {
            this.realm = SoulRealm.LINH_CANH;
        } else {
            this.realm = SoulRealm.PHAM_CANH;
        }
    }

    public void serializeTo(ConfigurationSection section) {
        section.set("SoulPower", soulPower);
        section.set("SoulControl", soulControl);
        section.set("SoulPerception", soulPerception);
        section.set("SoulCapacity", soulCapacity);
        section.set("CurrentSoul", currentSoul);
        section.set("Realm", realm.name());
    }

    public static PlayerSoulData deserializeFrom(ConfigurationSection section) {
        if (section == null) return new PlayerSoulData();
        double sp = section.getDouble("SoulPower", 50.0);
        double sc = section.getDouble("SoulControl", 0.70);
        double sep = section.getDouble("SoulPerception", 0.65);
        double scap = section.getDouble("SoulCapacity", 100.0);
        double cur = section.getDouble("CurrentSoul", scap);
        SoulRealm r = SoulRealm.PHAM_CANH;
        try {
            r = SoulRealm.valueOf(section.getString("Realm", SoulRealm.PHAM_CANH.name()));
        } catch (Exception ignored) {}
        return new PlayerSoulData(sp, sc, sep, scap, cur, r);
    }
}
