package tutien.cultivation.service;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.cultivation.data.PlayerCultivationData;
import tutien.cultivation.model.CongPhapProgress;
import tutien.integration.mythicmobs.BukkitSkillFallback;
import tutien.integration.mythicmobs.MythicMobsHook;
import tutien.integration.mythicmobs.MythicSkillAdapter;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dịch vụ Quản lý & Thi triển Thần Thông / Kỹ Năng Công Pháp (Phase 2.4):
 * - Tích hợp mềm (Soft-hook) MythicMobs không phụ thuộc cứng.
 * - Cơ chế Fallback an toàn thuần Bukkit khi không có MythicMobs.
 * - Kiểm tra Cảnh giới, Khẩu quyết đã học, Linh Lực (giảm theo Mastery), Cooldown.
 * - Tuyệt đối không mất Linh Lực oan nếu thi triển thất bại.
 */
public class CongPhapSkillService {

    public static class SkillCastResult {
        private final boolean success;
        private final String message;
        private final int manaDeducted;

        public SkillCastResult(boolean success, String message, int manaDeducted) {
            this.success = success;
            this.message = message;
            this.manaDeducted = manaDeducted;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getManaDeducted() { return manaDeducted; }
    }

    private final TuTienPlugin plugin;
    private FileConfiguration skillsConfig;
    private final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();

    public CongPhapSkillService(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "skills.yml");
        if (!file.exists()) {
            try { plugin.saveResource("skills.yml", false); }
            catch (Exception ignored) {}
        }

        if (file.exists()) {
            this.skillsConfig = YamlConfiguration.loadConfiguration(file);
        } else {
            InputStream in = plugin.getResource("skills.yml");
            if (in != null) {
                this.skillsConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }
    }

    public SkillCastResult castSkill(Player player, String skillId) {
        if (player == null || skillId == null) {
            return new SkillCastResult(false, "Tham số không hợp lệ!", 0);
        }

        if (skillsConfig == null || !skillsConfig.isConfigurationSection("skills." + skillId)) {
            return new SkillCastResult(false, "§cKỹ năng không tồn tại trong điển tịch!", 0);
        }

        ConfigurationSection skillSec = skillsConfig.getConfigurationSection("skills." + skillId);
        String skillName = skillSec.getString("display-name", skillId).replace('&', '§');
        String requiredCp = skillSec.getString("cong-phap", "");
        int requiredChapter = skillSec.getInt("required-chapter", 1);
        int baseManaCost = skillSec.getInt("mana-cost", 30);
        long cooldownSeconds = skillSec.getLong("cooldown-seconds", 5);
        String mythicSkill = skillSec.getString("mythic-skill", "");

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        PlayerCultivationData cultData = dataManager.getPlayerCultivationData(player);

        // 1. Kiểm tra quyền sở hữu và khẩu quyết tầng yêu cầu
        CongPhapProgress prog = cultData.getCongPhapProgress(requiredCp);
        if (prog == null || !prog.hasLearned(requiredChapter)) {
            return new SkillCastResult(false, "§cNgươi chưa lĩnh hội Tầng " + requiredChapter + " của công pháp " + requiredCp + "!", 0);
        }

        // 2. Kiểm tra Cooldown
        UUID uuid = player.getUniqueId();
        String cdKey = uuid.toString() + ":" + skillId;
        long now = System.currentTimeMillis();
        long expireTime = cooldownMap.getOrDefault(cdKey, 0L);
        if (now < expireTime) {
            long remainSec = (expireTime - now + 999) / 1000;
            return new SkillCastResult(false, "§cKỹ năng đang trong thời gian hồi chiêu (" + remainSec + "s)!", 0);
        }

        // 3. Tính toán chi phí Linh Lực (áp dụng giảm mana từ MasteryTier)
        MasteryService masteryService = plugin.getCongPhapManager().getMasteryService();
        double manaReduction = masteryService.getManaCostReduction(cultData, requiredCp);
        int actualManaCost = Math.max(1, (int) Math.round(baseManaCost * (1.0 - manaReduction)));

        if (dataManager.getLinhLuc(player) < actualManaCost) {
            return new SkillCastResult(false, "§cKhông đủ Linh Lực! Cần: §b" + actualManaCost + " Linh Lực§c (Hiện có: §e" + dataManager.getLinhLuc(player) + "§c).", 0);
        }

        // 4. Thi triển kỹ năng (Ưu tiên MythicMobs -> Fallback Bukkit)
        boolean executed = false;
        if (MythicMobsHook.isAvailable() && mythicSkill != null && !mythicSkill.isEmpty()) {
            executed = MythicSkillAdapter.castSkill(player, mythicSkill);
        }

        if (!executed) {
            ConfigurationSection fallbackSec = skillSec.getConfigurationSection("fallback");
            if (fallbackSec != null && fallbackSec.getBoolean("enabled", false)) {
                executed = BukkitSkillFallback.execute(player, fallbackSec);
            }
        }

        // 5. Nếu thi triển thất bại -> Hoàn trả toàn bộ, không trừ Linh Lực, không tính cooldown
        if (!executed) {
            return new SkillCastResult(false, "§cThi triển thần thông thất bại! Không tiêu hao linh lực.", 0);
        }

        // 6. Thi triển thành công -> Trừ Linh Lực, gán Cooldown, tích lũy MasteryExp
        dataManager.takeLinhLuc(player, actualManaCost);
        cooldownMap.put(cdKey, now + (cooldownSeconds * 1000L));
        masteryService.addMasteryExp(player, dataManager, requiredCp, MasteryService.ActivitySource.ABILITY_USE);

        player.sendMessage("§6§l✦ THẦN THÔNG XUẤT THẾ ✦ §fThi triển: " + skillName + " §7(Tiêu hao: §b-" + actualManaCost + " Linh Lực§7)!");
        return new SkillCastResult(true, "Thi triển thành công!", actualManaCost);
    }

    public void clearCooldown(UUID uuid) {
        cooldownMap.entrySet().removeIf(entry -> entry.getKey().startsWith(uuid.toString() + ":"));
    }
}
