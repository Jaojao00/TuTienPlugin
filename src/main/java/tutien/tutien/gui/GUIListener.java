package tutien.tutien.gui;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import tutien.core.TuTienPlugin;

/**
 * Lớp xử lý khi người chơi bấm vào các nút trong Giao Diện Chính (MainMenu 54 ô).
 */
public class GUIListener implements Listener {

    private final TuTienPlugin plugin;

    public GUIListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String menuTitle = plugin.getConfig().getString("Menu.MainMenu.Title", MainMenu.GUI_TITLE);

        if (event.getView().getTitle().equals(menuTitle)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            int slot = event.getRawSlot();
            if (slot >= 54) return;

            switch (slot) {
                case 4 -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
                case 10 -> {
                    player.closeInventory();
                    player.performCommand("tutien tuluyen");
                }
                case 12 -> DotPhaGUI.open(player, plugin);
                case 14 -> LuyenDanGUI.open(player, plugin.getPlayerDataManager());
                case 16 -> CheGUI.open(player, plugin.getPlayerDataManager(), plugin.getCheManager());
                case 19 -> VanGioiCacGUI.open(player);
                case 21 -> KhoangThachGUI.open(player, plugin.getPlayerDataManager());
                case 23 -> GachaGUI.open(player, plugin.getPlayerDataManager(), plugin.getGachaGUI());
                case 25 -> BeQuanShopGUI.open(player, plugin.getPlayerDataManager());
                case 28 -> LinhThuVienGUI.open(player, plugin.getPlayerDataManager(), plugin.getLinhThuVienManager());
                case 30 -> NhiemVuGUI.open(player, plugin.getNhiemVuManager());
                case 32 -> TuiDoGUI.open(player, plugin.getTuiDoManager());
                case 34 -> {
                    if (event.getCurrentItem().getType() == Material.BARRIER) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    } else {
                        player.closeInventory();
                        player.performCommand("tutien ngukiem");
                    }
                }
                case 39, 40 -> XepHangGUI.open(player, plugin);
                case 41 -> ChonHeGUI.open(player, plugin.getPlayerDataManager());
            }
        }
    }
}
