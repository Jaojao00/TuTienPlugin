package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.LinhCan;
import tutien.core.PlayerDataManager;
import tutien.task.NguKiemTask;

import java.util.Arrays;
import java.util.List;

/**
 * Giao diện trung tâm Hệ Thống Tu Tiên (54 ô).
 * v2.2 Skyblock Edition:
 *   - Tích hợp đầy đủ tất cả các tính năng từ Sprint 1 -> 4 + Skyblock vào cùng một bảng điều khiển trực quan.
 */
public class MainMenu {

    public static final String GUI_TITLE = "§8§lHệ Thống Tu Tiên";

    public static void open(Player player, PlayerDataManager dataManager) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        CanhGioi cg = dataManager.getCanhGioi(player);
        LinhCan lc = dataManager.getLinhCan(player);
        HeTuLuyen he = dataManager.getHeTuLuyen(player);
        CanhGioi tiepTheo = cg.getCanhGioiTiepTheo();

        String tuViString = (tiepTheo != null)
                ? (dataManager.getTuVi(player) + " / " + tiepTheo.getTuViYeuCau())
                : (dataManager.getTuVi(player) + " (Đỉnh phong)");
        double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                ? player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;

        // --- Ô 4: Thông Tin Đạo Hữu ---
        setItem(inv, 4, Material.NETHER_STAR, "§e§l✦ Thông Tin Đạo Hữu ✦", Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Đạo hiệu: §f" + player.getName(),
                "§7Cảnh giới: §b" + cg.getTenHienThi(),
                "§7Linh căn: §d" + lc.getTenHienThi() + " §7(x" + lc.getTocDo() + ")",
                "§7Hệ tu luyện: §6" + he.getTenHienThi(),
                "§7Tu vi: §a" + tuViString,
                "§7Linh lực: §b" + dataManager.getLinhLuc(player) + " / " + dataManager.getMaxLinhLucEffective(player),
                "§7Sinh lực tối đa: §c" + String.format("%.0f", maxHp) + " ❤",
                "§8━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Khoáng Thạch: §6" + dataManager.getKhoangThach(player) + " ⛏",
                "§7Đạo Niệm: §b" + dataManager.getDaoNiem(player) + " ✦",
                "§7Điểm Bế Quan: §e" + dataManager.getBeQuanDiem(player) + " ⚑",
                "§8━━━━━━━━━━━━━━━━━━━━━━━"
        ));

        // --- HÀNG 2: TU LUYỆN CỐT LÕI ---
        boolean isMed = dataManager.isDangTuLuyen(player);
        setItem(inv, 10, Material.AMETHYST_CLUSTER, "§d§lTọa Thiền Tu Luyện", Arrays.asList(
                "§7Nhập định hấp thu linh khí thiên địa",
                "§7ngay tại chỗ để tăng Tu Vi & Điểm Bế Quan.",
                "",
                "§7Trạng thái: " + (isMed ? "§aĐang tọa thiền" : "§7Chưa tọa thiền"),
                "§e► Nhấp để Bật/Tắt tọa thiền!"
        ));

        setItem(inv, 12, Material.LIGHTNING_ROD, "§c§l⚡ Độ Kiếp Đột Phá", Arrays.asList(
                "§7Khiêu chiến Thiên Kiếp để bước lên",
                "§7cảnh giới cao hơn.",
                "",
                "§7Cảnh giới kế: " + (tiepTheo != null ? "§e" + tiepTheo.getTenHienThi() : "§6Đỉnh Phong"),
                "§e► Nhấp để mở giao diện Độ Kiếp!"
        ));

        setItem(inv, 14, Material.BREWING_STAND, "§6§l⚗ Lò Luyện Đan Dược", Arrays.asList(
                "§7Luyện chế 47+ loại Linh Đan từ",
                "§7thảo dược trên đảo để tăng chỉ số",
                "§7vĩnh viễn và đột phá cảnh giới.",
                "",
                "§e► Nhấp để mở Lò Luyện Đan!"
        ));

        setItem(inv, 16, Material.ANVIL, "§6§l⚒ Lò Rèn Linh Khí", Arrays.asList(
                "§7Dùng Khoáng Thạch rèn Kiếm, Giáp,",
                "§7Phụ Kiện 8 phẩm cấp (Phàm -> Linh Khí)",
                "§7giúp tăng mạnh Sinh Lực chống Thiên Kiếp.",
                "",
                "§e► Nhấp để mở Lò Rèn!"
        ));

        // --- HÀNG 3: CỬA HÀNG & TRợ CHIẾN ---
        setItem(inv, 19, Material.EMERALD, "§a§l🏪 Vạn Giới Các (Cửa Hàng)", Arrays.asList(
                "§7Mua bán thảo dược luyện đan, đan dược",
                "§7đột phá và quy đổi Khoáng Thạch.",
                "",
                "§e► Nhấp để vào Vạn Giới Các!"
        ));

        setItem(inv, 21, Material.RAW_GOLD, "§6§l⛏ Quy Đổi Khoáng Thạch", Arrays.asList(
                "§7Đổi Kim Cương, Sắt, Vàng, Ngọc Lục Bảo",
                "§7từ máy Generator thành Khoáng Thạch.",
                "",
                "§7Hiện có: §e" + dataManager.getKhoangThach(player) + " Khoáng Thạch",
                "§e► Nhấp để quy đổi!"
        ));

        setItem(inv, 23, Material.DRAGON_BREATH, "§d§l🎴 Triệu Hồi Linh Thú (Gacha)", Arrays.asList(
                "§7Dùng Đạo Niệm triệu hồi Linh Thú",
                "§7trợ chiến (tăng tới +50% Tu Vi AFK,",
                "§7+20% Sát thương, +15% Phòng thủ).",
                "",
                "§7Đạo Niệm: §b" + dataManager.getDaoNiem(player),
                "§e► Nhấp để mở Triệu Hồi!"
        ));

        setItem(inv, 25, Material.ENDER_EYE, "§5§l⚑ Cửa Hàng Bế Quan", Arrays.asList(
                "§7Dùng Điểm Bế Quan tích lũy khi AFK",
                "§7để đổi Đan Dược đột phá, Đạo Niệm",
                "§7và Khoáng Thạch.",
                "",
                "§7Điểm Bế Quan: §e" + dataManager.getBeQuanDiem(player),
                "§e► Nhấp để mở Cửa Hàng Bế Quan!"
        ));

        // --- HÀNG 4: CHIẾN ĐẤU, NHIỆM VỤ & TIỆN ÍCH ---
        setItem(inv, 28, Material.IRON_SWORD, "§5§l🐾 Linh Thú Viên (Phụ Bản)", Arrays.asList(
                "§7Tham gia 3 khu vực tu luyện:",
                "§7  ▪ §5Huyền Ảnh Bí Cảnh §7(Cày Tu Vi & Đạo Niệm)",
                "§7  ▪ §6Thí Luyện Đạo Tràng §7(X3 Khoáng Thạch khi đào mỏ)",
                "§7  ▪ §cThiên Nguyên Cấm Địa §7(Khiêu chiến Boss Cổ Thần)",
                "",
                "§e► Nhấp để chọn khu vực!"
        ));

        setItem(inv, 30, Material.WRITABLE_BOOK, "§a§l📜 Nhiệm Vụ Hàng Ngày", Arrays.asList(
                "§7Hoàn thành 6 nhiệm vụ mỗi ngày để nhận",
                "§7Tu Vi, Điểm Bế Quan, Đạo Niệm & Khoáng Thạch.",
                "",
                "§e► Nhấp để xem tiến độ!"
        ));

        setItem(inv, 32, Material.ENDER_CHEST, "§b§l🎒 Túi Đồ Hư Không", Arrays.asList(
                "§7Kho chứa vật phẩm Tu Tiên phân theo 8 nhóm,",
                "§7hỗ trợ Tự Động Cất Đồ (AFK) và thanh lý nhanh.",
                "",
                "§e► Nhấp để mở Túi Đồ!"
        ));

        if (cg.ordinal() >= CanhGioi.KIM_DAN.ordinal()) {
            boolean flying = NguKiemTask.isFlying(player);
            int cost = NguKiemTask.getCost(player, dataManager);
            setItem(inv, 34, Material.NETHERITE_SWORD, "§b§l⚔ Ngự Kiếm Phi Hành", Arrays.asList(
                    "§7Đạp phi kiếm bay lượn tự do trên đảo.",
                    "§7Tiêu hao: §e" + cost + " Linh Lực §7/ 2 giây.",
                    "§7Trạng thái: " + (flying ? "§aĐang ngự kiếm" : "§cĐang hạ phàm"),
                    "",
                    "§e► Nhấp để Bật/Tắt Ngự Kiếm!"
            ));
        } else {
            setItem(inv, 34, Material.BARRIER, "§c§l⚔ Ngự Kiếm Phi Hành [Khóa]", Arrays.asList(
                    "§c[Khóa] Cần đạt cảnh giới §eKim Đan",
                    "§cmới có thể ngưng tụ linh lực ngự kiếm!"
            ));
        }

        // --- HÀNG 5: XẾP HẠNG & ĐỔI HỆ ---
        setItem(inv, 39, Material.GOLDEN_HELMET, "§6§l🏆 Bảng Xếp Hạng Thiên Hạ", Arrays.asList(
                "§7Xem Top 10 Cao Thủ theo Cảnh Giới,",
                "§7Tu Vi, Khoáng Thạch và Đạo Niệm.",
                "",
                "§e► Nhấp để xem Bảng Xếp Hạng!"
        ));

        setItem(inv, 41, Material.COMPASS, "§e§l☯ Chọn / Chuyển Hệ Đại Đạo", Arrays.asList(
                "§7Hệ hiện tại: §6" + he.getTenHienThi(),
                "§7Mở giao diện 7 Hệ Tu Luyện để xem",
                "§7thông tin hoặc Nghịch Thiên Cải Mệnh.",
                "",
                "§e► Nhấp để mở!"
        ));

        // Lấp đầy kính đen
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }

    private static void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
}
