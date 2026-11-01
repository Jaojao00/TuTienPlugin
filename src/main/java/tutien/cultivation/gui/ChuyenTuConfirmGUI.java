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
import tutien.cultivation.service.CongPhapTransitionService;
import tutien.element.LinhCanData;

import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện màn hình xác nhận Chuyển Tu 2 bước (Phase 2.4):
 * Tránh việc bấm nhầm chuyển tu.
 * Xem trước: Công pháp mới, Phẩm cấp, Tương thích Phi, Tu vi tiểu cảnh giới hao hụt, Thời gian thích nghi.
 */
public class ChuyenTuConfirmGUI {

    public static final String TITLE = "§8✦ Xác Nhận Chuyển Đổi Tu Luyện ✦";

    public static void open(Player player, TuTienPlugin plugin, String targetCongPhapId) {
        if (player == null || plugin == null || targetCongPhapId == null) return;

        CongPhap newCp = plugin.getCongPhapManager().getCongPhap(targetCongPhapId);
        if (newCp == null) {
            player.sendMessage("§c§l[!] §fCông pháp mục tiêu không tồn tại!");
            return;
        }

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);
        String oldPrimaryId = cultData.getActivePrimary();
        CongPhap oldCp = oldPrimaryId != null ? plugin.getCongPhapManager().getCongPhap(oldPrimaryId) : null;

        CanhGioi realm = dataManager.getCanhGioi(player);
        int currentTuVi = dataManager.getTuVi(player);

        // Tính trước tổn thất tu vi tiểu cảnh giới & thời gian thích nghi
        double penaltyRatio = (oldCp != null)
                ? plugin.getCongPhapManager().getTransitionService().calculatePenaltyRatio(oldCp, newCp, false)
                : 0.15;
        int tuViPerTang = (realm != null && realm.getTuViMoiTang() > 0) ? realm.getTuViMoiTang() : 100;
        int stageTuVi = currentTuVi % tuViPerTang;
        if (stageTuVi == 0 && currentTuVi > 0) stageTuVi = tuViPerTang;
        int estimatedTuViLost = (int) Math.round(stageTuVi * penaltyRatio);

        long adaptationSeconds = (oldCp != null)
                ? plugin.getCongPhapManager().getTransitionService().calculateAdaptationMinutes(oldCp, newCp, false) * 60L
                : 0L;

        ChuyenTuConfirmHolder holder = new ChuyenTuConfirmHolder(targetCongPhapId, estimatedTuViLost, adaptationSeconds);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.setInventory(inv);

        // Item giữa: Xem trước thông tin
        ItemStack infoItem = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§6§l✦ THÔNG TIN CHUYỂN TU ✦");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8───────────────────────────────");
        infoLore.add("§7Công pháp hiện tại: " + (oldCp != null ? oldCp.getDisplayName() : "§7Chưa có"));
        infoLore.add("§7Chuyển sang: §a" + newCp.getDisplayName());
        infoLore.add("§7Phẩm cấp: " + newCp.getFormattedGrade());

        LinhCanData lc = dataManager.getLinhCanData(player);
        double phi = plugin.getCongPhapManager().getCompatibilityService().calculateCompatibility(lc, newCp);
        infoLore.add("§7Độ tương thích Linh Căn: §bΦ = " + String.format("%.2f", phi));
        infoLore.add("§8───────────────────────────────");
        infoLore.add("§c⚠ Tổn thất Tu Vi tiểu cảnh giới: §e-" + estimatedTuViLost + " Tu Vi");
        infoLore.add("  §7(Chỉ trừ trên tiến độ tầng hiện tại,");
        infoLore.add("  §atuyệt đối KHÔNG hạ Đại Cảnh Giới§7!)");
        if (adaptationSeconds > 0) {
            infoLore.add("§e⏳ Thời gian thích nghi kinh mạch: §f" + (adaptationSeconds / 60) + " phút");
        }
        infoLore.add("§8───────────────────────────────");
        infoLore.add("§7Hãy cân nhắc kỹ lưỡng trước khi quyết định!");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inv.setItem(13, infoItem);

        // Nút Xác Nhận (Slot 11)
        ItemStack confirmItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName("§a§l✔ XÁC NHẬN CHUYỂN TU");
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("§7Nhấp vào đây để chính thức");
        confirmLore.add("§7đổi kinh mạch sang công pháp này.");
        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        inv.setItem(11, confirmItem);

        // Nút Hủy Bỏ (Slot 15)
        ItemStack cancelItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName("§c§l✖ HỦY BỎ & QUAY LẠI");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("§7Không thay đổi gì, quay lại");
        cancelLore.add("§7giao diện Tàng Kinh Các.");
        cancelMeta.setLore(cancelLore);
        cancelItem.setItemMeta(cancelMeta);
        inv.setItem(15, cancelItem);

        // Kính nền
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
    }
}
