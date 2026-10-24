package tutien.event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tutien.combat.StatsManager;
import tutien.core.HeTuLuyen;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;
import tutien.tutien.gui.LuyenDanGUI;

/**
 * Lớp xử lý tương tác vật phẩm (Đan dược, Bí kíp, Tâm pháp, Linh Thú, Tiền tệ).
 * v2.2 Skyblock Edition:
 *   - Cho phép ăn mọi loại Đan Dược (Luyện Đan, Vạn Giới Các, Bế Quan) bằng Chuột Phải.
 *   - Ngăn chặn đặt nhầm các đan dược có Material dạng Block (Beacon, End Crystal, Dragon Egg, Moss Block...).
 *   - Ngăn chặn đặt nhầm Trứng Linh Thú (Spawn Egg) ra đảo hoặc đổi Spawner trên Skyblock.
 *   - Cho phép Chuột Phải vào vật phẩm "Đạo Niệm" / "Linh Thạch" cũ để nạp vào tài khoản.
 */
public class ItemListener implements Listener {

    private final PlayerDataManager dataManager;
    private final NamespacedKey oraxenKey = new NamespacedKey("oraxen", "id");

    public ItemListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // Chỉ xử lý tay chính để tránh kích hoạt 2 lần
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String itemType = pdc.get(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING);
        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : "";
        String strippedName = ChatColor.stripColor(displayName).trim();

        // ==========================================
        // 1. CHẶN SPAWN EGG LINH THÚ (TRÁNH PHÁ SPAWNER / THẢ MOB VANI)
        // ==========================================
        if (ItemKeys.TYPE_PET.equals(itemType)
                || (item.getType().name().endsWith("_SPAWN_EGG") && meta.hasLore()
                && meta.getLore().stream().anyMatch(l -> l.contains("Linh Thú")))) {
            event.setCancelled(true);
            player.sendMessage("§d§l[Linh Thú] §fĐặt Linh Thú trong túi đồ hoặc tay phụ để tự động nhận hiệu ứng trợ chiến!");
            player.playSound(player.getLocation(), Sound.ENTITY_CAT_AMBIENT, 0.8f, 1.2f);
            return;
        }

        // ==========================================
        // 2. NẠP ĐẠO NIỆM VẬT LÝ (TƯƠNG THÍCH NGƯỢC)
        // ==========================================
        if (item.getType() == Material.EMERALD && strippedName.equalsIgnoreCase("Đạo Niệm")) {
            event.setCancelled(true);
            int amount = item.getAmount();
            player.getInventory().setItemInMainHand(null);
            dataManager.addDaoNiem(player, amount);
            player.sendMessage("§b§l[Đạo Niệm] §fĐã hấp thu §e+" + amount + " Đạo Niệm §fvào thức hải! (Tổng: §b" + dataManager.getDaoNiem(player) + "§f)");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            return;
        }

        // ==========================================
        // 3. CHẶN ĐẶT BOSS LOOT / PHỤ KIỆN DẠNG BLOCK
        // ==========================================
        if (ItemKeys.TYPE_BOSS_LOOT.equals(itemType) || ItemKeys.TYPE_ACCESSORY.equals(itemType)
                || strippedName.contains("Boss Loot") || strippedName.contains("Phụ Kiện")) {
            event.setCancelled(true);
            return;
        }

        // ==========================================
        // 4. XỬ LÝ ĐAN DƯỢC CHUẨN (PDC HOẶC TÊN TRONG DANDUOC.YML)
        // ==========================================
        LuyenDanGUI.DanDuocData matchedPill = null;
        String pillKey = pdc.get(ItemKeys.KEY_PILL_ID, PersistentDataType.STRING);
        if (pillKey != null) {
            matchedPill = LuyenDanGUI.findPillByKey(pillKey);
        }
        if (matchedPill == null && !strippedName.isEmpty()) {
            matchedPill = LuyenDanGUI.findPillByName(strippedName);
        }

        if (ItemKeys.TYPE_PILL.equals(itemType) || matchedPill != null) {
            event.setCancelled(true); // Chặn đặt block (Beacon, End Crystal, Dragon Egg, Moss Block...)

            double bonusDmg = pdc.getOrDefault(ItemKeys.KEY_PILL_DMG, PersistentDataType.DOUBLE,
                    matchedPill != null ? matchedPill.bonusDamage : 0.0);
            double bonusHp = pdc.getOrDefault(ItemKeys.KEY_PILL_HP, PersistentDataType.DOUBLE,
                    matchedPill != null ? matchedPill.bonusHealth : 0.0);
            int bonusTuVi = pdc.getOrDefault(ItemKeys.KEY_PILL_TUVI, PersistentDataType.INTEGER,
                    matchedPill != null ? matchedPill.bonusTuVi : 0);

            // Đặc biệt cho một số đan dược trong Bế Quan Shop không nằm trong danduoc.yml
            if (matchedPill == null && bonusDmg == 0 && bonusHp == 0 && bonusTuVi == 0) {
                if (strippedName.equalsIgnoreCase("Hoàng Long Đan")) {
                    bonusTuVi = 500;
                } else if (strippedName.equalsIgnoreCase("Đại Hoàn Đan")) {
                    int maxLL = dataManager.getMaxLinhLucEffective(player);
                    dataManager.setLinhLuc(player, maxLL);
                    player.sendMessage("§a§l[Đan Dược] §fĐã phục용 §6Đại Hoàn Đan§f, hồi phục toàn bộ §b" + maxLL + " Linh Lực§f!");
                    consume(player, item, true);
                    return;
                } else if (strippedName.equalsIgnoreCase("Tâm Pháp Đặc Biệt")) {
                    bonusTuVi = 1000;
                    bonusDmg = 1.0;
                    bonusHp = 5.0;
                } else if (strippedName.equalsIgnoreCase("Thần Thông Phù")) {
                    bonusDmg = 2.0;
                    bonusHp = 8.0;
                }
            }

            // Hệ Đan Tu: nhận thêm 25% hiệu quả dược lực
            double danTuMult = (dataManager.getHeTuLuyen(player) == HeTuLuyen.DAN_TU) ? 1.25 : 1.0;
            double finalDmg = Math.round(bonusDmg * danTuMult * 100.0) / 100.0;
            double finalHp  = Math.round(bonusHp * danTuMult * 100.0) / 100.0;
            int finalTuVi   = (int) Math.round(bonusTuVi * danTuMult);

            if (finalDmg > 0) dataManager.addBonusDamage(player, finalDmg);
            if (finalHp > 0)  dataManager.addBonusHealth(player, finalHp);
            if (finalTuVi > 0) {
                dataManager.addBonusTuVi(player, finalTuVi);
                dataManager.addTuVi(player, finalTuVi);
            }

            // Hồi thêm một phần Linh Lực khi dùng đan
            int maxLL = dataManager.getMaxLinhLucEffective(player);
            int curLL = dataManager.getLinhLuc(player);
            dataManager.setLinhLuc(player, Math.min(maxLL, curLL + Math.max(20, finalTuVi)));

            StatsManager.applyStats(player, dataManager);
            consume(player, item, true);

            player.sendMessage("§a§l[Luyện Hóa Đan Dược] §fĐã hấp thu §e" + strippedName + "§f!"
                    + (danTuMult > 1.0 ? " §d(Đan Tu +25%)" : ""));
            player.sendMessage("§7  ▪ Công kích vĩnh viễn: §c+" + finalDmg
                    + " §7| Sinh lực: §a+" + finalHp
                    + " §7| Tu Vi: §e+" + finalTuVi);
            return;
        }

        // ==========================================
        // 5. TƯƠNG THÍCH ORAXEN CŨ (NẾU SERVER CÓ CÀI ORAXEN)
        // ==========================================
        if (pdc.has(oraxenKey, PersistentDataType.STRING)) {
            String id = pdc.get(oraxenKey, PersistentDataType.STRING);
            if (id == null) return;

            event.setCancelled(true);

            switch (id.toLowerCase()) {
                case "tu_linh_dan" -> {
                    int ll = dataManager.getLinhLuc(player);
                    int maxLL = dataManager.getMaxLinhLucEffective(player);
                    dataManager.setLinhLuc(player, Math.min(maxLL, ll + 200));
                    player.sendMessage("§a[Hệ Thống] §fHồi phục 200 Linh Lực.");
                    consume(player, item, true);
                }
                case "hoang_long_dan" -> {
                    dataManager.addTuVi(player, 500);
                    player.sendMessage("§a[Hệ Thống] §fHấp thụ dược lực, tăng 500 Tu Vi.");
                    consume(player, item, true);
                }
                case "truc_co_dan" -> {
                    dataManager.addTuVi(player, 1000);
                    player.sendMessage("§e[Hệ Thống] §fDược lực Trúc Cơ Đan giúp củng cố căn cơ.");
                    consume(player, item, true);
                }
                case "thai_at_dan" -> {
                    castSkill(player, "Pill_ThaiAt_Effect");
                    player.sendMessage("§d[Huyền Thoại] §fThái Ất chân khí hộ thân, vạn pháp bất xâm!");
                    consume(player, item, true);
                }
                case "nhi_thien_dan" -> {
                    castSkill(player, "Pill_NghichThien_Effect");
                    player.sendMessage("§c[Nghịch Thiên] §fDược lực nấp trong tim, sẵn sàng cứu mạng.");
                    consume(player, item, true);
                }
                case "thanh_tam_dan" -> {
                    player.sendMessage("§b[Hệ Thống] §fTâm thần thanh tịnh, xua tan tạp niệm.");
                    player.getActivePotionEffects().forEach(eff -> player.removePotionEffect(eff.getType()));
                    consume(player, item, true);
                }
                case "cuu_chuyen_dan" -> {
                    dataManager.addTuVi(player, 50000);
                    player.sendMessage("§6[Thánh Vật] §fCửu Chuyển Đan giúp tu vi tăng vọt!");
                    consume(player, item, true);
                }
                case "thanh_tam_quyet", "ngu_hanh_cong", "truong_sinh_cong", "thiet_bo_diep", "ban_co_the" -> {
                    player.sendMessage("§e[Kỳ Ngộ] §fNgươi đã lĩnh ngộ bí kíp: §b" + meta.getDisplayName());
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                    dataManager.addBonusHealth(player, 10.0);
                    dataManager.addBonusDamage(player, 2.0);
                    StatsManager.applyStats(player, dataManager);
                    consume(player, item, false);
                }
                case "hoa_cau_thuat", "ngu_kiem_thuat", "van_kiem_quy_tong" -> {
                    player.sendMessage("§c[Võ Kỹ] §fNgươi đã học được thần thông: §e" + meta.getDisplayName());
                    player.sendMessage("§7(Sử dụng: Sneak + Chuột trái)");
                    consume(player, item, false);
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

            // Chỉ kích hoạt khi MythicMobs có mặt trên server để tránh spam Unknown Command
            if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) return;

            ItemStack item = player.getInventory().getItemInMainHand();
            int mana = dataManager.getLinhLuc(player);

            if (item.getType().name().contains("SWORD")) {
                if (mana >= 100) {
                    dataManager.setLinhLuc(player, mana - 100);
                    castSkill(player, "Skill_VanKiemQuyTong");
                } else {
                    player.sendMessage("§c[!] Không đủ Linh Lực!");
                }
            } else if (item.getType() == Material.AIR) {
                if (mana >= 30) {
                    dataManager.setLinhLuc(player, mana - 30);
                    castSkill(player, "Skill_HoaCauThuat");
                }
            }
        }
    }

    private void castSkill(Player player, String skillName) {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "mm test cast " + skillName + " --asPlayer " + player.getName());
        }
    }

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
