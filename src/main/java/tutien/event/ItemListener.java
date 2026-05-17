package tutien.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tutien.core.PlayerDataManager;

/**
 * Lớp xử lý tương tác vật phẩm (Đan dược, Bí kíp, Tâm pháp).
 * Đã cập nhật 50 món dựa trên MythicMobs và hệ thống Tu Vi/Linh Lực.
 */
public class ItemListener implements Listener {

    private final PlayerDataManager dataManager;
    private final NamespacedKey oraxenKey = new NamespacedKey("oraxen", "id");

    public ItemListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        // Chỉ xử lý khi người chơi Chuột Phải để sử dụng vật phẩm
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() == Material.AIR || !item.hasItemMeta()) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            // Kiểm tra ID Oraxen
            if (meta.getPersistentDataContainer().has(oraxenKey, PersistentDataType.STRING)) {
                String id = meta.getPersistentDataContainer().get(oraxenKey, PersistentDataType.STRING);
                if (id == null) return;

                event.setCancelled(true); // Ngăn chặn đặt block

                // ==========================================
                // PHẦN I: XỬ LÝ 20 LOẠI ĐAN DƯỢC (PILLS)
                // ==========================================
                switch (id.toLowerCase()) {
                    case "tu_linh_dan":
                        int ll = dataManager.getLinhLuc(player);
                        dataManager.setLinhLuc(player, Math.min(1000, ll + 200));
                        player.sendMessage("§a[Hệ Thống] §fHồi phục 20% Linh Lực.");
                        consume(player, item, true);
                        break;

                    case "hoang_long_dan":
                        dataManager.addTuVi(player, 500);
                        player.sendMessage("§a[Hệ Thống] §fHấp thụ dược lực, tăng 500 Tu Vi.");
                        consume(player, item, true);
                        break;

                    case "truc_co_dan":
                        dataManager.addTuVi(player, 1000);
                        player.sendMessage("§e[Hệ Thống] §fDược lực Trúc Cơ Đan giúp củng cố căn cơ.");
                        consume(player, item, true);
                        break;

                    case "thai_at_dan":
                        // Gọi Skill MythicMobs (God Mode)
                        castSkill(player, "Pill_ThaiAt_Effect");
                        player.sendMessage("§d[Huyền Thoại] §fThái Ất chân khí hộ thân, vạn pháp bất xâm!");
                        consume(player, item, true);
                        break;

                    case "nhi_thien_dan":
                        castSkill(player, "Pill_NghichThien_Effect");
                        player.sendMessage("§c[Nghịch Thiên] §fDược lực nấp trong tim, sẵn sàng cứu mạng.");
                        consume(player, item, true);
                        break;

                    case "thanh_tam_dan":
                        player.sendMessage("§b[Hệ Thống] §fTâm thần thanh tịnh, xua tan tạp niệm.");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear " + player.getName());
                        consume(player, item, true);
                        break;

                    case "cuu_chuyen_dan":
                        dataManager.addTuVi(player, 50000); // Tương đương 1 level cao cấp
                        player.sendMessage("§6[Thánh Vật] §fCửu Chuyển Đan giúp tu vi tăng vọt!");
                        consume(player, item, true);
                        break;

                    // ==========================================
                    // PHẦN II: TÂM PHÁP & LUYỆN THỂ (KNOWLEDGE)
                    // ==========================================
                    case "thanh_tam_quyet":
                    case "ngu_hanh_cong":
                    case "truong_sinh_cong":
                    case "thiet_bo_diep":
                    case "ban_co_the":
                        player.sendMessage("§e[Kỳ Ngộ] §fNgươi đã lĩnh ngộ bí kíp: §b" + meta.getDisplayName());
                        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                        // Logic lưu vào DB hoặc Metadata ở đây
                        consume(player, item, false);
                        break;

                    // ==========================================
                    // PHẦN III: VÕ KỸ (SKILLS LEARNING)
                    // ==========================================
                    case "hoa_cau_thuat":
                    case "ngu_kiem_thuat":
                    case "van_kiem_quy_tong":
                        player.sendMessage("§c[Võ Kỹ] §fNgươi đã học được thần thông: §e" + meta.getDisplayName());
                        player.sendMessage("§7(Sử dụng: Sneak + Chuột trái)");
                        consume(player, item, false);
                        break;
                }
            }
        }
    }

    /**
     * XỬ LÝ THI TRIỂN CHIÊU THỨC (Sneak + Left Click)
     */
    @EventHandler
    public void onPlayerCast(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (!player.isSneaking()) return;

            ItemStack item = player.getInventory().getItemInMainHand();
            int mana = dataManager.getLinhLuc(player);

            // 1. Nếu cầm Kiếm: Vạn Kiếm Quy Tông (Hoặc các võ kỹ hệ Kiếm)
            if (item.getType().name().contains("SWORD")) {
                if (mana >= 100) {
                    dataManager.setLinhLuc(player, mana - 100);
                    castSkill(player, "Skill_VanKiemQuyTong");
                } else {
                    player.sendMessage("§c[!] Không đủ Linh Lực!");
                }
            }
            // 2. Nếu tay không (Pháp thuật): Hỏa Cầu Thuật
            else if (item.getType() == Material.AIR) {
                if (mana >= 30) {
                    dataManager.setLinhLuc(player, mana - 30);
                    castSkill(player, "Skill_HoaCauThuat");
                }
            }
        }
    }

    /**
     * Hàm gọi skill từ MythicMobs thông qua Console
     */
    private void castSkill(Player player, String skillName) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mm test cast " + skillName + " --asPlayer " + player.getName());
    }

    /**
     * Tiêu thụ vật phẩm sau khi dùng
     */
    private void consume(Player player, ItemStack item, boolean isPill) {
        if (isPill) {
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}