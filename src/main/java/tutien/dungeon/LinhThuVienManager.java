package tutien.dungeon;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.*;

/**
 * Quản lý Linh Thú Viên — 3 khu phụ bản chính:
 *   1. Huyền Ảnh Bí Cảnh: Đánh quái tăng Tu Vi + Đạo Niệm
 *   2. Thí Luyện Đạo Tràng: Farm Khoáng Thạch + Linh Thạch
 *   3. Thiên Nguyên Cấm Địa: Boss Event phần thưởng đặc biệt
 * SPRINT 4: Nội dung phụ bản hoàn chỉnh.
 */
public class LinhThuVienManager implements Listener {

    public enum KhuVuc {
        HUYEN_ANH("§5Huyền Ảnh Bí Cảnh",
                "Đánh quái linh để tích lũy Tu Vi và Đạo Niệm.",
                CanhGioi.LUYEN_KHI),
        THI_LUYEN("§6Thí Luyện Đạo Tràng",
                "Khai thác tài nguyên: Khoáng Thạch, Linh Thạch.",
                CanhGioi.TRUC_CO),
        THIEN_NGUYEN("§cThiên Nguyên Cấm Địa",
                "Boss Event — phần thưởng đặc biệt cực phẩm.",
                CanhGioi.NGUYEN_ANH);

        public final String tenHienThi;
        public final String moTa;
        public final CanhGioi yeuCau;

        KhuVuc(String ten, String moTa, CanhGioi yc) {
            this.tenHienThi = ten; this.moTa = moTa; this.yeuCau = yc;
        }
    }

    // Định nghĩa wave quái vật theo tier
    private static final EntityType[][] WAVES = {
        {EntityType.ZOMBIE, EntityType.SKELETON},                       // Tier 1 (Luyện Khí)
        {EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED},        // Tier 2 (Trúc Cơ)
        {EntityType.WITHER_SKELETON, EntityType.PIGLIN_BRUTE},          // Tier 3 (Kim Đan)
        {EntityType.ELDER_GUARDIAN, EntityType.RAVAGER},                // Tier 4 (Nguyên Anh)
        {EntityType.WARDEN}                                              // Tier 5 (Hóa Thần+)
    };

    // Phần thưởng tu vi khi giết quái
    private static final int[] TU_VI_REWARD = {50, 150, 400, 1000, 3000};

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;

    // Track số quái đã giết trong session
    private final Map<UUID, Integer> killCount = new HashMap<>();
    // Track khu vực hiện tại của người chơi
    private final Map<UUID, KhuVuc> currentZone = new HashMap<>();
    // Boss đang active
    private final Map<UUID, LivingEntity> activeBoss = new HashMap<>();

    public LinhThuVienManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    /**
     * Người chơi vào khu Huyền Ảnh Bí Cảnh
     */
    public void enterHuyenAnh(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        if (cg.ordinal() < KhuVuc.HUYEN_ANH.yeuCau.ordinal()) {
            player.sendMessage("§c[Bí Cảnh] §fCần đạt §e" + KhuVuc.HUYEN_ANH.yeuCau.getTenHienThi() + "§f để vào!");
            return;
        }

        UUID uuid = player.getUniqueId();
        currentZone.put(uuid, KhuVuc.HUYEN_ANH);
        killCount.put(uuid, 0);

        player.sendMessage("§5§l[Huyền Ảnh Bí Cảnh] §fĐã bước vào vùng bí cảnh!");
        player.sendMessage("§7Tiêu diệt quái vật để nhận Tu Vi và Đạo Niệm.");
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.5f, 0.8f);

        // Spawn wave quái đầu tiên
        spawnWave(player);
    }

    /**
     * Người chơi vào khu Thí Luyện Đạo Tràng
     */
    public void enterThiLuyen(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        if (cg.ordinal() < KhuVuc.THI_LUYEN.yeuCau.ordinal()) {
            player.sendMessage("§c[Đạo Tràng] §fCần đạt §e" + KhuVuc.THI_LUYEN.yeuCau.getTenHienThi() + "§f để vào!");
            return;
        }

        currentZone.put(player.getUniqueId(), KhuVuc.THI_LUYEN);
        player.sendMessage("§6§l[Thí Luyện Đạo Tràng] §fĐã vào khu khai thác tài nguyên!");
        player.sendMessage("§7Phá block để nhận Khoáng Thạch và Linh Thạch.");

        // Cho phép tự do khai thác, drop xử lý trong BlockBreakEvent (nếu cần)
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1f);
    }

    /**
     * Người chơi vào Thiên Nguyên Cấm Địa — Spawn Boss
     */
    public void enterThienNguyen(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        if (cg.ordinal() < KhuVuc.THIEN_NGUYEN.yeuCau.ordinal()) {
            player.sendMessage("§c[Cấm Địa] §fCần đạt §e" + KhuVuc.THIEN_NGUYEN.yeuCau.getTenHienThi() + "§f để vào!");
            return;
        }

        UUID uuid = player.getUniqueId();
        if (activeBoss.containsKey(uuid)) {
            player.sendMessage("§c[Cấm Địa] §fBoss của bạn vẫn còn sống! Hãy tiêu diệt nó trước!");
            return;
        }

        currentZone.put(uuid, KhuVuc.THIEN_NGUYEN);
        Bukkit.broadcastMessage("§4§l[Thiên Địa Dị Tượng] §c" + player.getName() + " §fđã triệu hồi Boss tại Cấm Địa!");

        // Spawn Warden Boss với HP khổng lồ theo cảnh giới
        Location loc = player.getLocation().add(5, 0, 0);
        Warden warden = (Warden) player.getWorld().spawnEntity(loc, EntityType.WARDEN);
        warden.setCustomName("§4§l⚔ Cổ Thần Boss ⚔ §7[" + player.getName() + "]");
        warden.setCustomNameVisible(true);

        // Scale HP theo cảnh giới
        double bossHP = 200.0 * (cg.ordinal() + 1);
        if (warden.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
            warden.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossHP);
            warden.setHealth(bossHP);
        }

        activeBoss.put(uuid, warden);
        player.sendMessage("§c§l[Boss] §fCổ Thần đã xuất hiện! §c" + String.format("%.0f", bossHP) + " HP");
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 1f, 1f);
    }

    /**
     * Xử lý khi quái chết (lắng nghe EntityDeathEvent)
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        UUID uuid = killer.getUniqueId();
        KhuVuc zone = currentZone.get(uuid);
        if (zone == null) return;

        if (zone == KhuVuc.HUYEN_ANH) {
            // Tăng Tu Vi theo tier cảnh giới
            CanhGioi cg = dataManager.getCanhGioi(killer);
            int tier = Math.min(cg.ordinal() / 3, TU_VI_REWARD.length - 1);
            int reward = TU_VI_REWARD[tier];

            dataManager.addTuVi(killer, reward);
            killer.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent("§5+§e" + reward + " §5Tu Vi §7(Bí Cảnh)"));

            // Tăng kill count
            int kills = killCount.merge(uuid, 1, Integer::sum);
            // Mỗi 10 quái drop Đạo Niệm (Emerald)
            if (kills % 10 == 0) {
                ItemStack daoniem = createDaoNiem(kills / 10);
                event.getDrops().clear();
                event.getDrops().add(daoniem);
                killer.sendMessage("§5[Bí Cảnh] §fThu được §b" + (kills / 10) + "x §5Đạo Niệm§f!");
            } else {
                event.getDrops().clear(); // Clear drop thường
            }

            // Spawn wave tiếp theo mỗi 5 kill
            if (kills % 5 == 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> spawnWave(killer), 20L);
            }

        } else if (zone == KhuVuc.THIEN_NGUYEN) {
            // Kiểm tra có phải boss không
            LivingEntity boss = activeBoss.get(uuid);
            if (boss != null && boss.getEntityId() == entity.getEntityId()) {
                activeBoss.remove(uuid);
                currentZone.remove(uuid);
                grantBossReward(killer);
            }
        }
    }

    private void spawnWave(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        int tier = Math.min(cg.ordinal() / 3, WAVES.length - 1);
        EntityType[] waveTypes = WAVES[tier];

        Location base = player.getLocation();
        Random rand = new Random();

        for (int i = 0; i < 5; i++) {
            EntityType type = waveTypes[rand.nextInt(waveTypes.length)];
            double angle = (i / 5.0) * 2 * Math.PI;
            Location spawnLoc = base.clone().add(Math.cos(angle) * 6, 0, Math.sin(angle) * 6);

            try {
                LivingEntity mob = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, type);
                mob.setCustomName("§c[Bí Cảnh] " + formatEntityName(type));
                mob.setCustomNameVisible(true);
                // Scale HP theo cảnh giới
                if (mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
                    double hp = 20.0 * (cg.ordinal() + 1);
                    mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
                    mob.setHealth(hp);
                }
            } catch (Exception ignored) {}
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.2f);
    }

    private void grantBossReward(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);

        // Tu Vi thưởng lớn
        int tuViReward = cg.getTuViYeuCau() / 5;
        dataManager.addTuVi(player, tuViReward);

        // Drop Đạo Niệm x5
        player.getInventory().addItem(createDaoNiem(5));
        // Drop Đan Dược ngẫu nhiên
        player.getInventory().addItem(createBossLoot(cg));

        player.sendMessage("§6§l[Boss Tiêu Diệt!] §fPhần thưởng:");
        player.sendMessage("§7  ▪ Tu Vi: §e+" + tuViReward);
        player.sendMessage("§7  ▪ Đạo Niệm: §b×5");
        player.sendMessage("§7  ▪ Vật phẩm đặc biệt: §d✦ Boss Loot");

        Bukkit.broadcastMessage("§6§l[Cổ Thần] §a" + player.getName() + " §fđã tiêu diệt Boss và giành được phần thưởng huyền thoại!");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    private ItemStack createDaoNiem(int amount) {
        ItemStack item = new ItemStack(Material.EMERALD, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bĐạo Niệm");
        meta.setLore(Arrays.asList("§7Tinh hoa tu luyện từ chiến trận.", "§7Dùng để nâng cấp Kiếm.", "§8[Linh Thú Viên]"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBossLoot(CanhGioi cg) {
        // Tạo vật phẩm boss loot theo cảnh giới
        Material[] mats = {Material.NETHER_STAR, Material.BEACON, Material.END_CRYSTAL, Material.DRAGON_EGG};
        Material mat = mats[Math.min(cg.ordinal() / 4, mats.length - 1)];
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l✦ Boss Loot - " + cg.getTenHienThi());
        meta.setLore(Arrays.asList("§7Vật phẩm đặc biệt từ Boss Cổ Thần.", "§7Chứa đựng tinh hoa cảnh giới §e" + cg.getTenHienThi(), "§8[Cấm Địa Độc Quyền]"));
        item.setItemMeta(meta);
        return item;
    }

    private String formatEntityName(EntityType type) {
        return type.name().replace("_", " ");
    }

    public void cleanupPlayer(UUID uuid) {
        currentZone.remove(uuid);
        killCount.remove(uuid);
        // Kill boss nếu còn
        LivingEntity boss = activeBoss.remove(uuid);
        if (boss != null && !boss.isDead()) boss.remove();
    }
}
