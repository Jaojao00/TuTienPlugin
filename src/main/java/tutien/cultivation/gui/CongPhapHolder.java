package tutien.cultivation.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Holder nhận diện cho giao diện CongPhapGUI (Phase 2.4).
 * Ngăn chặn tuyệt đối khai thác shift-click, kéo vật phẩm hoặc nhầm lẫn title.
 */
public class CongPhapHolder implements InventoryHolder {

    private Inventory inventory;
    private int page = 0;

    public CongPhapHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
