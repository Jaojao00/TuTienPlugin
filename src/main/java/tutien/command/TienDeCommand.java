package tutien.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.*;

/**
 * Lớp này xử lý lệnh /tiende độc lập.
 * Chuyên dùng để thi triển Thần Thông Cấm Thuật của Tiên Đế.
 * v2.2 Skyblock Edition:
 *   - Bảo vệ Dân Làng (Villager), Thú nuôi (Animals), Giá để giáp (ArmorStand) trên đảo Skyblock khỏi sát thương AOE.
 *   - Thay strikeLightning bằng strikeLightningEffect trong Hô Phong Hoán Vũ để không đốt cháy đảo gỗ.
 *   - Tát Đậu Thành Binh giới hạn tối đa 3 Thiên Binh đồng thời, tự giải tán sau 60s để chống farm Sắt (Iron Golem) vô hạn.
 */
public class TienDeCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final Map<UUID, List<IronGolem>> activeGolems = new HashMap<>();

    public TienDeCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Lệnh này chỉ dùng trong game!");
            return true;
        }

        if (dataManager.getCanhGioi(player) != CanhGioi.TIEN_DE) {
            player.sendMessage("§c§l[!] §fNgươi chưa đạt đến cảnh giới Tiên Đế, không thể thi triển cấm thuật!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e=== §6§lThần Thông Tiên Đế §e===");
            player.sendMessage("§f/tiende thiendiet §7- Chân Thiên Diệt (Tiêu hao 500 Linh Lực)");
            player.sendMessage("§f/tiende hophong §7- Hô Phong Hoán Vũ (Tiêu hao 300 Linh Lực)");
            player.sendMessage("§f/tiende dialiet §7- Địa Liệt Sơn Băng (Tiêu hao 400 Linh Lực)");
            player.sendMessage("§f/tiende tatdau §7- Tát Đậu Thành Binh (Tiêu hao 200 Linh Lực)");
            player.sendMessage("§f/tiende huyetkhi §7- Huyết Khí Vô Tận (Tiêu hao 600 Linh Lực)");
            return true;
        }

        String skill = args[0].toLowerCase();
        int linhLuc = dataManager.getLinhLuc(player);

        switch (skill) {
            case "thiendiet" -> {
                if (linhLuc < 500) { player.sendMessage("§cKhông đủ 500 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 500);
                castThienDiet(player);
            }
            case "hophong" -> {
                if (linhLuc < 300) { player.sendMessage("§cKhông đủ 300 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 300);
                castHoPhong(player);
            }
            case "dialiet" -> {
                if (linhLuc < 400) { player.sendMessage("§cKhông đủ 400 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 400);
                castDiaLiet(player);
            }
            case "tatdau" -> {
                if (linhLuc < 200) { player.sendMessage("§cKhông đủ 200 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 200);
                castTatDau(player);
            }
            case "huyetkhi" -> {
                if (linhLuc < 600) { player.sendMessage("§cKhông đủ 600 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 600);
                castHuyetKhi(player);
            }
            default -> player.sendMessage("§cThần thông không tồn tại! Gõ /tiende để xem danh sách.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender instanceof Player p) {
            if (dataManager.getCanhGioi(p) == CanhGioi.TIEN_DE) {
                completions.add("thiendiet");
                completions.add("hophong");
                completions.add("dialiet");
                completions.add("tatdau");
                completions.add("huyetkhi");
            }
        }
        return completions;
    }

    /**
     * Chỉ gây sát thương cho quái vật (Monster / Boss) hoặc người chơi đối địch,
     * bảo vệ tuyệt đối Dân Làng, Thú nuôi, Golem hộ vệ, ArmorStand trên đảo Skyblock.
     */
    private boolean isValidSkillTarget(Player caster, Entity entity) {
        if (!(entity instanceof LivingEntity) || entity == caster) return false;
        if (entity instanceof ArmorStand || entity instanceof Villager || entity instanceof WanderingTrader) return false;
        if (entity instanceof Animals || entity instanceof Allay || entity instanceof Golem) return false;
        if (entity instanceof Tameable tameable && tameable.isTamed()) return false;
        if (entity instanceof Player targetPlayer) {
            return targetPlayer.getWorld().getPVP();
        }
        return entity instanceof Monster || entity instanceof Slime || entity instanceof Phantom
                || entity instanceof Ghast || entity instanceof Shulker || entity instanceof Boss
                || entity instanceof Warden;
    }

    private void castThienDiet(Player player) {
        Bukkit.broadcastMessage("§c§l[Thiên Mệnh] §fTiên Đế §e" + player.getName() + " §fthi triển cấm thuật: §4§lCHÂN THIÊN DIỆT!");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);

        Location center = player.getLocation();
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 5, 2, 2, 2, 0.1);

        for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
            if (isValidSkillTarget(player, entity)) {
                player.getWorld().strikeLightningEffect(entity.getLocation());
                ((LivingEntity) entity).damage(2000, player);
            }
        }
    }

    private void castHoPhong(Player player) {
        Bukkit.broadcastMessage("§b§l[Nguyên Tố] §fTiên Đế §e" + player.getName() + " §fthi triển: §b§lHÔ PHONG HOÁN VŨ!");
        player.getWorld().setStorm(true);
        player.getWorld().setThundering(true);

        new BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 50 || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                Location loc = player.getLocation().add(Math.random() * 20 - 10, 0, Math.random() * 20 - 10);
                // Dùng strikeLightningEffect để không đốt cháy đảo Skyblock hoặc biến Dân Làng thành Phù Thủy
                player.getWorld().strikeLightningEffect(loc);
                for (Entity e : player.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
                    if (isValidSkillTarget(player, e)) {
                        ((LivingEntity) e).damage(400, player);
                    }
                }
                time += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void castDiaLiet(Player player) {
        Bukkit.broadcastMessage("§6§l[Không Gian] §fTiên Đế §e" + player.getName() + " §fthi triển: §6§lĐỊA LIỆT SƠN BĂNG!");
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);

        for (Entity entity : player.getNearbyEntities(15, 10, 15)) {
            if (isValidSkillTarget(player, entity)) {
                entity.setVelocity(new Vector(0, 1.5, 0));
                player.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation(), 50, 1, 1, 1, Bukkit.createBlockData(Material.DIRT));
                ((LivingEntity) entity).damage(1000, player);
            }
        }
    }

    private void castTatDau(Player player) {
        // Giải tán Thiên Binh cũ nếu có để tránh spam lag và farm sắt
        List<IronGolem> oldList = activeGolems.remove(player.getUniqueId());
        if (oldList != null) {
            for (IronGolem g : oldList) {
                if (g != null && !g.isDead()) g.remove();
            }
        }

        player.sendMessage("§e§l[Thần Thông] §fTÁT ĐẬU THÀNH BINH! §7(Tồn tại 60 giây)");
        player.playSound(player.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);

        List<IronGolem> newList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Location spawnLoc = player.getLocation();
            IronGolem golem = player.getWorld().spawn(spawnLoc, IronGolem.class);
            golem.setCustomName("§eThiên Binh của " + player.getName());
            golem.setCustomNameVisible(true);
            golem.setPlayerCreated(true);
            newList.add(golem);
        }
        activeGolems.put(player.getUniqueId(), newList);

        // Tự động thu hồi sau 60 giây (1200 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (IronGolem g : newList) {
                if (g != null && !g.isDead()) {
                    g.getWorld().spawnParticle(Particle.CLOUD, g.getLocation().add(0, 1, 0), 15, 0.4, 0.5, 0.4, 0.05);
                    g.remove();
                }
            }
        }, 20L * 60);
    }

    private void castHuyetKhi(Player player) {
        player.sendMessage("§4§l[Quy Tắc Thân Thể] §fHUYẾT KHÍ VÔ TẬN!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);

        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 60, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 2));

        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
    }
}
