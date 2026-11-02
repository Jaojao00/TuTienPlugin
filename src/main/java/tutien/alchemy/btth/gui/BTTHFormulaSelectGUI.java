package tutien.alchemy.btth.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.alchemy.btth.model.AlchemistRank;
import tutien.alchemy.btth.model.BTTHFormula;
import tutien.alchemy.btth.model.BTTHIngredient;
import tutien.alchemy.btth.service.AlchemistRankService;
import tutien.alchemy.btth.service.BTTHFormulaManager;
import tutien.alchemy.btth.service.BTTHProcessController;
import tutien.alchemy.btth.service.SoulControlService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Giao diện Chọn Đan Phương trực quan 1-Click:
 * Hiển thị rõ công thức, yêu cầu và kiểm tra nguyên liệu trong kho đồ.
 */
public class BTTHFormulaSelectGUI {

    public static final String GUI_TITLE = "§0✦ CHỌN ĐAN PHƯƠNG LUYỆN CHẾ ✦";

    public static void open(Player player, BTTHFormulaManager formulaManager,
                            AlchemistRankService rankService, SoulControlService soulService) {
        if (player == null || formulaManager == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        AlchemistRank playerRank = rankService != null ? rankService.getRank(player) : AlchemistRank.HOC_DO;
        double soulPower = soulService != null ? soulService.getSoulData(player).getSoulPower() : 50.0;

        Collection<BTTHFormula> formulas = formulaManager.getAllFormulas();
        int slot = 10;

        for (BTTHFormula f : formulas) {
            if (slot >= 44) break;
            if (slot % 9 == 0 || slot % 9 == 8) slot++;

            boolean rankOk = playerRank.getTier() >= f.getRequiredRank().getTier();
            boolean soulOk = soulPower >= f.getRequiredSoulPower();

            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("§7Phẩm cấp: §e" + f.getTier() + " Phẩm §8(" + f.getChineseName() + "§8)");
            lore.add("§7Yêu cầu cấp: " + (rankOk ? "§a✔ " : "§c✘ ") + f.getRequiredRank().getDisplayName());
            lore.add("§7Yêu cầu Hồn Lực: " + (soulOk ? "§a✔ " : "§c✘ ") + "§b" + Math.round(f.getRequiredSoulPower()) + " SoulPower");
            lore.add("§7Nhiệt độ mục tiêu: §c" + Math.round(f.getTargetHeat()) + "°C");
            lore.add("§8─────────────────────────");
            lore.add("§eNguyên liệu cần thiết:");

            boolean hasAllMaterials = true;
            for (BTTHFormula.IngredientRequirement req : f.getIngredients()) {
                BTTHIngredient ing = formulaManager.getIngredient(req.getIngredientId());
                String ingName = ing != null ? ing.getDisplayName() : req.getIngredientId();
                int playerAmount = countItemInInventory(player, ing != null ? ing.getMaterial() : Material.PAPER);
                boolean enough = playerAmount >= req.getAmount();
                if (!enough) hasAllMaterials = false;

                lore.add(" §8• " + ingName + ": " + (enough ? "§a" : "§c") + playerAmount + "§7/" + req.getAmount());
            }

            lore.add("§8─────────────────────────");
            if (!f.getEffectLore().isEmpty()) {
                lore.add("§f" + f.getEffectLore());
            }
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");

            if (rankOk && soulOk && hasAllMaterials) {
                lore.add("§a§l▶ NHẤP CHUỘT TRÁI ĐỂ BẮT ĐẦU LUYỆN!");
            } else if (!rankOk || !soulOk) {
                lore.add("§c§l✘ CHƯA ĐỦ ĐIỀU KIỆN LUYỆN DƯỢC!");
            } else {
                lore.add("§e§l⚠ THIẾU NGUYÊN LIỆU TRONG TÚI ĐỒ!");
            }

            Material icon = f.getTier() >= 4 ? Material.ENCHANTED_BOOK : Material.BOOK;
            ItemStack item = createItem(icon, "§e§l" + f.getDisplayName() + " §8[" + f.getId() + "]", lore);
            inv.setItem(slot++, item);
        }

        // Slot 49: Nút quay lại
        inv.setItem(49, createItem(Material.ARROW, "§a§l◀ Quay Lại Đan Phòng", "§7Nhấp để trở về giao diện chính."));

        player.openInventory(inv);
    }

    private static int countItemInInventory(Player player, Material mat) {
        if (player == null || mat == null) return 0;
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) {
                count += is.getAmount();
            }
        }
        return count;
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
