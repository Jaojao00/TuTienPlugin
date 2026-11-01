package tutien.dungeon.boss;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Boss Thế Giới & Drop Độc Quyền (Phase 5).
 * - Triệu hồi thực thể LivingEntity thực tế trong Bukkit kèm BossBar và thẻ PDC "tutien:world_boss_id"
 * - Theo dõi sát thương chiến đấu của từng tu sĩ
 * - Xử lý EntityDeathEvent và phân chia phần thưởng theo Top 10
 */
public class WorldBossManager {

    public static final NamespacedKey BOSS_TAG = new NamespacedKey("tutien", "world_boss_id");
    public static final NamespacedKey BOSS_SPAWN_ID_TAG = new NamespacedKey("tutien", "world_boss_spawn_id");
    public static final NamespacedKey REWARD_TUVI_KEY = new NamespacedKey("tutien", "reward_tuvi");
    public static final NamespacedKey REWARD_DAONIEM_KEY = new NamespacedKey("tutien", "reward_daoniem");

    public static class ActiveBoss {
        public WorldBossType type;
        public double currentHealth;
        public final Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        public long spawnTime;
        public boolean alive = true;
        public boolean defeated = false;
        public boolean rewardDistributed = false;
        public LivingEntity entity;
        public BossBar bossBar;
        private final UUID spawnId = UUID.randomUUID();

        public ActiveBoss(WorldBossType type) {
            this.type = type;
            this.currentHealth = type.getMaxHealth();
            this.spawnTime = System.currentTimeMillis();
        }

        public UUID getSpawnId() {
            return spawnId;
        }

        public synchronized void dealDamage(UUID pId, double dmg) {
            if (rewardDistributed) return;
            damageDealt.put(pId, damageDealt.getOrDefault(pId, 0.0) + dmg);
            currentHealth -= dmg;
            if (bossBar != null) {
                double progress = Math.max(0.0, Math.min(1.0, currentHealth / type.getMaxHealth()));
                bossBar.setProgress(progress);
            }
            if (currentHealth <= 0) {
                currentHealth = 0;
                defeated = true;
                if (entity == null) {
                    alive = false; // Trong môi trường headless test không có entity Bukkit
                }
            }
        }

        public synchronized void syncHealth(double actualHealth) {
            this.currentHealth = Math.max(0.0, actualHealth);
            if (bossBar != null) {
                double progress = Math.max(0.0, Math.min(1.0, currentHealth / type.getMaxHealth()));
                bossBar.setProgress(progress);
            }
            if (this.currentHealth <= 0) {
                this.defeated = true;
            }
        }

        public List<UUID> getTopDamagers() {
            List<UUID> list = new ArrayList<>(damageDealt.keySet());
            list.sort((a, b) -> Double.compare(damageDealt.get(b), damageDealt.get(a)));
            return list;
        }
    }

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private ActiveBoss currentBoss;
    private List<UUID> lastDefeatedDamagers = Collections.emptyList();

    public WorldBossManager(TuTienPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public synchronized void cancelCurrentBoss() {
        if (currentBoss != null) {
            currentBoss.alive = false;
            if (currentBoss.bossBar != null) {
                try {
                    currentBoss.bossBar.removeAll();
                } catch (Throwable ignored) {}
            }
            if (currentBoss.entity != null && !currentBoss.entity.isDead()) {
                try {
                    currentBoss.entity.remove();
                } catch (Throwable ignored) {}
            }
            currentBoss = null;
        }
    }

    public synchronized ActiveBoss spawnBoss(WorldBossType type) {
        return spawnBoss(type, null);
    }

    public synchronized ActiveBoss spawnBoss(WorldBossType type, Location loc) {
        if (currentBoss != null && currentBoss.alive) {
            cancelCurrentBoss(); // Hủy boss cũ an toàn, không phân phát phần thưởng sai lệch
        }

        currentBoss = new ActiveBoss(type);
        lastDefeatedDamagers = Collections.emptyList();

        if (loc != null && loc.getWorld() != null && Bukkit.getServer() != null) {
            try {
                EntityType entityType = switch (type) {
                    case THUONG_CO_CU_MA -> EntityType.WARDEN;
                    case CUU_U_MINH_XA -> EntityType.WITHER;
                    case THAI_CO_THAN_LONG -> EntityType.ENDER_DRAGON;
                };

                LivingEntity le = (LivingEntity) loc.getWorld().spawnEntity(loc, entityType);
                le.setCustomName(type.getDisplayName());
                le.setCustomNameVisible(true);
                le.getPersistentDataContainer().set(BOSS_TAG, PersistentDataType.STRING, type.name());
                le.getPersistentDataContainer().set(BOSS_SPAWN_ID_TAG, PersistentDataType.STRING, currentBoss.getSpawnId().toString());
                try {
                    org.bukkit.attribute.AttributeInstance maxHealthAttr = le.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        maxHealthAttr.setBaseValue(type.getMaxHealth());
                    }
                    le.setHealth(type.getMaxHealth());
                } catch (Throwable ignored) {}
                currentBoss.entity = le;

                BossBar bar = Bukkit.createBossBar(type.getDisplayName(), BarColor.PURPLE, BarStyle.SEGMENTED_10);
                bar.setProgress(1.0);
                for (Player p : loc.getWorld().getPlayers()) {
                    bar.addPlayer(p);
                }
                currentBoss.bossBar = bar;
            } catch (Throwable ignored) {
                // Hỗ trợ headless tests
            }
        }

        return currentBoss;
    }

    public ActiveBoss getCurrentBoss() {
        return currentBoss;
    }

    public boolean isBossAlive() {
        return currentBoss != null && currentBoss.alive && !currentBoss.defeated;
    }

    public boolean isWorldBoss(org.bukkit.entity.Entity entity) {
        if (entity == null || currentBoss == null) return false;
        // 1. Kiểm tra Entity instance hoặc Entity UniqueId của phiên Boss hiện tại
        if (currentBoss.entity != null) {
            if (currentBoss.entity.equals(entity) || currentBoss.entity.getUniqueId().equals(entity.getUniqueId())) {
                return true;
            }
        }
        // 2. Bắt buộc kiểm tra PersistentDataContainer spawnId tag phải khớp chính xác spawnId phiên hiện tại
        if (entity.getPersistentDataContainer() != null) {
            if (entity.getPersistentDataContainer().has(BOSS_SPAWN_ID_TAG, PersistentDataType.STRING)) {
                String spawnIdStr = entity.getPersistentDataContainer().get(BOSS_SPAWN_ID_TAG, PersistentDataType.STRING);
                return currentBoss.getSpawnId().toString().equals(spawnIdStr);
            }
        }
        return false;
    }

    public void recordBossDamage(UUID playerId, double dmg) {
        recordBossDamage(playerId, dmg, null);
    }

    public void recordBossDamage(UUID playerId, double dmg, org.bukkit.entity.Entity entity) {
        if (currentBoss != null && !currentBoss.rewardDistributed) {
            if (entity instanceof LivingEntity le && !le.isDead()) {
                double remaining = Math.max(0.0, le.getHealth() - dmg);
                currentBoss.syncHealth(remaining);
            }
            currentBoss.dealDamage(playerId, dmg);
        }
    }

    public void handleEntityDeath(LivingEntity entity) {
        if (entity == null || currentBoss == null || currentBoss.rewardDistributed) return;
        boolean matches = false;
        if (currentBoss.entity != null) {
            if (currentBoss.entity.equals(entity) || currentBoss.entity.getUniqueId().equals(entity.getUniqueId())) {
                matches = true;
            }
        }
        if (!matches && entity.getPersistentDataContainer() != null) {
            if (entity.getPersistentDataContainer().has(BOSS_SPAWN_ID_TAG, PersistentDataType.STRING)) {
                String spawnIdStr = entity.getPersistentDataContainer().get(BOSS_SPAWN_ID_TAG, PersistentDataType.STRING);
                matches = currentBoss.getSpawnId().toString().equals(spawnIdStr);
            }
        }
        if (matches) {
            currentBoss.alive = false;
            currentBoss.defeated = true;
            onBossDefeated();
        }
    }

    /**
     * Trao thưởng khi Boss Thế Giới bị tiêu diệt
     */
    public synchronized List<UUID> onBossDefeated() {
        if (currentBoss == null || currentBoss.rewardDistributed) return lastDefeatedDamagers;
        ActiveBoss boss = currentBoss;
        boss.alive = false;
        boss.defeated = true;
        boss.rewardDistributed = true; // Chống trao thưởng lặp (Idempotent)

        if (boss.bossBar != null) {
            try {
                boss.bossBar.removeAll();
            } catch (Throwable ignored) {}
        }

        List<UUID> topDamagers = boss.getTopDamagers();
        lastDefeatedDamagers = topDamagers;

        // Trao phần thưởng cho Top 1-10
        int rank = 1;
        for (UUID pId : topDamagers) {
            if (rank > 10) break;
            int tuViReward = Math.max(500, (int) (10000 / rank));
            int daoNiemReward = Math.max(200, (int) (5000 / rank));
            if (dataManager != null && org.bukkit.Bukkit.getServer() != null) {
                Player onlinePlayer = org.bukkit.Bukkit.getPlayer(pId);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    dataManager.addTuVi(onlinePlayer, tuViReward);
                    dataManager.addDaoNiem(onlinePlayer, daoNiemReward);
                    onlinePlayer.sendMessage("§6§l[Thế Giới Boss] §aNgươi xếp hạng #" + rank + " sát thương! Nhận §e+"
                            + tuViReward + " Tu Vi §fvà §b+" + daoNiemReward + " Đạo Niệm§f.");
                } else if (plugin != null && plugin.getAlchemyEscrowManager() != null) {
                    // Người chơi offline -> Gửi gói quà thưởng có token nhận thưởng vào Hòm Thư Tu Tiên (Mailbox)
                    org.bukkit.inventory.ItemStack rewardItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD, Math.max(1, 10 / rank));
                    org.bukkit.inventory.meta.ItemMeta meta = rewardItem.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§6§lThưởng Boss Thế Giới #" + rank);
                        meta.setLore(List.of("§7Phần thưởng tiêu diệt Boss khi offline.", "§eTu Vi: +" + tuViReward, "§bĐạo Niệm: +" + daoNiemReward, "§a(Chuột phải để nhận)"));
                        try {
                            meta.getPersistentDataContainer().set(REWARD_TUVI_KEY, PersistentDataType.INTEGER, tuViReward);
                            meta.getPersistentDataContainer().set(REWARD_DAONIEM_KEY, PersistentDataType.INTEGER, daoNiemReward);
                        } catch (Throwable ignored) {}
                        rewardItem.setItemMeta(meta);
                    }
                    plugin.getAlchemyEscrowManager().depositToMailbox(pId, rewardItem);
                }
            }
            rank++;
        }

        currentBoss = null;
        return topDamagers;
    }
}
