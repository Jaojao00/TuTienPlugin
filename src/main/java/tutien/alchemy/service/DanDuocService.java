package tutien.alchemy.service;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tutien.alchemy.manager.DanDuocManager;
import tutien.alchemy.model.DanDuoc;
import tutien.alchemy.model.DanDuocEffect;
import tutien.core.CanhGioi;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service xử lý kiểm tra điều kiện và sử dụng Đan Dược an toàn (Single-transaction).
 */
public class DanDuocService {

    public enum ConsumeResult {
        SUCCESS,
        INVALID_ITEM,
        NOT_FOUND,
        REALM_TOO_LOW,
        COOLDOWN,
        ERROR
    }

    private final PlayerDataManager dataManager;
    private final DanDuocManager danDuocManager;
    private final DanDocService danDocService;
    private final DrugResistanceService drugResistanceService;

    // Bộ đệm chống spam click ăn đan (500ms)
    private final Map<UUID, Long> consumeDebounce = new ConcurrentHashMap<>();

    public DanDuocService(PlayerDataManager dataManager, DanDuocManager danDuocManager,
                          DanDocService danDocService, DrugResistanceService drugResistanceService) {
        this.dataManager = dataManager;
        this.danDuocManager = danDuocManager;
        this.danDocService = danDocService;
        this.drugResistanceService = drugResistanceService;
    }

    /**
     * Thực hiện ăn đan dược với đầy đủ kiểm tra bảo mật:
     * 1. Xác thực PDC chống hàng giả mạo Anvil.
     * 2. Kiểm tra Cảnh Giới yêu cầu.
     * 3. Trừ chính xác 1 vật phẩm (atomic).
     * 4. Áp dụng Đan Độc và Kháng Thuốc.
     * 5. Co giãn hiệu quả theo hệ số kháng thuốc M_R.
     */
    public synchronized ConsumeResult consumePill(Player player, ItemStack item) {
        if (player == null || item == null) return ConsumeResult.ERROR;

        // Chống spam click
        long now = System.currentTimeMillis();
        long lastUse = consumeDebounce.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastUse < 500) {
            return ConsumeResult.COOLDOWN;
        }
        consumeDebounce.put(player.getUniqueId(), now);

        // 1. Kiểm tra mã định danh PDC
        String pillId = ItemKeys.getPillId(item);
        if (pillId == null) {
            player.sendMessage(ChatColor.RED + "Vật phẩm này không phải Đan Dược Tu Tiên hợp lệ!");
            return ConsumeResult.INVALID_ITEM;
        }

        // 2. Tra cứu trong Catalog
        DanDuoc pill = danDuocManager.getDanDuoc(pillId);
        if (pill == null) {
            player.sendMessage(ChatColor.RED + "Không tìm thấy đan phương của loại đan dược này trong thiên địa!");
            return ConsumeResult.NOT_FOUND;
        }

        // 3. Kiểm tra cảnh giới tối thiểu
        CanhGioi playerRealm = dataManager.getCanhGioi(player);
        if (!playerRealm.isAtLeast(pill.getRequiredRealm())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Cảnh Giới Quá Thấp] Ngươi cần đạt tối thiểu &e" + pill.getRequiredRealm().getTenHienThi() +
                            " &cmới có thể luyện hóa &f" + pill.getDisplayName() + "&c!"));
            return ConsumeResult.REALM_TOO_LOW;
        }

        // 4. Trừ vật phẩm nguyên tử (Single Transaction) với khả năng rollback an toàn
        int originalAmount = item.getAmount();
        if (originalAmount > 1) {
            item.setAmount(originalAmount - 1);
        } else {
            item.setAmount(0);
        }

        // 5. Tính hệ số hiệu quả theo Kháng Thuốc: M_R
        String group = pill.getResistanceGroup();
        double mr = drugResistanceService.getEfficiencyMultiplier(player, group);

        double newDanDoc = 0.0;
        int finalTuVi = 0;
        int finalQi = 0;
        double finalDmg = 0.0;
        double finalHp = 0.0;

        try {
            // 6. Áp dụng Đan Độc & Kháng Thuốc
            newDanDoc = danDocService.addDanDoc(player, pill.getDanDocGain());
            drugResistanceService.addResistance(player, group, pill.getResistanceGain());

            // 7. Áp dụng hiệu quả thực tế (E_final = E_base * M_R)
            DanDuocEffect eff = pill.getEffect();

            // Tăng Tu Vi
            int baseTuVi = eff.getInstantTuVi();
            finalTuVi = (int) Math.round(baseTuVi * mr);
            if (finalTuVi > 0) {
                dataManager.addTuVi(player, finalTuVi);
            }

            // Hồi Linh Lực
            int baseQi = eff.getInstantLinhLuc();
            finalQi = (int) Math.round(baseQi * mr);
            if (finalQi > 0) {
                dataManager.addLinhLuc(player, finalQi);
            }

            // Tăng Sát Thương vĩnh viễn/tạm thời
            double baseDmg = eff.getBonusDamage();
            finalDmg = baseDmg * mr;
            if (finalDmg > 0) {
                dataManager.addBonusDamage(player, finalDmg);
            }

            // Tăng Sinh Lực vĩnh viễn/tạm thời
            double baseHp = eff.getBonusHealth();
            finalHp = baseHp * mr;
            if (finalHp > 0) {
                dataManager.addBonusHealth(player, finalHp);
            }

            // Hồi máu tức thời
            double heal = eff.getInstantHeal() * mr;
            if (heal > 0 && !player.isDead()) {
                double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + heal);
                player.setHealth(newHealth);
            }

            // Tăng số lần dùng đan
            dataManager.addSoLanLuyenDan(player, 1);
        } catch (Throwable t) {
            // ROLLBACK: Hoàn nguyên vật phẩm nếu áp dụng hiệu quả gặp lỗi bất thường
            item.setAmount(originalAmount);
            danDocService.reduceDanDoc(player, pill.getDanDocGain());
            drugResistanceService.reduceResistance(player, group, pill.getResistanceGain());
            player.sendMessage(ChatColor.RED + "Luyện hóa thất bại do dị biến kinh mạch! Đan dược đã được hoàn trả.");
            return ConsumeResult.ERROR;
        }

        // 8. Phản hồi người chơi
        sendFeedback(player, pill, finalTuVi, finalQi, finalDmg, finalHp, mr, newDanDoc);

        return ConsumeResult.SUCCESS;
    }

    private void sendFeedback(Player player, DanDuoc pill, int tuVi, int qi, double dmg, double hp, double mr, double danDoc) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✦ [Luyện Hóa Đan Dược] &fĐã phục dụng: " + pill.getDisplayName()));

        if (tuVi > 0) player.sendMessage(ChatColor.GREEN + "  + " + tuVi + " Tu Vi");
        if (qi > 0) player.sendMessage(ChatColor.AQUA + "  + " + qi + " Linh Lực");
        if (dmg > 0) player.sendMessage(ChatColor.RED + "  + " + String.format("%.2f", dmg) + " Sát Thương");
        if (hp > 0) player.sendMessage(ChatColor.LIGHT_PURPLE + "  + " + String.format("%.2f", hp) + " Sinh Lực");

        if (mr < 0.999) {
            int reducedPercent = (int) Math.round((1.0 - mr) * 100);
            player.sendMessage(ChatColor.YELLOW + "  (Kháng thuốc " + pill.getResistanceGroup() +
                    " làm giảm " + reducedPercent + "% hiệu lực)");
        }

        player.sendMessage(ChatColor.DARK_RED + "  [Đan Độc: " + String.format("%.1f", danDoc) + "/1000]");

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.2f);
        } catch (Exception ignored) {}
    }
}
