package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Lớp tạo giao diện rương đồ với các vật phẩm hiển thị ngẫu nhiên
 * và phân chia chức năng theo từng Cảnh Giới.
 */
public class MainMenu {

    // Danh sách các vật phẩm ngẫu nhiên để làm icon cho Shop và Đổi Thưởng
    private static final List<Material> RANDOM_ICONS = Arrays.asList(
            Material.DIAMOND, Material.EMERALD, Material.AMETHYST_SHARD,
            Material.GOLD_INGOT, Material.LAPIS_LAZULI, Material.NETHER_STAR, Material.ECHO_SHARD
    );

    public static void open(Player player, PlayerDataManager dataManager) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8§lHệ Thống Tu Tiên");
        CanhGioi cg = dataManager.getCanhGioi(player);
        Random random = new Random();

        // --- 1. Kiểm tra tu vi (Ô số 10) ---
        ItemStack statsItem = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName("§e§lThông Tin Tu Vi");

        CanhGioi tiepTheo = cg.getCanhGioiTiepTheo();
        String tuViString = (tiepTheo != null) ? (dataManager.getTuVi(player) + " / " + tiepTheo.getTuViYeuCau()) : (dataManager.getTuVi(player) + " (Đỉnh phong)");

        statsMeta.setLore(Arrays.asList(
                "§7Cảnh giới hiện tại: §b" + cg.getTenHienThi(),
                "§7Tu vi hiện tại: §a" + tuViString
        ));
        statsItem.setItemMeta(statsMeta);
        inv.setItem(10, statsItem);

        // --- 2. Đổi Thưởng (Ô số 12) - Mở khóa từ Luyện Khí ---
        Material randomRewardMat = RANDOM_ICONS.get(random.nextInt(RANDOM_ICONS.size()));
        ItemStack rewardItem = new ItemStack(randomRewardMat);
        ItemMeta rewardMeta = rewardItem.getItemMeta();
        rewardMeta.setDisplayName("§b§lĐổi Thưởng Vật Phẩm");

        if (cg.ordinal() >= CanhGioi.LUYEN_KHI.ordinal()) {
            rewardMeta.setLore(Arrays.asList("§7Dùng vật phẩm rớt từ quái", "§7để đổi lấy điểm Tu Vi.", "", "§a[!] Đã mở khóa (Nhấp để xem)"));
        } else {
            rewardItem.setType(Material.BARRIER); // Đổi thành hình rào cản nếu chưa đủ cấp
            rewardMeta.setLore(Arrays.asList("§c[Khóa] Cần đạt cảnh giới §eLuyện Khí", "§cđể mở khóa chức năng này!"));
        }
        rewardItem.setItemMeta(rewardMeta);
        inv.setItem(12, rewardItem);

        // --- 3. Cửa Hàng (Ô số 14) - MỞ CỬA TỰ DO ---
        Material randomShopMat = RANDOM_ICONS.get(random.nextInt(RANDOM_ICONS.size()));
        ItemStack shopItem = new ItemStack(randomShopMat);
        ItemMeta shopMeta = shopItem.getItemMeta();
        shopMeta.setDisplayName("§a§lVạn Giới Các (Cửa Hàng)");
        shopMeta.setLore(Arrays.asList("§7Dùng Linh Thạch để mua sắm", "§7Đan Dược và Pháp Bảo.", "§7(Vật phẩm tùy cấp bậc mới mua được)", "", "§a[!] Nhấp để vào xem hàng"));
        shopItem.setItemMeta(shopMeta);
        inv.setItem(14, shopItem);

        // --- 4. Ngự Kiếm Phi Hành (Ô số 16) - Mở khóa từ Kim Đan ---
        ItemStack flyItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta flyMeta = flyItem.getItemMeta();
        flyMeta.setDisplayName("§d§lNgự Kiếm Phi Hành");

        if (cg.ordinal() >= CanhGioi.KIM_DAN.ordinal()) {
            String status = player.getAllowFlight() ? "§aĐang bay" : "§cĐang hạ phàm";
            flyMeta.setLore(Arrays.asList(
                    "§7Thi triển pháp thuật đạp kiếm bay",
                    "§7tự do trên không trung.",
                    "",
                    "§7Trạng thái: " + status,
                    "§e[!] Nhấp để bật/tắt bay!"
            ));
        } else {
            flyItem.setType(Material.BARRIER);
            flyMeta.setLore(Arrays.asList("§c[Khóa] Cần đạt cảnh giới §eKim Đan", "§cđể ngưng tụ linh lực ngự kiếm!"));
        }
        flyItem.setItemMeta(flyMeta);
        inv.setItem(16, flyItem);

        // --- Trang trí (Lấp đầy kính đen) ---
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        // Mở giao diện
        player.openInventory(inv);
    }
}