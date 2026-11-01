package tutien.cultivation.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Holder nhận diện cho giao diện ChuyenTuConfirmGUI (Phase 2.4).
 */
public class ChuyenTuConfirmHolder implements InventoryHolder {

    private Inventory inventory;
    private final String targetCongPhapId;
    private final int tuViLost;
    private final long remainingSeconds;

    public ChuyenTuConfirmHolder(String targetCongPhapId, int tuViLost, long remainingSeconds) {
        this.targetCongPhapId = targetCongPhapId;
        this.tuViLost = tuViLost;
        this.remainingSeconds = remainingSeconds;
    }

    public String getTargetCongPhapId() {
        return targetCongPhapId;
    }

    public int getTuViLost() {
        return tuViLost;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
