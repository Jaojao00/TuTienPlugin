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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tutien.combat.StatsManager;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;

import java.util.*;

/**
 * Hệ thống Gacha Linh Thú — Triệu hồi Linh Thú bằng Đạo Niệm.
 * v2.2 Skyblock Edition:
 *   - Đồng bộ sử dụng Đạo Niệm từ PlayerDataManager (hỗ trợ tự động quy đổi Đạo Niệm vật lý cũ nếu có).
 *   - Pity counter lưu trữ bền vững trong PlayerDataManager.
 *   - Linh Thú được gắn PDC tag KEY_PET_ID để kích hoạt buff thực tế (Sát thương, Phòng thủ, Tốc độ bay, Tu Vi AFK).
 */
public class GachaGUI implements Listener {

    public static final String GUI_TITLE = "§d§l🎴 Triệu Hồi Linh Thú 🎴";

    // Linh Thú với tỷ lệ và hiệu ứng
    public enum LinhThu {
        HOA_LINH_MAO ("§7Hoa Linh Mao",      Material.CAT_SPAWN_EGG,    60, "§7+5 Sinh lực tối đa."),
        THANH_LANG    ("§aThanh Lang Vũ",     Material.WOLF_SPAWN_EGG,   20, "§a+10% Tu Vi AFK."),
        HUYET_PHU_QUY ("§cHuyết Phù Quy",     Material.TURTLE_SPAWN_EGG, 10, "§c+15% Phòng thủ (Giảm sát thương)."),
        THIEN_MAU_HO  ("§6Thiên Mãu Hổ",     Material.FOX_SPAWN_EGG,    6,  "§6+20% Sát thương cận chiến."),
        KIM_LONG      ("§eLong Mã Kim Cương", Material.HORSE_SPAWN_EGG,  3,  "§e+30% Tốc độ di chuyển & bay."),
        THIEN_PHUONG  ("§bThiên Phượng",     Material.PARROT_SPAWN_EGG, 1,  "§b+50% Tu Vi AFK & +25 Sinh lực.");

        public final String tenHienThi;
        public final Material icon;
        public final int weight;
        public final String effect;

        LinhThu(String ten, Material icon, int weight, String effect) {
            this.tenHienThi = ten;
            this.icon = icon;
            this.weight = weight;
            this.effect = effect;
        }
    }

    private static final int COST_SINGLE = 10;
    private static final int COST_MULTI = 90;
    private static final int PITY_THRESHOLD = 90;

    private final PlayerDataManager dataManager;
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
        // Tự động gộp các vật phẩm "Đạo Niệm" vật lý trong túi vào số dư ảo để đồng bộ
        absorbLegacyDaoNiem(player, data);

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Hướng dẫn kiếm Đạo Niệm
        setItem(inv, 0, Material.KNOWLEDGE_BOOK, "§e§l📖 Hướng Dẫn Đạo Niệm",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Đạo Niệm là tinh hoa triệu hồi Linh Thú.",
                "§7Cách kiếm Đạo Niệm trên Skyblock:",
                "",
                "§6❶ §fPhụ bản Linh Thú Viên (§e/tt linhthuvien§f)",
                "§7  → Huyền Ảnh Bí Cảnh: §e+2~5§7 mỗi 10 quái",
                "§7  → Thí Luyện Đạo Tràng: §eĐào mỏ rớt ngẫu nhiên",
                "§7  → Thiên Nguyên Boss: §e+15~30§7/lần",
                "",
                "§6❷ §fNhiệm Vụ Hàng Ngày (§e/tt nhiemvu§f)",
                "§7  → Hoàn thành nhiệm vụ nhận Đạo Niệm",
                "",
                "§6❸ §fĐổi Điểm Bế Quan (§e/tt bequan§f)",
                "§7  → 50 điểm Bế Quan = §e10§7 Đạo Niệm",
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§aMẹo: §7Giữ Linh Thú trong túi đồ để nhận buff!"
            ));

        long daoNiem = data.getDaoNiem(player);
        int pity = data.getGachaPity(player);

        // Header info
        setItem(inv, 4, Material.AMETHYST_CLUSTER, "§d§lTriệu Hồi Linh Thú",
            Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Đạo Niệm sở hữu: §b" + daoNiem + " ✦",
                "§7Bảo hiểm (Pity): §c" + pity + "§7/" + PITY_THRESHOLD,
                "§8(Đạt " + PITY_THRESHOLD + " lần chắc chắn nhận §eLong Mã§8+)",
                "§8━━━━━━━━━━━━━━━━━━━━━"
            ));

        // Nút triệu hồi 1 lần
        setItem(inv, 20, Material.NETHER_STAR, "§a§lTriệu Hồi × 1",
            Arrays.asList(
                "§7Chi phí: §b" + COST_SINGLE + " Đạo Niệm",
                "",
                daoNiem >= COST_SINGLE ? "§a► Nhấp để triệu hồi!" : "§c✘ Không đủ Đạo Niệm!"
            ));

        // Nút triệu hồi 10 lần
        setItem(inv, 24, Material.BEACON, "§6§lTriệu Hồi × 10",
            Arrays.asList(
                "§7Chi phí: §b" + COST_MULTI + " Đạo Niệm §a(Tiết kiệm 10!)",
                "",
                daoNiem >= COST_MULTI ? "§a► Nhấp để triệu hồi!" : "§c✘ Không đủ Đạo Niệm!",
                "§8(Đảm bảo ít nhất 1 §aThanh Lang Vũ§8 trở lên)"
            ));

        // Bảng tỷ lệ
        setItem(inv, 40, Material.BOOK, "§e§lBảng Tỷ Lệ & Hiệu Ứng",
            Arrays.asList(
                "§7Hoa Linh Mao   §8(60%) §7→ +5 Sinh lực",
                "§aThanh Lang Vũ  §8(20%) §7→ +10% Tu Vi AFK",
                "§cHuyết Phù Quy  §8(10%) §7→ +15% Phòng thủ",
                "§6Thiên Mãu Hổ  §8( 6%) §7→ +20% Sát thương",
                "§eLong Mã        §8( 3%) §7→ +30% Tốc độ",
                "§bThiên Phượng  §8( 1%) §7→ +50% Tu Vi AFK & +25 HP",
                "",
                "§e* Giữ Linh Thú trong túi đồ để kích hoạt!"
            ));

        // Fill
        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, border);

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

    /**
     * Tự động thu hồi các viên Emerald tên "Đạo Niệm" cũ trong túi đồ vào hệ thống tiền tệ ảo.
     */
    private static void absorbLegacyDaoNiem(Player player, PlayerDataManager data) {
        int absorbed = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.EMERALD && is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                if (is.getItemMeta().getDisplayName().contains("Đạo Niệm")) {
                    absorbed += is.getAmount();
                    is.setAmount(0);
                }
            }
        }
        if (absorbed > 0) {
            data.addDaoNiem(player, absorbed);
            player.sendMessage("§b[Hệ Thống] §fĐã đồng bộ §e+" + absorbed + " Đạo Niệm §ftừ túi đồ vào tài khoản!");
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getRawSlot();
        if (slot >= 54) return;

        if (slot == 20) {
            if (!consumeDaoNiem(player, COST_SINGLE)) {
                player.sendMessage("§c[Gacha] §fKhông đủ Đạo Niệm! Cần §b" + COST_SINGLE + " Đạo Niệm§f.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            LinhThu result = rollSingle(player);
            showResult(player, result);
            StatsManager.applyStats(player, dataManager);
            open(player, dataManager, this);

        } else if (slot == 24) {
            if (!consumeDaoNiem(player, COST_MULTI)) {
                player.sendMessage("§c[Gacha] §fKhông đủ Đạo Niệm! Cần §b" + COST_MULTI + " Đạo Niệm§f.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            List<LinhThu> results = rollMulti(player, 10);
            if (results.stream().allMatch(lt -> lt == LinhThu.HOA_LINH_MAO)) {
                results.set(random.nextInt(10), LinhThu.THANH_LANG);
            }
            for (LinhThu lt : results) {
                ItemKeys.giveItemSafe(player, createLinhThuItem(lt));
                if (lt.weight <= 3) {
                    Bukkit.broadcastMessage("§d§l[Thiên Mệnh] §e" + player.getName() + " §fđã triệu hồi được Linh Thú hiếm " + lt.tenHienThi + "§f!");
                }
            }
            StatsManager.applyStats(player, dataManager);
            long rare = results.stream().filter(lt -> lt.weight <= 6).count();
            player.sendMessage("§d§l[Gacha] §fTriệu hồi ×10 hoàn tất! §cLinh Thú Hiếm: §e" + rare);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            open(player, dataManager, this);
        }
    }

    private LinhThu rollSingle(Player player) {
        int pity = dataManager.getGachaPity(player) + 1;
        dataManager.setGachaPity(player, pity);

        if (pity >= PITY_THRESHOLD) {
            dataManager.setGachaPity(player, 0);
            return LinhThu.KIM_LONG;
        }

        int roll = random.nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (LinhThu lt : LinhThu.values()) {
            cumulative += lt.weight;
            if (roll < cumulative) {
                if (lt.weight <= 3) {
                    dataManager.setGachaPity(player, 0);
                }
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

    private void showResult(Player player, LinhThu lt) {
        player.sendMessage("§d§l[Gacha] §fTriệu hồi được: " + lt.tenHienThi);
        player.sendMessage("§7Hiệu ứng: " + lt.effect);
        ItemKeys.giveItemSafe(player, createLinhThuItem(lt));

        if (lt.weight <= 3) {
            Bukkit.broadcastMessage("§d§l[Thiên Mệnh] §e" + player.getName() + " §fđã triệu hồi được " + lt.tenHienThi + "§f!");
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        }
    }

    public static ItemStack createLinhThuItem(LinhThu lt) {
        ItemStack item = new ItemStack(lt.icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lt.tenHienThi);
            meta.setLore(Arrays.asList(
                "§7Linh Thú: " + lt.tenHienThi,
                "§7Hiệu ứng: " + lt.effect,
                "§7Tỷ lệ: §c" + String.format("%.1f%%", (lt.weight * 100.0 / TOTAL_WEIGHT)),
                "",
                "§e[Trợ Chiến] §7Để trong túi đồ để kích hoạt hiệu ứng",
                "§8[Linh Thú Hư Không]"
            ));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING, ItemKeys.TYPE_PET);
            pdc.set(ItemKeys.KEY_PET_ID, PersistentDataType.STRING, lt.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean consumeDaoNiem(Player player, int amount) {
        absorbLegacyDaoNiem(player, dataManager);
        long current = dataManager.getDaoNiem(player);
        if (current < amount) return false;
        dataManager.setDaoNiem(player, current - amount);
        return true;
    }
}
