package tutien.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.LinhCan;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.tutien.gui.LuyenDanGUI;
import tutien.tutien.gui.MainMenu;
import tutien.tutien.gui.DotPhaGUI;
import tutien.tutien.gui.TuiDoGUI;
import tutien.tongmon.TongMonManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lớp này xử lý lệnh /tutien trong game.
 * v2.2 Skyblock Edition:
 *   - Sửa /tutien laptong lưu vị trí hiện tại trên đảo Skyblock (không dịch chuyển ra tọa độ 10000, 10000 hay đặt hàng rào phá đảo).
 *   - Bổ sung /tutien tongmon sethome và /tutien doikhoang.
 */
public class TuTienCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final TongMonManager tongMonManager;

    public TuTienCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
        this.tongMonManager = plugin.getTongMonManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e=== §6§lHệ Thống Tu Tiên (Skyblock Edition) §e===");
            sender.sendMessage("§f/tutien menu §7- Mở giao diện chính");
            sender.sendMessage("§f/tutien congphap §7- Tàng Kinh Các (Công Pháp & Tàn Quyển)");
            sender.sendMessage("§f/tutien thamngo §7- Tọa thiền tham ngộ khẩu quyết");
            sender.sendMessage("§f/tutien skill <id> §7- Thi triển thần thông công pháp");
            sender.sendMessage("§f/tutien tuluyen §7- Tọa thiền tại chỗ (AFK)");
            sender.sendMessage("§f/tutien dokiep §7- Đột phá cảnh giới");
            sender.sendMessage("§f/tutien ngukhong §7- Ngự không phi hành (Kim Đan+, đạp mây phi hành)");
            sender.sendMessage("§f/tutien luyendan §7- Lò luyện đan dược (47+ loại đan)");
            sender.sendMessage("§f/tutien chetac §7- Lò rèn trang bị (8 phẩm cấp)");
            sender.sendMessage("§f/tutien doikhoang §7- Quy đổi Khoáng Thạch");
            sender.sendMessage("§f/tutien nhiemvu §7- Nhiệm vụ hàng ngày");
            sender.sendMessage("§f/tutien bequan §7- Cửa hàng điểm Bế Quan");
            sender.sendMessage("§f/tutien linhthuvien §7- Linh Thú Viên & Phụ bản");
            sender.sendMessage("§f/tutien gacha §7- Triệu hồi Linh Thú");
            sender.sendMessage("§f/tutien vangioi §7- Vạn Giới Các (Cửa hàng)");
            sender.sendMessage("§f/tutien xephang §7- Bảng xếp hạng Top 10");
            sender.sendMessage("§f/tutien tongmon §7- Quản lý Tông Môn");
            sender.sendMessage("§f/tutien tuido §7- Túi Đồ Hư Không");
            sender.sendMessage("§f/tutien homthu §7- Hòm Thư nhận đan dược / nguyên liệu hoàn trả");
            sender.sendMessage("§f/tutien book §7- Nhận Cẩm Nang Tu Tiên Giới (Sách hướng dẫn)");
            sender.sendMessage("§f/tutien xem [tên] §7- Xem thông tin đạo hữu");

            if (sender.hasPermission("tutien.admin")) {
                sender.sendMessage("§c=== §4§lQuyền Lực Thiên Đạo (Admin) §c===");
                sender.sendMessage("§f/tutien admin tuvi <add/set/remove> <tên> <số>");
                sender.sendMessage("§f/tutien admin canhgioi <tên> <CẢNH_GIỚI>");
            }
            return true;
        }

        String subCmd = args[0].toLowerCase();

        if (subCmd.equals("book") || subCmd.equals("guide") || subCmd.equals("camnang")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cChỉ có người chơi mới có thể nhận Cẩm Nang Tu Tiên!");
                return true;
            }
            giveGuideBook(p);
            return true;
        }

        if (subCmd.equals("tuluyen")) {
            if (!(sender instanceof Player p)) return true;
            if (dataManager.getCanhGioi(p) == CanhGioi.PHAM_NHAN) {
                p.sendMessage("§c§l[!] §fNgươi là §7Phàm Nhân§f thân mang trần cấu, kinh mạch bế tắc chưa thể Tọa Thiền!");
                p.sendMessage("§e§l[Gợi Ý] §fHãy mở giao diện §a/tutien nhapdao §fđể tiến hành Tôi Thể, Tẩy Tủy và Khai Mạch Dẫn Khí!");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
                return true;
            }
            boolean isMeditating = dataManager.isDangTuLuyen(p);

            if (!isMeditating) {
                dataManager.setTuLuyenMode(p, true);
                Location loc = p.getLocation();
                loc.setX(loc.getBlockX() + 0.5);
                loc.setZ(loc.getBlockZ() + 0.5);
                p.teleport(loc);

                p.sendMessage("§a§l[Tu Tiên] §fNgươi đã nhập định tọa thiền tại chỗ. Linh khí thiên địa đang hội tụ...");
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
            } else {
                dataManager.setTuLuyenMode(p, false);
                p.sendMessage("§e§l[Tu Tiên] §fNgươi đã xuất định, kết thúc quá trình tu luyện.");
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1f);
            }
            return true;
        }

        if (subCmd.equals("congphap")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length > 1 && (args[1].equalsIgnoreCase("huongdan") || args[1].equalsIgnoreCase("guide"))) {
                tutien.cultivation.gui.CongPhapGuideGUI.open(p);
                return true;
            }
            tutien.cultivation.gui.CongPhapGUI.open(p, plugin, 0);
            return true;
        }

        if (subCmd.equals("givebitich")) {
            if (!sender.hasPermission("tutien.admin")) {
                sender.sendMessage("�c�?o h?u kh�ng c� quy?n h?n d�ng l?nh n�y!");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("�cS? d?ng: /tutien givebitich <player> <congphap_id> [chapter]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("�cKh�ng t�m th?y ngu?i choi: " + args[1]);
                return true;
            }
            String cpId = args[2].toUpperCase();
            tutien.cultivation.model.CongPhap cp = plugin.getCongPhapManager().getCongPhap(cpId);
            if (cp == null) {
                sender.sendMessage("�cKh�ng t�m th?y c�ng ph�p: " + cpId);
                return true;
            }
            int chapter = 1;
            if (args.length >= 4) {
                try { chapter = Integer.parseInt(args[3]); } catch (Exception ignored) {}
            }
            org.bukkit.inventory.ItemStack bitich = tutien.cultivation.item.BiTichItem.createBiTich(cp, chapter);
            target.getInventory().addItem(bitich);
            sender.sendMessage("�a[Tu Ti�n Admin] �f�� trao B� T?ch: �e" + cp.getDisplayName() + " �7[T?ng " + chapter + "] �fcho �b" + target.getName());
            target.sendMessage("�6�l? CO DUY�N THI�N �?O ? �fNguoi nh?n du?c B� T?ch: �e" + cp.getDisplayName() + " �7[T?ng " + chapter + "]");
            return true;
        }

        if (subCmd.equals("dandoc")) {
            if (!(sender instanceof Player p)) return true;
            tutien.alchemy.model.DanDocData ddd = dataManager.getDanDocData(p);
            p.sendMessage("§e═════════ §c§lTRẠNG THÁI ĐAN ĐỘC §e═════════");
            p.sendMessage("§fTrạng thái: " + ddd.getToxicityStatus().getDisplayName());
            p.sendMessage("§fĐan độc tích tụ: §c" + String.format("%.1f", ddd.getDanDoc()) + "§7/1000.0");
            p.sendMessage("§fHiệu suất tu luyện: §e" + (int)(ddd.getToxicityStatus().getCultivationEfficiency() * 100) + "%");
            p.sendMessage("§e── Kháng Thuốc Từng Nhóm ──");
            for (tutien.alchemy.model.ResistanceGroup grp : tutien.alchemy.model.ResistanceGroup.values()) {
                double res = ddd.getResistance(grp.name());
                double eff = plugin.getDrugResistanceService().getEfficiencyMultiplier(p, grp.name());
                p.sendMessage("  §7• " + grp.getDisplayName() + ": §f" + String.format("%.1f", res) + "% §7(Hiệu lực: §a" + (int)(eff * 100) + "%§7)");
            }
            p.sendMessage("§e══════════════════════════════════");
            return true;
        }

        if (subCmd.equals("hoadoc")) {
            if (!(sender instanceof Player p)) return true;
            if (plugin.getDetoxService() == null) return true;
            if (plugin.getDetoxService().isDetoxing(p)) {
                plugin.getDetoxService().stopDetoxSession(p, "Chủ động kết thúc");
            } else {
                plugin.getDetoxService().startDetoxSession(p);
            }
            return true;
        }

        if (subCmd.equals("taytran")) {
            if (!(sender instanceof Player p)) return true;
            if (plugin.getDetoxService() == null) return true;
            plugin.getDetoxService().purifyAtSpring(p, null);
            return true;
        }

        if (subCmd.equals("givepill")) {
            if (!sender.hasPermission("tutien.admin")) {
                sender.sendMessage("§cĐạo hữu không có quyền hạn dùng lệnh này!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cSử dụng: /tutien givepill <id_đan> [số_lượng] [tên_người_chơi]");
                return true;
            }
            String pillId = args[1].toUpperCase();
            int amount = 1;
            if (args.length >= 3) {
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
            }
            Player target = (sender instanceof Player p) ? p : null;
            if (args.length >= 4) {
                target = Bukkit.getPlayer(args[3]);
            }
            if (target == null) {
                sender.sendMessage("§cKhông xác định được người nhận!");
                return true;
            }
            ItemStack item = plugin.getDanDuocManager().createPillItem(pillId, amount);
            if (item == null) {
                sender.sendMessage("§cKhông tìm thấy đan dược có ID: " + pillId);
                return true;
            }
            target.getInventory().addItem(item);
            sender.sendMessage("§aĐã trao " + amount + "x " + item.getItemMeta().getDisplayName() + " §acho " + target.getName() + "!");
            return true;
        }

        if (subCmd.equals("givegem")) {
            if (!sender.hasPermission("tutien.admin")) {
                sender.sendMessage("§cĐạo hữu không có quyền hạn dùng lệnh này!");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cSử dụng: /tutien givegem <KIM|MOC|THUY|HOA|THO> <SO_CAP|TRUNG_CAP|CAO_CAP|CUC_PHAM> [số_lượng] [tên_người_chơi]");
                return true;
            }
            tutien.khidao.model.LinhThachType type = tutien.khidao.model.LinhThachType.fromElement(args[1]);
            tutien.khidao.model.LinhThachType.Tier tier = tutien.khidao.model.LinhThachType.Tier.fromName(args[2]);
            int amount = 1;
            if (args.length >= 4) {
                try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
            }
            Player target = (sender instanceof Player p) ? p : null;
            if (args.length >= 5) {
                target = Bukkit.getPlayer(args[4]);
            }
            if (target == null) {
                sender.sendMessage("§cKhông xác định được người nhận!");
                return true;
            }
            tutien.khidao.model.LinhThachData gemData = new tutien.khidao.model.LinhThachData(type, tier);
            ItemStack item = gemData.toItemStack();
            item.setAmount(Math.max(1, amount));
            target.getInventory().addItem(item);
            sender.sendMessage("§aĐã trao " + amount + "x " + gemData.getDisplayName() + " §acho " + target.getName() + "!");
            return true;
        }

        if (subCmd.equals("reload-danduoc") || subCmd.equals("reload-alchemy")) {
            if (!sender.hasPermission("tutien.admin")) {
                sender.sendMessage("§cĐạo hữu không có quyền hạn dùng lệnh này!");
                return true;
            }
            plugin.getDanDuocManager().loadAll();
            sender.sendMessage("§a§l[Đan Đạo] §fĐã nạp lại toàn bộ cấu hình Đan Dược, Đan Độc và Kháng Thuốc thành công!");
            return true;
        }

        if (subCmd.equals("thamngo")) {
            if (!(sender instanceof Player p)) return true;
            tutien.cultivation.gui.ThamNgoGUI.open(p, plugin);
            return true;
        }

        if (subCmd.equals("skill")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length < 2) {
                p.sendMessage("§cSử dụng: /tutien skill <tên_kỹ_năng>");
                return true;
            }
            String skillId = args[1].toUpperCase();
            tutien.cultivation.service.CongPhapSkillService.SkillCastResult res = plugin.getCongPhapSkillService().castSkill(p, skillId);
            if (!res.isSuccess()) {
                p.sendMessage(res.getMessage());
            }
            return true;
        }

        if (subCmd.equals("reload-gui") || (subCmd.equals("reload") && sender.hasPermission("tutien.admin"))) {
            if (!sender.hasPermission("tutien.admin")) {
                sender.sendMessage("§cĐạo hữu không có quyền hạn dùng lệnh này!");
                return true;
            }
            plugin.getCongPhapGUIConfig().load();
            plugin.getCongPhapManager().load();
            plugin.getCongPhapSkillService().load();
            sender.sendMessage("§a§l[Tu Tiên] §fĐã nạp lại toàn bộ cấu hình GUI Công Pháp, Tham Ngộ và Kỹ Năng thành công!");
            return true;
        }
        if (subCmd.equals("xem")) {
            Player target = null;
            if (args.length == 1 && sender instanceof Player) target = (Player) sender;
            else if (args.length >= 2) target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage("§cKhông tìm thấy người chơi này!");
                return true;
            }

            int tuVi = dataManager.getTuVi(target);
            CanhGioi canhGioi = dataManager.getCanhGioi(target);
            sender.sendMessage("§eCảnh giới của §b" + target.getName() + "§e: §b" + canhGioi.getTenHienThi()
                    + " " + canhGioi.getTieuCanhGioi(dataManager.getTang(target))
                    + " §e| Tu vi: §a" + tuVi + "/" + canhGioi.getTuViYeuCau()
                    + " §e| Khoáng thạch: §6" + dataManager.getKhoangThach(target)
                    + " §e| Đạo niệm: §b" + dataManager.getDaoNiem(target));
            return true;
        }

        if (subCmd.equals("menu")) {
            if (sender instanceof Player p) MainMenu.open(p, dataManager);
            else sender.sendMessage("§cLệnh này chỉ dùng trong game.");
            return true;
        }

        if (subCmd.equals("danphong") || subCmd.equals("luyenduoc")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cLệnh này chỉ dùng trong game.");
                return true;
            }
            TuTienPlugin pl = TuTienPlugin.getPlugin(TuTienPlugin.class);
            if (args.length > 2 && args[1].equalsIgnoreCase("start")) {
                String formulaId = args[2].toUpperCase();
                tutien.alchemy.btth.service.BTTHProcessController.ProcessResult res =
                        pl.getBtthProcessController().startSession(p, formulaId);
                p.sendMessage(res.isSuccess() ? "§a" + res.getMessage() : "§c" + res.getMessage());
                if (res.isSuccess()) {
                    tutien.alchemy.btth.gui.BTTHAlchemyGUI.open(p, pl.getBtthProcessController(),
                            pl.getBtthSoulService(), pl.getBtthRankService(),
                            pl.getBtthFlameService(), pl.getBtthCauldronService());
                }
                return true;
            }
            tutien.alchemy.btth.gui.BTTHAlchemyGUI.open(p, pl.getBtthProcessController(),
                    pl.getBtthSoulService(), pl.getBtthRankService(),
                    pl.getBtthFlameService(), pl.getBtthCauldronService());
            return true;
        }

        if (subCmd.equals("luyendan")) {
            if (sender instanceof Player p) {
                if (plugin.getAlchemyService() != null && plugin.getAlchemyService().isAlchemizing(p.getUniqueId())) {
                    tutien.alchemy.gui.AlchemyMiniGameGUI.open(p, plugin.getAlchemyService());
                } else {
                    LuyenDanGUI.open(p, dataManager);
                }
            } else {
                sender.sendMessage("§cLệnh này chỉ dùng trong game.");
            }
            return true;
        }

        if (subCmd.equals("khailo")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cLệnh này chỉ dùng trong game.");
                return true;
            }
            if (args.length < 2) {
                p.sendMessage("§cDùng: /tutien khailo <mã_công_thức>");
                return true;
            }
            String recipeId = args[1].toUpperCase();
            if (plugin.getAlchemyService() != null) {
                tutien.alchemy.model.AlchemyResult res = plugin.getAlchemyService().startSession(p, recipeId);
                if (res.isSuccessful()) {
                    tutien.alchemy.gui.AlchemyMiniGameGUI.open(p, plugin.getAlchemyService());
                } else {
                    p.sendMessage("§c[Đan Lò] " + res.getMessage());
                }
            }
            return true;
        }

        if (subCmd.equals("tuido")) {
            if (!(sender instanceof Player p)) return true;
            TuiDoGUI.open(p, plugin.getTuiDoManager());
            return true;
        }

        if (subCmd.equals("homthu") || subCmd.equals("claim")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cLệnh này chỉ dùng trong game.");
                return true;
            }
            if (plugin.getAlchemyEscrowManager() != null) {
                plugin.getAlchemyEscrowManager().claimMailbox(p);
            } else {
                p.sendMessage("§c§l[Hòm Thư] §fHệ thống hòm thư hiện không khả dụng.");
            }
            return true;
        }

        if (subCmd.equals("dokiep") || subCmd.equals("dotpha")) {
            if (!(sender instanceof Player p)) return true;
            DotPhaGUI.open(p, plugin);
            return true;
        }

        if (subCmd.equals("nhapdao") || subCmd.equals("khaimach")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.NhapDaoGUI.open(p, plugin);
            return true;
        }

        if (subCmd.equals("ngukhong") || subCmd.equals("ngukiem")) {
            if (!(sender instanceof Player player)) return true;
            CanhGioi cg = dataManager.getCanhGioi(player);

            if (cg.ordinal() < CanhGioi.KIM_DAN.ordinal()) {
                player.sendMessage("§cCần đạt §bKim Đan §ftrở lên mới có thể thi triển độn thuật Ngự Không!");
                return true;
            }

            if (tutien.task.NguKiemTask.isFlying(player)) {
                tutien.task.NguKiemTask.stopFlight(player);
            } else {
                int cost = tutien.task.NguKiemTask.getCost(player, dataManager);
                if (dataManager.getLinhLuc(player) < cost) {
                    player.sendMessage("§c[Ngự Không] §fCần §e" + cost + " Linh Lực §fđể khởi động độn thuật!");
                    return true;
                }
                tutien.task.NguKiemTask.startFlight(player, dataManager);
            }
            return true;
        }

        if (subCmd.equals("bequan")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.BeQuanShopGUI.open(p, dataManager);
            return true;
        }

        if (subCmd.equals("chetac") || subCmd.equals("loren") || subCmd.equals("khidao")) {
            if (!(sender instanceof Player p)) return true;
            p.sendMessage("§e═════════ §6§lLÒ RÈN & KHÍ ĐẠO (PHASE 4) §e═════════");
            p.sendMessage("§f• §e8 Phẩm cấp: §7Phàm Khí §8-> §aPháp Khí §8-> §bLinh Khí §8-> §d§lBảo Khí §8-> §6§lCổ Bảo §8-> §c§lHậu Thiên §8-> §5§lTiên Thiên §8-> §e§lThông Thiên.");
            p.sendMessage("§f• §bKhảm Nạm: §7Linh Thạch Ngũ Hành (Kim, Mộc, Thủy, Hỏa, Thổ) tương sinh tương khắc với Boss.");
            p.sendMessage("§f• §aKhí Linh: §7Trang bị từ Cổ Bảo trở lên có cơ hội thức tỉnh Khí Linh hỗ trợ chiến đấu tự động.");
            p.sendMessage("§e═════════════════════════════════════════════════");
            tutien.tutien.gui.CheGUI.open(p, dataManager, plugin.getCheManager());
            return true;
        }

        // === PHASE 5: LINH ĐỊA & PHỤ BẢN ===
        if (subCmd.equals("dongphu")) {
            if (!(sender instanceof Player p)) return true;
            tutien.linhdia.DongPhuManager dpm = plugin.getDongPhuManager();
            if (args.length >= 2 && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("lap"))) {
                dpm.setDongPhu(p, p.getLocation());
                return true;
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("nangcap")) {
                dpm.upgradeLinhMach(p);
                return true;
            }
            tutien.linhdia.DongPhuManager.DongPhuData dp = dpm.getDongPhu(p.getUniqueId());
            p.sendMessage("§e═════════ §a§lĐỘNG PHỦ & LINH MẠCH (PHASE 5) §e═════════");
            if (dp == null) {
                p.sendMessage("§7Chưa khai lập Động Phủ. Hãy đứng tại đảo của mình và gõ: §f/tutien dongphu lap");
            } else {
                p.sendMessage("§fLinh Mạch: " + dp.tier.getDisplayName());
                p.sendMessage("§fGia tăng tốc độ Tu Vi: §a+" + (int)((dp.tier.getTuViMultiplier() - 1.0) * 100) + "%");
                p.sendMessage("§fHồi phục Linh Lực: §b+" + (int)dp.tier.getManaRegenPerSec() + " /giây");
                p.sendMessage("§fTrạng thái hiện tại: " + (dpm.isInsideDongPhu(p) ? "§aĐang ở trong Động Phủ (Nhận buff)" : "§7Ngoài phạm vi Động Phủ"));
                p.sendMessage("§eGõ §f/tutien dongphu nangcap §eđể bồi dưỡng Linh Mạch.");
            }
            p.sendMessage("§e═════════════════════════════════════════════════");
            return true;
        }

        if (subCmd.equals("bicanh")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length >= 3 && args[1].equalsIgnoreCase("vao")) {
                try {
                    tutien.dungeon.bicanh.BiCanhTier tier = tutien.dungeon.bicanh.BiCanhTier.valueOf(args[2].toUpperCase());
                    if (!plugin.getBiCanhManager().canEnter(p, tier)) {
                        p.sendMessage("§c§l[Bí Cảnh] §fCảnh giới chưa đủ! Yêu cầu: §e" + tier.getRequiredRank().getTenHienThi());
                        return true;
                    }
                    tutien.dungeon.bicanh.BiCanhSession session = plugin.getBiCanhManager().getOrCreateActiveSession(tier);
                    plugin.getBiCanhManager().joinSession(p, session.getSessionId());
                    p.sendMessage("§5§l[Bí Cảnh] §fNgươi đã tiến vào §d" + tier.getDisplayName() + "§f! Hãy tiêu diệt quái vật để đoạt bảo.");
                    return true;
                } catch (Exception e) {
                    p.sendMessage("§cSử dụng: /tutien bicanh vao <TIER_1|TIER_2|TIER_3|TIER_4|TIER_5>");
                    return true;
                }
            }
            p.sendMessage("§e═════════ §5§lBÍ CẢNH PHÂN TẦNG (PHASE 5) §e═════════");
            for (tutien.dungeon.bicanh.BiCanhTier t : tutien.dungeon.bicanh.BiCanhTier.values()) {
                boolean eligible = plugin.getBiCanhManager().canEnter(p, t);
                p.sendMessage("  " + t.getDisplayName() + " §7(Yêu cầu: §e" + t.getRequiredRank().getTenHienThi() + "§7) - " + (eligible ? "§aĐủ điều kiện" : "§cChưa đủ cảnh giới"));
            }
            p.sendMessage("§fGõ: §e/tutien bicanh vao <TIER_1..5> §fđể tiến vào Bí Cảnh.");
            p.sendMessage("§fQuy tắc: Đóng góp sát thương & đỡ đòn nhận Rương Thần Thoại / Chí Bảo.");
            p.sendMessage("§e═════════════════════════════════════════════════");
            return true;
        }

        // === PHASE 6: THIÊN KIẾP & XÃ HỘI ===
        if (subCmd.equals("daolu") || subCmd.equals("songtu")) {
            if (!(sender instanceof Player p)) return true;
            tutien.social.DaoLuService dls = plugin.getDaoLuService();
            if (args.length >= 3 && args[1].equalsIgnoreCase("ketduyen")) {
                Player partner = Bukkit.getPlayer(args[2]);
                if (partner == null || partner.equals(p)) {
                    p.sendMessage("§cĐạo hữu không hợp lệ hoặc đang offline!");
                    return true;
                }
                dls.requestPair(p, partner);
                return true;
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("chapnhan")) {
                dls.acceptPair(p);
                return true;
            } else if (args.length >= 2 && (args[1].equalsIgnoreCase("huydoan") || args[1].equalsIgnoreCase("huy"))) {
                if (!dls.hasPartner(p.getUniqueId())) {
                    p.sendMessage("§c§l[Đạo Lữ] §fNgươi chưa có Đạo Lữ để hủy kết duyên!");
                    return true;
                }
                dls.dissolveBond(p.getUniqueId());
                p.sendMessage("§d§l[Đạo Lữ] §fĐã cắt đứt duyên phận Đạo Lữ thành công.");
                return true;
            }
            tutien.social.DaoLuService.DaoLuBond bond = dls.getBond(p.getUniqueId());
            p.sendMessage("§e═════════ §d§lĐẠO LỮ SONG TU (PHASE 6) §e═════════");
            if (bond == null) {
                p.sendMessage("§7Hiện chưa kết duyên đạo lữ. Gõ: §f/tutien daolu ketduyen <tên_người_chơi>");
            } else {
                UUID partnerId = bond.getPartner(p.getUniqueId());
                String partnerName = Bukkit.getOfflinePlayer(partnerId).getName();
                p.sendMessage("§fĐạo Lữ Bản Mệnh: §d" + partnerName);
                p.sendMessage("§fĐộ Thân Mật: §e" + bond.intimacy + "/1000");
                p.sendMessage("§a• Kỹ Năng Hợp Thể: +15% Sát thương khi ở cạnh nhau.");
                p.sendMessage("§a• Song Tu Tọa Thiền: x2.0 Tốc độ Tu Vi khi cùng ngồi thiền.");
            }
            p.sendMessage("§e═════════════════════════════════════════════════");
            return true;
        }

        if (subCmd.equals("doikhoang") || subCmd.equals("khoangthach")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.KhoangThachGUI.open(p, dataManager);
            return true;
        }

        if (subCmd.equals("nhiemvu")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.NhiemVuGUI.open(p, plugin.getNhiemVuManager());
            return true;
        }

        if (subCmd.equals("linhthuvien")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.LinhThuVienGUI.open(p, dataManager, plugin.getLinhThuVienManager());
            return true;
        }

        if (subCmd.equals("gacha")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.GachaGUI.open(p, dataManager, plugin.getGachaGUI());
            return true;
        }

        if (subCmd.equals("vangioi")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.VanGioiCacGUI.open(p);
            return true;
        }

        if (subCmd.equals("xephang")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.XepHangGUI.open(p, plugin);
            return true;
        }

        if (subCmd.equals("laptong")) {
            if (!(sender instanceof Player player)) return true;

            if (args.length < 2) {
                player.sendMessage("§cSử dụng: /tutien laptong <Tên_Tông_Môn>");
                return true;
            }

            CanhGioi cg = dataManager.getCanhGioi(player);
            if (cg.ordinal() < CanhGioi.HOA_THAN.ordinal()) {
                player.sendMessage("§cNgươi chưa đủ tư cách! Phải đạt cảnh giới §d§lHóa Thần §cmới có thể lập phái!");
                return true;
            }

            if (tongMonManager.hasTongMon(player)) {
                player.sendMessage("§cNgươi đã là chủ một Tông môn rồi!");
                return true;
            }

            Location loc = player.getLocation();
            if (!loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                player.sendMessage("§c[Tông Môn] §fHãy đứng trên mặt đất vững chắc trên đảo của bạn để lập Đại Điện Tông Môn!");
                return true;
            }

            String tenTongMon = args[1];
            int centerX = loc.getBlockX();
            int centerZ = loc.getBlockZ();
            int radius = 25;

            player.sendMessage("§6§l[Tông Môn] §eĐang khai tông lập phái ngay tại vị trí hiện tại, dẫn động địa mạch tạo kết giới (Bán kính 25 block)...");
            tongMonManager.createTongMon(player, tenTongMon, centerX, centerZ, radius);
            player.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            Bukkit.broadcastMessage("§6§l[Thiên Hạ Truyền Âm] §c" + player.getName() + " §fđã tu thành chính quả, thành lập §b§l" + tenTongMon + "§f!");
            return true;
        }

        if (subCmd.equals("tongmon")) {
            if (!(sender instanceof Player player)) return true;

            if (args.length < 2) {
                player.sendMessage("§e=== §6§lQuản Lý Tông Môn §e===");
                player.sendMessage("§f/tutien tongmon moi <tên> §7- Thu nhận đệ tử");
                player.sendMessage("§f/tutien tongmon duoi <tên> §7- Trục xuất đệ tử");
                player.sendMessage("§f/tutien tongmon roi §7- Trở thành Tán Tu (Rời tông)");
                player.sendMessage("§f/tutien tongmon ve §7- Truyền tống về Tông Môn");
                player.sendMessage("§f/tutien tongmon sethome §7- Đặt lại điểm truyền tống Tông Môn");
                return true;
            }

            String action = args[1];

            if (action.equalsIgnoreCase("sethome")) {
                if (!tongMonManager.hasTongMon(player)) {
                    player.sendMessage("§cNgươi không phải là Tông Chủ!");
                    return true;
                }
                if (!player.getLocation().clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                    player.sendMessage("§cHãy đứng trên mặt đất an toàn để đặt điểm truyền tống!");
                    return true;
                }
                tongMonManager.setTongMonHome(player, player.getLocation());
                player.sendMessage("§a§l[Tông Môn] §fĐã cập nhật vị trí Đại Điện Tông Môn tại tọa độ hiện tại!");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
                return true;
            }

            if (action.equalsIgnoreCase("ve")) {
                UUID myTongMonId = tongMonManager.getTongMonIdOfPlayer(player);
                if (myTongMonId == null) {
                    player.sendMessage("§cNgươi chưa gia nhập Tông Môn nào!");
                    return true;
                }
                Location loc = tongMonManager.getTongMonLocation(myTongMonId);
                if (loc != null) {
                    player.sendMessage("§b[Trận Pháp] §fĐang truyền tống về Tông Môn...");
                    player.teleport(loc);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                } else {
                    player.sendMessage("§cVị trí Tông Môn không an toàn hoặc chưa có mặt đất! Hãy báo Tông Chủ dùng /tutien tongmon sethome.");
                }
                return true;
            }

            if (action.equalsIgnoreCase("moi")) {
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /tutien tongmon moi <tên_người_chơi>");
                    return true;
                }
                if (!tongMonManager.hasTongMon(player)) {
                    player.sendMessage("§cNgươi không phải là Tông Chủ, không có quyền thu nhận đệ tử!");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage("§cKhông tìm thấy người chơi này!");
                    return true;
                }
                if (tongMonManager.getTongMonIdOfPlayer(target) != null) {
                    player.sendMessage("§cNgười này đã có môn phái, không thể đoạt nhân tài!");
                    return true;
                }

                tongMonManager.addMember(player.getUniqueId(), target, "DE_TU");
                player.sendMessage("§aĐã thu nhận §b" + target.getName() + " §avào Tông Môn!");
                target.sendMessage("§a§l[Kỳ Ngộ] §fNgươi đã được thu nhận vào §e" + tongMonManager.getTenTongMon(player.getUniqueId()) + "§f!");
                return true;
            }

            if (action.equalsIgnoreCase("duoi")) {
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /tutien tongmon duoi <tên_người_chơi>");
                    return true;
                }
                if (!tongMonManager.hasTongMon(player)) {
                    player.sendMessage("§cNgươi không phải là Tông Chủ!");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage("§cNgười chơi này không online!");
                    return true;
                }
                if (tongMonManager.removeMember(player.getUniqueId(), target)) {
                    player.sendMessage("§cĐã trục xuất §b" + target.getName() + " §ckhỏi Tông Môn!");
                    target.sendMessage("§c§l[Nghiệt Duyên] §fNgươi đã bị trục xuất khỏi Tông Môn!");
                } else {
                    player.sendMessage("§cNgười này không thuộc Tông Môn của ngươi!");
                }
                return true;
            }

            if (action.equalsIgnoreCase("roi")) {
                UUID myTongMonId = tongMonManager.getTongMonIdOfPlayer(player);
                if (myTongMonId == null) {
                    player.sendMessage("§cNgươi vốn là Tán Tu, làm gì có Tông Môn mà rời?");
                    return true;
                }
                if (tongMonManager.hasTongMon(player)) {
                    player.sendMessage("§cNgươi là Tông Chủ, không thể rời bỏ cơ đồ!");
                    return true;
                }
                tongMonManager.removeMember(myTongMonId, player);
                player.sendMessage("§aNgươi đã rời khỏi Tông Môn, mây trôi nước chảy, tự do tự tại!");
                return true;
            }

            if (action.equalsIgnoreCase("tuyenchien")) {
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /tutien tongmon tuyenchien <tên_tông_môn_đối_thủ>");
                    return true;
                }
                if (!tongMonManager.hasTongMon(player)) {
                    player.sendMessage("§cChỉ có Tông Chủ mới có quyền phát động chiến tranh Tông Môn!");
                    return true;
                }
                String enemyName = args[2];
                UUID mySect = player.getUniqueId();
                UUID enemySect = tongMonManager.findTongMonByName(enemyName);
                if (enemySect == null || enemySect.equals(mySect)) {
                    player.sendMessage("§cKhông tìm thấy Tông Môn đối thủ hợp lệ để tuyên chiến!");
                    return true;
                }
                if (plugin.getTongMonWarManager() != null) {
                    tutien.tongmon.TongMonWarManager.SectWar war = plugin.getTongMonWarManager().declareWar(mySect, enemySect);
                    if (war != null) {
                        Bukkit.broadcastMessage("§4§l[Chiến Tranh Tông Môn] §e" + tongMonManager.getTenTongMon(mySect)
                                + " §4đã chính thức tuyên chiến với §e" + enemyName + "§4! Cuộc chiến tranh đoạt linh mạch bắt đầu!");
                    }
                }
                return true;
            }

            if (action.equalsIgnoreCase("hientang") || action.equalsIgnoreCase("tangkinhcac")) {
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /tutien tongmon hientang <tên_hoặc_id_công_pháp>");
                    return true;
                }
                String cpId = args[2].toUpperCase();
                if (plugin.getTangKinhCacService() != null) {
                    plugin.getTangKinhCacService().contributeFromPlayer(player, cpId);
                }
                return true;
            }

            if (action.equalsIgnoreCase("linhngo") || action.equalsIgnoreCase("hoc")) {
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /tutien tongmon linhngo <tên_hoặc_id_công_pháp>");
                    return true;
                }
                String cpId = args[2].toUpperCase();
                if (plugin.getTangKinhCacService() != null) {
                    plugin.getTangKinhCacService().linhNgoCongPhap(player, cpId);
                }
                return true;
            }

            if (action.equalsIgnoreCase("chiem")) {
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /tutien tongmon chiem <nodeId>");
                    return true;
                }
                UUID myTongMonId = tongMonManager.getTongMonIdOfPlayer(player);
                if (myTongMonId == null) {
                    player.sendMessage("§cNgươi chưa gia nhập Tông Môn nào!");
                    return true;
                }
                String nodeId = args[2].toUpperCase();
                if (plugin.getTongMonWarManager() != null) {
                    long cooldownRemaining = plugin.getTongMonWarManager().getCaptureCooldownRemainingMs(player.getUniqueId());
                    if (cooldownRemaining > 0) {
                        player.sendMessage("§cĐang trong thời gian chờ chiếm cứ điểm! Vui lòng đợi " + (cooldownRemaining / 1000 + 1) + " giây.");
                        return true;
                    }
                    tutien.tongmon.TongMonWarManager.TranNhanNode node = plugin.getTongMonWarManager().getNode(nodeId);
                    if (node == null) {
                        player.sendMessage("§cKhông tìm thấy cứ điểm trận nhãn " + nodeId + "!");
                        return true;
                    }
                    if (node.location != null) {
                        if (player.getLocation() == null || node.location.getWorld() == null
                                || !node.location.getWorld().equals(player.getWorld())
                                || player.getLocation().distance(node.location) > 15.0) {
                            player.sendMessage("§cNgươi phải đứng trong phạm vi 15 khối quanh Trận Nhãn để chiếm đóng!");
                            return true;
                        }
                    }
                    boolean success = plugin.getTongMonWarManager().occupyNode(myTongMonId, nodeId, 34.0);
                    if (success) {
                        plugin.getTongMonWarManager().setCaptureCooldown(player.getUniqueId());
                        player.sendMessage("§a§l[Chiến Tranh Tông Môn] §fĐã chiếm đóng cứ điểm §e" + nodeId + " §fcho Tông Môn!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
                    } else {
                        player.sendMessage("§cKhông tìm thấy chiến trường hoặc cứ điểm trận nhãn không hợp lệ!");
                    }
                }
                return true;
            }
        }

        if (subCmd.equals("boss")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("spawn")) {
                if (!sender.hasPermission("tutien.admin")) {
                    sender.sendMessage("§cĐạo hữu không có quyền hạn dùng lệnh này!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cSử dụng: /tutien boss spawn <THUONG_CO_CU_MA|CUU_U_MINH_XA|THAI_CO_THAN_LONG>");
                    return true;
                }
                try {
                    tutien.dungeon.boss.WorldBossType bossType = tutien.dungeon.boss.WorldBossType.valueOf(args[2].toUpperCase());
                    Location spawnLoc = null;
                    if (sender instanceof Player p) {
                        spawnLoc = p.getLocation();
                    } else {
                        if (Bukkit.getWorlds() != null && !Bukkit.getWorlds().isEmpty()) {
                            spawnLoc = Bukkit.getWorlds().get(0).getSpawnLocation();
                        }
                    }
                    if (spawnLoc == null) {
                        sender.sendMessage("§cKhông thể xác định vị trí triệu hồi Boss! Vui lòng thực hiện bởi Player hoặc đảm bảo thế giới đã tải.");
                        return true;
                    }
                    if (plugin.getWorldBossManager() != null) {
                        plugin.getWorldBossManager().spawnBoss(bossType, spawnLoc);
                        Bukkit.broadcastMessage("§4§l[Thế Giới Dị Biến] §e" + bossType.getDisplayName()
                                + " §4đã giáng thế! Mau chóng liên thủ tiêu diệt để đoạt cơ duyên!");
                    }
                    return true;
                } catch (Exception e) {
                    sender.sendMessage("§cLoại Boss không hợp lệ: " + args[2]);
                    return true;
                }
            }
            if (plugin.getWorldBossManager() != null && plugin.getWorldBossManager().isBossAlive()) {
                tutien.dungeon.boss.WorldBossManager.ActiveBoss b = plugin.getWorldBossManager().getCurrentBoss();
                sender.sendMessage("§6§l[Thế Giới Boss] §e" + b.type.getDisplayName() + " §7- Sinh lực: §c"
                        + (int) b.currentHealth + "§7/§c" + (int) b.type.getMaxHealth());
            } else {
                sender.sendMessage("§7Hiện không có Boss Thế Giới nào xuất hiện.");
            }
            return true;
        }

        if (subCmd.equals("admin")) {
            if (!sender.hasPermission("tutien.admin")) {
                sender.sendMessage("§cĐạo hữu chưa đủ cảnh giới (quyền hạn) để dùng pháp thuật này!");
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage("§cSử dụng: /tutien admin <tuvi/linhluc/canhgioi/linhcan/he> ...");
                return true;
            }

            String type = args[1].toLowerCase();
            String targetName = args[2];
            Player target = Bukkit.getPlayer(targetName);

            if (type.equals("tuvi")) {
                if (args.length < 5) {
                    sender.sendMessage("§cSử dụng: /tutien admin tuvi <add/set/remove> <người_chơi> <số_lượng>");
                    return true;
                }
                String action = args[2].toLowerCase();
                target = Bukkit.getPlayer(args[3]);

                if (target == null) { sender.sendMessage("§cNgười chơi không online!"); return true; }

                try {
                    int amount = Integer.parseInt(args[4]);
                    int current = dataManager.getTuVi(target);

                    if (action.equals("add")) {
                        dataManager.addTuVi(target, amount);
                        sender.sendMessage("§aĐã truyền " + amount + " tu vi cho §b" + target.getName());
                    } else if (action.equals("set")) {
                        dataManager.setTuVi(target, amount);
                        sender.sendMessage("§aĐã đặt Tu Vi của §b" + target.getName() + " §athành §e" + amount);
                    } else if (action.equals("remove")) {
                        dataManager.setTuVi(target, Math.max(0, current - amount));
                        sender.sendMessage("§aĐã phế trừ §e" + amount + " Tu Vi §acủa §b" + target.getName());
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cSố lượng Tu Vi không hợp lệ!");
                }
                return true;
            }

            if (type.equals("linhluc")) {
                if (args.length < 5) {
                    sender.sendMessage("§cSử dụng: /tutien admin linhluc <add/set/remove> <người_chơi> <số_lượng>");
                    return true;
                }
                String action = args[2].toLowerCase();
                target = Bukkit.getPlayer(args[3]);
                if (target == null) { sender.sendMessage("§cNgười chơi không online!"); return true; }

                try {
                    int amount = Integer.parseInt(args[4]);
                    int current = dataManager.getLinhLuc(target);
                    int maxLinhLuc = dataManager.getMaxLinhLucEffective(target);

                    if (action.equals("add")) {
                        dataManager.setLinhLuc(target, Math.min(maxLinhLuc, current + amount));
                        sender.sendMessage("§aĐã bơm §e" + amount + " Linh Lực §acho §b" + target.getName());
                    } else if (action.equals("set")) {
                        dataManager.setLinhLuc(target, Math.min(maxLinhLuc, amount));
                        sender.sendMessage("§aĐã đặt Linh Lực của §b" + target.getName() + " §athành §e" + amount);
                    } else if (action.equals("remove")) {
                        dataManager.setLinhLuc(target, Math.max(0, current - amount));
                        sender.sendMessage("§aĐã rút đi §e" + amount + " Linh Lực §ccủa §b" + target.getName());
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cSố lượng Linh Lực không hợp lệ!");
                }
                return true;
            }

            if (target == null) { sender.sendMessage("§cNgười chơi không online!"); return true; }
            String value = args[3].toUpperCase();

            try {
                if (type.equals("canhgioi")) {
                    CanhGioi cg = CanhGioi.valueOf(value);
                    dataManager.setCanhGioi(target, cg);
                    tutien.combat.StatsManager.applyStats(target, dataManager);
                    sender.sendMessage("§aĐã cưỡng ép nâng/hạ cảnh giới của §b" + target.getName() + " §athành §e" + cg.getTenHienThi());
                    target.sendMessage("§c§l[Thiên Đạo] §fCảnh giới của ngươi bị Thiên Đạo cưỡng chế biến đổi thành §b" + cg.getTenHienThi());
                }
                else if (type.equals("linhcan")) {
                    LinhCan lc = LinhCan.valueOf(value);
                    dataManager.setLinhCan(target, lc);
                    sender.sendMessage("§aĐã hoán cốt tẩy tủy, đổi Linh Căn của §b" + target.getName() + " §athành §e" + lc.getTenHienThi());
                    target.sendMessage("§c§l[Thiên Đạo] §fLinh căn của ngươi bị bóp méo, hóa thành " + lc.getTenHienThi());
                }
                else if (type.equals("he")) {
                    HeTuLuyen he = HeTuLuyen.valueOf(value);
                    dataManager.setHeTuLuyen(target, he);
                    tutien.combat.StatsManager.applyStats(target, dataManager);
                    sender.sendMessage("§aĐã ép §b" + target.getName() + " §atu luyện hệ §e" + he.getTenHienThi());
                } else {
                    sender.sendMessage("§cLoại dữ liệu không hợp lệ! Dùng: tuvi, linhluc, canhgioi, linhcan, he");
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cGiá trị nhập vào không tồn tại!");
            }
            return true;
        }

        sender.sendMessage("§cLệnh không hợp lệ! Gõ /tutien để xem danh sách lệnh.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("menu"); completions.add("tuluyen"); completions.add("xem");
            completions.add("book"); completions.add("guide");
            completions.add("congphap"); completions.add("thamngo"); completions.add("skill");
            completions.add("dokiep"); completions.add("ngukhong"); completions.add("ngukiem"); completions.add("luyendan");
            completions.add("laptong"); completions.add("tongmon"); completions.add("tuido");
            completions.add("bequan"); completions.add("chetac"); completions.add("doikhoang");
            completions.add("nhiemvu"); completions.add("linhthuvien"); completions.add("gacha");
            completions.add("vangioi"); completions.add("xephang"); completions.add("nhapdao"); completions.add("dandoc");
            completions.add("hoadoc"); completions.add("taytran"); completions.add("khailo");
            completions.add("homthu"); completions.add("claim"); completions.add("loren"); completions.add("khidao");
            completions.add("dongphu"); completions.add("bicanh"); completions.add("daolu"); completions.add("songtu");
            completions.add("boss");
            if (sender.hasPermission("tutien.admin")) {
                completions.add("admin");
                completions.add("givebitich");
                completions.add("reload-gui");
                completions.add("givepill");
                completions.add("givegem");
                completions.add("reload-danduoc");
            }
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("givepill") && sender.hasPermission("tutien.admin")) {
                completions.addAll(plugin.getDanDuocManager().getCatalog().keySet());
            } else if (args[0].equalsIgnoreCase("khailo")) {
                if (plugin.getAlchemyManager() != null) {
                    for (tutien.alchemy.model.AlchemyRecipe r : plugin.getAlchemyManager().getAllRecipes()) {
                        completions.add(r.getId());
                    }
                }
            } else if (args[0].equalsIgnoreCase("xem")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            } else if (args[0].equalsIgnoreCase("boss")) {
                if (sender.hasPermission("tutien.admin")) {
                    completions.add("spawn");
                }
            } else if (args[0].equalsIgnoreCase("tongmon")) {
                completions.add("moi"); completions.add("duoi"); completions.add("roi"); completions.add("ve"); completions.add("sethome");
                completions.add("chiem"); completions.add("tuyenchien"); completions.add("hientang"); completions.add("linhngo");
            } else if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("tutien.admin")) {
                completions.add("tuvi"); completions.add("linhluc"); completions.add("canhgioi"); completions.add("linhcan"); completions.add("he");
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("boss") && args[1].equalsIgnoreCase("spawn") && sender.hasPermission("tutien.admin")) {
                for (tutien.dungeon.boss.WorldBossType wbt : tutien.dungeon.boss.WorldBossType.values()) {
                    completions.add(wbt.name());
                }
            } else if (args[0].equalsIgnoreCase("tongmon") && args[1].equalsIgnoreCase("chiem")) {
                completions.add("TRAN_NHAN_1"); completions.add("TRAN_NHAN_2"); completions.add("TRAN_NHAN_3");
            } else if (args[0].equalsIgnoreCase("tongmon") && (args[1].equalsIgnoreCase("moi") || args[1].equalsIgnoreCase("duoi"))) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            } else if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("tutien.admin")) {
                if (args[1].equalsIgnoreCase("tuvi") || args[1].equalsIgnoreCase("linhluc")) {
                    completions.add("add"); completions.add("set"); completions.add("remove");
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
                }
            }
        }
        else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("tutien.admin")) {
            if (args[1].equalsIgnoreCase("tuvi") || args[1].equalsIgnoreCase("linhluc")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            } else if (args[1].equalsIgnoreCase("canhgioi")) {
                for (CanhGioi cg : CanhGioi.values()) completions.add(cg.name());
            } else if (args[1].equalsIgnoreCase("linhcan")) {
                for (LinhCan lc : LinhCan.values()) completions.add(lc.name());
            } else if (args[1].equalsIgnoreCase("he")) {
                for (HeTuLuyen he : HeTuLuyen.values()) completions.add(he.name());
            }
        }

        return completions;
    }

    private void giveGuideBook(Player p) {
        org.bukkit.inventory.ItemStack book = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle("§6§lCẩm Nang Tu Tiên");
            meta.setAuthor("§c§lThiên Đạo Chân Nhân");

            // Trang 1: Nhập Môn & Tọa Thiền
            meta.addPage(
                "§4§l╔═══════════════╗\n" +
                "§4§l  CẨM NANG TU TIÊN\n" +
                "§4§l╚═══════════════╝\n\n" +
                "§0Chào mừng đạo hữu đến với thế giới §1Tu Tiên Giới§0!\n\n" +
                "§8▶ §1§l1. TỌA THIỀN (AFK):\n" +
                "§0Dùng §2/tutien tuluyen§0 để nhập định tại chỗ. Nhận Tu Vi, Linh Lực và Điểm Bế Quan mỗi phút!\n\n" +
                "§8▶ §1§l2. MENU CHÍNH:\n" +
                "§0Gõ §2/tutien menu§0 hoặc §2/menu§0 để mở toàn bộ giao diện tu tiên."
            );

            // Trang 2: Cảnh Giới & Đột Phá
            meta.addPage(
                "§4§l╔═══════════════╗\n" +
                "§4§l ĐỘT PHÁ CẢNH GIỚI\n" +
                "§4§l╚═══════════════╝\n\n" +
                "§8▶ §1§l17 ĐẠI CẢNH GIỚI:\n" +
                "§0Từ §8Phàm Nhân§0 ➔ §aLuyện Khí§0 ➔ §bTrúc Cơ§0 ➔ §dKim Đan§0 ➔ §5Nguyên Anh§0 ... đến đỉnh cao §4§lĐạp Thiên§0!\n\n" +
                "§8▶ §1§lCÁCH ĐỘT PHÁ:\n" +
                "§0Gõ §2/tutien dokiep§0 khi đầy thanh Tu Vi. Chuẩn bị đan dược và đề phòng Cửu Đạo Lôi Kiếp giáng xuống!"
            );

            // Trang 3: Luyện Đan & Kháng Thuốc
            meta.addPage(
                "§4§l╔═══════════════╗\n" +
                "§4§l  BÁT QUÁI LUYỆN ĐAN\n" +
                "§4§l╚═══════════════╝\n\n" +
                "§8▶ §1§lLÒ LUYỆN ĐAN:\n" +
                "§0Gõ §2/tutien luyendan§0. Chứa hơn 50+ công thức đan từ Nhất Phẩm đến Cửu Phẩm.\n\n" +
                "§8▶ §1§lĐAN ĐỘC & KHÁNG THUỐC:\n" +
                "§0Uống đan tăng đan độc (§2/tutien dandoc§0). Tẩy độc tại linh tuyền hoặc dùng §2/tutien hoadoc§0 để bài trừ tạp chất."
            );

            // Trang 4: Rèn Trang Bị & Máy Tạo Đá
            meta.addPage(
                "§4§l╔═══════════════╗\n" +
                "§4§l LÒ RÈN & KHOÁNG THẠCH\n" +
                "§4§l╚═══════════════╝\n\n" +
                "§8▶ §1§lLÒ RÈN PHÁP BẢO:\n" +
                "§0Gõ §2/tutien chetac§0 để rèn vũ khí, giáp với 8 phẩm cấp (Phàm ➔ Cực Phẩm Thần Khí).\n\n" +
                "§8▶ §1§lMÁY TẠO ĐÁ SKYBLOCK:\n" +
                "§0Máy tạo đá trên đảo có tỷ lệ rơi Linh Khoáng đặc thù theo cảnh giới của bạn! Gõ §2/tutien doikhoang§0 để đổi lấy Khoáng Thạch."
            );

            // Trang 5: Ngự Không & Thần Thông
            meta.addPage(
                "§4§l╔═══════════════╗\n" +
                "§4§l NGỰ KHÔNG & KỸ NĂNG\n" +
                "§4§l╚═══════════════╝\n\n" +
                "§8▶ §1§lNGỰ KHÔNG PHI HÀNH:\n" +
                "§0Đạt §dKim Đan§0 trở lên, gõ §2/tutien ngukhong§0 để đạp mây bay lượn trên không trung!\n\n" +
                "§8▶ §1§lCÔNG PHÁP & KỸ NĂNG:\n" +
                "§0Dùng §2/tutien congphap§0 để học bí tịch, thức tỉnh thần thông tiêu diệt yêu ma trong Bí Cảnh (§2/tutien linhthuvien§0)!"
            );

            // Trang 6: Lời Khuyên & Hỗ Trợ
            meta.addPage(
                "§4§l╔═══════════════╗\n" +
                "§4§l  LỜI KHUYÊN ĐẠO ĐỒ\n" +
                "§4§l╚═══════════════╝\n\n" +
                "§8• §0Luôn điểm danh hàng ngày qua §2/reward§0 để nhận bổng lộc.\n" +
                "§8• §0Đổi bảo vật tại §2/kytrancac§0 bằng Đạo Niệm & Khoáng Thạch.\n" +
                "§8• §0Gia nhập hoặc lập Tông Môn qua §2/tutien laptong§0.\n\n" +
                "§c§oChúc đạo hữu sớm đắc đạo phi thăng, vấn đỉnh Tiên Giới!"
            );

            book.setItemMeta(meta);
        }
        p.getInventory().addItem(book);
        p.sendMessage("§a§l[Tu Tiên] §fThiên Đạo ban tặng cho ngươi một cuốn §6§lCẩm Nang Tu Tiên Giới§f! Hãy mở ra đọc kỹ.");
        p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }
}
