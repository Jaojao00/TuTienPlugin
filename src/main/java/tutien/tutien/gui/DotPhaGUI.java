package tutien.tutien.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import tutien.core.CanhGioi;
import tutien.core.ItemKeys;
import tutien.core.LinhCan;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * HỆ THỐNG ĐỘT PHÁ & ĐỘ KIẾP HOÀN CHỈNH (TÍCH HỢP 2 TRONG 1)
 * v2.2 Skyblock Edition:
 * - Đổi strikeLightning thành strikeLightningEffect để KHÔNG đốt cháy đảo gỗ, KHÔNG biến Villager thành Witch và KHÔNG phá Item Frame/Armor Stand.
 * - Kiểm tra PersistentDataContainer (ItemKeys.TYPE_PILL) để chống dùng Đê (Anvil) đổi tên Đất/Đá thành Đan Đột Phá.
 */
public class DotPhaGUI implements Listener {

    private static final int GUI_SIZE = 27;
    private static final String GUI_TITLE = ChatColor.BLACK + "ĐỘT PHÁ CẢNH GIỚI";

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;

    public DotPhaGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    private static double getRequiredMinHP(CanhGioi nextRank) {
        return switch (nextRank) {
            case LUYEN_KHI -> 0;
            case TRUC_CO -> 0;
            case KIM_DAN -> 30.0;
            case NGUYEN_ANH -> 50.0;
            case HOA_THAN -> 80.0;
            case LUYEN_HU -> 120.0;
            case HOP_THE -> 180.0;
            case DAI_THUA -> 280.0;
            case DO_KIEP -> 400.0;
            case CHAN_TIEN -> 600.0;
            case DIA_TIEN -> 900.0;
            case THIEN_TIEN -> 1400.0;
            case HUYEN_TIEN -> 2200.0;
            case TIEN_QUAN -> 3500.0;
            case TIEN_VUONG -> 6000.0;
            case TIEN_DE -> 10000.0;
            default -> 0;
        };
    }

    public static void open(Player player, TuTienPlugin plugin) {
        // Cập nhật lại chỉ số trước khi kiểm tra HP tối đa
        tutien.combat.StatsManager.applyStats(player, plugin.getPlayerDataManager());

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        PlayerDataManager data = plugin.getPlayerDataManager();

        CanhGioi currentRank = data.getCanhGioi(player);
        CanhGioi nextRank = currentRank.getCanhGioiTiepTheo();

        if (nextRank == null) {
            player.sendMessage("§cNgươi đã đạt đỉnh phong, không thể đột phá thêm!");
            return;
        }

        int currentTuVi = data.getTuVi(player);
        int maxTuVi = nextRank.getTuViYeuCau();
        String requiredPillName = getRequiredPillName(nextRank);
        double requiredHP = getRequiredMinHP(nextRank);

        ItemStack breakthroughButton = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = breakthroughButton.getItemMeta();
        meta.setDisplayName("§c§lĐỘT PHÁ LÊN " + nextRank.getTenHienThi().toUpperCase());

        List<String> lore = new ArrayList<>();
        lore.add("§7Sau khi tu vi viên mãn, cần nghịch thiên");
        lore.add("§7chống lại Thiên Kiếp để thăng cấp.");
        lore.add("");
        lore.add("§f§lĐIỀU KIỆN ĐỘT PHÁ:");

        String tuViColor = (currentTuVi >= maxTuVi) ? "§a" : "§c";
        lore.add(tuViColor + " ❖ Tu Vi: " + currentTuVi + " / " + maxTuVi);

        if (!requiredPillName.equals("Không")) {
            boolean hasPill = hasBreakthroughPill(player, requiredPillName);
            String pillColor = hasPill ? "§a" : "§c";
            lore.add(pillColor + " ❖ Yêu cầu đan dược: " + requiredPillName);
        } else {
            lore.add("§a ❖ Yêu cầu đan dược: Không");
        }

        double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHP = player.getHealth();
        boolean isHealthy = currentHP >= maxHP * 0.9;
        String hpColor = isHealthy ? "§a" : "§c";
        lore.add(hpColor + " ❖ Thể trạng: " + (isHealthy ? "Khí Huyết Sung Mãn" : "Đang Trọng Thương (Cần hồi máu)"));

        if (requiredHP > 0) {
            boolean hpEnough = maxHP >= requiredHP;
            String hpMinColor = hpEnough ? "§a" : "§c";
            lore.add(hpMinColor + " ❖ Sinh lực tối thiểu: " + String.format("%.0f", maxHP) + " / " + String.format("%.0f", requiredHP) + " HP");
            if (!hpEnough) {
                lore.add("§c   ⚡ Luyện đan hoặc mặc Giáp Linh Khí để tăng HP!");
            }
        }

        lore.add("");
        lore.add("§f§lCHỈ SỐ DỰ KIẾN (SAU KHI THÀNH CÔNG):");
        lore.add("§7 ▪ Sát thương: §c" + currentRank.getBaseDamage() + " ➜ " + nextRank.getBaseDamage());
        lore.add("§7 ▪ Sinh lực: §c" + currentRank.getMaxMau() + " ➜ " + nextRank.getMaxMau());
        lore.add("§7 ▪ Linh lực: §b" + currentRank.getMaxLinhLuc() + " ➜ " + nextRank.getMaxLinhLuc());
        lore.add("");

        if (nextRank.ordinal() > CanhGioi.TRUC_CO.ordinal()) {
            lore.add("§4⚠ CẢNH BÁO: Đột phá sẽ dẫn động Cửu Đạo Thiên Kiếp!");
            lore.add("§4Cơ thể yếu ớt sẽ bị đánh tan xác, mất đi Tu Vi!");
            lore.add("");
            lore.add("§6💡 MẸO: Hãy trang bị TOÀN BỘ đồ mạnh nhất");
            lore.add("§6trước khi đột phá để tăng tối đa HP!");
        } else {
            lore.add("§eĐột phá an toàn (Không có lôi kiếp).");
        }

        lore.add("");
        lore.add("§a(Bấm để bắt đầu Đột Phá)");

        meta.setLore(lore);
        breakthroughButton.setItemMeta(meta);
        gui.setItem(13, breakthroughButton);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < GUI_SIZE; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, glass);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getRawSlot() == 13) {
            tutien.combat.StatsManager.applyStats(player, dataManager);

            CanhGioi currentRank = dataManager.getCanhGioi(player);
            CanhGioi nextRank = currentRank.getCanhGioiTiepTheo();
            if (nextRank == null) return;

            int currentTuVi = dataManager.getTuVi(player);
            int maxTuVi = nextRank.getTuViYeuCau();
            String requiredPillName = getRequiredPillName(nextRank);
            double requiredHP = getRequiredMinHP(nextRank);

            if (currentTuVi < maxTuVi) {
                player.sendMessage("§cNgươi chưa tu luyện đủ Tu Vi để độ kiếp!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (!requiredPillName.equals("Không") && !hasBreakthroughPill(player, requiredPillName)) {
                player.sendMessage("§cKhông có §e" + requiredPillName + " §chợp lệ, ngưng tụ linh khí thất bại!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            if (player.getHealth() < maxHP * 0.9) {
                player.sendMessage("§cKhí huyết đang suy yếu! Cần hồi phục thể trạng trước khi Đột Phá để tránh bạo thể!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (requiredHP > 0 && maxHP < requiredHP) {
                player.sendMessage("§c§l[Thiên Đạo] §cSinh lực quá yếu! Cần tối thiểu §e" + String.format("%.0f", requiredHP) + " HP §cđể chịu được Lôi Kiếp!");
                player.sendMessage("§6💡 Hãy luyện đan tăng Sinh Lực hoặc mặc Giáp Linh Khí trước khi đột phá!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (dataManager.isDokiepOnCooldown(player)) {
                long remaining = 30 - (System.currentTimeMillis() - dataManager.getDokiepCooldown(player)) / 1000;
                player.sendMessage("§c[Độ Kiếp] §fĐang trong giai đoạn hồi phục! Còn §e" + remaining + "s§f nữa.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            dataManager.setDokiepCooldown(player, System.currentTimeMillis());

            player.closeInventory();

            if (!requiredPillName.equals("Không")) {
                consumeBreakthroughPill(player, requiredPillName);
            }

            player.sendMessage("§e§l[Thiên Đạo] §fBắt đầu dẫn động thiên kiếp, hãy chuẩn bị ứng kiếp!");
            new DoKiepTask(plugin, player, nextRank).runTaskTimer(plugin, 0L, 20L);
        }
    }

    public static String getRequiredPillName(CanhGioi nextRank) {
        return switch (nextRank) {
            case LUYEN_KHI  -> "Tụ Khí Đan";
            case TRUC_CO    -> "Trúc Cơ Đan";
            case KIM_DAN    -> "Định Linh Đan";
            case NGUYEN_ANH -> "Nguyên Anh Đan";
            case HOA_THAN   -> "Hóa Thần Đan";
            case LUYEN_HU   -> "Luyện Hư Đan";
            case HOP_THE    -> "Hợp Thể Đan";
            case DAI_THUA   -> "Đại Thừa Đan";
            case DO_KIEP    -> "Độ Kiếp Đan";
            case CHAN_TIEN  -> "Chân Tiên Đan";
            case DIA_TIEN   -> "Địa Tiên Đan";
            case THIEN_TIEN -> "Thiên Tiên Đan";
            case HUYEN_TIEN -> "Huyền Tiên Đan";
            case TIEN_QUAN  -> "Tiên Quân Đan";
            case TIEN_VUONG -> "Tiên Vương Đan";
            case TIEN_DE    -> "Tiên Đế Đan";
            default -> "Không";
        };
    }

    private static boolean isValidPillMatch(ItemStack item, String requiredName) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        // Chống giả mạo bằng Đê (Anvil): Phải là vật phẩm Tu Tiên hợp lệ hoặc có Lore chuẩn của Đan Dược
        boolean hasPdc = ItemKeys.TYPE_PILL.equals(ItemKeys.getItemType(item)) || ItemKeys.isTuTienItem(item);
        boolean hasPillLore = item.getItemMeta().hasLore() && item.getItemMeta().getLore().stream()
                .anyMatch(l -> l.contains("Chuột Phải") || l.contains("DMG:") || l.contains("Đan đột phá"));
        if (!hasPdc && !hasPillLore) return false;

        String cleanName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName())).trim();
        return cleanName.equalsIgnoreCase(requiredName)
                || (requiredName.equals("Tiên Quân Đan") && cleanName.equalsIgnoreCase("Tiên Quan Đan"))
                || cleanName.contains(requiredName);
    }

    private static boolean hasBreakthroughPill(Player player, String requiredName) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isValidPillMatch(item, requiredName)) {
                return true;
            }
        }
        return false;
    }

    private static void consumeBreakthroughPill(Player player, String requiredName) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isValidPillMatch(item, requiredName)) {
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }

    public static class DoKiepTask extends BukkitRunnable {

        private final PlayerDataManager dataManager;
        private final Player player;
        private final CanhGioi nextRank;
        private final boolean isNormalBreakthrough;

        private int ticks = 0;
        private int strikeCount = 0;
        private final int MAX_STRIKES = 9;

        public DoKiepTask(TuTienPlugin plugin, Player player, CanhGioi nextRank) {
            this.dataManager = plugin.getPlayerDataManager();
            this.player = player;
            this.nextRank = nextRank;

            this.isNormalBreakthrough = nextRank.ordinal() <= CanhGioi.TRUC_CO.ordinal();

            if (!isNormalBreakthrough) {
                Bukkit.broadcastMessage("§8[§c§lThiên Địa Dị Tượng§8] §fTrên bầu trời §e" + player.getName() + " §fkiếp vân ngưng tụ, chuẩn bị đón nhận Cửu Đạo Thiên Kiếp để thăng cấp §b" + nextRank.getTenHienThi() + "§f!");
            }
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                if (!isNormalBreakthrough) handleFailure();
                this.cancel();
                return;
            }

            if (isNormalBreakthrough) {
                player.sendMessage("§e§l[Đột Phá] §fChân khí ngưng tụ đan điền, đột phá thành công!");
                handleSuccess();
                this.cancel();
                return;
            }

            ticks++;
            Location playerLoc = player.getLocation();
            Location cloudLoc = playerLoc.clone().add(0, 6, 0);

            LinhCan lc = dataManager.getLinhCan(player);
            Color cloudColor = getCloudColor(lc);
            Particle.DustOptions cloudDust = new Particle.DustOptions(cloudColor, 4.0f);

            player.getWorld().spawnParticle(Particle.DUST, cloudLoc, 60, 4, 0.5, 4, cloudDust);
            player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, cloudLoc, 10, 4, 0.5, 4, 0.02);

            if (ticks % 4 == 2) {
                player.playSound(playerLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 0.5f);
                if (strikeCount == 0) player.sendMessage("§8[§c!§8] §cThiên kiếp đã khóa chặt khí tức của bạn...");

            } else if (ticks % 4 == 0) {
                strikeCount++;
                // SKYBLOCK SAFE: Dùng strikeLightningEffect để không tạo lửa cháy đảo hay biến đổi Villager
                player.getWorld().strikeLightningEffect(playerLoc);

                double maxHealth = 20.0;
                AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealth = maxHealthAttr.getValue();
                }

                double damagePercent = Math.min(0.25, 0.03 + (strikeCount * 0.005));
                double rawDamage = maxHealth * damagePercent;

                player.damage(rawDamage);

                if (strikeCount < MAX_STRIKES) {
                    player.sendMessage("§e§l[Thiên Kiếp] §fĐã chống đỡ đạo thiên lôi thứ §c" + strikeCount + "§f/9!");
                } else {
                    player.sendMessage("§4§l[Thiên Kiếp] §cĐạo thiên lôi cuối cùng mang theo sức mạnh hủy diệt đã giáng xuống!");
                }

                if (strikeCount >= MAX_STRIKES) {
                    Bukkit.getScheduler().runTaskLater(TuTienPlugin.getPlugin(TuTienPlugin.class), () -> {
                        if (player.isOnline() && !player.isDead()) {
                            handleSuccess();
                        }
                    }, 20L);
                    this.cancel();
                }
            }
        }

        private Color getCloudColor(LinhCan lc) {
            return switch (lc) {
                case HON_DON_LINH_CAN -> Color.fromRGB(220, 20, 60);
                case THIEN_LINH_CAN -> Color.fromRGB(255, 215, 0);
                case DIA_LINH_CAN -> Color.fromRGB(255, 105, 180);
                case CHAN_LINH_CAN -> Color.fromRGB(50, 205, 50);
                case TAP_LINH_CAN -> Color.fromRGB(169, 169, 169);
                default -> Color.fromRGB(105, 105, 105);
            };
        }

        private void handleSuccess() {
            Location loc = player.getLocation();

            dataManager.setCanhGioi(player, nextRank);

            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttr != null) {
                player.setHealth(maxHealthAttr.getValue());
            }

            dataManager.setLinhLuc(player, dataManager.getMaxLinhLucEffective(player));

            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 100, 1, 1, 1, 0.5);
            player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

            Bukkit.broadcastMessage("§6§l[Phá Kiếp] §a" + player.getName() + " §fđã thăng cấp thành công, chính thức bước vào §b§l" + nextRank.getTenHienThi() + "§f!");
        }

        private void handleFailure() {
            if (player.isOnline()) {
                player.sendMessage("§c§l[Thất Bại] §fNhục thân không chịu nổi lực lượng Thiên Kiếp, đạo tâm vỡ nát!");

                int currentTuVi = dataManager.getTuVi(player);
                int phatTuVi = currentTuVi / 10;
                dataManager.addTuVi(player, -phatTuVi);

                player.sendMessage("§7* Bạn bị phản phệ mất đi §c" + phatTuVi + " §7Tu Vi.");
                Bukkit.broadcastMessage("§8[§c§lLạc Đạo§8] §e" + player.getName() + " §fđã gục ngã dưới uy áp của Thiên Kiếp, độ kiếp thất bại!");
            }
        }
    }
}
