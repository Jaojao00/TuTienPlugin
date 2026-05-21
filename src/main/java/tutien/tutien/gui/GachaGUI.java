package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.PlayerDataManager;

import java.util.*;

/**
 * Hệ thống Gacha Linh Thú — Triệu hồi Linh Thú bằng Đạo Niệm.
 * SPRINT 4: Gacha system với tỷ lệ weighted random, pity counter.
 */
public class GachaGUI implements Listener {

    public static final String GUI_TITLE = "§d§l🎴 Triệu Hồi Linh Thú 🎴";

    // Linh Thú với tỷ lệ và hiệu ứng
    public enum LinhThu {
        HOA_LINH_MAO ("§7Hoa Linh Mao",      Material.CAT_SPAWN_EGG,    60, "§7Thú cơ bản."),
        THANH_LANG    ("§aThanh Lang Vũ",     Material.WOLF_SPAWN_EGG,   20, "§a+10% Tu Vi AFK."),
        HUYET_PHU_QUY ("§cHuyết Phù Quy",     Material.TURTLE_SPAWN_EGG, 10, "§c+15% Phòng thủ."),
        THIEN_MAU_HO  ("§6Thiên Mãu Hổ",     Material.FOX_SPAWN_EGG,    6,  "§6+20% Sát thương."),
        KIM_LONG      ("§eLong Mã Kim Cương", Material.HORSE_SPAWN_EGG,  3,  "§e+30% Tốc độ bay."),
        THIEN_PHUONG  ("§bThiên Phượng",     Material.PARROT_SPAWN_EGG, 1,  "§b+50% Tu Vi AFK.");

        public final String tenHienThi;
        public final Material icon;
        public final int weight; // Trọng số
        public final String effect;

        LinhThu(String ten, Material icon, int weight, String effect) {
            this.tenHienThi = ten; this.icon = icon; this.weight = weight; this.effect = effect;
        }
    }

    // Chi phí triệu hồi (Đạo Niệm = Emerald)
    private static final int COST_SINGLE = 10;  // 1 lần
    private static final int COST_MULTI = 90;    // 10 lần (tiết kiệm 10)
    private static final int PITY_THRESHOLD = 90; // Đảm bảo Linh Thú hiếm sau 90 lần

    private final PlayerDataManager dataManager;
    // Pity counter: UUID -> số lần kéo kể từ lần cuối có được SR
    private final Map<UUID, Integer> pityCount = new HashMap<>();

    private static final Random random = new Random();
    private static final int TOTAL_WEIGHT;

    static {
        int total = 0;
        for (LinhThu lt : LinhThu.values()) total += lt.weight;
        TOTAL_WEIGHT = total;
    }

    public GachaGUI(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public static void open(Player player, PlayerDataManager data, GachaGUI gacha) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Hướng dẫn kiếm Đạo Niệm
        setItem(inv, 0, Material.KNOWLEDGE_BOOK, "§e§l📖 Hướng Dẫn Đạo Niệm",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Đạo Niệm là tiền tệ triệu hồi.",
                "§7Cách kiếm Đạo Niệm:",
                "",
                "§6❶ §fPhụ bản Linh Thú Viên",
                "§7  → Huyền Ảnh Mê Cung: §e+20~50§7/lần",
                "§7  → Thí Luyện Chi Địa: §e+10~30§7/lần",
                "§7  → Thiên Nguyên Boss: §e+50~100§7/lần",
                "",
                "§6❷ §fNhiệm Vụ Hàng Ngày",
                "§7  → Hoàn thành 5 quest: §e+bonus§7",
                "",
                "§6❸ §fĐổi Điểm Bế Quan",
                "§7  → 100 điểm BQ = §e10§7 Đạo Niệm",
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Dùng §e/tutien linhthuvien §7để vào phụ bản."
            ));

        long daoNiem = countEmerald(player);
        int pity = gacha.pityCount.getOrDefault(player.getUniqueId(), 0);

        // Header info
        setItem(inv, 4, Material.AMETHYST_CLUSTER, "§d§lTriệu Hồi Linh Thú",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Đạo Niệm sở hữu: §e" + daoNiem,
                "§7Pity: §c" + pity + "§7/" + PITY_THRESHOLD,
                "§8(Sau " + PITY_THRESHOLD + " lần đảm bảo có §bKim Long§8)",
                "§8━━━━━━━━━━━━━━━━━━━━━"
            ));

        // Nút triệu hồi 1 lần
        setItem(inv, 20, Material.NETHER_STAR, "§a§lTriệu Hồi × 1",
            Arrays.asList(
                "§7Chi phí: §e" + COST_SINGLE + " Đạo Niệm",
                "",
                daoNiem >= COST_SINGLE ? "§a► Nhấp để triệu hồi!" : "§cKhông đủ Đạo Niệm!"
            ));

        // Nút triệu hồi 10 lần
        setItem(inv, 24, Material.BEACON, "§6§lTriệu Hồi × 10",
            Arrays.asList(
                "§7Chi phí: §e" + COST_MULTI + " Đạo Niệm §a(Tiết kiệm 10!)",
                "",
                daoNiem >= COST_MULTI ? "§a► Nhấp để triệu hồi!" : "§cKhông đủ Đạo Niệm!",
                "§8(Đảm bảo 1 Linh Thú §a★R §8trong 10 lần)"
            ));

        // Bảng tỷ lệ
        setItem(inv, 40, Material.BOOK, "§e§lBảng Tỷ Lệ",
            Arrays.asList(
                "§7Hoa Linh Mao   §860%",
                "§aThanh Lang Vũ  §820%",
                "§cHuyết Phù Quy  §810%",
                "§6Thiên Mãu Hổ  §8 6%",
                "§eLong Mã        §8 3%",
                "§bThiên Phượng  §8 1%"
            ));

        // Fill
        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta(); bm.setDisplayName(" "); border.setItemMeta(bm);
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, border);

        player.openInventory(inv);
    }

    private static void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta(); m.setDisplayName(name); m.setLore(lore); item.setItemMeta(m);
        inv.setItem(slot, item);
    }

    private static long countEmerald(Player player) {
        long total = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.EMERALD) {
                // Kiểm tra tên đặc biệt "Đạo Niệm"
                if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()
                        && is.getItemMeta().getDisplayName().contains("Đạo Niệm")) {
                    total += is.getAmount();
                }
            }
        }
        return total;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getRawSlot();

        if (slot == 20) {
            // Triệu hồi 1 lần
            if (!consumeDaoNiem(player, COST_SINGLE)) {
                player.sendMessage("§c[Gacha] §fKhông đủ Đạo Niệm!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            LinhThu result = rollSingle(player);
            showResult(player, result, 1);
            open(player, dataManager, this);

        } else if (slot == 24) {
            // Triệu hồi 10 lần
            if (!consumeDaoNiem(player, COST_MULTI)) {
                player.sendMessage("§c[Gacha] §fKhông đủ Đạo Niệm!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            List<LinhThu> results = rollMulti(player, 10);
            // Đảm bảo có 1 Thanh Lang trở lên trong 10 lần
            if (results.stream().allMatch(lt -> lt == LinhThu.HOA_LINH_MAO)) {
                results.set(random.nextInt(10), LinhThu.THANH_LANG);
            }
            for (LinhThu lt : results) {
                player.getInventory().addItem(createLinhThuItem(lt));
            }
            long rare = results.stream().filter(lt -> lt.weight <= 6).count();
            player.sendMessage("§d§l[Gacha] §fTriệu hồi ×10 hoàn tất! §cHiếm: §e" + rare);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            open(player, dataManager, this);
        }
    }

    private LinhThu rollSingle(Player player) {
        UUID uuid = player.getUniqueId();
        int pity = pityCount.merge(uuid, 1, Integer::sum);

        // Pity: nếu đủ ngưỡng thì đảm bảo Kim Long
        if (pity >= PITY_THRESHOLD) {
            pityCount.put(uuid, 0);
            return LinhThu.KIM_LONG;
        }

        int roll = random.nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (LinhThu lt : LinhThu.values()) {
            cumulative += lt.weight;
            if (roll < cumulative) {
                // Reset pity nếu ra SR (hiếm)
                if (lt.weight <= 3) pityCount.put(uuid, 0);
                return lt;
            }
        }
        return LinhThu.HOA_LINH_MAO;
    }

    private List<LinhThu> rollMulti(Player player, int count) {
        List<LinhThu> results = new ArrayList<>();
        for (int i = 0; i < count; i++) results.add(rollSingle(player));
        return results;
    }

    private void showResult(Player player, LinhThu lt, int count) {
        player.sendMessage("§d§l[Gacha] §fTriệu hồi được: " + lt.tenHienThi);
        player.sendMessage("§7Hiệu ứng: " + lt.effect);
        player.getInventory().addItem(createLinhThuItem(lt));

        if (lt.weight <= 3) {
            Bukkit.broadcastMessage("§d§l[Thiên Mệnh] §f" + player.getName() + " §fđã triệu hồi được " + lt.tenHienThi + "§f!");
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        }
    }

    private ItemStack createLinhThuItem(LinhThu lt) {
        ItemStack item = new ItemStack(lt.icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(lt.tenHienThi);
        meta.setLore(Arrays.asList(
            "§7Linh Thú: " + lt.tenHienThi,
            "§7Hiệu ứng: " + lt.effect,
            "§7Tỷ lệ: §c" + String.format("%.1f%%", (lt.weight * 100.0 / TOTAL_WEIGHT)),
            "",
            "§8[Linh Thú Hư Không]"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private boolean consumeDaoNiem(Player player, int amount) {
        // Đếm Đạo Niệm
        if (countEmerald(player) < amount) return false;
        int toRemove = amount;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is == null || is.getType() != Material.EMERALD) continue;
            if (!is.hasItemMeta() || !is.getItemMeta().getDisplayName().contains("Đạo Niệm")) continue;
            if (is.getAmount() >= toRemove) { is.setAmount(is.getAmount() - toRemove); return true; }
            else { toRemove -= is.getAmount(); is.setAmount(0); }
        }
        return toRemove <= 0;
    }
}
