package tutien.tutien.gui;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import tutien.core.TuTienPlugin;

/**
 * Lớp xử lý khi người chơi bấm vào các nút trong Giao Diện.
 * Đã cập nhật: Sử dụng gọi hàm Static để mở Vạn Giới Các, sửa lỗi missing method trong Plugin.
 */
public class GUIListener implements Listener {

    private final TuTienPlugin plugin;

    // Khởi tạo và nhận tham chiếu từ lớp chính
    public GUIListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Lấy tên Menu từ config.yml (có giá trị mặc định nếu chưa chỉnh)
        String menuTitle = plugin.getConfig().getString("Menu.MainMenu.Title", "§8§lHệ Thống Tu Tiên");

        // Kiểm tra đúng tên Menu
        if (event.getView().getTitle().equals(menuTitle)) {
            event.setCancelled(true); // Ngăn không cho người chơi lấy đồ ra ngoài

            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();

            int slot = event.getRawSlot();

            // Lấy vị trí các nút từ config.yml để linh hoạt chỉnh sửa
            int infoSlot = plugin.getConfig().getInt("Menu.MainMenu.Items.ThongTin.Slot", 10);
            int rewardSlot = plugin.getConfig().getInt("Menu.MainMenu.Items.DoiThuong.Slot", 12);
            int shopSlot = plugin.getConfig().getInt("Menu.MainMenu.Items.CuaHang.Slot", 14);
            int flySlot = plugin.getConfig().getInt("Menu.MainMenu.Items.NguKiem.Slot", 16);

            if (slot == infoSlot) { // --- Nút Thông Tin ---
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            }
            else if (slot == rewardSlot) { // --- Nút Đổi Thưởng ---
                if (event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    String msg = plugin.getConfig().getString("Messages.DoiThuong", "§b[Hệ Thống] Giao diện đổi thưởng đang được thiết kế!");
                    player.sendMessage(msg);
                    player.closeInventory();
                }
            }
            else if (slot == shopSlot) { // --- Nút Cửa Hàng (Vạn Giới Các) - MỞ CỬA TỰ DO ---
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
                VanGioiCacGUI.open(player);
            }
            else if (slot == flySlot) { // --- Nút Ngự Kiếm Phi Hành ---
                if (event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
                    player.closeInventory();
                    player.performCommand("tutien ngukiem");
                }
            }
        }
    }
}