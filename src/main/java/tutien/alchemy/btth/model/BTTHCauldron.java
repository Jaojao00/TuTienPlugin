package tutien.alchemy.btth.model;

/**
 * Lớp biểu diễn Dược Đỉnh trong Đấu Phá Thương Khung:
 * - Phàm Đỉnh: Lò rèn thông thường
 * - Nhất -> Lục Phẩm: Dược đỉnh chế tác từ các loại khoáng thạch quý
 * - Thất Phẩm trở lên / Thiên Đỉnh: Khắc linh trận, chịu được Đan Lôi
 */
public class BTTHCauldron {

    private final String id;
    private final String name;
    private final String displayName;
    private final int tier;
    private final double maxHeat;
    private final double stability;
    private final int maxSlots;
    private final int maxDurability;
    private int currentDurability;
    private final double tribulationResist;
    private final String description;

    public BTTHCauldron(String id, String name, String displayName, int tier,
                        double maxHeat, double stability, int maxSlots,
                        int maxDurability, int currentDurability, double tribulationResist,
                        String description) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.tier = tier;
        this.maxHeat = maxHeat;
        this.stability = stability;
        this.maxSlots = maxSlots;
        this.maxDurability = maxDurability;
        this.currentDurability = currentDurability > 0 ? currentDurability : maxDurability;
        this.tribulationResist = tribulationResist;
        this.description = description != null ? description : "";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public int getTier() { return tier; }
    public double getMaxHeat() { return maxHeat; }
    public double getStability() { return stability; }
    public int getMaxSlots() { return maxSlots; }
    public int getMaxDurability() { return maxDurability; }
    public int getCurrentDurability() { return currentDurability; }
    public void setCurrentDurability(int durability) { this.currentDurability = Math.max(0, Math.min(maxDurability, durability)); }
    public double getTribulationResist() { return tribulationResist; }
    public String getDescription() { return description; }

    public boolean isBroken() { return currentDurability <= 0; }

    public void reduceDurability(int amount) {
        this.currentDurability = Math.max(0, this.currentDurability - amount);
    }

    public void repair(int amount) {
        this.currentDurability = Math.min(this.maxDurability, this.currentDurability + amount);
    }
}
