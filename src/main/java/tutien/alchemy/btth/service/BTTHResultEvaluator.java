package tutien.alchemy.btth.service;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.alchemy.btth.model.BTTHAlchemySession;
import tutien.alchemy.btth.model.BTTHFormula;
import tutien.alchemy.btth.model.BTTHPillQuality;

import java.util.ArrayList;
import java.util.List;

/**
 * Đánh giá phẩm chất thành phẩm đan dược và tạo ItemStack:
 * - Phẩm cấp: 1 -> 10
 * - Phẩm chất: Hạ, Trung, Thượng, Cực phẩm
 * - Đan văn: 0 -> 3 Văn
 * - Đan độc tạo ra tương ứng độ tinh thuần
 */
public class BTTHResultEvaluator {

    public static BTTHPillQuality evaluate(BTTHAlchemySession session, double woodBonus) {
        if (session == null || session.getFormula() == null) {
            return new BTTHPillQuality(1, BTTHPillQuality.PillGrade.HA_PHAM, 0, 0.65, 15.0);
        }

        BTTHFormula formula = session.getFormula();
        double basePurity = session.getOverallPurity();

        // Bonus từ Dược đỉnh
        double cauldronStability = session.getCauldron() != null ? session.getCauldron().getStability() : 0.70;
        basePurity += (cauldronStability - 0.70) * 0.10;

        // Bonus từ Hỏa diễm
        double flameBonus = session.getCurrentFlame() != null ? session.getCurrentFlame().getQualityBonus() : 0.0;
        basePurity += flameBonus;

        // Bonus từ Mộc linh căn
        basePurity *= woodBonus;

        // Giới hạn độ tinh thuần (0.50 .. 1.00)
        double finalPurity = Math.max(0.50, Math.min(1.00, basePurity));

        // Xác định số Đan Văn (nếu đan phương hỗ trợ)
        int lines = 0;
        if (formula.getMaxLines() > 0 && formula.isFlameSwitchingRequired()) {
            lines = Math.min(formula.getMaxLines(), session.getFlameSwitchesCount());
            // Càng nhiều đan văn càng tăng độ tinh thuần
            finalPurity = Math.min(1.00, finalPurity + (lines * 0.03));
        }

        BTTHPillQuality.PillGrade grade = BTTHPillQuality.PillGrade.fromPurity(finalPurity);
        double toxicity = grade.getDefaultToxicity();
        if (lines > 0) {
            // Đan dược có văn khí mạnh mẽ có thể tạo thêm chút phản phệ độc tố nếu chưa đạt Cực Phẩm
            toxicity += lines * 2.0;
        }

        return new BTTHPillQuality(formula.getTier(), grade, lines, finalPurity, toxicity);
    }

    public static ItemStack createPillItem(BTTHFormula formula, BTTHPillQuality quality) {
        if (formula == null || quality == null) return new ItemStack(Material.POTION);

        ItemStack item = new ItemStack(Material.FERMENTED_SPIDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = quality.getFormattedDisplay(formula.getDisplayName());
            meta.setDisplayName(name);

            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("§7Phẩm cấp: §e" + formula.getTier() + " Phẩm §8(" + formula.getChineseName() + "§8)");
            lore.add("§7Phẩm chất: " + quality.getGrade().getTag());
            if (quality.getDanLines() > 0) {
                lore.add("§7Đan văn: §e" + quality.getDanLines() + " Văn §6✦✦✦");
            }
            lore.add("§7Độ tinh thuần: §a" + String.format("%.1f%%", quality.getPurity() * 100.0));
            lore.add("§7Đan độc tích lũy: " + (quality.getDanDoc() > 0 ? "§c+" + Math.round(quality.getDanDoc()) : "§a0 (Thuần khiết)"));
            lore.add("§8───────────────────────────");
            if (!formula.getEffectLore().isEmpty()) {
                lore.add("§f" + formula.getEffectLore());
            }
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("§eNhấp chuột phải để phục dụng linh đan.");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
