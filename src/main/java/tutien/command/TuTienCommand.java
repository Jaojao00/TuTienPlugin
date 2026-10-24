package tutien.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
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
            sender.sendMessage("§f/tutien tuluyen §7- Tọa thiền tại chỗ (AFK)");
            sender.sendMessage("§f/tutien dokiep §7- Đột phá cảnh giới");
            sender.sendMessage("§f/tutien ngukiem §7- Ngự kiếm phi hành (Kim Đan+, tiêu Linh Lực)");
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
            sender.sendMessage("§f/tutien xem [tên] §7- Xem thông tin đạo hữu");

            if (sender.hasPermission("tutien.admin")) {
                sender.sendMessage("§c=== §4§lQuyền Lực Thiên Đạo (Admin) §c===");
                sender.sendMessage("§f/tutien admin tuvi <add/set/remove> <tên> <số>");
                sender.sendMessage("§f/tutien admin canhgioi <tên> <CẢNH_GIỚI>");
            }
            return true;
        }

        String subCmd = args[0].toLowerCase();

        if (subCmd.equals("tuluyen")) {
            if (!(sender instanceof Player p)) return true;
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
            CanhGioi tiepTheo = canhGioi.getCanhGioiTiepTheo();
            sender.sendMessage("§eCảnh giới của §b" + target.getName() + "§e: §b" + canhGioi.getTenHienThi()
                    + " §e| Tu vi: §a" + tuVi + (tiepTheo != null ? "/" + tiepTheo.getTuViYeuCau() : "")
                    + " §e| Khoáng thạch: §6" + dataManager.getKhoangThach(target)
                    + " §e| Đạo niệm: §b" + dataManager.getDaoNiem(target));
            return true;
        }

        if (subCmd.equals("menu")) {
            if (sender instanceof Player p) MainMenu.open(p, dataManager);
            else sender.sendMessage("§cLệnh này chỉ dùng trong game.");
            return true;
        }

        if (subCmd.equals("luyendan")) {
            if (sender instanceof Player p) LuyenDanGUI.open(p, dataManager);
            else sender.sendMessage("§cLệnh này chỉ dùng trong game.");
            return true;
        }

        if (subCmd.equals("tuido")) {
            if (!(sender instanceof Player p)) return true;
            TuiDoGUI.open(p, plugin.getTuiDoManager());
            return true;
        }

        if (subCmd.equals("dokiep") || subCmd.equals("dotpha")) {
            if (!(sender instanceof Player p)) return true;
            DotPhaGUI.open(p, plugin);
            return true;
        }

        if (subCmd.equals("ngukiem")) {
            if (!(sender instanceof Player player)) return true;
            CanhGioi cg = dataManager.getCanhGioi(player);

            if (cg.ordinal() < CanhGioi.KIM_DAN.ordinal()) {
                player.sendMessage("§cCần đạt §bKim Đan §ftrở lên mới có thể ngự kiếm!");
                return true;
            }

            if (tutien.task.NguKiemTask.isFlying(player)) {
                tutien.task.NguKiemTask.stopFlight(player);
            } else {
                int cost = tutien.task.NguKiemTask.getCost(player, dataManager);
                if (dataManager.getLinhLuc(player) < cost) {
                    player.sendMessage("§c[Ngự Kiếm] §fCần §e" + cost + " Linh Lực §fđể xuất kiếm!");
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

        if (subCmd.equals("chetac")) {
            if (!(sender instanceof Player p)) return true;
            tutien.tutien.gui.CheGUI.open(p, dataManager, plugin.getCheManager());
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
            completions.add("dokiep"); completions.add("ngukiem"); completions.add("luyendan");
            completions.add("laptong"); completions.add("tongmon"); completions.add("tuido");
            completions.add("bequan"); completions.add("chetac"); completions.add("doikhoang");
            completions.add("nhiemvu"); completions.add("linhthuvien"); completions.add("gacha");
            completions.add("vangioi"); completions.add("xephang");
            if (sender.hasPermission("tutien.admin")) completions.add("admin");
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("xem")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            } else if (args[0].equalsIgnoreCase("tongmon")) {
                completions.add("moi"); completions.add("duoi"); completions.add("roi"); completions.add("ve"); completions.add("sethome");
            } else if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("tutien.admin")) {
                completions.add("tuvi"); completions.add("linhluc"); completions.add("canhgioi"); completions.add("linhcan"); completions.add("he");
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("tongmon") && (args[1].equalsIgnoreCase("moi") || args[1].equalsIgnoreCase("duoi"))) {
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
}
