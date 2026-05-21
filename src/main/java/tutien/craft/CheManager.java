package tutien.craft;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.TuTienPlugin;

import java.util.*;

/**
 * Quản lý hệ thống chế tác trang bị với 8 phẩm cấp.
 * SPRINT 3: Chế Tác Trang Bị hoàn chỉnh.
 */
public class CheManager {

    // 8 phẩm cấp từ thấp đến cao
    public enum PhamCap {
        PHAM("§7Phàm Phẩm", 60),
        HA("§aHạ Phẩm", 15),
        TRUNG("§bTrung Phẩm", 10),
        THUONG("§dThượng Phẩm", 7),
        CHI_BAO("§6Chí Bảo", 4),
        LINH_BAO("§cLinh Bảo", 2),
        CHAN_BAO("§5Chân Bảo", 1),
        LINH_KHI("§e§lLinh Khí", 1); // Mỗi 200 lần roll

        public final String display;
        public final int weight; // Trọng số xác suất

        PhamCap(String display, int weight) {
            this.display = display;
            this.weight = weight;
        }
    }

    // Các công thức chế tác
    public enum CheRecipe {
        KIEM_CAO("Kiếm Linh Khí",     Material.NETHERITE_SWORD, 15, "Kiếm"),
        KIEM_TRUNG("Kiếm Linh Khí",   Material.IRON_SWORD,      5,  "Kiếm"),
        GIAP_THAN("Giáp Thần Lực",   Material.NETHERITE_CHESTPLATE, 20, "Giáp"),
        GIAP_TRUNG("Giáp Thiên Phong",Material.IRON_CHESTPLATE, 8, "Giáp"),
        MU_THAN("Mũ Linh Quang",      Material.NETHERITE_HELMET, 8, "Giáp"),
        QUAN_THAN("Quần Linh Phong",  Material.NETHERITE_LEGGINGS, 10, "Giáp"),
        GIAY_THAN("Giày Linh Vân",    Material.NETHERITE_BOOTS, 8, "Giáp"),
        PHU_KIEN("Phụ Kiện Hộ Mệnh", Material.NETHER_STAR, 12, "Phụ Kiện");

        public final String displayName;
        public final Material material;
        public final int oreRequired;
        public final String type;

        CheRecipe(String displayName, Material material, int oreRequired, String type) {
            this.displayName = displayName; this.material = material;
            this.oreRequired = oreRequired; this.type = type;
        }
    }

    private final TuTienPlugin plugin;
    private final Map<UUID, Integer> capCheMap = new HashMap<>();
    private final Map<UUID, Integer> soLanCheMap = new HashMap<>();
    private static final Random random = new Random();

    public CheManager(TuTienPlugin plugin) { this.plugin = plugin; }

    public int getCapChe(Player p) { return capCheMap.getOrDefault(p.getUniqueId(), 1); }
    public void setCapChe(Player p, int cap) { capCheMap.put(p.getUniqueId(), cap); }
    public int getSoLanChe(Player p) { return soLanCheMap.getOrDefault(p.getUniqueId(), 0); }
    public void addSoLanChe(Player p, int n) { soLanCheMap.merge(p.getUniqueId(), n, Integer::sum); }

    /**
     * Lăn phẩm cấp của trang bị dựa trên trọng số và bonus cấp lò.
     */
    public static ItemStack rollCraft(CheRecipe recipe, int capBonus) {
        // Tính tổng trọng số
        int totalWeight = 0;
        for (PhamCap pc : PhamCap.values()) totalWeight += pc.weight;

        // Áp dụng bonus cấp lò: giảm tỷ lệ Phàm phẩm, tăng tỷ lệ các cấp cao
        int roll = random.nextInt(Math.max(1, totalWeight - capBonus));
        int cumulative = 0;
        PhamCap selected = PhamCap.PHAM;
        for (PhamCap pc : PhamCap.values()) {
            cumulative += pc.weight;
            if (roll < cumulative) { selected = pc; break; }
        }

        // Tạo item với tên phẩm cấp
        ItemStack item = new ItemStack(recipe.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(selected.display + " " + recipe.displayName);

        // Tính chỉ số theo phẩm cấp
        int phamCapIndex = selected.ordinal(); // 0=Phàm, 7=Linh Khí
        List<String> lore = new ArrayList<>();
        lore.add("§7Phẩm cấp: " + selected.display);
        lore.add("§7Loại: §e" + recipe.type);
        if (recipe.type.equals("Kiếm")) {
            double dmg = 2.0 + phamCapIndex * 3.0;
            lore.add("§7Sát thương: §c+" + String.format("%.1f", dmg));
        } else if (recipe.type.equals("Giáp")) {
            double hp = 5.0 + phamCapIndex * 8.0;
            lore.add("§7Sinh lực: §a+" + String.format("%.1f", hp));
        } else {
            lore.add("§7Hiệu ứng: §dLinh Khí +" + (phamCapIndex + 1));
        }
        lore.add("");
        lore.add("§8[Trang bị Tu Tiên]");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
