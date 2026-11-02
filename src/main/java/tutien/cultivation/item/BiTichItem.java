package tutien.cultivation.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tutien.cultivation.model.CongPhap;

import java.util.ArrayList;
import java.util.List;

/**
 * Trình khởi tạo & nhận diện Bí Tịch / Tàn Quyển Công Pháp vật phẩm:
 * - Gắn thẻ định danh vĩnh viễn qua PersistentDataContainer (tutien:congphap_id, tutien:chapter).
 * - Tương thích 100% với Tàng Kinh Các Tông Môn và hệ thống rơi đồ Bí Cảnh / MythicMobs.
 */
public class BiTichItem {

    public static final NamespacedKey KEY_CONG_PHAP = new NamespacedKey("tutien", "congphap_id");
    public static final NamespacedKey KEY_CHAPTER = new NamespacedKey("tutien", "chapter");

    /**
     * Tạo một vật phẩm Bí Tịch / Tàn Quyển chuẩn của công pháp.
     */
    public static ItemStack createBiTich(CongPhap cp, int chapter) {
        if (cp == null) return null;
        int ch = Math.max(1, Math.min(chapter, cp.getTotalChapters()));

        Material mat = switch (cp.getGradeTier()) {
            case TIEN -> Material.NETHER_STAR;
            case THIEN -> Material.ECHO_SHARD;
            case DIA -> Material.ENCHANTED_BOOK;
            default -> Material.BOOK;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lBí Tịch: §f" + cp.getDisplayName() + " §e§l[Tầng " + ch + "]");

            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("§7Xuất xứ: " + cp.getSourceDisplayName());
            lore.add("§7Phẩm cấp: " + cp.getFormattedGrade());
            lore.add("§7Yêu cầu cảnh giới: §b" + cp.getChapterRequiredRealm(ch).getTenHienThi());
            if (!cp.getElements().isEmpty()) {
                lore.add("§7Thuộc tính: §a" + String.join(", ", cp.getElements()));
            }
            if (cp.getRequiredMythicBoss() != null && !cp.getRequiredMythicBoss().isEmpty()) {
                lore.add("§c⚔ Cần hạ gục: §e" + cp.getRequiredMythicBoss());
            }
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("§e► Chuột phải trên tay để đọc & lĩnh ngộ!");
            lore.add("§7(Hoặc đem hiến tặng Tàng Kinh Các Tông Môn)");

            meta.setLore(lore);

            // Ghi thẻ PDC
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_CONG_PHAP, PersistentDataType.STRING, cp.getId());
            pdc.set(KEY_CHAPTER, PersistentDataType.INTEGER, ch);

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Kiểm tra vật phẩm có phải là Bí Tịch Công Pháp không.
     */
    public static boolean isBiTich(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(KEY_CONG_PHAP, PersistentDataType.STRING);
    }

    /**
     * Lấy ID Công Pháp từ vật phẩm Bí Tịch.
     */
    public static String getCongPhapId(ItemStack item) {
        if (!isBiTich(item)) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(KEY_CONG_PHAP, PersistentDataType.STRING);
    }

    /**
     * Lấy số tầng / chương từ vật phẩm Bí Tịch.
     */
    public static int getChapter(ItemStack item) {
        if (!isBiTich(item)) return 1;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer ch = pdc.get(KEY_CHAPTER, PersistentDataType.INTEGER);
        return ch != null ? ch : 1;
    }
}
