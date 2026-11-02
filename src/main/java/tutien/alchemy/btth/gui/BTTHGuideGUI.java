package tutien.alchemy.btth.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Cẩm Nang Hướng Dẫn Luyện Dược Sư Toàn Tập (Đấu Phá Thương Khung).
 */
public class BTTHGuideGUI {

    public static final String GUI_TITLE = "§0✦ CẨM NANG LUYỆN DƯỢC ĐẤU PHÁ ✦";

    public static void open(Player player) {
        if (player == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Slot 11: Điều Kiện & Linh Căn
        inv.setItem(11, createItem(Material.FIRE_CHARGE, "§c§l1. Điều Kiện & Linh Căn",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Theo Đấu Phá Thương Khung, muốn luyện đan",
                "§7cần có 2 thiên phú cốt lõi:",
                "§f• §c§lHỏa Linh Căn (Bắt buộc): §7Để ngưng tụ",
                "§7  và điều khiển hỏa diễm luyện chế.",
                "§f• §a§lMộc Linh Căn (Phụ trợ): §7Tăng §a+20% - 25%",
                "§7  tỷ lệ chiết xuất tinh hoa dược thảo.",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 13: 4 Chỉ Số Linh Hồn Lực
        inv.setItem(13, createItem(Material.EXPERIENCE_BOTTLE, "§b§l2. 4 Chỉ Số Linh Hồn",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§f• §bSoulPower (Cường độ): §7Áp chế dược tính,",
                "§7  quy định phẩm cấp đan dược có thể luyện.",
                "§f• §6SoulControl (Khống chế): §7Giữ nhiệt chuẩn,",
                "§7  giảm thiểu biến động ngọn lửa.",
                "§f• §cSoulPerception (Cảm nhận): §7Phát hiện",
                "§7  tạp chất và độ tinh thuần.",
                "§f• §dSoulCapacity: §7Bể chứa hồn lực.",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§74 Cảnh giới: §7Phàm -> §bLinh (8 phẩm)",
                "§7-> §dThiên (9 phẩm) -> §eĐế Cảnh."));

        // Slot 15: Dược Đỉnh & Nhiệt Độ
        inv.setItem(15, createItem(Material.CAULDRON, "§e§l3. Dược Đỉnh & Nguy Cơ Nổ Lò",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Mỗi dược đỉnh có giới hạn chịu nhiệt riêng:",
                "§f• §7Phàm Đỉnh: §7Tối đa §c180°C §7(dễ nổ).",
                "§f• §aHắc Thiết Đỉnh: §7Tối đa §c300°C.",
                "§f• §bXích Đồng Đỉnh: §7Tối đa §c500°C.",
                "§f• §eHuyền Nham Đỉnh: §7Tối đa §c800°C.",
                "§c* Chú ý: Nếu nhiệt độ vượt quá giới hạn",
                "§c  quá 60°C, dược đỉnh sẽ PHÁT NỔ và hỏng!"));

        // Slot 29: Quy Trình 5 Bước Luyện Đan Thực Chiến
        inv.setItem(29, createItem(Material.BREWING_STAND, "§6§l4. Quy Trình 5 Bước Luyện Đan",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§f1. §eChọn Đan Phương: §7Bấm vào [CHỌN ĐAN PHƯƠNG]",
                "§7   để đưa nguyên liệu vào dược đỉnh.",
                "§f2. §eThao Hỏa Điều Ôn: §7Bấm Tăng/Giảm Hỏa",
                "§7   để nhiệt độ đạt mốc §a[NHIỆT ĐỘ HOÀN HẢO].",
                "§f3. §eTinh Luyện: §7Bấm vào từng loại thảo dược",
                "§7   để ép lấy dược dịch tinh khiết.",
                "§f4. §eDung Hợp: §7Bấm Dung Hợp Dược Dịch.",
                "§f5. §eThu Đan: §7Bấm Thu Đan để nhận thành phẩm!"));

        // Slot 31: Bí Quyết Đan Văn & Cực Phẩm
        inv.setItem(31, createItem(Material.NETHER_STAR, "§d§l5. Đan Văn & Cực Phẩm",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§f• §6Cực Phẩm (Độ tinh thuần >= 99%):",
                "§7  Hoàn toàn không có Đan Độc!",
                "§f• §eĐan Văn (Tam Văn Thanh Linh Đan):",
                "§7  Phải dùng kỹ thuật chuyển đổi 3 loại hỏa diễm",
                "§7  khác nhau để sinh ra 3 Đan Văn tối thượng!",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 33: Cấp Bậc & Khảo Hạch
        inv.setItem(33, createItem(Material.GOLDEN_APPLE, "§a§l6. Khảo Hạch Thăng Cấp",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Muốn thăng cấp Luyện Dược Sư cần:",
                "§f1. §7Đủ điểm EXP luyện dược.",
                "§f2. §7Tự tay luyện thành công đan Thượng/Cực phẩm.",
                "§f3. §7Linh hồn lực đạt chuẩn cấp tiếp theo.",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 49: Nút quay lại
        inv.setItem(49, createItem(Material.ARROW, "§a§l◀ Quay Lại Đan Phòng", "§7Nhấp để trở về giao diện luyện đan."));

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
