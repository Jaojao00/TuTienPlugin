package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tutien.combat.StatsManager;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện Khai Mạch Nhập Đạo (Phàm Nhân -> Luyện Khí Tầng 1).
 * Mô phỏng quy trình tu luyện thực thụ theo Phàm Nhân Tu Tiên & Tiên Nghịch:
 *   1. Tôi Thể (Rèn Luyện Thể Phách Phàm Nhân, Căn Cơ >= 20.0).
 *   2. Tẩy Tủy (Bài Trừ Trần Cấu Phàm Thai, ngâm suối nước / dược thang).
 *   3. Dẫn Khí (Lĩnh Ngộ Dẫn Khí Quyết, cảm ứng thiên địa linh khí).
 */
public class NhapDaoGUI implements Listener {

    public static final String GUI_TITLE = "§0§lKHAI MẠCH NHẬP ĐẠO";
    private static final int GUI_SIZE = 27;

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;

    public NhapDaoGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    public static void open(Player player, TuTienPlugin plugin) {
        PlayerDataManager data = plugin.getPlayerDataManager();
        CanhGioi cg = data.getCanhGioi(player);

        if (cg != CanhGioi.PHAM_NHAN) {
            player.sendMessage("§aĐạo hữu đã là tu sĩ §e" + cg.getTenHienThi() + "§a, không cần phải Khai Mạch phàm thai nữa!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        // Nền kính trang trí
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, border);
        }

        // Slot 4: Thông tin phàm thân
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName("§e§lPHÀM THÂN: §f" + player.getName());
        List<String> headLore = new ArrayList<>();
        headLore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
        headLore.add("§7Trạng thái: §8Phàm Thai (Chưa Nhập Đạo)");
        headLore.add("§7Căn Cơ hiện tại: §a" + String.format("%.1f", data.getCanCo(player)) + " §7/ 100.0");
        headLore.add("§7Thể Chất: §c" + String.format("%.1f", data.getTheChat(player)));
        headLore.add("§7Ngộ Tính: §b" + String.format("%.1f", data.getNgoTinh(player)));
        headLore.add("§7Linh Lực: §80 / 0 §7(Kinh mạch chưa thông)");
        headLore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
        headLore.add("§7Khai Mạch là quá trình rũ bỏ phàm trần,");
        headLore.add("§7mở đan điền nạp khí để bước vào Luyện Khí.");
        skullMeta.setLore(headLore);
        head.setItemMeta(skullMeta);
        inv.setItem(4, head);

        // Kiểm tra điều kiện các bước
        boolean hasToiThe = data.hasKhaiMachStep(player, "TOI_THE") || data.getToiTheProgress(player) >= 100.0;
        boolean hasTayTuy = data.hasKhaiMachStep(player, "TAY_TUY");
        boolean hasDanKhi = data.hasKhaiMachStep(player, "DAN_KHI");

        // Đồng bộ nếu đã đủ tiến độ mà chưa đánh dấu TOI_THE
        if (data.getToiTheProgress(player) >= 100.0 && !data.hasKhaiMachStep(player, "TOI_THE")) {
            data.addKhaiMachStep(player, "TOI_THE");
            hasToiThe = true;
        }

        // Slot 11: Bước 1: Tôi Thể
        ItemStack toiThe = new ItemStack(hasToiThe ? Material.IRON_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE);
        ItemMeta toiMeta = toiThe.getItemMeta();
        toiMeta.setDisplayName(hasToiThe ? "§a§l✔ BƯỚC 1: TÔI THỂ (HOÀN THÀNH)" : "§e§l❖ BƯỚC 1: TÔI THỂ");
        List<String> toiLore = new ArrayList<>();
        toiLore.add("§7Rèn luyện thể phách, làm vững gân cốt.");
        toiLore.add("");
        toiLore.add("§fYêu cầu: §eTiến độ Tôi Thể đạt 100%");
        toiLore.add("§fHiện tại: " + (hasToiThe ? "§a" : "§c") + String.format("%.1f%%", data.getToiTheProgress(player)) + " / 100%");
        toiLore.add("");
        if (hasToiThe) {
            toiLore.add("§a✦ Thể phách vững vàng, đã chịu được linh áp!");
        } else {
            toiLore.add("§7Cách rèn luyện trong tự nhiên:");
            toiLore.add("§7 - Khai thác các loại đá/quặng tự nhiên");
            toiLore.add("§7 - Đốn cây lâm sản hoặc chiến đấu sinh tồn");
            toiLore.add("§7 (Lưu ý: Không tính block do người chơi đặt)");
            toiLore.add("");
            toiLore.add("§b► Nhấp để đối chiếu & kích hoạt nếu đã đạt 100%!");
        }
        toiMeta.setLore(toiLore);
        toiThe.setItemMeta(toiMeta);
        inv.setItem(11, toiThe);

        // Slot 13: Bước 2: Tẩy Tủy
        ItemStack tayTuy = new ItemStack(hasTayTuy ? Material.WATER_BUCKET : Material.POTION);
        ItemMeta tayMeta = tayTuy.getItemMeta();
        tayMeta.setDisplayName(hasTayTuy ? "§a§l✔ BƯỚC 2: TẨY TỦY (HOÀN THÀNH)" : "§e§l❖ BƯỚC 2: TẨY TỦY");
        List<String> tayLore = new ArrayList<>();
        tayLore.add("§7Bài trừ trần cấu phàm thai, làm sạch đan điền.");
        tayLore.add("");
        if (hasTayTuy) {
            tayLore.add("§a✦ Kinh mạch thanh tịnh, không còn tạp chất trần thế!");
        } else {
            tayLore.add("§fTrạng thái: §cChưa gột rửa tạp chất");
            tayLore.add("");
            tayLore.add("§7Cách thực hiện trong tự nhiên:");
            tayLore.add("§7 - Đứng ngâm mình trong dòng nước trong mát (Water)");
            tayLore.add("§7 - Hoặc uống một bình Nước Thanh Khiết / Dược Thang");
            tayLore.add("");
            tayLore.add("§b► Nhấp để gột rửa nếu ngươi đang đứng dưới dòng nước!");
        }
        tayMeta.setLore(tayLore);
        tayTuy.setItemMeta(tayMeta);
        inv.setItem(13, tayTuy);

        // Slot 15: Bước 3: Dẫn Khí
        ItemStack danKhi = new ItemStack(hasDanKhi ? Material.ENCHANTED_BOOK : Material.BOOK);
        ItemMeta danMeta = danKhi.getItemMeta();
        danMeta.setDisplayName(hasDanKhi ? "§a§l✔ BƯỚC 3: DẪN KHÍ (HOÀN THÀNH)" : "§e§l❖ BƯỚC 3: DẪN KHÍ");
        List<String> danLore = new ArrayList<>();
        danLore.add("§7Lĩnh ngộ phương pháp cảm ứng và dẫn dắt");
        danLore.add("§7thiên địa linh khí nhập vào kinh mạch.");
        danLore.add("");
        if (hasDanKhi) {
            danLore.add("§a✦ Đã lĩnh ngộ tâm pháp [Dẫn Khí Khái Yếu]!");
        } else {
            danLore.add("§fTrạng thái: §cChưa lĩnh ngộ");
            danLore.add("");
            danLore.add("§e► Nhấp vào đây để tiếp nhận tâm pháp Dẫn Khí!");
        }
        danMeta.setLore(danLore);
        danKhi.setItemMeta(danMeta);
        inv.setItem(15, danKhi);

        // Slot 22: Nút Khai Mạch Nhập Đạo
        boolean ready = hasToiThe && hasTayTuy && hasDanKhi;
        ItemStack actionItem = new ItemStack(ready ? Material.NETHER_STAR : Material.GRAY_DYE);
        ItemMeta actionMeta = actionItem.getItemMeta();
        actionMeta.setDisplayName(ready ? "§6§l✦ DẪN KHÍ KHAI MẠCH - BƯỚC VÀO TIÊN ĐẠO ✦" : "§7§l✦ DẪN KHÍ KHAI MẠCH (CHƯA ĐỦ ĐIỀU KIỆN) ✦");
        List<String> actionLore = new ArrayList<>();
        actionLore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
        actionLore.add("§7Khai thông kỳ kinh bát mạch, mở đan điền.");
        actionLore.add("§7Trút bỏ phàm thai, chính thức trở thành tu sĩ:");
        actionLore.add("§e   ✦ Luyện Khí Tầng 1 ✦");
        actionLore.add("");
        actionLore.add("§f§lĐIỀU KIỆN:");
        actionLore.add((hasToiThe ? "§a ✔ " : "§c ✖ ") + "§71. Tôi Thể (Tiến độ 100%)");
        actionLore.add((hasTayTuy ? "§a ✔ " : "§c ✖ ") + "§72. Tẩy Tủy (Gột rửa trần cấu)");
        actionLore.add((hasDanKhi ? "§a ✔ " : "§c ✖ ") + "§73. Dẫn Khí (Lĩnh ngộ tâm pháp)");
        actionLore.add("");
        actionLore.add("§f§lCHỈ SỐ THAY ĐỔI:");
        actionLore.add("§7 ▪ Sinh lực: §c20 ❤ ➜ 24 ❤");
        actionLore.add("§7 ▪ Linh lực: §80 ➜ §b100 / 100");
        actionLore.add("§7 ▪ Thần thức: §80 ➜ §d10");
        actionLore.add("§7 ▪ Mở khóa: §aTọa Thiền Tu Luyện (§e/tutien tuluyen§a)");
        actionLore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
        if (ready) {
            actionLore.add("§a► NHẤP ĐỂ BẮT ĐẦU KHAI MẠCH NHẬP ĐẠO!");
        } else {
            actionLore.add("§c⚠ Vui lòng hoàn thành đủ 3 bước trên!");
        }
        actionMeta.setLore(actionLore);
        actionItem.setItemMeta(actionMeta);
        inv.setItem(22, actionItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        CanhGioi cg = dataManager.getCanhGioi(player);
        if (cg != CanhGioi.PHAM_NHAN) {
            player.closeInventory();
            player.sendMessage("§aĐạo hữu đã hoàn thành nhập đạo trước đó!");
            return;
        }

        int slot = event.getRawSlot();

        // Slot 11: Tôi thể
        if (slot == 11) {
            double progress = dataManager.getToiTheProgress(player);
            if (progress >= 100.0) {
                if (!dataManager.hasKhaiMachStep(player, "TOI_THE")) {
                    dataManager.addKhaiMachStep(player, "TOI_THE");
                    player.sendMessage("§a§l✦ [Tôi Thể Hoàn Tất] §fThể phách rắn rỏi, gân cốt kiên cường! Tiến độ Tôi Thể: §e100%");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    open(player, plugin);
                } else {
                    player.sendMessage("§aĐạo hữu đã hoàn thành bước Tôi Thể.");
                }
            } else {
                player.sendMessage("§c§l[!] §fTiến độ Tôi Thể hiện tại (§e" + String.format("%.1f%%", progress) + "§f) chưa đạt §e100%§f!");
                player.sendMessage("§7Gợi ý: Hãy khai thác đá quặng tự nhiên, đốn gỗ hoặc chiến đấu tiêu diệt yêu ma ban đêm.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            return;
        }

        // Slot 13: Tẩy tủy
        if (slot == 13) {
            if (dataManager.hasKhaiMachStep(player, "TAY_TUY")) {
                player.sendMessage("§aĐạo hữu đã hoàn thành gột rửa trần cấu!");
                return;
            }

            // Kiểm tra xem người chơi có đang đứng trong nước không
            Material blockType = player.getLocation().getBlock().getType();
            if (blockType == Material.WATER) {
                dataManager.addKhaiMachStep(player, "TAY_TUY");
                player.sendMessage("§b§l✦ [Tẩy Tủy Hoàn Tất] §fDòng nước thanh lương gột rửa trần cấu! Kinh mạch thông suốt, phàm thai thanh tịnh!");
                player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

                // Dị tượng khói đen bốc lên rồi biến mất, thay bằng bọt nước sáng
                player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0, 0.5, 0), 20, 0.3, 0.5, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 0.5, 0), 40, 0.4, 0.5, 0.4, 0.1);

                open(player, plugin);
            } else {
                player.sendMessage("§c§l[!] §fNgươi chưa tìm thấy dòng nước thanh khiết!");
                player.sendMessage("§7Hãy tìm một con suối, bờ hồ hoặc vùng nước tự nhiên, đứng ngâm mình trong nước rồi bấm lại vào đây để tẩy tủy.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            return;
        }

        // Slot 15: Dẫn khí
        if (slot == 15) {
            if (dataManager.hasKhaiMachStep(player, "DAN_KHI")) {
                player.sendMessage("§aĐạo hữu đã lĩnh ngộ Dẫn Khí Quyết.");
                return;
            }

            dataManager.addKhaiMachStep(player, "DAN_KHI");
            player.sendMessage("§b§l✦ [Tâm Pháp Dẫn Khí] §fĐạo sinh nhất, nhất sinh nhị, nhị sinh tam, tam sinh vạn vật...");
            player.sendMessage("§aNgươi đã cảm ứng được sợi thiên địa linh khí đầu tiên bập bùng nơi đan điền!");
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.2, 0), 30, 0.5, 0.5, 0.5, 0.1);
            open(player, plugin);
            return;
        }

        // Slot 22: Nghi thức Khai Mạch Nhập Đạo
        if (slot == 22) {
            boolean hasToiThe = dataManager.hasKhaiMachStep(player, "TOI_THE") || dataManager.getToiTheProgress(player) >= 100.0;
            boolean hasTayTuy = dataManager.hasKhaiMachStep(player, "TAY_TUY");
            boolean hasDanKhi = dataManager.hasKhaiMachStep(player, "DAN_KHI");

            if (!hasToiThe || !hasTayTuy || !hasDanKhi) {
                player.sendMessage("§c§l[!] §fNgươi chưa hoàn thành đủ 3 bước Tôi Thể, Tẩy Tủy và Dẫn Khí!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // TIẾN HÀNH KHAI MẠCH NHẬP ĐẠO!
            player.closeInventory();

            // Cập nhật Cảnh Giới & Tầng
            dataManager.setCanhGioi(player, CanhGioi.LUYEN_KHI);
            dataManager.setTang(player, 1);
            dataManager.setTuVi(player, 0);

            // Cập nhật Tiến độ & Căn Cơ & Thần Thức ban đầu
            dataManager.setToiTheProgress(player, 100.0);
            dataManager.addKhaiMachStep(player, "TOI_THE");
            dataManager.addKhaiMachStep(player, "TAY_TUY");
            dataManager.addKhaiMachStep(player, "DAN_KHI");
            double curCanCo = dataManager.getCanCo(player);
            dataManager.setCanCo(player, Math.max(curCanCo, 25.0));
            dataManager.setThanThuc(player, 10.0);

            // Khai mở Linh Lực
            int maxLinhLuc = dataManager.getMaxLinhLucEffective(player);
            dataManager.setLinhLuc(player, maxLinhLuc);

            // Thức tỉnh Linh Căn bẩm sinh chuẩn Phase 2.1
            tutien.element.LinhCanData lcData = dataManager.getLinhCanData(player);
            if (lcData == null) {
                lcData = plugin.getLinhCanManager().generateRandomLinhCan();
                dataManager.setLinhCanData(player, lcData);
            }

            // Cập nhật thuộc tính chiến đấu
            StatsManager.applyStats(player, dataManager);
            player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());

            // Dị tượng khai mạch
            player.getWorld().strikeLightningEffect(player.getLocation());
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.0, 0), 100, 0.8, 1.2, 0.8, 0.2);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 0.5, 0), 50, 0.5, 0.8, 0.5, 0.05);
            player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1.5, 0), 3, 0.1, 0.1, 0.1, 0);

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.9f);

            // Thông báo
            Bukkit.broadcastMessage("§6§l✦ TIÊN ĐẠO KHỞI NGUYÊN ✦ §fPhàm nhân §b" + player.getName()
                    + " §fđã tôi thể tẩy tủy, khai mở đan điền, chính thức bước vào §e§l[Luyện Khí Tầng 1]§f!");

            if (lcData.getCurrentType() == tutien.element.LinhCanType.THIEN_LINH_CAN || lcData.getCurrentType() == tutien.element.LinhCanType.DI_LINH_CAN) {
                Bukkit.broadcastMessage("§e§l✦ THIÊN CƠ DỊ TƯỢNG ✦ §fĐạo hữu §b" + player.getName()
                        + " §fkhai mạch ngộ đạo, thức tỉnh xuất thế: " + lcData.getFormattedDisplayName() + "!");
            }

            player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§a§l❖ CHÚC MỪNG ĐẠO HỮU ĐÃ CHÍNH THỨC NHẬP ĐẠO! ❖");
            player.sendMessage("§f▪ Cảnh giới: §e§lLuyện Khí Tầng 1 §7(1/13 Tầng)");
            player.sendMessage("§f▪ Linh Căn: " + lcData.getFormattedDisplayName());
            player.sendMessage("§f▪ Thuần Khiết Bản Nguyên (Purity): §b" + String.format("%.1f%%", lcData.getPurity() * 100.0));
            player.sendMessage("§f▪ Linh Lực: §b" + maxLinhLuc + " / " + maxLinhLuc + " §7(Đã khai mở)");
            player.sendMessage("§f▪ Đã mở khóa: §aTọa Thiền Tu Luyện (§e/tutien tuluyen§a)");
            player.sendMessage("§fTừ nay hãy tìm kiếm linh địa, hấp thu thiên địa linh khí,");
            player.sendMessage("§fluyện chế linh đan để không ngừng đột phá các tầng tiếp theo!");
            player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }
}
