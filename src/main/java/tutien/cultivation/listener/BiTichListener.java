package tutien.cultivation.listener;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.item.BiTichItem;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;
import tutien.cultivation.service.CongPhapService;

/**
 * Xử lý sự kiện tu sĩ đọc và lĩnh ngộ Bí Tịch / Tàn Quyển cầm trên tay:
 * - Click chuột phải để tự động thu thập và giác ngộ khẩu quyết.
 * - Hiệu ứng âm thanh và hạt chân thực khi thức hải tiếp nhận đạo văn.
 */
public class BiTichListener implements Listener {

    private final TuTienPlugin plugin;

    public BiTichListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!BiTichItem.isBiTich(item)) return;

        event.setCancelled(true);

        String cpId = BiTichItem.getCongPhapId(item);
        int chapter = BiTichItem.getChapter(item);

        if (cpId == null || plugin.getCongPhapManager() == null) return;
        CongPhap cp = plugin.getCongPhapManager().getCongPhap(cpId);
        if (cp == null) {
            player.sendMessage("§c§l[Bí Tịch] §fQuyển cổ tịch này ghi chép đạo văn cổ xưa không thể đọc được!");
            return;
        }

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);
        CongPhapProgress prog = cultData.getOrCreateProgress(cpId);

        // 1. Nếu đã học và đã thu thập rồi
        if (prog.hasLearned(chapter)) {
            player.sendMessage("§e§l[Bí Tịch] §fNgươi đã từng lĩnh ngộ thấu đáo §eTầng " + chapter + " §fcủa "
                    + cp.getDisplayName() + " §frồi!");
            player.sendMessage("§7Hãy đem quyển này hiến tặng vào §bTàng Kinh Các §7Tông Môn để nhận Đạo Niệm!");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // 2. Thu thập tàn quyển vào thức hải
        boolean newlyCollected = prog.addCollectedChapter(chapter);

        // Trừ 1 cuốn bí tịch trên tay
        item.setAmount(item.getAmount() - 1);

        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.2, 0), 30, 0.4, 0.4, 0.4, 0.1);

        if (newlyCollected) {
            player.sendMessage("§a§l✦ THỨC HẢI TIẾP NHẬN ✦ §fNgươi đã thu nạp tàn quyển: " + cp.getDisplayName()
                    + " §e§l[Tầng " + chapter + "]§f!");
        }

        // 3. Thử trực tiếp lĩnh hội nếu thỏa mãn toàn bộ điều kiện (thứ tự tầng, cảnh giới, boss)
        CongPhapService cpService = plugin.getCongPhapService();
        if (cpService != null && prog.canLearnChapter(chapter)) {
            boolean learned = cpService.learnChapter(player, dataManager, cpId, chapter);
            if (learned) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.0, 0), 25, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage("§6§l✦ NGỘ ĐẠO THÀNH CÔNG ✦ §fĐạo văn dung nhập kinh mạch, giác ngộ trọn vẹn Tầng " + chapter + "!");
                return;
            }
        }

        player.sendMessage("§7(Đã ghi nhớ tàn quyển vào thức hải. Hãy dùng lệnh §b/tutien thamngo §7hoặc nâng cao tu vi để lĩnh ngộ!)");
    }
}
