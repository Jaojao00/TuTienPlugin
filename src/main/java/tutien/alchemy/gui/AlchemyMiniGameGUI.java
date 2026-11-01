package tutien.alchemy.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.alchemy.model.AlchemyHeatAction;
import tutien.alchemy.model.AlchemyQuality;
import tutien.alchemy.model.AlchemyRecipe;
import tutien.alchemy.model.AlchemySession;
import tutien.alchemy.service.AlchemyService;

import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện Lò Luyện Đan tương tác thời gian thực (Real-time Cauldron Mini-game).
 * Hiển thị nhiệt kế trực quan, hỏa hầu và các nút thao tác.
 */
public class AlchemyMiniGameGUI implements Listener {

    public static final String GUI_TITLE_PREFIX = "§6§l🔥 HỎA HẦU ĐAN LÒ";

    private final AlchemyService alchemyService;

    public AlchemyMiniGameGUI(AlchemyService alchemyService) {
        this.alchemyService = alchemyService;
    }

    public static void open(Player player, AlchemyService service) {
        if (player == null || service == null) return;
        AlchemySession session = service.getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c[Đan Lò] Ngươi không có mẻ đan nào đang luyện!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX);
        render(inv, session);
        player.openInventory(inv);
    }

    /**
     * Cập nhật lại toàn bộ slot giao diện trong thời gian thực
     */
    public static void render(Inventory inv, AlchemySession session) {
        if (inv == null || session == null) return;

        AlchemyRecipe recipe = session.getRecipe();
        double currentTemp = session.getCurrentTemperature();
        double idealTemp = recipe.getIdealTemperature();
        double minTol = recipe.getMinToleranceTemp();
        double maxTol = recipe.getMaxToleranceTemp();
        int remaining = session.getRemainingSeconds();
        double score = session.calculateScore();
        AlchemyQuality expectedQuality = AlchemyQuality.fromScore(score);

        // 1. HÀNG 1 (Slot 0..8): Thanh Nhiệt Kế Trực Quan
        // 9 mức: < minTol (Xanh Dương), trong dải (Lục/Lime), > maxTol (Cam/Đỏ)
        for (int i = 0; i < 9; i++) {
            double slotTemp = 50.0 + (i * 120.0); // 50..1010°C
            Material mat;
            String statusName;

            if (slotTemp < minTol) {
                mat = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                statusName = "§b❄ Nhiệt Độ Lạnh";
            } else if (slotTemp <= maxTol) {
                mat = Material.LIME_STAINED_GLASS_PANE;
                statusName = "§a✔ VÙNG HỎA HẦU CHUẨN";
            } else if (slotTemp < recipe.getExplosionTemp()) {
                mat = Material.ORANGE_STAINED_GLASS_PANE;
                statusName = "§6⚠ Hơi Quá Nhiệt";
            } else {
                mat = Material.RED_STAINED_GLASS_PANE;
                statusName = "§c§l☠ NGUY HIỂM: NỔ LÒ!";
            }

            // Đánh dấu vị trí kim đo nhiệt độ hiện tại
            boolean isNeedle = Math.abs(currentTemp - slotTemp) <= 60.0;
            if (isNeedle) {
                mat = Material.BLAZE_POWDER;
                statusName = "§e§l▶ KIM NHIỆT: " + String.format("%.0f", currentTemp) + "°C";
            }

            ItemStack pane = new ItemStack(mat);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(statusName);
                List<String> lore = new ArrayList<>();
                lore.add("§7Vùng nhiệt: §f" + String.format("%.0f", slotTemp) + "°C");
                lore.add("§7Nhiệt độ hiện tại: " + getTempColor(currentTemp, minTol, maxTol) + String.format("%.1f", currentTemp) + "°C");
                lore.add("§7Mục tiêu lý tưởng: §a" + String.format("%.0f", idealTemp) + "°C §8(" + String.format("%.0f", minTol) + " - " + String.format("%.0f", maxTol) + "°C)");
                meta.setLore(lore);
                pane.setItemMeta(meta);
            }
            inv.setItem(i, pane);
        }

        // 2. HÀNG 2: CÁC NÚT THAO TÁC HỎA HẦU
        // Slot 10: Văn Hỏa (+15°C)
        ItemStack vanHoa = createActionItem(Material.TORCH, "§e§l[🔥 Văn Hỏa (+15°C)]",
                List.of("§7Lửa nhỏ, nâng nhiệt độ từ từ.", "§e[Nhấp để thêm lửa]"));
        inv.setItem(10, vanHoa);

        // Slot 11: Vũ Hỏa (+40°C)
        ItemStack vuHoa = createActionItem(Material.FIRE_CHARGE, "§c§l[💥 Vũ Hỏa (+40°C)]",
                List.of("§7Lửa lớn, cấp tốc nâng nhiệt khi lò nguội.", "§e[Nhấp để hừng lửa]"));
        inv.setItem(11, vuHoa);

        // Slot 13: Lò Đan Trung Tâm
        ItemStack furnace = new ItemStack(Material.BLAST_FURNACE);
        ItemMeta fMeta = furnace.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName("§6§lBát Quái Đan Lò: §b" + recipe.getDisplayName());
            List<String> fLore = new ArrayList<>();
            fLore.add("§8━━━━━━━━━━━━━━━━━━━━━");
            fLore.add("§7Nhiệt độ lò: " + getTempColor(currentTemp, minTol, maxTol) + String.format("%.1f", currentTemp) + "°C");
            fLore.add("§7Dải hỏa hầu chuẩn: §a" + String.format("%.0f", minTol) + "°C §7- §a" + String.format("%.0f", maxTol) + "°C");
            fLore.add("§7Thời gian còn lại: §e" + remaining + " giây");
            fLore.add("§7Điểm ổn định: §a" + String.format("%.1f", score) + "/100");
            fLore.add("§7Dự kiến: " + expectedQuality.getDisplayName());
            if (session.getConsecutiveOverheatTicks() > 0) {
                fLore.add("§c§l⚠ CẢNH BÁO QUÁ NHIỆT (" + session.getConsecutiveOverheatTicks() + "/3) - NGUY CƠ NỔ LÒ!");
            }
            fLore.add("§8━━━━━━━━━━━━━━━━━━━━━");
            fLore.add("§fHãy duy trì nhiệt độ trong dải xanh lục!");
            fMeta.setLore(fLore);
            furnace.setItemMeta(fMeta);
        }
        inv.setItem(13, furnace);

        // Slot 15: Hạ Hỏa (-25°C)
        ItemStack haHoa = createActionItem(Material.SNOWBALL, "§b§l[❄ Hạ Hỏa (-25°C)]",
                List.of("§7Hạ nhiệt gấp khi lò sắp quá nhiệt.", "§e[Nhấp để hạ nhiệt]"));
        inv.setItem(15, haHoa);

        // Slot 16: Thuần Hỏa (Cân bằng)
        ItemStack thuanHoa = createActionItem(Material.AMETHYST_SHARD, "§d§l[✨ Thuần Lửa (Linh Lực)]",
                List.of("§7Truyền linh lực ổn định hỏa diễm.", "§e[Nhấp để thuần hóa]"));
        inv.setItem(16, thuanHoa);

        // 3. HÀNG 3: KẾT ĐAN & HỦY LÒ
        // Slot 18: Hủy Lò (Hoàn nguyên)
        ItemStack cancel = createActionItem(Material.BARRIER, "§c§l[🛑 Dập Lửa & Hủy Lò]",
                List.of("§7Dập tắt lò đan và hoàn trả", "§7100% nguyên liệu ký quỹ về túi đồ.", "§c[Nhấp để hủy mẻ luyện]"));
        inv.setItem(18, cancel);

        // Slot 22: Ký Quỹ Thông Tin
        ItemStack escrowInfo = new ItemStack(Material.CHEST);
        ItemMeta cMeta = escrowInfo.getItemMeta();
        if (cMeta != null) {
            cMeta.setDisplayName("§e§lNguyên Liệu Đã Khóa (Ký Quỹ)");
            List<String> cLore = new ArrayList<>();
            for (ItemStack is : session.getEscrowItems()) {
                if (is != null) {
                    cLore.add("§7 ▪ §f" + is.getType().name() + " §ex" + is.getAmount());
                }
            }
            cLore.add("");
            cLore.add("§a✔ Bảo vệ nguyên tử chống mất đồ và dupe đồ.");
            cMeta.setLore(cLore);
            escrowInfo.setItemMeta(cMeta);
        }
        inv.setItem(22, escrowInfo);

        // Slot 26: Kết Đan Sớm
        ItemStack ketDan = createActionItem(Material.NETHER_STAR, "§6§l[★ Kết Đan Ngay]",
                List.of("§7Ngưng tụ dược lực ngay lập tức.", "§e[Nhấp để thu hoạch đan]"));
        inv.setItem(26, ketDan);
    }

    private static ItemStack createActionItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String getTempColor(double current, double min, double max) {
        if (current < min) return "§b";
        if (current <= max) return "§a";
        return "§c";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(GUI_TITLE_PREFIX)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        AlchemySession session = alchemyService.getSession(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 10 -> { // Văn hỏa
                alchemyService.applyHeatAction(player, AlchemyHeatAction.VAN_HOA);
                render(event.getInventory(), session);
            }
            case 11 -> { // Vũ hỏa
                alchemyService.applyHeatAction(player, AlchemyHeatAction.VU_HOA);
                render(event.getInventory(), session);
            }
            case 15 -> { // Hạ hỏa
                alchemyService.applyHeatAction(player, AlchemyHeatAction.HA_HOA);
                render(event.getInventory(), session);
            }
            case 16 -> { // Thuần lửa
                alchemyService.applyHeatAction(player, AlchemyHeatAction.THUAN_LUA);
                render(event.getInventory(), session);
            }
            case 18 -> { // Hủy lò
                player.closeInventory();
                alchemyService.applyHeatAction(player, AlchemyHeatAction.HUY_LO);
            }
            case 26 -> { // Kết đan sớm
                player.closeInventory();
                alchemyService.applyHeatAction(player, AlchemyHeatAction.KET_DAN);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().startsWith(GUI_TITLE_PREFIX)) return;
        // Người chơi đóng GUI: KHÔNG làm mất đồ ký quỹ! Mẻ đan tiếp tục chạy ở background hoặc người chơi có thể mở lại qua /tutien luyendan
    }
}
