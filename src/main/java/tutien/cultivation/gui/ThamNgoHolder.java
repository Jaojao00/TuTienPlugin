package tutien.cultivation.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Holder nhận diện cho giao diện ThamNgoGUI (Phase 2.4).
 */
public class ThamNgoHolder implements InventoryHolder {

    private Inventory inventory;
    private final String congPhapId;

    public ThamNgoHolder(String congPhapId) {
        this.congPhapId = congPhapId;
    }

    public String getCongPhapId() {
        return congPhapId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
