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

        if (identifier.equalsIgnoreCase("canhgioi_full")) {
            CanhGioi cg = dataManager.getCanhGioi(player);
            int tang = dataManager.getTang(player);
            return cg.getTenHienThi() + " " + cg.getTieuCanhGioi(tang);
        }

        if (identifier.equalsIgnoreCase("tang")) {
            return String.valueOf(dataManager.getTang(player));
        }

        if (identifier.equalsIgnoreCase("tieucanhgioi")) {
            CanhGioi cg = dataManager.getCanhGioi(player);
            int tang = dataManager.getTang(player);
            return cg.getTieuCanhGioi(tang);
        }

        if (identifier.equalsIgnoreCase("canco")) {
            return String.format("%.1f", dataManager.getCanCo(player));
        }

        if (identifier.equalsIgnoreCase("toithe_progress")) {
            return String.format("%.1f", dataManager.getToiTheProgress(player));
        }

        if (identifier.equalsIgnoreCase("thechat")) {
            return String.format("%.1f", dataManager.getTheChat(player));
        }

        if (identifier.equalsIgnoreCase("ngotinh")) {
            return String.format("%.1f", dataManager.getNgoTinh(player));
        }

        if (identifier.equalsIgnoreCase("thanthuc")) {
            return String.format("%.1f", dataManager.getThanThuc(player));
        }

        if (identifier.equalsIgnoreCase("dandoc")) {
            return String.format("%.1f", dataManager.getDanDoc(player));
        }

        if (identifier.equalsIgnoreCase("congphap")) {
            String cpId = dataManager.getCongPhapChuTu(player);
            if (cpId == null || cpId.equalsIgnoreCase("CHUA_CO")) return "§7Chưa có";
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin.getCongPhapManager() != null) {
                tutien.cultivation.model.CongPhap cp = plugin.getCongPhapManager().getCongPhap(cpId);
                if (cp != null) return cp.getDisplayName();
            }
            return cpId;
        }

        if (identifier.equalsIgnoreCase("congphap_grade")) {
            String cpId = dataManager.getCongPhapChuTu(player);
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin.getCongPhapManager() != null && cpId != null) {
                tutien.cultivation.model.CongPhap cp = plugin.getCongPhapManager().getCongPhap(cpId);
                if (cp != null) return cp.getFormattedGrade();
            }
            return "§7Không";
        }

        if (identifier.equalsIgnoreCase("congphap_chapter")) {
            String cpId = dataManager.getCongPhapChuTu(player);
            tutien.cultivation.data.PlayerCultivationData cd = dataManager.getPlayerCultivationData(player);
            tutien.cultivation.model.CongPhapProgress prog = cd.getCongPhapProgress(cpId);
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            int total = 1;
            if (plugin.getCongPhapManager() != null && cpId != null) {
                tutien.cultivation.model.CongPhap cp = plugin.getCongPhapManager().getCongPhap(cpId);
                if (cp != null) total = cp.getTotalChapters();
            }
            int learned = prog != null ? prog.getHighestLearnedChapter() : 0;
            return "Tầng " + learned + "/" + total;
        }

        if (identifier.equalsIgnoreCase("congphap_adapting")) {
            tutien.cultivation.data.PlayerCultivationData cd = dataManager.getPlayerCultivationData(player);
            if (cd.getAdaptation().isActive()) {
                long secs = cd.getAdaptation().getRemainingSeconds();
                return "Đang hòa hợp (" + (secs / 60) + "m)";
            }
            return "Hài hòa";
        }

        if (identifier.equalsIgnoreCase("tuvi")) {
            return String.valueOf(dataManager.getTuVi(player));
        }

        if (identifier.equalsIgnoreCase("tuvi_max")) {
            CanhGioi cg = dataManager.getCanhGioi(player);
            return cg != null ? String.valueOf(cg.getTuViYeuCau()) : "0";
        }

        if (identifier.equalsIgnoreCase("tuvi_next_realm_max")) {
            CanhGioi tiepTheo = dataManager.getCanhGioi(player).getCanhGioiTiepTheo();
            return tiepTheo != null ? String.valueOf(tiepTheo.getTuViYeuCau()) : "Đỉnh Phong";
        }

        if (identifier.equalsIgnoreCase("tuvi_tang")) {
            CanhGioi cg = dataManager.getCanhGioi(player);
            int cur = dataManager.getTuVi(player);
            int per = Math.max(1, cg.getTuViMoiTang());
            int tang = dataManager.getTang(player);
            int inTang = (tang >= cg.getMaxSubStages() && cur >= cg.getTuViYeuCau()) ? per : (cur % per);
            return String.valueOf(inTang);
        }

        if (identifier.equalsIgnoreCase("tuvi_tang_max")) {
            CanhGioi cg = dataManager.getCanhGioi(player);
            return String.valueOf(cg.getTuViMoiTang());
        }

        if (identifier.equalsIgnoreCase("tuvi_percent")) {
            CanhGioi cg = dataManager.getCanhGioi(player);
            int cur = dataManager.getTuVi(player);
            int max = Math.max(1, cg.getTuViYeuCau());
            return String.format("%.1f%%", Math.min(100.0, ((double) cur / max) * 100.0));
        }

        // %tutien_linhcan% & %tutien_linhcan_compact%: Định dạng rút gọn chống tràn TAB/Scoreboard
        if (identifier.equalsIgnoreCase("linhcan") || identifier.equalsIgnoreCase("linhcan_compact")) {
            return dataManager.getLinhCanData(player).getCompactDisplayName();
        }

        // %tutien_linhcan_short%: Chỉ lấy tên loại linh căn (e.g. "Thiên Linh Căn")
        if (identifier.equalsIgnoreCase("linhcan_short")) {
            return dataManager.getLinhCanData(player).getCurrentType().getTenHienThi();
        }

        // %tutien_linhcan_full% & %tutien_linhcan_detail%: Định dạng chi tiết đầy đủ phẩm cấp từng nguyên tố
        if (identifier.equalsIgnoreCase("linhcan_full") || identifier.equalsIgnoreCase("linhcan_detail")) {
            return dataManager.getLinhCanData(player).getFormattedDisplayName();
        }

        if (identifier.equalsIgnoreCase("linhcan_type")) {
            return dataManager.getLinhCanData(player).getCurrentType().getTenHienThi();
        }

        if (identifier.equalsIgnoreCase("linhcan_purity")) {
            return String.format("%.1f%%", dataManager.getLinhCanData(player).getPurity() * 100.0);
        }

        if (identifier.equalsIgnoreCase("linhcan_primary")) {
            String primary = dataManager.getLinhCanData(player).getPrimaryElement();
            return tutien.element.LinhCanData.getElementDisplayTag(primary);
        }

        if (identifier.equalsIgnoreCase("linhcan_count")) {
            return String.valueOf(dataManager.getLinhCanData(player).getElementCount());
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

        if (identifier.equalsIgnoreCase("alchemist_rank")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin != null && plugin.getBtthRankService() != null) {
                return plugin.getBtthRankService().getRank(player).getPrefix();
            }
            return "§7[Học Đồ]";
        }

        if (identifier.equalsIgnoreCase("soul_realm")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin != null && plugin.getBtthSoulService() != null) {
                return plugin.getBtthSoulService().getSoulData(player).getRealm().getFormattedTag();
            }
            return "§7[Phàm Cảnh]";
        }

        if (identifier.equalsIgnoreCase("soul_power")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin != null && plugin.getBtthSoulService() != null) {
                return String.valueOf(Math.round(plugin.getBtthSoulService().getSoulData(player).getSoulPower()));
            }
            return "50";
        }

        if (identifier.equalsIgnoreCase("soul_cur")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin != null && plugin.getBtthSoulService() != null) {
                return String.valueOf(Math.round(plugin.getBtthSoulService().getSoulData(player).getCurrentSoul()));
            }
            return "100";
        }

        if (identifier.equalsIgnoreCase("soul_max")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin != null && plugin.getBtthSoulService() != null) {
                return String.valueOf(Math.round(plugin.getBtthSoulService().getSoulData(player).getSoulCapacity()));
            }
            return "100";
        }

        if (identifier.equalsIgnoreCase("flame_name")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin != null && plugin.getBtthFlameService() != null) {
                return plugin.getBtthFlameService().getActiveFlame(player).getDisplayName();
            }
            return "§eĐấu Khí Chi Hỏa";
        }

        if (identifier.equalsIgnoreCase("cauldron_name")) {
            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (plugin != null && plugin.getBtthCauldronService() != null) {
                return plugin.getBtthCauldronService().getActiveCauldron(player).getDisplayName();
            }
            return "§7Phàm Đỉnh";
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
