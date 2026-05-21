package tutien.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * Lớp này biến plugin của chúng ta thành một tiện ích mở rộng của PlaceholderAPI.
 * Cho phép dùng các biến như %tutien_canhgioi% trong các plugin khác như Scoreboard, Chat.
 */
public class TuTienExpansion extends PlaceholderExpansion {

    private final PlayerDataManager dataManager;

    public TuTienExpansion(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public String getIdentifier() {
        return "tutien"; // Đây là tiền tố của biến. VD: %tutien_...
    }

    @Override
    public String getAuthor() {
        return "TaiDevNguyen";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // Giữ cho expansion này luôn hoạt động
    }

    // Nơi xử lý khi một plugin khác (Scoreboard/TAB) gọi biến số
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        // Biến %tutien_canhgioi% -> Trả về (Ví dụ: Luyện Khí)
        if (identifier.equalsIgnoreCase("canhgioi")) {
            return dataManager.getCanhGioi(player).getTenHienThi();
        }

        // Biến %tutien_tuvi% -> Trả về (Ví dụ: 1500)
        if (identifier.equalsIgnoreCase("tuvi")) {
            return String.valueOf(dataManager.getTuVi(player));
        }

        // Biến %tutien_tuvi_max% -> Trả về mốc cần để đột phá
        if (identifier.equalsIgnoreCase("tuvi_max")) {
            CanhGioi tiepTheo = dataManager.getCanhGioi(player).getCanhGioiTiepTheo();
            return tiepTheo != null ? String.valueOf(tiepTheo.getTuViYeuCau()) : "Đỉnh Phong";
        }

        // Biến %tutien_linhcan% -> Trả về (Ví dụ: Thiên Linh Căn)
        if (identifier.equalsIgnoreCase("linhcan")) {
            return dataManager.getLinhCan(player).getTenHienThi();
        }

        // Biến %tutien_linhluc% -> Trả về Mana hiện tại
        if (identifier.equalsIgnoreCase("linhluc")) {
            return String.valueOf(dataManager.getLinhLuc(player));
        }

        // Biến %tutien_linhluc_max% -> Trả về Mana tối đa theo cảnh giới (Dùng cho Scoreboard)
        if (identifier.equalsIgnoreCase("linhluc_max")) {
            return String.valueOf(dataManager.getCanhGioi(player).getMaxLinhLuc());
        }

        // Biến %tutien_bonus_damage% -> Sát thương vĩnh viễn từ đan dược
        if (identifier.equalsIgnoreCase("bonus_damage")) {
            return String.format("%.1f", dataManager.getBonusDamage(player));
        }

        // Biến %tutien_bonus_health% -> Sinh lực vĩnh viễn từ đan dược
        if (identifier.equalsIgnoreCase("bonus_health")) {
            return String.format("%.1f", dataManager.getBonusHealth(player));
        }

        // Biến %tutien_danlo_cap% -> Cấp lò luyện đan
        if (identifier.equalsIgnoreCase("danlo_cap")) {
            return String.valueOf(dataManager.getDanLoCapDo(player));
        }

        // Biến %tutien_luyen_count% -> Số lần đã luyện đan
        if (identifier.equalsIgnoreCase("luyen_count")) {
            return String.valueOf(dataManager.getSoLanLuyenDan(player));
        }

        // Biến %tutien_balance% -> Số dư tiền tệ Vault
        if (identifier.equalsIgnoreCase("balance")) {
            tutien.core.TuTienPlugin plugin = tutien.core.TuTienPlugin.getPlugin(tutien.core.TuTienPlugin.class);
            tutien.core.EconomyManager eco = plugin.getEconomyManager();
            if (eco != null && eco.isVaultEnabled()) {
                return eco.formatMoney(eco.getBalance(player));
            }
            return "0";
        }

        // =============================================
        // v2.1: PLACEHOLDER MỚI
        // =============================================

        // %tutien_khoangthach% -> Số Khoáng Thạch
        if (identifier.equalsIgnoreCase("khoangthach"))
            return String.valueOf(dataManager.getKhoangThach(player));

        // %tutien_daoniem% -> Số Đạo Niệm
        if (identifier.equalsIgnoreCase("daoniem"))
            return String.valueOf(dataManager.getDaoNiem(player));

        // %tutien_bequan_diem% -> Điểm Bế Quan
        if (identifier.equalsIgnoreCase("bequan_diem"))
            return String.valueOf(dataManager.getBeQuanDiem(player));

        // %tutien_canhgioi_1% -> %tutien_canhgioi_10%: Top Cảnh Giới
        // %tutien_tuvi_top_1% -> %tutien_tuvi_top_10%: Top Tu Vi
        // %tutien_khoangthach_top_1% -> %tutien_khoangthach_top_10%
        // %tutien_daoniem_top_1% -> %tutien_daoniem_top_10%
        // %tutien_bequandiem_top_1% -> %tutien_bequandiem_top_10%
        if (identifier.toLowerCase().matches("(canhgioi|tuvi_top|khoangthach_top|daoniem_top|bequandiem_top)_\\d+")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            LeaderboardManager lb = plugin.getLeaderboardManager();
            if (lb == null) return "N/A";

            String[] parts = identifier.split("_");
            int rank;
            try { rank = Integer.parseInt(parts[parts.length - 1]); }
            catch (NumberFormatException e) { return "N/A"; }

            String category;
            if (identifier.toLowerCase().startsWith("canhgioi_")) category = "canhgioi";
            else if (identifier.toLowerCase().startsWith("tuvi_top_")) category = "tuvi";
            else if (identifier.toLowerCase().startsWith("khoangthach_top_")) category = "khoangthach";
            else if (identifier.toLowerCase().startsWith("daoniem_top_")) category = "daoniem";
            else if (identifier.toLowerCase().startsWith("bequandiem_top_")) category = "bequandiem";
            else return "N/A";

            LeaderboardManager.LeaderEntry entry = lb.getEntry(category, rank);
            if (entry == null) return "§7Trống";
            if (category.equals("canhgioi")) {
                return entry.playerName() + " §7- §d" + entry.extra();
            }
            return entry.playerName() + " §7- §e" + entry.value();
        }

        return null; // Trả về null nếu người dùng gõ sai tên biến
    }
}