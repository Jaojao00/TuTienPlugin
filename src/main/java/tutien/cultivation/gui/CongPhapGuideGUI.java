package tutien.cultivation.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Cẩm Nang Hướng Dẫn Toàn Tập Công Pháp & Thần Thông Tu Tiên (Phase 2.4+):
 * Hướng dẫn 4 đại kỳ thư (Phàm Nhân, Tru Tiên, Tiên Nghịch, Đấu Phá),
 * cơ chế rơi Bí Tịch từ MythicMobs, tham ngộ khẩu quyết và thi triển thần thông.
 */
public class CongPhapGuideGUI {

    public static final String GUI_TITLE = "§0✦ CẨM NANG CÔNG PHÁP & THẦN THÔNG ✦";

    public static void open(Player player) {
        if (player == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Slot 11: Tứ Đại Kỳ Thư
        inv.setItem(11, createItem(Material.WRITABLE_BOOK, "§a§l1. Tứ Đại Kỳ Thư Xuất Xứ",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Hệ thống công pháp đúc kết từ 4 bộ tiểu thuyết:",
                "§f• §aPhàm Nhân Tu Tiên: §7Trường Xuân Công, Thanh",
                "§7  Nguyên Kiếm Quyết, Đại Diễn Quyết, Phạn Thánh...",
                "§f• §bTru Tiên: §7Thái Cực Huyền Thanh Đạo, Đại Phạm",
                "§7  Bát Nhã, Phần Hương Ngọc Sách, Thiên Thư...",
                "§f• §cTiên Nghịch: §7Hoàng Tuyền Thăng Khiếu, Cổ Thần",
                "§7  Quyết, Sinh Tử Luân Hồi Ấn, Tàn Dạ...",
                "§f• §eĐấu Phá Thương Khung: §7Phần Quyết, Bát Cực",
                "§7  Băng, Tam Thiên Lôi Động, Phật Nộ Hỏa Liên...",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 13: Ngũ Phẩm Giai Tầng
        inv.setItem(13, createItem(Material.EXPERIENCE_BOTTLE, "§6§l2. 5 Bậc Phẩm Cấp (Thấp Đến Cao)",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§f• §eHoàng Giai: §7Nhập môn giới tu chân (Luyện Khí).",
                "§f• §aHuyền Giai: §7Tiểu thành đạo pháp (Trúc Cơ).",
                "§f• §bĐịa Giai: §7Đại thừa tông phái (Kim Đan -> Nguyên Anh).",
                "§f• §dThiên Giai: §7Trấn phái tuyệt thế (Hóa Thần -> Luyện Hư).",
                "§f• §cTiên Giai: §7Vô thượng chí tôn (Hợp Thể -> Độ Kiếp).",
                "§7Mỗi phẩm cấp có 3 phẩm chất: §7Hạ §f| §bTrung §f| §6Thượng.",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 15: Môi Trường MythicMobs & Bí Cảnh
        inv.setItem(15, createItem(Material.DRAGON_HEAD, "§c§l3. Môi Trường MythicMobs & Săn Bí Tịch",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Công pháp gắn liền mật thiết với MythicMobs:",
                "§f• §cSăn Quái & Boss: §7Tiêu diệt quái vật hoặc Boss",
                "§7  trong Bí Cảnh để nhận Bí Tịch Tàn Quyển rơi ra.",
                "§f• §6Xiềng Xích Phong Ấn: §7Công pháp cao cấp (Cổ Thần,",
                "§7  Phần Quyết, Tàn Dạ...) yêu cầu phải trảm sát Boss",
                "§7  chỉ định trước khi được phép lĩnh ngộ!",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 29: Quy Trình Học & Lĩnh Ngộ
        inv.setItem(29, createItem(Material.ENCHANTED_BOOK, "§b§l4. Cách Học & Lĩnh Ngộ Bí Tịch",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Có 2 cách để lĩnh ngộ công pháp:",
                "§f1. §eChuột Phải Bí Tịch: §7Cầm quyển Bí Tịch trên tay,",
                "§7   click chuột phải để trực tiếp nạp vào thức hải.",
                "§f2. §eTọa Thiền Tham Ngộ: §7Dùng lệnh §b/tutien thamngo",
                "§7   để ngồi thiền tích lũy Điểm Đạo Ngộ đột phá từng tầng.",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 31: Chủ Tu & Phụ Tu
        inv.setItem(31, createItem(Material.BEACON, "§e§l5. Thiết Lập Chủ Tu & Phụ Tu",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§f• §6Công Pháp Chủ Tu (Duy nhất 1): §7Định hình",
                "§7  kinh mạch, quyết định hệ số hấp thu linh khí.",
                "§f• §bCông Pháp Phụ Tu (1 - 3 bộ): §7Bổ trợ thể phách,",
                "§7  thần thức tu vi, thừa hưởng 35% tốc độ tu luyện.",
                "§7  (Click chuột phải vào danh mục để gán phụ tu).",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 33: Thi Triển Thần Thông
        inv.setItem(33, createItem(Material.NETHER_STAR, "§d§l6. Thi Triển Thần Thông (Mythic Skills)",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Mỗi tầng công pháp sẽ thức tỉnh các Thần Thông riêng:",
                "§f• §eLệnh thi triển: §b/tutien skill <tên_kỹ_năng>",
                "§f• §aCơ chế: §7Tiêu hao Linh Lực, có Cooldown.",
                "§f• §6Tích hợp MythicMobs: §7Kích hoạt chiêu thức 3D",
                "§7  chân thực từ MythicMobs (có fallback Bukkit an toàn)!",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 49: Nút quay lại
        inv.setItem(49, createItem(Material.ARROW, "§a§l◀ Quay Lại Tàng Kinh Các", "§7Nhấp để trở về danh mục công pháp."));

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                List<String> list = new ArrayList<>();
                for (String l : lore) list.add(l);
                meta.setLore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
