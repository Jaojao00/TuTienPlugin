package tutien.dungeon;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tutien.core.CanhGioi;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.quest.NhiemVuManager;

import java.util.*;

/**
 * Quản lý Linh Thú Viên — 3 khu phụ bản chính:
 *   1. Huyền Ảnh Bí Cảnh: Đánh quái tăng Tu Vi + Đạo Niệm
 *   2. Thí Luyện Đạo Tràng: Khai thác máy tạo đá/quặng (Cobblestone Generator) nhận Khoáng Thạch + Tu Vi + Đạo Niệm
 *   3. Thiên Nguyên Cấm Địa: Boss Event phần thưởng đặc biệt
 * v2.2 Skyblock Edition:
 *   - Tìm mặt đất an toàn trước khi spawn quái/Boss trên đảo Skyblock (không rơi xuống Void).
 *   - Loại bỏ Elder Guardian (gây Mining Fatigue làm liệt máy đào đá Skyblock).
 *   - Tag quái Bí Cảnh bằng PDC để không xóa đồ rớt của Mob Grinder thông thường trên đảo.
 *   - Xử lý BlockBreakEvent cho máy đào đá/quặng Skyblock (cộng Khoáng Thạch, Tu Vi, tiến độ nhiệm vụ KHAI_MO).
 */
public class LinhThuVienManager implements Listener {

    public enum KhuVuc {
        HUYEN_ANH("§5Huyền Ảnh Bí Cảnh",
                "Đánh quái linh để tích lũy Tu Vi và Đạo Niệm.",
                CanhGioi.LUYEN_KHI),
        THI_LUYEN("§6Thí Luyện Đạo Tràng",
                "Khai thác tài nguyên: Khoáng Thạch, Linh Thạch, Đạo Niệm.",
                CanhGioi.TRUC_CO),
        THIEN_NGUYEN("§cThiên Nguyên Cấm Địa",
                "Boss Event — phần thưởng đặc biệt cực phẩm.",
                CanhGioi.NGUYEN_ANH);

        public final String tenHienThi;
        public final String moTa;
        public final CanhGioi yeuCau;

        KhuVuc(String ten, String moTa, CanhGioi yc) {
            this.tenHienThi = ten;
            this.moTa = moTa;
            this.yeuCau = yc;
        }
    }

    // Định nghĩa wave quái vật theo tier (Đã bỏ Elder Guardian để tránh Mining Fatigue trên đảo Skyblock)
    private static final EntityType[][] WAVES = {
        {EntityType.ZOMBIE, EntityType.SKELETON},                       // Tier 1 (Luyện Khí)
        {EntityType.HUSK, EntityType.STRAY, EntityType.SPIDER},         // Tier 2 (Trúc Cơ)
        {EntityType.WITHER_SKELETON, EntityType.PIGLIN_BRUTE},          // Tier 3 (Kim Đan)
        {EntityType.VINDICATOR, EntityType.RAVAGER},                    // Tier 4 (Nguyên Anh)
        {EntityType.EVOKER, EntityType.WITHER_SKELETON}                 // Tier 5 (Hóa Thần+)
    };

    private static final int[] TU_VI_REWARD = {50, 150, 400, 1000, 3000};
    private static final NamespacedKey KEY_DUNGEON_MOB = new NamespacedKey("tutien", "dungeon_mob");
    private static final NamespacedKey KEY_DUNGEON_OWNER = new NamespacedKey("tutien", "dungeon_owner");

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final Random random = new Random();

    private final Map<UUID, Integer> killCount = new HashMap<>();
    private final Map<UUID, KhuVuc> currentZone = new HashMap<>();
    private final Map<UUID, LivingEntity> activeBoss = new HashMap<>();

    public LinhThuVienManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    public void enterHuyenAnh(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        if (cg.ordinal() < KhuVuc.HUYEN_ANH.yeuCau.ordinal()) {
            player.sendMessage("§c[Bí Cảnh] §fCần đạt §e" + KhuVuc.HUYEN_ANH.yeuCau.getTenHienThi() + "§f để vào!");
            return;
        }

        UUID uuid = player.getUniqueId();
        currentZone.put(uuid, KhuVuc.HUYEN_ANH);
        killCount.put(uuid, 0);

        player.sendMessage("§5§l[Huyền Ảnh Bí Cảnh] §fĐã kích hoạt Huyền Ảnh Bí Cảnh ngay trên đảo của bạn!");
        player.sendMessage("§7Tiêu diệt quái linh xuất hiện xung quanh để nhận Tu Vi và Đạo Niệm.");
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.5f, 0.8f);

        spawnWave(player);
    }

    public void enterThiLuyen(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        if (cg.ordinal() < KhuVuc.THI_LUYEN.yeuCau.ordinal()) {
            player.sendMessage("§c[Đạo Tràng] §fCần đạt §e" + KhuVuc.THI_LUYEN.yeuCau.getTenHienThi() + "§f để vào!");
            return;
        }

        currentZone.put(player.getUniqueId(), KhuVuc.THI_LUYEN);
        player.sendMessage("§6§l[Thí Luyện Đạo Tràng] §fĐã bật chế độ §eLinh Khoáng Thí Luyện§f!");
        player.sendMessage("§7Hãy đào Đá Cuội / Đá / Quặng tại máy tạo khoáng (Generator) trên đảo để nhận §eKhoáng Thạch§7, §eTu Vi§7 và §bĐạo Niệm§7 với tỷ lệ x3!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f);
    }

    public void enterThienNguyen(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        if (cg.ordinal() < KhuVuc.THIEN_NGUYEN.yeuCau.ordinal()) {
            player.sendMessage("§c[Cấm Địa] §fCần đạt §e" + KhuVuc.THIEN_NGUYEN.yeuCau.getTenHienThi() + "§f để vào!");
            return;
        }

        UUID uuid = player.getUniqueId();
        LivingEntity existing = activeBoss.get(uuid);
        if (existing != null && !existing.isDead() && existing.isValid()) {
            player.sendMessage("§c[Cấm Địa] §fBoss của bạn vẫn còn sống! Hãy tiêu diệt nó trước!");
            return;
        }

        currentZone.put(uuid, KhuVuc.THIEN_NGUYEN);
        Bukkit.broadcastMessage("§4§l[Thiên Địa Dị Tượng] §c" + player.getName() + " §fđã triệu hồi Cổ Thần Boss tại Cấm Địa!");

        Location loc = findSafeSpawnLocation(player.getLocation(), 4);
        Warden warden = (Warden) player.getWorld().spawnEntity(loc, EntityType.WARDEN);
        warden.setCustomName("§4§l⚔ Cổ Thần Boss ⚔ §7[" + player.getName() + "]");
        warden.setCustomNameVisible(true);
        warden.getPersistentDataContainer().set(KEY_DUNGEON_MOB, PersistentDataType.STRING, "BOSS");
        warden.getPersistentDataContainer().set(KEY_DUNGEON_OWNER, PersistentDataType.STRING, uuid.toString());

        double bossHP = 200.0 * (cg.ordinal() + 1);
        if (warden.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            warden.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossHP);
            warden.setHealth(bossHP);
        }

        activeBoss.put(uuid, warden);
        player.sendMessage("§c§l[Boss] §fCổ Thần đã xuất hiện! §c" + String.format("%.0f", bossHP) + " HP");
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 1f, 1f);
    }

    /**
     * Xử lý đào máy tạo đá/quặng trên đảo Skyblock (BlockBreakEvent).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Block block = event.getBlock();
        Material type = block.getType();
        if (!isMineableGeneratorBlock(type)) return;

        // Cập nhật nhiệm vụ đào mỏ hàng ngày
        if (plugin.getNhiemVuManager() != null) {
            plugin.getNhiemVuManager().addProgress(player, NhiemVuManager.NhiemVu.KHAI_MO, 1);
        }

        boolean inThiLuyen = currentZone.get(player.getUniqueId()) == KhuVuc.THI_LUYEN;
        boolean isRichOre = type.name().contains("ORE") || type == Material.ANCIENT_DEBRIS
                || type == Material.OBSIDIAN || type == Material.AMETHYST_CLUSTER;

        // Tỷ lệ nhận Khoáng Thạch & Tu Vi khi đào mỏ trên Skyblock
        double chance = isRichOre ? 0.35 : 0.08;
        if (inThiLuyen) chance *= 2.5; // Buff mạnh khi bật Thí Luyện Đạo Tràng

        if (random.nextDouble() < chance) {
            long ktGain = isRichOre ? (inThiLuyen ? 2 : 1) : 1;
            int tuViGain = isRichOre ? (inThiLuyen ? 15 : 5) : (inThiLuyen ? 5 : 2);

            dataManager.addKhoangThach(player, ktGain);
            dataManager.addTuVi(player, tuViGain);

            String extraMsg = "";
            // Cơ hội tìm thấy Đạo Niệm khi đang ở chế độ Thí Luyện Đạo Tràng
            if (inThiLuyen && random.nextDouble() < 0.12) {
                dataManager.addDaoNiem(player, 1);
                extraMsg = " §7| §b+1 Đạo Niệm";
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§6⛏ [Linh Khoáng] §e+" + ktGain + " Khoáng Thạch §7| §a+" + tuViGain + " Tu Vi" + extraMsg));
        }
    }

    private boolean isMineableGeneratorBlock(Material mat) {
        String name = mat.name();
        return mat == Material.COBBLESTONE
                || mat == Material.STONE
                || mat == Material.DEEPSLATE
                || mat == Material.COBBLED_DEEPSLATE
                || mat == Material.BASALT
                || mat == Material.BLACKSTONE
                || mat == Material.NETHERRACK
                || mat == Material.END_STONE
                || mat == Material.OBSIDIAN
                || mat == Material.AMETHYST_CLUSTER
                || mat == Material.ANCIENT_DEBRIS
                || name.endsWith("_ORE");
    }

    /**
     * Xử lý khi quái Bí Cảnh hoặc Boss chết (Chỉ can thiệp vào quái có PDC KEY_DUNGEON_MOB để bảo vệ Mob Grinder của đảo)
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String mobType = entity.getPersistentDataContainer().get(KEY_DUNGEON_MOB, PersistentDataType.STRING);
        if (mobType == null) return; // Quái thường của đảo -> Không can thiệp!

        Player killer = entity.getKiller();
        if (killer == null) {
            String ownerStr = entity.getPersistentDataContainer().get(KEY_DUNGEON_OWNER, PersistentDataType.STRING);
            if (ownerStr != null) {
                try { killer = Bukkit.getPlayer(UUID.fromString(ownerStr)); } catch (Exception ignored) {}
            }
        }
        if (killer == null) return;

        UUID uuid = killer.getUniqueId();

        if ("WAVE".equals(mobType)) {
            CanhGioi cg = dataManager.getCanhGioi(killer);
            int tier = Math.min(cg.ordinal() / 3, TU_VI_REWARD.length - 1);
            int reward = TU_VI_REWARD[tier];

            dataManager.addTuVi(killer, reward);
            event.getDrops().clear(); // Xóa drop rác của quái Bí Cảnh

            int kills = killCount.merge(uuid, 1, Integer::sum);
            if (kills % 10 == 0) {
                long daoNiemGain = Math.max(2, (tier + 1) * 2L);
                dataManager.addDaoNiem(killer, daoNiemGain);
                killer.sendMessage("§5§l[Bí Cảnh] §fTiêu diệt §e" + kills + " §fquái linh! Nhận §b+" + daoNiemGain + " Đạo Niệm§f! (Tổng: §b" + dataManager.getDaoNiem(killer) + "§f)");
                killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            }

            killer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("§5⚔ [Bí Cảnh] §e+" + reward + " Tu Vi §7(" + kills + " hạ gục)"));

            // Spawn wave tiếp theo mỗi 5 kill (tối đa 50 quái mỗi lượt mở để tránh treo AFK бесконечный)
            if (kills % 5 == 0 && kills < 50 && currentZone.get(uuid) == KhuVuc.HUYEN_ANH) {
                Player finalKiller = killer;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (finalKiller.isOnline()) spawnWave(finalKiller);
                }, 30L);
            } else if (kills >= 50) {
                currentZone.remove(uuid);
                killer.sendMessage("§5§l[Huyền Ảnh Bí Cảnh] §aĐã hoàn thành lượt khiêu chiến 50 quái linh!");
            }

        } else if ("BOSS".equals(mobType)) {
            activeBoss.remove(uuid);
            currentZone.remove(uuid);
            event.getDrops().clear();
            grantBossReward(killer);
        }
    }

    private void spawnWave(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        int tier = Math.min(cg.ordinal() / 3, WAVES.length - 1);
        EntityType[] waveTypes = WAVES[tier];

        Location base = player.getLocation();

        for (int i = 0; i < 5; i++) {
            EntityType type = waveTypes[random.nextInt(waveTypes.length)];
            Location spawnLoc = findSafeSpawnLocation(base, 4);

            try {
                LivingEntity mob = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, type);
                mob.setCustomName("§c[Bí Cảnh] " + formatEntityName(type));
                mob.setCustomNameVisible(true);
                mob.getPersistentDataContainer().set(KEY_DUNGEON_MOB, PersistentDataType.STRING, "WAVE");
                mob.getPersistentDataContainer().set(KEY_DUNGEON_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());

                if (mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    double hp = 20.0 * (cg.ordinal() + 1);
                    mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
                    mob.setHealth(hp);
                }
            } catch (Exception ignored) {}
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.2f);
    }

    /**
     * Tìm vị trí mặt đất an toàn gần người chơi trên đảo Skyblock (tránh spawn ngoài rìa Void).
     */
    private Location findSafeSpawnLocation(Location origin, int radius) {
        World world = origin.getWorld();
        if (world == null) return origin;

        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            for (int dy = 2; dy >= -3; dy--) {
                Block ground = world.getBlockAt(origin.getBlockX() + dx, origin.getBlockY() + dy - 1, origin.getBlockZ() + dz);
                Block feet   = world.getBlockAt(origin.getBlockX() + dx, origin.getBlockY() + dy, origin.getBlockZ() + dz);
                Block head   = world.getBlockAt(origin.getBlockX() + dx, origin.getBlockY() + dy + 1, origin.getBlockZ() + dz);
                if (ground.getType().isSolid() && !feet.getType().isSolid() && !head.getType().isSolid()
                        && feet.getType() != Material.LAVA && ground.getType() != Material.LAVA) {
                    return new Location(world, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                }
            }
        }
        return origin.clone();
    }

    private void grantBossReward(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);

        int tuViReward = Math.max(500, cg.getTuViYeuCau() / 5);
        long daoNiemReward = 20L;
        long ktReward = 10L;

        dataManager.addTuVi(player, tuViReward);
        dataManager.addDaoNiem(player, daoNiemReward);
        dataManager.addKhoangThach(player, ktReward);

        ItemKeys.giveItemSafe(player, createBossLoot(cg));

        player.sendMessage("§6§l[Boss Tiêu Diệt!] §fPhần thưởng chiến thắng:");
        player.sendMessage("§7  ▪ Tu Vi: §e+" + tuViReward);
        player.sendMessage("§7  ▪ Đạo Niệm: §b+" + daoNiemReward + " §7(Đã cộng vào tài khoản)");
        player.sendMessage("§7  ▪ Khoáng Thạch: §6+" + ktReward);
        player.sendMessage("§7  ▪ Vật phẩm đặc biệt: §d✦ Boss Loot");

        Bukkit.broadcastMessage("§6§l[Cổ Thần] §a" + player.getName() + " §fđã tiêu diệt Boss Cổ Thần và giành được phần thưởng huyền thoại!");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    private ItemStack createBossLoot(CanhGioi cg) {
        Material[] mats = {Material.NETHER_STAR, Material.AMETHYST_SHARD, Material.HEART_OF_THE_SEA, Material.NETHERITE_SCRAP};
        Material mat = mats[Math.min(cg.ordinal() / 4, mats.length - 1)];
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§l✦ Tinh Hoa Cổ Thần - " + cg.getTenHienThi());
            meta.setLore(Arrays.asList(
                "§7Chiến lợi phẩm quý hiếm từ Boss Cổ Thần.",
                "§7Chứa đựng tinh hoa cảnh giới §e" + cg.getTenHienThi(),
                "§7Có thể cất vào §bTúi Đồ Hư Không §7để đổi Linh Thạch.",
                "§8[Cấm Địa Độc Quyền]"
            ));
            meta.getPersistentDataContainer().set(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING, ItemKeys.TYPE_BOSS_LOOT);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatEntityName(EntityType type) {
        return type.name().replace("_", " ");
    }

    public void cleanupPlayer(UUID uuid) {
        currentZone.remove(uuid);
        killCount.remove(uuid);
        LivingEntity boss = activeBoss.remove(uuid);
        if (boss != null && !boss.isDead()) boss.remove();
    }
}
