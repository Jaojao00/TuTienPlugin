package tutien.dungeon.bicanh;

import org.bukkit.entity.Player;
import tutien.core.CanhGioi;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Phụ Bản Bí Cảnh Phân Tầng (Phase 5).
 */
public class BiCanhManager {

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final Map<UUID, BiCanhSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerSessionMap = new ConcurrentHashMap<>();
    private final Map<UUID, org.bukkit.Location> previousLocations = new ConcurrentHashMap<>();
    private final Map<BiCanhTier, org.bukkit.Location> arenaLocations = new EnumMap<>(BiCanhTier.class);

    public BiCanhManager(TuTienPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        initDefaultArenas();
    }

    private void initDefaultArenas() {
        if (org.bukkit.Bukkit.getServer() != null) {
            try {
                List<org.bukkit.World> worlds = org.bukkit.Bukkit.getWorlds();
                if (worlds != null && !worlds.isEmpty()) {
                    org.bukkit.World w = worlds.get(0);
                    arenaLocations.put(BiCanhTier.TANG_1, new org.bukkit.Location(w, 1000.0, 70.0, 1000.0));
                    arenaLocations.put(BiCanhTier.TANG_2, new org.bukkit.Location(w, 2000.0, 70.0, 2000.0));
                    arenaLocations.put(BiCanhTier.TANG_3, new org.bukkit.Location(w, 3000.0, 70.0, 3000.0));
                    arenaLocations.put(BiCanhTier.TANG_4, new org.bukkit.Location(w, 4000.0, 70.0, 4000.0));
                }
            } catch (Throwable ignored) {}
        }
        loadArenasFromConfig();
    }

    public void loadArenasFromConfig() {
        if (plugin == null) return;
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
            if (cfg != null && cfg.isConfigurationSection("bicanh.arenas")) {
                org.bukkit.configuration.ConfigurationSection sec = cfg.getConfigurationSection("bicanh.arenas");
                for (BiCanhTier tier : BiCanhTier.values()) {
                    if (sec.isConfigurationSection(tier.name())) {
                        String wName = sec.getString(tier.name() + ".world");
                        org.bukkit.World w = wName != null ? org.bukkit.Bukkit.getWorld(wName) : null;
                        if (w != null) {
                            double x = sec.getDouble(tier.name() + ".x", 0.0);
                            double y = sec.getDouble(tier.name() + ".y", 70.0);
                            double z = sec.getDouble(tier.name() + ".z", 0.0);
                            arenaLocations.put(tier, new org.bukkit.Location(w, x, y, z));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public void setArenaLocation(BiCanhTier tier, org.bukkit.Location loc) {
        if (tier != null && loc != null) {
            arenaLocations.put(tier, loc.clone());
        }
    }

    public org.bukkit.Location getArenaLocation(BiCanhTier tier) {
        return arenaLocations.get(tier);
    }

    /**
     * Bắt đầu một phiên Bí Cảnh phân tầng
     */
    public BiCanhSession createSession(BiCanhTier tier) {
        BiCanhSession session = new BiCanhSession(tier);
        activeSessions.put(session.getSessionId(), session);
        return session;
    }

    /**
     * Lấy phiên Bí Cảnh đang mở cùng tầng để tạo shared instance nhiều người cùng chơi,
     * hoặc tạo mới nếu chưa có phiên nào còn hoạt động.
     */
    public synchronized BiCanhSession getOrCreateActiveSession(BiCanhTier tier) {
        if (tier == null) return null;
        for (BiCanhSession s : activeSessions.values()) {
            if (s.getTier() == tier && s.isActive() && !s.isExpired()) {
                return s;
            }
        }
        return createSession(tier);
    }

    /**
     * Kiểm tra người chơi có đủ điều kiện Cảnh Giới để vào Bí Cảnh không
     */
    public boolean canEnter(Player p, BiCanhTier tier) {
        if (p == null || tier == null || dataManager == null) return false;
        CanhGioi current = dataManager.getCanhGioi(p);
        return current.ordinal() >= tier.getRequiredRank().ordinal();
    }

    public boolean joinSession(Player p, UUID sessionId) {
        BiCanhSession session = activeSessions.get(sessionId);
        if (session == null || !session.isActive()) return false;
        if (!canEnter(p, session.getTier())) return false;

        if (p.getLocation() != null) {
            previousLocations.put(p.getUniqueId(), p.getLocation().clone());
        }

        playerSessionMap.put(p.getUniqueId(), sessionId);

        // Truyền tống người chơi vào khu vực Bí Cảnh (nếu có server thật)
        try {
            if (p.getWorld() != null && org.bukkit.Bukkit.getServer() != null) {
                org.bukkit.Location targetLoc = getArenaLocation(session.getTier());
                if (targetLoc != null) {
                    p.teleport(targetLoc);
                } else {
                    p.teleport(p.getLocation().clone().add(0, 1, 0));
                }
            }
        } catch (Throwable ignored) {}

        return true;
    }

    public boolean isPlayerInsideBiCanh(UUID playerId) {
        if (playerId == null) return false;
        UUID sId = playerSessionMap.get(playerId);
        if (sId == null) return false;
        BiCanhSession session = activeSessions.get(sId);
        return session != null && session.isActive();
    }

    public boolean isPlayerInsideBiCanh(Player p) {
        if (p == null) return false;
        if (!isPlayerInsideBiCanh(p.getUniqueId())) return false;
        BiCanhSession session = getPlayerSession(p.getUniqueId());
        if (session != null && p.getLocation() != null) {
            org.bukkit.Location arenaLoc = getArenaLocation(session.getTier());
            if (arenaLoc != null) {
                if (arenaLoc.getWorld() == null || p.getWorld() == null || !arenaLoc.getWorld().equals(p.getWorld())) {
                    return false;
                }
                return p.getLocation().distance(arenaLoc) <= 150.0;
            }
        }
        return true;
    }

    public BiCanhSession getPlayerSession(UUID playerId) {
        UUID sId = playerSessionMap.get(playerId);
        return (sId != null) ? activeSessions.get(sId) : null;
    }

    public void leaveSession(UUID playerId) {
        playerSessionMap.remove(playerId);
        org.bukkit.Location prev = previousLocations.remove(playerId);
        if (prev != null && org.bukkit.Bukkit.getServer() != null) {
            Player p = org.bukkit.Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                p.teleport(prev);
            }
        }
    }

    public void recordDamage(UUID playerId, double dmg) {
        BiCanhSession session = getPlayerSession(playerId);
        if (session != null && session.isActive()) {
            session.recordDamage(playerId, dmg);
        }
    }

    public void recordDamageTanked(UUID playerId, double dmg) {
        BiCanhSession session = getPlayerSession(playerId);
        if (session != null && session.isActive()) {
            session.recordDamageTanked(playerId, dmg);
        }
    }

    public void recordKill(UUID playerId) {
        BiCanhSession session = getPlayerSession(playerId);
        if (session != null && session.isActive()) {
            session.recordKill(playerId);
        }
    }

    public void onPlayerQuit(Player p) {
        if (p == null) return;
        onPlayerQuit(p.getUniqueId());
    }

    public void onPlayerQuit(UUID playerId) {
        if (playerId == null) return;
        UUID sessionId = playerSessionMap.remove(playerId);
        if (sessionId != null) {
            BiCanhSession session = activeSessions.get(sessionId);
            if (session != null) {
                // Kiểm tra nếu tất cả người chơi đều đã rời thì dọn dẹp phiên
                boolean hasRemainingPlayers = playerSessionMap.containsValue(sessionId);
                if (!hasRemainingPlayers && session.isExpired()) {
                    concludeSession(sessionId);
                }
            }
        }
    }

    public void cleanupExpiredSessions() {
        for (Map.Entry<UUID, BiCanhSession> entry : activeSessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                concludeSession(entry.getKey());
            }
        }
    }

    /**
     * Kết thúc phiên Bí Cảnh và trao phần thưởng theo đóng góp (Idempotent - Chống trao lặp)
     */
    public synchronized Map<UUID, BiCanhSession.RewardBoxTier> concludeSession(UUID sessionId) {
        BiCanhSession session = activeSessions.remove(sessionId);
        if (session == null || session.isRewardDistributed()) return Collections.emptyMap();

        session.setRewardDistributed(true);
        session.setActive(false);
        List<UUID> ranked = session.getRankedContributors();
        Map<UUID, BiCanhSession.RewardBoxTier> rewards = new LinkedHashMap<>();

        int rank = 1;
        for (UUID pId : ranked) {
            BiCanhSession.RewardBoxTier box = session.getRewardBoxTier(rank);
            rewards.put(pId, box);

            int baseTuVi = session.getTier().getBaseTuViReward();
            int baseDaoNiem = session.getTier().getBaseDaoNiemReward();
            int finalTuVi = (int) (baseTuVi * box.bonusMultiplier);
            int finalDaoNiem = (int) (baseDaoNiem * box.bonusMultiplier);

            // Tạo vật phẩm Rương Thưởng thực tế
            org.bukkit.inventory.ItemStack rewardBoxItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHEST, 1);
            try {
                org.bukkit.inventory.meta.ItemMeta meta = rewardBoxItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6§lRương Bí Cảnh: " + box.display);
                    meta.setLore(List.of(
                            "§7Phụ bản: §f" + session.getTier().getDisplayName(),
                            "§7Hạng cống hiến: §e#" + rank,
                            "§aTu Vi: +" + finalTuVi,
                            "§bĐạo Niệm: +" + finalDaoNiem
                    ));
                    rewardBoxItem.setItemMeta(meta);
                }
            } catch (Throwable ignored) {}

            Player onlinePlayer = (org.bukkit.Bukkit.getServer() != null) ? org.bukkit.Bukkit.getPlayer(pId) : null;
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                if (dataManager != null) {
                    dataManager.addTuVi(onlinePlayer, finalTuVi);
                    dataManager.addDaoNiem(onlinePlayer, finalDaoNiem);
                }
                onlinePlayer.sendMessage("§5§l[Bí Cảnh Hoàn Tất] §aNgươi xếp hạng #" + rank
                        + " cống hiến! Nhận: " + box.display + " §f(+§e" + finalTuVi + " Tu Vi§f, +§b" + finalDaoNiem + " Đạo Niệm§f).");

                // Phát rương thưởng vào túi đồ, nếu đầy thì gửi vào Mailbox
                try {
                    if (onlinePlayer.getInventory() != null) {
                        HashMap<Integer, org.bukkit.inventory.ItemStack> leftovers = onlinePlayer.getInventory().addItem(rewardBoxItem);
                        if (leftovers != null && !leftovers.isEmpty() && plugin != null && plugin.getAlchemyEscrowManager() != null) {
                            for (org.bukkit.inventory.ItemStack left : leftovers.values()) {
                                plugin.getAlchemyEscrowManager().depositToMailbox(pId, left);
                            }
                            onlinePlayer.sendMessage("§e[Bí Cảnh] Túi đồ đã đầy, rương thưởng đã được chuyển vào Hòm Thư (/tutien homthu)!");
                        }
                    }
                } catch (Throwable ignored) {}

                // Dịch chuyển về vị trí cũ trước khi vào Bí Cảnh
                org.bukkit.Location prev = previousLocations.remove(pId);
                if (prev != null) {
                    try {
                        onlinePlayer.teleport(prev);
                    } catch (Throwable ignored) {}
                }
            } else {
                // Người chơi offline -> Lưu trữ rương thưởng an toàn vào Hòm Thư Tu Tiên (Mailbox)
                previousLocations.remove(pId);
                if (plugin != null && plugin.getAlchemyEscrowManager() != null) {
                    plugin.getAlchemyEscrowManager().depositToMailbox(pId, rewardBoxItem);
                }
            }

            playerSessionMap.remove(pId);
            rank++;
        }

        return rewards;
    }
}
