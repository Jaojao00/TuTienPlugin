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
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp này xử lý lệnh /tiende độc lập.
 * Chuyên dùng để thi triển Thần Thông Cấm Thuật của Tiên Đế.
 * Đã fix toàn bộ lỗi tương thích API trên Minecraft 1.20.6+.
 */
public class TienDeCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;

    public TienDeCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dùng trong game!");
            return true;
        }

        Player player = (Player) sender;

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
            case "thiendiet":
                if (linhLuc < 500) { player.sendMessage("§cKhông đủ 500 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 500);
                castThienDiet(player);
                break;
            case "hophong":
                if (linhLuc < 300) { player.sendMessage("§cKhông đủ 300 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 300);
                castHoPhong(player);
                break;
            case "dialiet":
                if (linhLuc < 400) { player.sendMessage("§cKhông đủ 400 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 400);
                castDiaLiet(player);
                break;
            case "tatdau":
                if (linhLuc < 200) { player.sendMessage("§cKhông đủ 200 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 200);
                castTatDau(player);
                break;
            case "huyetkhi":
                if (linhLuc < 600) { player.sendMessage("§cKhông đủ 600 Linh Lực!"); return true; }
                dataManager.setLinhLuc(player, linhLuc - 600);
                castHuyetKhi(player);
                break;
            default:
                player.sendMessage("§cThần thông không tồn tại! Gõ /tiende để xem danh sách.");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender instanceof Player) {
            if (dataManager.getCanhGioi((Player) sender) == CanhGioi.TIEN_DE) {
                completions.add("thiendiet");
                completions.add("hophong");
                completions.add("dialiet");
                completions.add("tatdau");
                completions.add("huyetkhi");
            }
        }
        return completions;
    }

    // ==============================================================
    // KỸ NĂNG CỦA TIÊN ĐẾ
    // ==============================================================

    private void castThienDiet(Player player) {
        Bukkit.broadcastMessage("§c§l[Thiên Mệnh] §fTiên Đế §e" + player.getName() + " §fthi triển cấm thuật: §4§lCHÂN THIÊN DIỆT!");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);

        Location center = player.getLocation();

        // FIX: Đổi từ EXPLOSION_HUGE sang EXPLOSION_EMITTER (bản 1.20.6)
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 5, 2, 2, 2, 0.1);

        for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
            if (entity instanceof LivingEntity && entity != player) {
                player.getWorld().strikeLightningEffect(entity.getLocation());
                ((LivingEntity) entity).damage(2000, player); // Sát thương khủng khiếp
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
                if (time >= 50 || !player.isOnline()) { // Kéo dài 5 giây (10 * 5)
                    this.cancel();
                    return;
                }
                Location loc = player.getLocation().add(Math.random() * 20 - 10, 0, Math.random() * 20 - 10);
                player.getWorld().strikeLightning(player.getWorld().getHighestBlockAt(loc).getLocation());
                time += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void castDiaLiet(Player player) {
        Bukkit.broadcastMessage("§6§l[Không Gian] §fTiên Đế §e" + player.getName() + " §fthi triển: §6§lĐỊA LIỆT SƠN BĂNG!");
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);

        for (Entity entity : player.getNearbyEntities(15, 10, 15)) {
            if (entity instanceof LivingEntity && entity != player) {
                entity.setVelocity(new Vector(0, 1.5, 0)); // Hất tung lên không trung

                // FIX: Đổi từ BLOCK_CRACK sang BLOCK (bản 1.20.6)
                player.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation(), 50, 1, 1, 1, Bukkit.createBlockData(Material.DIRT));
                ((LivingEntity) entity).damage(1000, player);
            }
        }
    }

    private void castTatDau(Player player) {
        player.sendMessage("§e§l[Thần Thông] §fTÁT ĐẬU THÀNH BINH!");
        player.playSound(player.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);

        // Triệu hồi 3 Thiên Binh Khôi Lỗi
        for (int i = 0; i < 3; i++) {
            Location spawnLoc = player.getLocation().add(Math.random() * 4 - 2, 0, Math.random() * 4 - 2);
            IronGolem golem = player.getWorld().spawn(spawnLoc, IronGolem.class);
            golem.setCustomName("§eThiên Binh của " + player.getName());
            golem.setCustomNameVisible(true);
            golem.setPlayerCreated(true);
        }
    }

    private void castHuyetKhi(Player player) {
        player.sendMessage("§4§l[Quy Tắc Thân Thể] §fHUYẾT KHÍ VÔ TẬN!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);

        // Phục hồi máu tối đa
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        }

        // Áp đặt buff siêu cường (1 phút)
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 60, 4));

        // FIX: Đổi tên PotionEffectType theo bản 1.20.6
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 2));

        // Hiệu ứng hạt tim
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
    }
}