package tutien.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * Tiện ích mở rộng của PlaceholderAPI cho TuTienPlugin.
 * v2.2 Skyblock Edition:
 *   - Hỗ trợ đầy đủ %tutien_canhgioi_#%, %tutien_tuvi_#%, %tutien_khoangthach_#%, %tutien_daoniem_#%, %tutien_bequandiem_#%
 *   - Hỗ trợ %tutien_canhgioi_1% -> %tutien_canhgioi_10% và các bảng Top khác.
 */
public class TuTienExpansion extends PlaceholderExpansion {

    private final PlayerDataManager dataManager;

    public TuTienExpansion(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public String getIdentifier() {
        return "tutien";
    }

    @Override
    public String getAuthor() {
        return "TaiDevNguyen";
    }

    @Override
    public String getVersion() {
        return "2.2";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        // Các biến Top 1..10 có thể gọi kể cả khi player == null
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

        if (player == null) {
            return "";
        }

        // Biến %tutien_canhgioi_#% / %tutien_tuvi_#% / %tutien_khoangthach_#% / %tutien_daoniem_#% -> Thứ hạng của người chơi
        if (identifier.toLowerCase().matches("(canhgioi|tuvi|khoangthach|daoniem|bequandiem)_#")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            LeaderboardManager lb = plugin.getLeaderboardManager();
            if (lb == null) return "Chưa xếp hạng";
            String cat = identifier.substring(0, identifier.indexOf('_'));
            int rank = lb.getPlayerRank(cat, player.getUniqueId());
            return rank > 0 ? "#" + rank : "Ngoài Top 10";
        }

        if (identifier.equalsIgnoreCase("canhgioi")) {
            return dataManager.getCanhGioi(player).getTenHienThi();
        }

        if (identifier.equalsIgnoreCase("tuvi")) {
            return String.valueOf(dataManager.getTuVi(player));
        }

        if (identifier.equalsIgnoreCase("tuvi_max")) {
            CanhGioi tiepTheo = dataManager.getCanhGioi(player).getCanhGioiTiepTheo();
            return tiepTheo != null ? String.valueOf(tiepTheo.getTuViYeuCau()) : "Đỉnh Phong";
        }

        if (identifier.equalsIgnoreCase("linhcan")) {
            return dataManager.getLinhCan(player).getTenHienThi();
        }

        if (identifier.equalsIgnoreCase("he")) {
            return dataManager.getHeTuLuyen(player).getTenHienThi();
        }

        if (identifier.equalsIgnoreCase("linhluc")) {
            return String.valueOf(dataManager.getLinhLuc(player));
        }

        if (identifier.equalsIgnoreCase("linhluc_max")) {
            return String.valueOf(dataManager.getMaxLinhLucEffective(player));
        }

        if (identifier.equalsIgnoreCase("bonus_damage")) {
            return String.format("%.1f", dataManager.getBonusDamage(player));
        }

        if (identifier.equalsIgnoreCase("bonus_health")) {
            return String.format("%.1f", dataManager.getBonusHealth(player));
        }

        if (identifier.equalsIgnoreCase("danlo_cap")) {
            return String.valueOf(dataManager.getDanLoCapDo(player));
        }

        if (identifier.equalsIgnoreCase("luyen_count")) {
            return String.valueOf(dataManager.getSoLanLuyenDan(player));
        }

        if (identifier.equalsIgnoreCase("chetac_cap")) {
            return String.valueOf(dataManager.getCapChe(player));
        }

        if (identifier.equalsIgnoreCase("balance")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            EconomyManager eco = plugin.getEconomyManager();
            if (eco != null && eco.isVaultEnabled()) {
                return eco.formatMoney(eco.getBalance(player));
            }
            return "0";
        }

        if (identifier.equalsIgnoreCase("khoangthach")) {
            return String.valueOf(dataManager.getKhoangThach(player));
        }

        if (identifier.equalsIgnoreCase("daoniem")) {
            return String.valueOf(dataManager.getDaoNiem(player));
        }

        if (identifier.equalsIgnoreCase("bequan_diem")) {
            return String.valueOf(dataManager.getBeQuanDiem(player));
        }

        return null;
    }
}
