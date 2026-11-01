package tutien.cultivation.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.MeditationType;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.cultivation.service.MasteryService;
import tutien.element.LinhCanData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Giao diện Tọa Thiền Tham Ngộ Khẩu Quyết (ThamNgoGUI) - Phase 2.4:
 * - Hiển thị tiến độ lĩnh hội trực quan qua thanh tiến độ Unicode.
 * - Cho phép Bật/Tắt Tọa Thiền Tham Ngộ (MeditationType.THAM_NGO).
 * - Sử dụng Điểm Đạo Ngộ (DaoNgoPoints) để gia tốc thấu triệt khẩu quyết.
 */
public class ThamNgoGUI {

    public static void open(Player player, TuTienPlugin plugin) {
        if (player == null || plugin == null) return;

        CongPhapGUIConfig config = plugin.getCongPhapGUIConfig();
        if (config == null) return;

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);
        String activePrimaryId = cultData.getActivePrimary();

        if (activePrimaryId == null || activePrimaryId.equalsIgnoreCase("CHUA_CO") || plugin.getCongPhapManager() == null) {
            player.sendMessage("§c§l[!] §fNgươi chưa kích hoạt Công Pháp Chủ Tu để tham ngộ!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            CongPhapGUI.open(player, plugin, 0);
            return;
        }

        CongPhap cp = plugin.getCongPhapManager().getCongPhap(activePrimaryId);
        if (cp == null) {
            player.sendMessage("§c§l[!] §fCông pháp chủ tu không tồn tại trong điển tịch!");
            CongPhapGUI.open(player, plugin, 0);
            return;
        }

        CongPhapProgress prog = cultData.getOrCreateProgress(activePrimaryId);
        int targetChapter = prog.getTargetChapter();

        ThamNgoHolder holder = new ThamNgoHolder(activePrimaryId);
        Inventory inv = Bukkit.createInventory(holder, config.getThamNgoSize(), config.getThamNgoTitle());
        holder.setInventory(inv);

        LinhCanData linhCan = dataManager.getLinhCanData(player);
        double phi = plugin.getCongPhapManager().getCompatibilityService().calculateCompatibility(linhCan, cp);
        double ngoTinh = dataManager.getNgoTinh(player);
        long daoNgoPoints = dataManager.getDaoNgoPoints(player);
        boolean isThamNgoActive = dataManager.getMeditationType(player) == MeditationType.THAM_NGO;

        // ==========================================
        // 1. Ô THÔNG TIN CÔNG PHÁP ĐANG THAM NGỘ (Slot 13)
        // ==========================================
        ItemStack techItem = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta techMeta = techItem.getItemMeta();
        techMeta.setDisplayName("§b§l✦ ĐANG THAM NGỘ: §f" + cp.getDisplayName() + " ✦");
        List<String> techLore = new ArrayList<>();
        techLore.add("§8───────────────────────────────");
        techLore.add("§7Phẩm cấp: " + cp.getFormattedGrade());
        techLore.add("§7Thuộc tính: §b" + String.join(", ", cp.getElements()));
        techLore.add("§7Độ tương thích Linh Căn: §bΦ = " + String.format("%.2f", phi));
        techLore.add("§7Ngộ Tính của ngươi: §e" + String.format("%.1f", ngoTinh));

        MasteryService.MasteryTier mt = MasteryService.MasteryTier.fromExp(prog.getMasteryExp());
        techLore.add("§7Độ thành thục công pháp: " + mt.getDisplayName() + " §7(" + prog.getMasteryExp() + " EXP)");
        techLore.add("§8───────────────────────────────");
        if (targetChapter <= cp.getTotalChapters()) {
            techLore.add("§fMục tiêu tham ngộ: §e§lTầng " + targetChapter + " §7/ " + cp.getTotalChapters());
            boolean hasCollected = prog.hasCollected(targetChapter);
            techLore.add("§7Tàn quyển tầng này: " + (hasCollected ? "§aĐã có trong tay" : "§cChưa sở hữu tàn quyển"));
        } else {
            techLore.add("§a✦ Đã lĩnh hội toàn bộ khẩu quyết viên mãn!");
        }
        techMeta.setLore(techLore);
        techItem.setItemMeta(techMeta);
        inv.setItem(config.getThamNgoCurrentTechSlot(), techItem);

        // ==========================================
        // 2. THANH TIẾN ĐỘ THAM NGỘ UNICODE (Slot 22)
        // ==========================================
        ItemStack progressItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta progMeta = progressItem.getItemMeta();
        progMeta.setDisplayName("§e§l✦ TIẾN ĐỘ LĨNH HỘI TẦNG " + targetChapter + " ✦");
        List<String> progLore = new ArrayList<>();
        progLore.add("§8───────────────────────────────");
        if (targetChapter <= cp.getTotalChapters()) {
            double p = prog.getComprehensionProgress();
            progLore.add("§7Tiến trình: " + config.renderProgressBar(p));
            progLore.add("");
            progLore.add("§7Tốc độ cơ sở: §f0.50% / phút");
            progLore.add("§7Hệ số Ngộ Tính: §a×" + String.format("%.2f", Math.max(0.5, Math.min(2.0, 1.0 + (ngoTinh - 10.0) / 40.0))));
            progLore.add("§7Hệ số Tương Thích: §b×" + String.format("%.2f", phi));
            progLore.add("§7Độ khó tầng " + targetChapter + ": §e×" + String.format("%.2f", Math.max(0.25, 1.0 - (targetChapter - 1) * 0.05)));
            progLore.add("§8───────────────────────────────");
            progLore.add("§fTích lũy đạt §a100.0% §fsẽ lập tức đột phá khẩu quyết!");
        } else {
            progLore.add("§aCông pháp này đã đạt cảnh giới Viên Mãn!");
        }
        progMeta.setLore(progLore);
        progressItem.setItemMeta(progMeta);
        inv.setItem(config.getThamNgoProgressBarSlot(), progressItem);

        // ==========================================
        // 3. TIÊU HAO ĐẠO NGỘ ĐIỂM +10 (Slot 29)
        // ==========================================
        ItemStack dn10 = new ItemStack(Material.SUNFLOWER);
        ItemMeta dn10Meta = dn10.getItemMeta();
        dn10Meta.setDisplayName("§6§l✦ Tiêu Hao 10 Điểm Đạo Ngộ ✦");
        double gain10 = 10.0 * 1.0 * phi;
        dn10Meta.setLore(Arrays.asList(
                "§7Đốt cháy §b10 Điểm Đạo Ngộ§7 để tâm thần thấu suốt,",
                "§7lập tức đẩy nhanh tiến độ tham ngộ thêm:",
                "  §a+" + String.format("%.1f%%", gain10) + " §7(đã tính theo Φ)",
                "",
                "§7Đạo Ngộ hiện có: §e" + daoNgoPoints + " điểm",
                "§e► Nhấp chuột để gia tốc!"
        ));
        dn10.setItemMeta(dn10Meta);
        inv.setItem(config.getThamNgoDaoNgo10Slot(), dn10);

        // ==========================================
        // 4. TIÊU HAO ĐẠO NGỘ ĐIỂM +50 (Slot 31)
        // ==========================================
        ItemStack dn50 = new ItemStack(Material.NETHER_STAR);
        ItemMeta dn50Meta = dn50.getItemMeta();
        dn50Meta.setDisplayName("§d§l✦ Tiêu Hao 50 Điểm Đạo Ngộ ✦");
        double gain50 = 50.0 * 1.0 * phi;
        dn50Meta.setLore(Arrays.asList(
                "§7Đốt cháy §d50 Điểm Đạo Ngộ§7 để thiên nhân tương hợp,",
                "§7lập tức đại gia tốc tiến độ tham ngộ thêm:",
                "  §a+" + String.format("%.1f%%", gain50) + " §7(đã tính theo Φ)",
                "",
                "§7Đạo Ngộ hiện có: §e" + daoNgoPoints + " điểm",
                "§e► Nhấp chuột để đại gia tốc!"
        ));
        dn50.setItemMeta(dn50Meta);
        inv.setItem(config.getThamNgoDaoNgo50Slot(), dn50);

        // ==========================================
        // 5. NÚT BẬT / TẮT TỌA THIỀN THAM NGỘ (Slot 33)
        // ==========================================
        ItemStack toggleItem = new ItemStack(isThamNgoActive ? Material.SOUL_LANTERN : Material.LANTERN);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        if (isThamNgoActive) {
            toggleMeta.setDisplayName("§b§l✦ Đang Tọa Thiền Tham Ngộ [BẬT] ✦");
            toggleMeta.setLore(Arrays.asList(
                    "§7Tâm thần ngươi đang nhập định sâu sắc,",
                    "§7tiến độ chương liên tục tích lũy từng giây.",
                    "",
                    "§7Trạng thái: §aĐANG THAM NGỘ",
                    "§c► Nhấp để XUẤT ĐỊNH (Dừng lại, bảo lưu tiến độ)!"
            ));
        } else {
            toggleMeta.setDisplayName("§e§l✦ Bắt Đầu Tọa Thiền Tham Ngộ [TẮT] ✦");
            toggleMeta.setLore(Arrays.asList(
                    "§7Nhập định tại chỗ để bắt đầu tham ngộ,",
                    "§7quá trình diễn ra tự động theo thời gian.",
                    "",
                    "§7Trạng thái: §7CHƯA NHẬP ĐỊNH",
                    "§a► Nhấp để BẮT ĐẦU TỌA THIỀN THAM NGỘ!"
            ));
        }
        toggleItem.setItemMeta(toggleMeta);
        inv.setItem(config.getThamNgoToggleSlot(), toggleItem);

        // ==========================================
        // 6. NÚT QUAY LẠI (Slot 40)
        // ==========================================
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName("§c◄ Quay Lại Tàng Kinh Các");
        back.setItemMeta(bMeta);
        inv.setItem(config.getThamNgoBackSlot(), back);

        // Kính nền
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
    }
}
