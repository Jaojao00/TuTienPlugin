package tutien.event;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tutien.combat.StatsManager;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.quest.NhiemVuManager;
import tutien.task.NguKiemTask;
import tutien.tutien.gui.ChonHeGUI;

import java.util.HashMap;
import java.util.UUID;

/**
 * Lớp lắng nghe sự kiện người chơi.
 * v2.2 Skyblock Edition:
 *   - Sửa Thuấn Di (Blink): Chỉ kích hoạt khi người chơi cầm Kiếm VÀ nhìn chếch lên trên (pitch < -15 độ).
 *     Tuyệt đối không còn bị bay văng xuống vực Void khi vừa chạy vừa bấm Shift bắc cầu (Speed-bridging) trên đảo Skyblock!
 *   - Khi thoát game chỉ tắt Fly nếu đang dùng Ngự Kiếm (tôn trọng quyền /is fly của plugin Skyblock).
 */
public class PlayerListener implements Listener {

    private final PlayerDataManager dataManager;
    private final TuTienPlugin plugin;

    private final HashMap<UUID, Long> thuanDiCooldowns = new HashMap<>();

    public PlayerListener(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        dataManager.loadPlayer(player);

        if (plugin.getTuiDoManager() != null) {
            plugin.getTuiDoManager().loadPlayer(player);
        }

        StatsManager.applyStats(player, dataManager);

        if (plugin.getAlchemyEscrowManager() != null) {
            plugin.getAlchemyEscrowManager().onPlayerJoin(player);
        }

        if (dataManager.getHeTuLuyen(player) == HeTuLuyen.CHUA_CHON) {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> ChonHeGUI.open(player, dataManager), 40L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                StatsManager.applyStats(player, dataManager);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (dataManager.isMeditating(player)) {
            dataManager.setMeditationType(player, tutien.core.MeditationType.NONE);
            player.removePotionEffect(PotionEffectType.LEVITATION);
        }

        // Chỉ tắt Fly khi thoát nếu người chơi đang bật Ngự Kiếm (tránh mất /is fly của Skyblock)
        if (NguKiemTask.isFlying(player)) {
            NguKiemTask.cleanupPlayer(player.getUniqueId());
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }

        thuanDiCooldowns.remove(player.getUniqueId());

        if (plugin.getNhiemVuManager() != null) {
            plugin.getNhiemVuManager().cleanupPlayer(player.getUniqueId());
        }

        if (plugin.getLinhThuVienManager() != null) {
            plugin.getLinhThuVienManager().cleanupPlayer(player.getUniqueId());
        }

        if (plugin.getDrugResistanceService() != null) {
            plugin.getDrugResistanceService().onPlayerQuit(player.getUniqueId());
        }

        if (plugin.getDetoxService() != null && plugin.getDetoxService().isDetoxing(player)) {
            plugin.getDetoxService().stopDetoxSession(player, "Thoát game");
        }

        if (plugin.getAlchemyService() != null && plugin.getAlchemyService().isAlchemizing(player.getUniqueId())) {
            plugin.getAlchemyService().onPlayerQuit(player);
        }

        if (plugin.getBiCanhManager() != null) {
            plugin.getBiCanhManager().onPlayerQuit(player);
        }

        if (plugin.getTuiDoManager() != null) {
            plugin.getTuiDoManager().savePlayer(player);
        }

        dataManager.savePlayer(player);
    }

    // ==========================================
    // THẦN THÔNG: THUẤN DI (BLINK AN TOÀN CHO SKYBLOCK)
    // Thao tác: Cầm Kiếm + Nhìn lên trên (pitch < -15) + Đang chạy nhanh (Sprint) -> Bấm Shift
    // ==========================================
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking() && player.isSprinting()) {
            // Điều kiện an toàn Skyblock: Phải cầm Kiếm và nhìn chếch lên trên để không kích hoạt nhầm khi đang bắc cầu (bridging)
            String handType = player.getInventory().getItemInMainHand().getType().name();
            if (!handType.contains("SWORD") || player.getLocation().getPitch() > -15.0f) {
                return;
            }

            CanhGioi cg = dataManager.getCanhGioi(player);
            if (cg.ordinal() >= CanhGioi.KIM_DAN.ordinal()) {
                long currentTime = System.currentTimeMillis();
                long lastUsed = thuanDiCooldowns.getOrDefault(player.getUniqueId(), 0L);

                if (currentTime - lastUsed < 5000) {
                    long timeLeft = 5 - ((currentTime - lastUsed) / 1000);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§c[!] §fThuấn Di đang hồi chiêu (" + timeLeft + "s)"));
                    return;
                }

                int linhLuc = dataManager.getLinhLuc(player);
                if (linhLuc < 300) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§c[!] §fKhông đủ 300 Linh Lực để thi triển Thuấn Di!"));
                    return;
                }

                dataManager.setLinhLuc(player, linhLuc - 300);
                thuanDiCooldowns.put(player.getUniqueId(), currentTime);

                Location loc = player.getLocation();
                Vector dir = loc.getDirection().normalize();
                Location target = loc.clone();

                for (int i = 1; i <= 12; i++) {
                    Location checkLoc = loc.clone().add(dir.clone().multiply(i));
                    if (checkLoc.getBlock().getType().isSolid() || checkLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                        break;
                    }
                    target = checkLoc;
                }

                target.setYaw(loc.getYaw());
                target.setPitch(loc.getPitch());

                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.5, 1, 0.5, 0.1);
                player.teleport(target);
                player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                player.getWorld().spawnParticle(Particle.PORTAL, target, 50, 0.5, 1, 0.5, 0.2);
                player.sendMessage("§b§l[Thần Thông] §fThuấn Di!");
            }
        }
    }

    // ==========================================
    // KHÓA DI CHUYỂN NGANG KHI ĐANG TỌA THIỀN
    // ==========================================
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (plugin.getDetoxService() != null && plugin.getDetoxService().isDetoxing(player)) {
            Location to = event.getTo();
            if (to != null && (event.getFrom().getBlockX() != to.getBlockX() || event.getFrom().getBlockZ() != to.getBlockZ())) {
                plugin.getDetoxService().stopDetoxSession(player, "Di chuyển khỏi tọa thiền");
            }
        }

        if (dataManager.isMeditating(player)) {
            Location to = event.getTo();
            if (to != null && (event.getFrom().getX() != to.getX() ||
                    event.getFrom().getZ() != to.getZ())) {
                Location fixed = event.getFrom().clone();
                fixed.setY(to.getY());
                fixed.setPitch(to.getPitch());
                fixed.setYaw(to.getYaw());
                event.setTo(fixed);
            }
        }
    }

    // ==========================================
    // HỦY TRẠNG THÁI TỌA THIỀN KHI BỊ TẤN CÔNG
    // ==========================================
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (plugin.getDetoxService() != null && plugin.getDetoxService().isDetoxing(player)) {
                plugin.getDetoxService().stopDetoxSession(player, "Bị tấn công");
            }
            if (dataManager.isMeditating(player)) {
                tutien.core.MeditationType prevType = dataManager.getMeditationType(player);
                dataManager.setMeditationType(player, tutien.core.MeditationType.NONE);
                if (prevType == tutien.core.MeditationType.THAM_NGO) {
                    player.sendMessage("§c§l[!] §fBị tấn công! Quá trình tham ngộ bị gián đoạn (tiến độ vẫn được bảo lưu an toàn)!");
                } else {
                    player.sendMessage("§c§l[!] §fBị tấn công! Quá trình tọa thiền tu luyện bị gián đoạn!");
                }
                player.removePotionEffect(PotionEffectType.LEVITATION);
            }
        }
    }

    // ==========================================
    // NHIỆM VỤ HÀNG NGÀY: KIẾN THIẾT ĐẢO TIÊN (ĐẶT KHỐI) & CHỐNG FARM TÔI THỂ
    // ==========================================
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Đánh dấu block do người chơi đặt để chống farm Tôi Thể
        event.getBlock().setMetadata("player_placed", new FixedMetadataValue(plugin, true));

        Player player = event.getPlayer();
        if (plugin.getNhiemVuManager() != null) {
            if (plugin.getTongMonManager() != null) {
                UUID owner = plugin.getTongMonManager().checkKhongChoPhepTuongTac(event.getBlock().getLocation(), player);
                if (owner == null) {
                    plugin.getNhiemVuManager().addProgress(player, NhiemVuManager.NhiemVu.XAY_DUNG, 1);
                }
            }
        }
    }

    // ==========================================
    // CHỐNG FARM TÔI THỂ TỪ SPAWNER
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            event.getEntity().setMetadata("from_spawner", new FixedMetadataValue(plugin, true));
        }
    }

    // ==========================================
    // CƠ CHẾ TỊCH CỐC & ỔN ĐỊNH ĐỘ ĐÓI THEO CẢNH GIỚI
    // ==========================================
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        CanhGioi cg = dataManager.getCanhGioi(player);
        int tichCoc = cg.getTichCocLevel();

        // Nguyên Anh trở lên (Level 4): Tịch Cốc Đạo Thể - Tuyệt đối không tiêu hao đói, luôn giữ no đủ
        if (tichCoc >= 4) {
            event.setCancelled(true);
            if (player.getFoodLevel() < 20) {
                player.setFoodLevel(20);
                player.setSaturation(20f);
            }
            return;
        }

        // Kim Đan (Level 3): Giảm 85% tiêu hao đói, thanh đói không bao giờ dưới 18
        if (tichCoc == 3) {
            if (player.getFoodLevel() <= 18 || Math.random() < 0.85) {
                event.setCancelled(true);
                if (player.getFoodLevel() < 18) player.setFoodLevel(20);
                return;
            }
        }

        // Trúc Cơ (Level 2): Giảm 60% tiêu hao đói
        if (tichCoc == 2) {
            if (Math.random() < 0.60) {
                event.setCancelled(true);
                return;
            }
        }

        // Luyện Khí (Level 1): Giảm 30% tiêu hao đói
        if (tichCoc == 1) {
            if (Math.random() < 0.30) {
                event.setCancelled(true);
            }
        }
    }

    // ==========================================
    // ĐỀ KHÁNG HIỆU ỨNG TIÊU CỰC THEO CẢNH GIỚI
    // ==========================================
    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) return;
        if (event.getNewEffect() == null) return;

        PotionEffectType type = event.getNewEffect().getType();
        boolean isNegative = type == PotionEffectType.POISON
                || type == PotionEffectType.WITHER
                || type == PotionEffectType.SLOWNESS
                || type == PotionEffectType.WEAKNESS
                || type == PotionEffectType.MINING_FATIGUE
                || type == PotionEffectType.BLINDNESS
                || type == PotionEffectType.DARKNESS;

        if (!isNegative) return;

        CanhGioi cg = dataManager.getCanhGioi(player);

        // Khuy Niết trở lên (Bước 2): Bách Độc Bất Xâm - Miễn nhiễm hoàn toàn debuff tiêu cực
        if (cg.ordinal() >= CanhGioi.KHUY_NIET.ordinal()) {
            event.setCancelled(true);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§a✦ [Chân Tiên Hộ Thể] Miễn nhiễm tà khí tiêu cực!"));
            return;
        }

        // Kim Đan trở lên: Có cơ hội hóa giải độc tố
        double cleanseChance = switch (cg) {
            case KIM_DAN -> 0.30;
            case NGUYEN_ANH -> 0.50;
            case HOA_THAN -> 0.65;
            case ANH_BIEN -> 0.75;
            case VAN_DINH, AM_HU, DUONG_THUC -> 0.85;
            default -> 0.0;
        };

        if (cleanseChance > 0 && Math.random() < cleanseChance) {
            event.setCancelled(true);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§b✦ [Linh Lực Thanh Tẩy] Hóa giải tà khí độc hại!"));
            player.getWorld().spawnParticle(Particle.WAX_OFF, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        }
    }

    // ==========================================
    // NHIỆM VỤ HÀNG NGÀY & TÔI THỂ CHIẾN ĐẤU
    // ==========================================
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            if (!(event.getEntity() instanceof Player)) {
                if (plugin.getNhiemVuManager() != null) {
                    plugin.getNhiemVuManager().addProgress(player, NhiemVuManager.NhiemVu.DANH_QUAI, 1);
                }

                // Phàm Nhân: Rèn luyện thể phách qua chiến đấu sinh tồn (Tôi Thể)
                if (dataManager.getCanhGioi(player) == CanhGioi.PHAM_NHAN) {
                    if (dataManager.getToiTheProgress(player) < 100.0 && !event.getEntity().hasMetadata("from_spawner")) {
                        dataManager.addToiTheProgress(player, 5.0);
                        double cur = dataManager.getToiTheProgress(player);
                        if (cur < 100.0) {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                    new TextComponent("§e⚔ [Tôi Thể] Huyết chiến sinh tồn! Tiến độ: §a" + String.format("%.1f", cur) + "% §7/ 100%"));
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // PHÀM NHÂN: TÔI THỂ LAO ĐỘNG (KHAI KHOÁNG, ĐỐN GỖ TỰ NHIÊN)
    // ==========================================
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreakMortal(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (dataManager.getCanhGioi(player) != CanhGioi.PHAM_NHAN) return;
        if (dataManager.getToiTheProgress(player) >= 100.0) return;

        // Chống farm: Không tính block do người chơi đặt
        if (event.getBlock().hasMetadata("player_placed")) return;

        Material mat = event.getBlock().getType();
        String name = mat.name();
        boolean isPhysicalLabor = name.contains("ORE") || name.contains("LOG") || name.contains("WOOD")
                || mat == Material.STONE || mat == Material.COBBLESTONE || mat == Material.DEEPSLATE;

        if (isPhysicalLabor && Math.random() < 0.25) {
            dataManager.addToiTheProgress(player, 2.5);
            double cur = dataManager.getToiTheProgress(player);
            if (cur < 100.0) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("§e⚒ [Tôi Thể] Khí huyết tăng cường! Tiến độ: §a" + String.format("%.1f", cur) + "% §7/ 100%"));
            }
        }
    }

    // ==========================================
    // PHÀM NHÂN: TẨY TỦY KHI UỐNG DƯỢC THANG / NƯỚC THANH LƯƠNG
    // ==========================================
    @EventHandler(ignoreCancelled = true)
    public void onMortalConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (dataManager.getCanhGioi(player) != CanhGioi.PHAM_NHAN) return;

        if (dataManager.hasKhaiMachStep(player, "TAY_TUY")) return;

        Material type = event.getItem().getType();
        if (type == Material.POTION || type == Material.MILK_BUCKET || type == Material.HONEY_BOTTLE) {
            dataManager.addKhaiMachStep(player, "TAY_TUY");
            player.sendMessage("§b§l✦ [Tẩy Tủy Hoàn Tất] §fDược dịch thanh lương gột rửa trần cấu! Đan điền rỗng không thanh tịnh, kinh mạch thông suốt!");
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

            player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0, 0.5, 0), 25, 0.3, 0.5, 0.3, 0.02);
            player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 0.5, 0), 30, 0.4, 0.5, 0.4, 0.1);
        }
    }
}
