package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.dungeon.LinhThuVienManager;

import java.util.Arrays;
import java.util.List;

/**
 * GUI Linh Thú Viên — Menu chọn khu phụ bản.
 * SPRINT 4: Nội dung phụ bản.
 */
public class LinhThuVienGUI implements Listener {

    public static final String GUI_TITLE = "§d§l🐾 Linh Thú Viên 🐾";
    private final PlayerDataManager dataManager;
    private final LinhThuVienManager linhThuVienManager;

    public LinhThuVienGUI(PlayerDataManager dataManager, LinhThuVienManager manager) {
        this.dataManager = dataManager;
        this.linhThuVienManager = manager;
    }

    public static void open(Player player, PlayerDataManager data, LinhThuVienManager manager) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        CanhGioi cg = data.getCanhGioi(player);

        // Header
        setItem(inv, 4, Material.AMETHYST_SHARD, "§d§lLinh Thú Viên",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Khu v\u1ef1c tu luyện chiến đấu.",
                "§7Cảnh giới hiện tại: §e" + cg.getTenHienThi(),
                "§8━━━━━━━━━━━━━━━━━━━━━"
            ));

        // 1. Huyền Ảnh Bí Cảnh
        boolean canHuyenAnh = cg.ordinal() >= CanhGioi.LUYEN_KHI.ordinal();
        setItem(inv, 10, canHuyenAnh ? Material.PURPLE_CONCRETE : Material.GRAY_CONCRETE,
            (canHuyenAnh ? "§5" : "§8") + "Huyền Ảnh Bí Cảnh",
            Arrays.asList(
                "§7Đánh quái linh thu Tu Vi & Đạo Niệm",
                "",
                "§7Yêu cầu: §e" + CanhGioi.LUYEN_KHI.getTenHienThi(),
                "§7Phần thưởng:",
                "§7  ▪ §eTu Vi §7theo tier cảnh giới",
                "§7  ▪ §bĐạo Niệm §7mỗi 10 kill",
                "",
                canHuyenAnh ? "§a► Nhấp để vào!" : "§cChưa đủ cảnh giới!"
            ));

        // 2. Thí Luyện Đạo Tràng
        boolean canThiLuyen = cg.ordinal() >= CanhGioi.TRUC_CO.ordinal();
        setItem(inv, 13, canThiLuyen ? Material.ORANGE_CONCRETE : Material.GRAY_CONCRETE,
            (canThiLuyen ? "§6" : "§8") + "Thí Luyện Đạo Tràng",
            Arrays.asList(
                "§7Khai thác tài nguyên đặc biệt",
                "",
                "§7Yêu cầu: §e" + CanhGioi.TRUC_CO.getTenHienThi(),
                "§7Phần thưởng:",
                "§7  ▪ §7Khoáng Thạch (nguyên liệu chế tác)",
                "§7  ▪ §fLinh Thạch (tiền tệ)",
                "§7  ▪ §aViên Ngọc §7(tăng chỉ số)",
                "",
                canThiLuyen ? "§a► Nhấp để vào!" : "§cChưa đủ cảnh giới!"
            ));

        // 3. Thiên Nguyên Cấm Địa
        boolean canThienNguyen = cg.ordinal() >= CanhGioi.NGUYEN_ANH.ordinal();
        setItem(inv, 16, canThienNguyen ? Material.RED_CONCRETE : Material.GRAY_CONCRETE,
            (canThienNguyen ? "§c" : "§8") + "Thiên Nguyên Cấm Địa",
            Arrays.asList(
                "§7Thách đấu Boss Cổ Thần",
                "",
                "§7Yêu cầu: §e" + CanhGioi.NGUYEN_ANH.getTenHienThi(),
                "§7Boss HP scale theo cảnh giới",
                "§7Phần thưởng:",
                "§7  ▪ §eTu Vi §7khổng lồ",
                "§7  ▪ §bĐạo Niệm ×5",
                "§7  ▪ §d✦ Boss Loot §7đặc biệt",
                "§4⚠ CẢNH BÁO: Rất nguy hiểm!",
                "",
                canThienNguyen ? "§a► Nhấp để vào!" : "§cChưa đủ cảnh giới!"
            ));

        // Fill border
        ItemStack border = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }

        player.openInventory(inv);
    }

    private static void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(name);
        m.setLore(lore);
        item.setItemMeta(m);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        int slot = event.getRawSlot();
        CanhGioi cg = dataManager.getCanhGioi(player);

        switch (slot) {
            case 10 -> {
                player.closeInventory();
                linhThuVienManager.enterHuyenAnh(player);
            }
            case 13 -> {
                player.closeInventory();
                linhThuVienManager.enterThiLuyen(player);
            }
            case 16 -> {
                player.closeInventory();
                linhThuVienManager.enterThienNguyen(player);
            }
        }
    }
}
