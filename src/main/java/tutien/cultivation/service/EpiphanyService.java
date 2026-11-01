package tutien.cultivation.service;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import tutien.core.PlayerDataManager;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.manager.CongPhapManager;
import tutien.cultivation.model.CongPhap;
import tutien.cultivation.model.CongPhapProgress;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dịch vụ xử lý Cơ Duyên Đốn Ngộ (Epiphany) (Phase 2.3):
 *
 * 1. Ba loại Đốn Ngộ:
 *    - TIEU_DON_NGO: Tăng 20-35% tiến độ tham ngộ chương hiện tại, hoặc nhận 15-30 Điểm Đạo Ngộ.
 *    - DAI_DON_NGO: Lập tức đột phá 100% khẩu quyết chương đang tham ngộ + 800 MasteryExp.
 *    - DON_NGO_BAN_NGUYEN: Cơ duyên nghịch thiên, vĩnh viễn tăng Căn Cơ hoặc Ngộ Tính (+1.0).
 *
 * 2. Chỉ kiểm tra xác suất khi có SỰ KIỆN ĐỦ ĐIỀU KIỆN (không kiểm tra mỗi tick):
 *    - Hạ gục Boss cấp cao (BOSS_DEFEATED).
 *    - Sinh tử thoát hiểm khi HP < 15% (NEAR_DEATH_SURVIVAL).
 *    - Đột phá đại/tiểu cảnh giới (BREAKTHROUGH).
 *    - Tham ngộ tại thiên địa linh địa (SACRED_SITE).
 *
 * 3. Chống farm và Cooldown: Tối thiểu 15 phút giữa các lần đốn ngộ, reset EpiphanyPotential.
 */
public class EpiphanyService {

    public enum EpiphanyType {
        TIEU_DON_NGO("Tiểu Đốn Ngộ", "§b§l[Tiểu Đốn Ngộ]", "Tâm thần sáng tỏ, thấu triệt khẩu quyết uẩn khúc!"),
        DAI_DON_NGO("Đại Đốn Ngộ", "§d§l[Đại Đốn Ngộ]", "Thiên địa cộng minh, vạn pháp quy nguyên, đốn ngộ một tầng công pháp!"),
        DON_NGO_BAN_NGUYEN("Đốn Ngộ Bản Nguyên", "§6§l[ĐỐN NGỘ BẢN NGUYÊN]", "Chạm tới đại đạo chân lý, cải tạo căn cơ phàm thai vi thượng căn!");

        private final String name;
        private final String displayName;
        private final String description;

        EpiphanyType(String name, String displayName, String description) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum EpiphanyEvent {
        BOSS_DEFEATED(1.5),
        NEAR_DEATH_SURVIVAL(2.0),
        BREAKTHROUGH(1.8),
        SACRED_SITE(1.2);

        private final double contextMultiplier;

        EpiphanyEvent(double contextMultiplier) {
            this.contextMultiplier = contextMultiplier;
        }

        public double getContextMultiplier() {
            return contextMultiplier;
        }
    }

    public static final long EPIPHANY_COOLDOWN_MS = 15 * 60 * 1000L; // 15 phút
    public static final double BASE_CHANCE = 0.0005; // 0.05%
    public static final double MAX_CHANCE = 0.0100;  // 1.0% tối đa

    private final CongPhapManager congPhapManager;
    private final Map<UUID, Long> lastEpiphanyTimeMap = new ConcurrentHashMap<>();

    public EpiphanyService(CongPhapManager congPhapManager) {
        this.congPhapManager = congPhapManager;
    }

    /**
     * Kích hoạt kiểm tra Đốn Ngộ khi người chơi hoàn thành một sự kiện hợp lệ.
     *
     * @return EpiphanyType nếu kích hoạt thành công, null nếu không xảy ra
     */
    public EpiphanyType triggerEpiphanyCheck(Player player, PlayerDataManager dataManager, EpiphanyEvent event) {
        if (player == null || dataManager == null || event == null) return null;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastTime = lastEpiphanyTimeMap.getOrDefault(uuid, 0L);

        // Kiểm tra Cooldown
        if (now - lastTime < EPIPHANY_COOLDOWN_MS) {
            return null;
        }

        PlayerCultivationData data = dataManager.getPlayerCultivationData(player);
        String primaryId = data.getActivePrimary();
        if (primaryId == null) return null;

        CongPhap cp = congPhapManager != null ? congPhapManager.getCongPhap(primaryId) : null;
        CongPhapProgress prog = data.getOrCreateProgress(primaryId);

        // Tính toán xác suất
        double daoFactor = 1.0 + (dataManager.getDaoNgo(player) * 0.0001);
        double potentialBonus = prog.getEpiphanyPotential() * 0.0005;
        double chance = BASE_CHANCE * daoFactor * event.getContextMultiplier() + potentialBonus;
        chance = Math.min(MAX_CHANCE, chance);

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        if (rand.nextDouble() > chance) {
            // Không trúng: tích lũy tiềm năng đốn ngộ (+0.5 -> 1.0)
            prog.addEpiphanyPotential(0.5 + rand.nextDouble() * 0.5);
            return null;
        }

        // KÍCH HOẠT ĐỐN NGỘ THÀNH CÔNG!
        lastEpiphanyTimeMap.put(uuid, now);
        prog.setEpiphanyPotential(0.0); // Reset potential

        // Xác định loại đốn ngộ
        int roll = rand.nextInt(100);
        EpiphanyType type;
        if (roll < 75) {
            type = EpiphanyType.TIEU_DON_NGO; // 75%
        } else if (roll < 95) {
            type = EpiphanyType.DAI_DON_NGO;  // 20%
        } else {
            type = EpiphanyType.DON_NGO_BAN_NGUYEN; // 5% siêu hiếm
        }

        applyEpiphanyRewards(player, dataManager, cp, prog, type);
        return type;
    }

    private void applyEpiphanyRewards(Player player, PlayerDataManager dataManager, CongPhap cp,
                                      CongPhapProgress prog, EpiphanyType type) {
        switch (type) {
            case TIEU_DON_NGO: {
                double gain = 25.0 + ThreadLocalRandom.current().nextDouble() * 10.0;
                prog.addComprehensionProgress(gain);
                dataManager.addDaoNgoPoints(player, 20);

                player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage(type.getDisplayName() + " " + type.getDescription());
                player.sendMessage("§f▪ Tiến độ tham ngộ: §a+" + String.format("%.1f%%", gain));
                player.sendMessage("§f▪ Thưởng Đạo Ngộ Điểm: §b+20 Điểm");
                player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
                break;
            }
            case DAI_DON_NGO: {
                int targetChapter = prog.getTargetChapter();
                if (cp != null && targetChapter <= cp.getTotalChapters()) {
                    prog.addLearnedChapter(targetChapter);
                    prog.setComprehensionProgress(0.0);
                }
                prog.addMasteryExp(800);
                dataManager.addDaoNgoPoints(player, 50);

                player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage(type.getDisplayName() + " " + type.getDescription());
                player.sendMessage("§f▪ Đột phá trực tiếp khẩu quyết tầng: §e§l[Tầng " + targetChapter + "]");
                player.sendMessage("§f▪ Thưởng Độ Thành Thục: §6+800 MasteryExp");
                player.sendMessage("§f▪ Thưởng Đạo Ngộ Điểm: §b+50 Điểm");
                player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.2, 0), 60, 0.5, 0.8, 0.5, 0.1);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                break;
            }
            case DON_NGO_BAN_NGUYEN: {
                // Tăng vĩnh viễn chỉ số bản nguyên Căn Cơ hoặc Ngộ Tính
                boolean boostCanCo = ThreadLocalRandom.current().nextBoolean();
                if (boostCanCo) {
                    dataManager.setCanCo(player, dataManager.getCanCo(player) + 1.0);
                } else {
                    dataManager.setNgoTinh(player, dataManager.getNgoTinh(player) + 1.0);
                }
                dataManager.addDaoNgoPoints(player, 100);

                player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage(type.getDisplayName() + " " + type.getDescription());
                player.sendMessage("§f▪ Thưởng cải tạo bản nguyên: " + (boostCanCo ? "§aCăn Cơ +1.0" : "§eNgộ Tính +1.0"));
                player.sendMessage("§f▪ Thưởng Đạo Ngộ Điểm: §b+100 Điểm");
                player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                // Thông báo toàn server
                Bukkit.broadcastMessage("§6§l✦ THIÊN ĐẠO ĐỒN NGỘ ✦ §fĐạo hữu §b" + player.getName()
                        + " §fchạm tới chân lý đại đạo, đạt được §6§l[Đốn Ngộ Bản Nguyên]§f, nghịch thiên cải mệnh!");

                player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1.5, 0), 5, 0.2, 0.2, 0.2, 0);
                player.getWorld().strikeLightningEffect(player.getLocation());
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                break;
            }
        }
    }
}
