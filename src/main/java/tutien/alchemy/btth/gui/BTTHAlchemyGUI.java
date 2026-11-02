package tutien.alchemy.btth.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.alchemy.btth.model.*;
import tutien.alchemy.btth.service.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện Luyện Đan Tương Tác chuẩn Đấu Phá Thương Khung:
 * - Điều chỉnh nhiệt độ dược đỉnh (+/- hỏa hầu).
 * - Hiển thị độ tinh thuần dược dịch và các bước chiết xuất động.
 * - Khống chế ngưng đan và thu đan thời gian thực.
 * - Tự động dọn dẹp item rác nếu từng bị kéo nhầm ra túi đồ.
 */
public class BTTHAlchemyGUI {

    public static final String GUI_TITLE = "§0✦ ĐAN PHÒNG - LUYỆN DƯỢC ✦";

    public static void open(Player player, BTTHProcessController controller,
                            SoulControlService soulService, AlchemistRankService rankService,
                            AlchemyFlameService flameService, AlchemyCauldronService cauldronService) {
        if (player == null) return;

        // Dọn dẹp an toàn các item nút bấm GUI nếu người chơi từng bị kéo nhầm ra túi
        cleanGlitchItems(player);

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        BTTHAlchemySession session = controller != null ? controller.getSession(player.getUniqueId()) : null;

        PlayerSoulData soul = soulService != null ? soulService.getSoulData(player) : new PlayerSoulData();
        AlchemistRank rank = rankService != null ? rankService.getRank(player) : AlchemistRank.HOC_DO;
        BTTHFlame flame = flameService != null ? flameService.getActiveFlame(player) : null;
        BTTHCauldron cauldron = cauldronService != null ? cauldronService.getActiveCauldron(player) : null;

        // Khung kính viền
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Slot 4: Nút [CHỌN ĐAN PHƯƠNG]
        inv.setItem(4, createItem(Material.ENCHANTED_BOOK, "§e§l✦ CHỌN ĐAN PHƯƠNG ✦",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Xem danh sách đan phương và chọn",
                "§7loại linh đan muốn luyện chế.",
                "§a▶ Nhấp để mở danh mục đan phương!",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 8: Nút [CẨM NANG HƯỚNG DẪN]
        inv.setItem(8, createItem(Material.KNOWLEDGE_BOOK, "§a§l📖 HƯỚNG DẪN LUYỆN ĐAN",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Cẩm nang quy trình luyện đan,",
                "§7mẹo giữ hỏa hầu và bí kíp Đan Văn.",
                "§b▶ Nhấp để xem chi tiết hướng dẫn!",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 13: Trạng thái Dược Đỉnh & Nhiệt Độ
        double curHeat = session != null ? session.getCurrentHeat() : 25.0;
        String stageDesc = session != null ? session.getCurrentStage().getDescription() : "Chưa Bắt Đầu";
        double maxCauldronHeat = cauldron != null ? cauldron.getMaxHeat() : 180.0;
        String cauldronName = cauldron != null ? cauldron.getDisplayName() : "§7Phàm Đỉnh";
        int durability = cauldron != null ? cauldron.getCurrentDurability() : 50;
        int maxDur = cauldron != null ? cauldron.getMaxDurability() : 50;

        List<String> cauldronLore = new ArrayList<>();
        cauldronLore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");
        cauldronLore.add("§7Dược đỉnh: " + cauldronName);
        cauldronLore.add("§7Độ bền: §a" + durability + "/" + maxDur);
        cauldronLore.add("§7Nhiệt độ hiện tại: §c" + Math.round(curHeat) + "°C §7/ §4Max " + Math.round(maxCauldronHeat) + "°C");
        cauldronLore.add("§7Giai đoạn: §b" + stageDesc);

        if (session != null) {
            BTTHFormula f = session.getFormula();
            cauldronLore.add("§8─────────────────────────");
            cauldronLore.add("§7Đang luyện: §e§l" + f.getDisplayName() + " §8(" + f.getChineseName() + "§8)");
            cauldronLore.add("§7Nhiệt độ mục tiêu: §6" + Math.round(f.getTargetHeat()) + "°C");

            // Chỉ báo trạng thái nhiệt độ
            double diff = curHeat - f.getTargetHeat();
            if (Math.abs(diff) <= 20.0) {
                cauldronLore.add("§a✔ [NHIỆT ĐỘ HOÀN HẢO - HÃY TINH LUYỆN DƯỢC LIỆU!]");
            } else if (diff < -20.0) {
                cauldronLore.add("§b❄ [CHƯA ĐỦ NHIỆT ĐỘ - HÃY BẤM TĂNG HỎA Ở DƯỚI]");
            } else {
                cauldronLore.add("§c⚠ [CẢNH BÁO QUÁ NHIỆT - HÃY BẤM GIẢM HỎA Ở DƯỚI!]");
            }

            cauldronLore.add("§8─────────────────────────");
            cauldronLore.add("§e▶ Bước tiếp theo: " + getNextStepTip(session));
        } else {
            cauldronLore.add("§8─────────────────────────");
            cauldronLore.add("§e▶ Nhấp vào [CHỌN ĐAN PHƯƠNG] ở trên để bắt đầu!");
        }
        cauldronLore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");
        inv.setItem(13, createItem(Material.CAULDRON, "§e§l✦ TRẠNG THÁI DƯỢC ĐỈNH ✦", cauldronLore));

        // Slot 20, 21: Nút giảm hỏa
        inv.setItem(20, createItem(Material.BLUE_ICE, "§b§lGiảm Hỏa [-50°C]", "§7Nhấp để hạ nhiệt độ mạnh."));
        inv.setItem(21, createItem(Material.PACKED_ICE, "§b§lGiảm Hỏa [-10°C]", "§7Nhấp để tinh chỉnh hạ nhiệt."));

        // Slot 22: Ngọn lửa đang dùng
        String flameName = flame != null ? flame.getDisplayName() : "§eĐấu Khí Chi Hỏa";
        String flameCat = flame != null ? flame.getCategory().name() : "DAU_KHI";
        double heatPower = flame != null ? flame.getHeatPower() : 60.0;
        double heatCtrl = flame != null ? flame.getHeatControl() : 0.95;
        double soulDrain = flame != null ? flame.getSoulConsumption() : 3.0;
        inv.setItem(22, createItem(Material.CAMPFIRE, flameName,
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Chủng loại: §e" + flameCat,
                "§7Công suất nhiệt: §c" + Math.round(heatPower),
                "§7Độ chuẩn khống chế: §a" + Math.round(heatCtrl * 100) + "%",
                "§7Tiêu hao hồn lực: §d" + soulDrain + "/s",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 23, 24: Nút tăng hỏa
        inv.setItem(23, createItem(Material.BLAZE_POWDER, "§c§lTăng Hỏa [+10°C]", "§7Nhấp để tinh chỉnh tăng nhiệt."));
        inv.setItem(24, createItem(Material.FIRE_CHARGE, "§4§lTăng Hỏa [+50°C]", "§7Nhấp để nâng nhiệt độ mạnh."));

        // Slot 28, 29, 30: Danh mục nguyên liệu cần tinh luyện (Dynamic)
        if (session != null) {
            BTTHFormula f = session.getFormula();
            int ingSlot = 28;
            for (BTTHFormula.IngredientRequirement req : f.getIngredients()) {
                if (ingSlot > 30) break;
                boolean extracted = session.getExtractedIngredients().contains(req.getIngredientId());
                Material icon = extracted ? Material.POTION : Material.SWEET_BERRIES;
                if (req.getIngredientId().contains("MA_HACH")) {
                    icon = extracted ? Material.DRAGON_BREATH : Material.EMERALD;
                }

                List<String> ingLore = new ArrayList<>();
                ingLore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");
                ingLore.add("§7Vai trò: §e" + req.getRole());
                ingLore.add("§7Số lượng: §f" + req.getAmount());
                if (extracted) {
                    double p = session.getIngredientPurities().getOrDefault(req.getIngredientId(), 0.85);
                    ingLore.add("§a✔ ĐÃ CHIẾT XUẤT XONG!");
                    ingLore.add("§7Độ tinh thuần: §b" + Math.round(p * 100) + "%");
                } else {
                    ingLore.add("§e▶ Nhấp để tiến hành Tinh Luyện!");
                    ingLore.add("§7(Đảm bảo nhiệt độ đạt chuẩn)");
                }
                ingLore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");

                inv.setItem(ingSlot++, createItem(icon, (extracted ? "§a§l✔ " : "§e§l") + req.getIngredientId(), ingLore));
            }
        }

        // Slot 31: Nút Dung Hợp
        boolean canFuse = session != null && session.getCurrentStage() == BTTHAlchemyStage.FUSION;
        Material fuseMat = canFuse ? Material.BREWING_STAND : Material.GLASS_BOTTLE;
        String fuseTitle = canFuse ? "§a§l[✦ DUNG HỢP DƯỢC DỊCH ✦]" : "§7[Dung Hợp Dược Dịch]";
        inv.setItem(31, createItem(fuseMat, fuseTitle,
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                canFuse ? "§a▶ Tất cả dược dịch đã sẵn sàng! Nhấp để dung hợp."
                        : "§7Cần tinh luyện toàn bộ thảo dược trước khi dung hợp.",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 40: Nút Thu Đan
        boolean canCollect = session != null && (session.getCurrentStage() == BTTHAlchemyStage.CONDENSATION
                || session.getCurrentStage() == BTTHAlchemyStage.NOURISHMENT
                || session.getCurrentStage() == BTTHAlchemyStage.COLLECTION);
        Material collectMat = canCollect ? Material.NETHER_STAR : Material.GRAY_DYE;
        String collectTitle = canCollect ? "§6§l✦ THU ĐAN NGAY ✦" : "§7[Chưa Thể Thu Đan]";
        inv.setItem(40, createItem(collectMat, collectTitle,
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━",
                canCollect ? "§e▶ Đan phôi đã thành hình! Nhấp để thu lấy linh đan."
                        : "§7Hãy hoàn thành tinh luyện và dung hợp trước.",
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Slot 45: Nút Hủy Phiên Luyện Đan (nếu muốn dừng)
        if (session != null) {
            inv.setItem(45, createItem(Material.BARRIER, "§c§l[Hủy Phiên Luyện Đan]", "§7Nhấp để hủy phiên luyện hiện tại."));
        }

        // Slot 47: Thông tin Luyện Dược Sư
        inv.setItem(47, createItem(Material.BOOK, "§a§lPhẩm Cấp Luyện Dược Sư",
                "§7Cấp bậc: §e" + rank.getDisplayName(),
                "§7Kinh nghiệm: §b" + (rankService != null ? rankService.getExp(player) : 0) + " EXP"));

        // Slot 49: Trạng thái Linh Hồn Lực
        inv.setItem(49, createItem(Material.EXPERIENCE_BOTTLE, "§d§lTrạng Thái Linh Hồn",
                "§7Cảnh giới: " + soul.getRealm().getFormattedTag(),
                "§7Linh hồn lực: §a" + Math.round(soul.getCurrentSoul()) + " §7/ §e" + Math.round(soul.getSoulCapacity()),
                "§7Cường độ (Power): §b" + Math.round(soul.getSoulPower()),
                "§7Khống chế (Control): §6" + Math.round(soul.getSoulControl() * 100) + "%",
                "§7Cảm nhận (Perception): §c" + Math.round(soul.getSoulPerception() * 100) + "%"));

        player.openInventory(inv);
    }

    private static String getNextStepTip(BTTHAlchemySession session) {
        if (session == null) return "Bấm [CHỌN ĐAN PHƯƠNG] để bắt đầu.";
        return switch (session.getCurrentStage()) {
            case PREPARATION, HEATING -> "Điều chỉnh nhiệt độ đạt mốc mục tiêu, sau đó bấm vào thảo dược ở giữa!";
            case EXTRACTING_HERBS, EXTRACTING_CORE -> "Bấm vào các ô thảo dược (Slot 28-30) để chiết xuất!";
            case FUSION -> "Dược dịch đã sẵn sàng! Bấm nút [DUNG HỢP DƯỢC DỊCH] ở Slot 31.";
            case CONDENSATION, NOURISHMENT, COLLECTION -> "Đan phôi đã kết tinh! Bấm nút [THU ĐAN] ở Slot 40.";
            default -> "Hoàn tất.";
        };
    }

    /**
     * Dọn sạch an toàn các item nút bấm GUI bị kéo nhầm ra kho đồ người chơi do lỗi trước đó.
     */
    public static void cleanGlitchItems(Player player) {
        if (player == null) return;
        // Kiểm tra con trỏ chuột
        ItemStack cursor = player.getItemOnCursor();
        if (isGlitchItem(cursor)) {
            player.setItemOnCursor(null);
        }
        // Kiểm tra toàn bộ túi đồ và off-hand
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack is = player.getInventory().getItem(i);
            if (isGlitchItem(is)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public static boolean isGlitchItem(ItemStack is) {
        if (is == null || !is.hasItemMeta() || !is.getItemMeta().hasDisplayName()) return false;
        String name = is.getItemMeta().getDisplayName();
        return name.contains("Giảm Hỏa") || name.contains("Tăng Hỏa") || name.contains("THU ĐAN")
                || name.contains("Dung Hợp Dược Dịch") || name.contains("Phàm Đỉnh Rèn Thô")
                || name.contains("Trạng Thái Linh Hồn") || name.contains("Phẩm Cấp Luyện Dược Sư")
                || name.contains("TRẠNG THÁI DƯỢC ĐỈNH") || name.contains("CHỌN ĐAN PHƯƠNG")
                || name.contains("HƯỚNG DẪN LUYỆN ĐAN") || name.contains("Hủy Phiên Luyện Đan")
                || name.equals(" ");
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        List<String> list = new ArrayList<>();
        if (lore != null) {
            for (String l : lore) list.add(l);
        }
        return createItem(mat, name, list);
    }
}
