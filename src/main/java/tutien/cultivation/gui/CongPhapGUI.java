package tutien.cultivation.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.cultivation.service.MasteryService;
import tutien.element.LinhCanData;
import tutien.tutien.gui.MainMenu;

import java.util.*;

/**
 * Giao diện Quản lý Công Pháp & Tàn Quyển (CongPhapGUI) - Phase 2.4:
 * - Sử dụng CongPhapHolder độc quyền để chống gian lận và kéo nhầm vật phẩm.
 * - Hỗ trợ phân trang danh mục tàn quyển không giới hạn số lượng công pháp.
 * - Hiển thị đầy đủ Chủ Tu, Phụ Tu, Tương Thích Phi, Thành Thục Mastery.
 */
public class CongPhapGUI {

    public static void open(Player player, TuTienPlugin plugin) {
        open(player, plugin, 0);
    }

    public static void open(Player player, TuTienPlugin plugin, int page) {
        if (player == null || plugin == null) return;

        CongPhapGUIConfig config = plugin.getCongPhapGUIConfig();
        if (config == null) return;

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);
        CanhGioi realm = dataManager.getCanhGioi(player);
        LinhCanData linhCan = dataManager.getLinhCanData(player);

        CongPhapHolder holder = new CongPhapHolder(page);
        Inventory inv = Bukkit.createInventory(holder, config.getCongPhapSize(), config.getCongPhapTitle());
        holder.setInventory(inv);

        // ==========================================
        // 1. Ô CÔNG PHÁP CHỦ TU (Slot Primary)
        // ==========================================
        String activePrimaryId = cultData.getActivePrimary();
        CongPhap primaryCp = (activePrimaryId != null && !activePrimaryId.equalsIgnoreCase("CHUA_CO"))
                ? plugin.getCongPhapManager().getCongPhap(activePrimaryId) : null;

        ItemStack primaryItem;
        if (primaryCp != null) {
            primaryItem = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = primaryItem.getItemMeta();
            meta.setDisplayName("§6§l✦ CÔNG PHÁP CHỦ TU: §e" + primaryCp.getDisplayName() + " ✦");
            List<String> lore = new ArrayList<>();
            lore.add("§8───────────────────────────────");
            lore.add("§7Phẩm cấp: " + primaryCp.getFormattedGrade());
            lore.add("§7Cảnh giới giới hạn: §e" + primaryCp.getMaxRealm().getTenHienThi());
            lore.add("§7Thuộc tính: §b" + String.join(", ", primaryCp.getElements()));

            double phi = plugin.getCongPhapManager().getCompatibilityService().calculateCompatibility(linhCan, primaryCp);
            lore.add("§7Độ tương thích Linh Căn: §bΦ = " + String.format("%.2f", phi));

            CongPhapProgress prog = cultData.getCongPhapProgress(activePrimaryId);
            int learnedCount = prog != null ? prog.getLearnedChapters().size() : 0;
            int total = primaryCp.getTotalChapters();
            lore.add("§7Khẩu quyết đã lĩnh hội: §a" + learnedCount + " / " + total + " tầng");

            int masteryExp = prog != null ? prog.getMasteryExp() : 0;
            MasteryService.MasteryTier tier = MasteryService.MasteryTier.fromExp(masteryExp);
            lore.add("§7Độ Thành Thục: " + tier.getDisplayName() + " §7(" + masteryExp + " EXP)");
            lore.add("§7Giảm tiêu hao linh lực: §a-" + String.format("%.0f%%", tier.getManaCostReduction() * 100.0));
            lore.add("§7Hệ số hấp thu linh khí: §a+" + String.format("%.0f%%", (primaryCp.getQiAbsorbRate() - 1.0) * 100.0));

            if (cultData.getAdaptation().isActive()) {
                long remain = cultData.getAdaptation().getRemainingSeconds();
                lore.add("§c⚠ Đang thích nghi kinh mạch: §e" + (remain / 60) + "m " + (remain % 60) + "s");
                lore.add("  §7(Giảm " + String.format("%.0f%%", cultData.getAdaptation().getPenaltyRatio() * 100.0) + " hiệu quả tu luyện)");
            }
            lore.add("§8───────────────────────────────");
            lore.add("§e► Nhấp nút Tham Ngộ bên dưới để tu luyện khẩu quyết!");
            meta.setLore(lore);
            primaryItem.setItemMeta(meta);
        } else {
            primaryItem = new ItemStack(Material.BOOK);
            ItemMeta meta = primaryItem.getItemMeta();
            meta.setDisplayName("§7§l✦ CHƯA CÓ CÔNG PHÁP CHỦ TU ✦");
            List<String> lore = new ArrayList<>();
            lore.add("§8───────────────────────────────");
            lore.add("§7Ngươi hiện chưa tu luyện bộ công pháp chủ tu nào.");
            lore.add("§7Hãy chọn một bộ công pháp từ danh mục bên dưới");
            lore.add("§7và nhấp để tiến hành nhập môn tu luyện.");
            meta.setLore(lore);
            primaryItem.setItemMeta(meta);
        }
        inv.setItem(config.getCongPhapPrimarySlot(), primaryItem);

        // ==========================================
        // 2. CÁC Ô PHỤ TU (Auxiliary Slots)
        // ==========================================
        int maxAuxSlots = getMaxAuxiliarySlots(realm);
        List<Integer> auxSlots = config.getCongPhapAuxiliarySlots();
        List<String> activeAux = cultData.getActiveAuxiliary();

        for (int i = 0; i < auxSlots.size(); i++) {
            int slot = auxSlots.get(i);
            if (i < maxAuxSlots) {
                if (i < activeAux.size()) {
                    String auxId = activeAux.get(i);
                    CongPhap auxCp = plugin.getCongPhapManager().getCongPhap(auxId);
                    ItemStack auxItem = new ItemStack(Material.BOOK);
                    ItemMeta meta = auxItem.getItemMeta();
                    meta.setDisplayName("§b§l✦ Phụ Tu " + (i + 1) + ": §f" + (auxCp != null ? auxCp.getDisplayName() : auxId));
                    List<String> lore = new ArrayList<>();
                    lore.add("§8───────────────────────────────");
                    if (auxCp != null) {
                        lore.add("§7Phẩm cấp: " + auxCp.getFormattedGrade());
                        lore.add("§7Hệ số hấp thu thừa hưởng: §a+" + String.format("%.1f%%", (auxCp.getQiAbsorbRate() - 1.0) * 35.0));
                    }
                    lore.add("§e► Click chuột phải để GỠ phụ tu này!");
                    meta.setLore(lore);
                    auxItem.setItemMeta(meta);
                    inv.setItem(slot, auxItem);
                } else {
                    ItemStack emptyItem = new ItemStack(Material.GRAY_DYE);
                    ItemMeta meta = emptyItem.getItemMeta();
                    meta.setDisplayName("§7§l✦ Ô Phụ Tu " + (i + 1) + " (Trống)");
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Ô phụ tu đang để trống.");
                    lore.add("§7Click chuột phải vào một công pháp bên dưới");
                    lore.add("§7để thiết lập làm công pháp phụ tu.");
                    meta.setLore(lore);
                    emptyItem.setItemMeta(meta);
                    inv.setItem(slot, emptyItem);
                }
            } else {
                ItemStack lockedItem = new ItemStack(Material.BARRIER);
                ItemMeta meta = lockedItem.getItemMeta();
                meta.setDisplayName("§c§l✦ Ô Phụ Tu " + (i + 1) + " [Khóa]");
                List<String> lore = new ArrayList<>();
                lore.add("§7Yêu cầu cảnh giới cao hơn để mở ô phụ tu này.");
                meta.setLore(lore);
                lockedItem.setItemMeta(meta);
                inv.setItem(slot, lockedItem);
            }
        }

        // ==========================================
        // 3. DANH MỤC CÔNG PHÁP & TÀN QUYỂN (PHÂN TRANG)
        // ==========================================
        List<CongPhap> allCp = new ArrayList<>(plugin.getCongPhapManager().getAllCongPhap());
        allCp.sort(Comparator.comparing(CongPhap::getGradeTier).thenComparing(CongPhap::getId));

        int listStart = config.getCongPhapListStart();
        int listEnd = config.getCongPhapListEnd();
        int pageSize = (listEnd - listStart) + 1;
        if (pageSize <= 0) pageSize = 16;

        int totalPages = Math.max(1, (int) Math.ceil((double) allCp.size() / pageSize));
        int curPage = Math.min(page, totalPages - 1);
        holder.setPage(curPage);

        int startIndex = curPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allCp.size());

        int slotIdx = listStart;
        for (int i = startIndex; i < endIndex; i++) {
            CongPhap cp = allCp.get(i);
            CongPhapProgress prog = cultData.getCongPhapProgress(cp.getId());

            boolean isPrimary = cp.getId().equalsIgnoreCase(activePrimaryId);
            boolean isAux = cultData.isAuxiliary(cp.getId());
            boolean hasLearnedAny = prog != null && !prog.getLearnedChapters().isEmpty();
            boolean hasCollectedAny = prog != null && !prog.getCollectedChapters().isEmpty();

            Material mat = isPrimary ? Material.ENCHANTED_BOOK : (hasLearnedAny ? Material.WRITTEN_BOOK : Material.PAPER);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            String prefix = isPrimary ? "§6§l[Chủ Tu] " : (isAux ? "§b§l[Phụ Tu] " : "");
            meta.setDisplayName(prefix + "§f§l" + cp.getDisplayName() + " §7(" + cp.getFormattedGrade() + "§7)");

            List<String> lore = new ArrayList<>();
            lore.add("§8───────────────────────────────");
            lore.add("§7Thuộc tính: §b" + String.join(", ", cp.getElements()));
            lore.add("§7Cảnh giới tối đa: §e" + cp.getMaxRealm().getTenHienThi());

            double phi = plugin.getCongPhapManager().getCompatibilityService().calculateCompatibility(linhCan, cp);
            lore.add("§7Tương thích Linh Căn: §bΦ = " + String.format("%.2f", phi));

            int collectedCount = prog != null ? prog.getCollectedChapters().size() : 0;
            int learnedCount = prog != null ? prog.getLearnedChapters().size() : 0;
            lore.add("§7Tàn quyển đã có: §e" + collectedCount + " / " + cp.getTotalChapters());
            lore.add("§7Tầng đã lĩnh hội: §a" + learnedCount + " / " + cp.getTotalChapters());

            if (prog != null) {
                MasteryService.MasteryTier mt = MasteryService.MasteryTier.fromExp(prog.getMasteryExp());
                lore.add("§7Độ thành thục: " + mt.getDisplayName() + " §7(" + prog.getMasteryExp() + " EXP)");
            }

            lore.add("§8───────────────────────────────");
            if (isPrimary) {
                lore.add("§a✔ Đang là Công Pháp Chủ Tu");
            } else {
                lore.add("§e► Click chuột TRÁI để CHUYỂN TU (Đổi sang Chủ Tu)!");
                if (!isAux) {
                    lore.add("§b► Click chuột PHẢI để PHỤ TU bộ này!");
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slotIdx++, item);
        }

        // ==========================================
        // 4. CÁC NÚT ĐIỀU HƯỚNG & HÀNH ĐỘNG
        // ==========================================
        // Nút trang trước
        if (curPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prev.getItemMeta();
            pMeta.setDisplayName("§e◄ Trang Trước (" + curPage + "/" + totalPages + ")");
            prev.setItemMeta(pMeta);
            inv.setItem(config.getCongPhapPrevPageSlot(), prev);
        }

        // Nút trang sau
        if (curPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            nMeta.setDisplayName("§eTrang Sau (" + (curPage + 2) + "/" + totalPages + ") ►");
            next.setItemMeta(nMeta);
            inv.setItem(config.getCongPhapNextPageSlot(), next);
        }

        // Nút mở Tham Ngộ
        ItemStack openComp = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta compMeta = openComp.getItemMeta();
        compMeta.setDisplayName("§b§l☯ Vào Giao Diện Tham Ngộ");
        compMeta.setLore(Arrays.asList(
                "§7Xem khẩu quyết, thanh tiến độ lĩnh hội",
                "§7và tiêu hao Điểm Đạo Ngộ để gia tốc.",
                "",
                "§e► Nhấp để mở!"
        ));
        openComp.setItemMeta(compMeta);
        inv.setItem(config.getCongPhapOpenComprehensionSlot(), openComp);

        // Nút quay lại MainMenu
        ItemStack back = new ItemStack(Material.COMPASS);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName("§c◄ Quay Lại Menu Chính");
        back.setItemMeta(bMeta);
        inv.setItem(config.getCongPhapBackSlot(), back);

        // Kính nền
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    public static int getMaxAuxiliarySlots(CanhGioi realm) {
        if (realm == null || realm == CanhGioi.PHAM_NHAN) return 0;
        if (realm == CanhGioi.LUYEN_KHI) return 1;
        if (realm == CanhGioi.TRUC_CO) return 2;
        return 3;
    }
}
