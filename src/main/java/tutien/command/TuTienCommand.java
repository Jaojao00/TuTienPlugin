package tutien.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
 * Đã cập nhật: Bổ sung lệnh /tutien tongmon ve (Truyền tống về tông).
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
            sender.sendMessage("§e=== §6§lHệ Thống Tu Tiên §e===");
            sender.sendMessage("§f/tutien menu §7- Mở giao diện chính");
            sender.sendMessage("§f/tutien tuluyen §7- Tọa thiền (AFK)");
            sender.sendMessage("§f/tutien dokiep §7- Đột phá cảnh giới");
            sender.sendMessage("§f/tutien ngukiem §7- Ngự kiếm phi hành (Kiếm Đan+, tiêu Linh Lực)");
            sender.sendMessage("§f/tutien luyendan §7- Lượng đan luyện");
            sender.sendMessage("§f/tutien chetac §7- Chế tác trang bị (Sprint 3)");
            sender.sendMessage("§f/tutien nhiemvu §7- Nhiệm vụ hàng ngày (Sprint 3)");
            sender.sendMessage("§f/tutien bequan §7- Cửa hàng điểm Bế Quan (Sprint 2)");
            sender.sendMessage("§f/tutien linhthuvien §7- Linh Thú Viên phụ bản (Sprint 4)");
            sender.sendMessage("§f/tutien gacha §7- Triệu hồi Linh Thú (Sprint 4)");
            sender.sendMessage("§f/tutien tongmon §7- Quản lý Tông Môn");
            sender.sendMessage("§f/tutien tuido §7- Túi Đồ Hư Không");
            sender.sendMessage("§f/tutien xem [tên] §7- Xem thông tin");

            if (sender.hasPermission("tutien.admin")) {
                sender.sendMessage("§c=== §4§lQuyền Lực Thiên Đạo (Admin) §c===");
                sender.sendMessage("§f/tutien admin tuvi <add/set/remove> <tên> <số>");
                sender.sendMessage("§f/tutien admin canhgioi <tên> <CẢNH_GIỚI>");
            }
            return true;
        }

        String subCmd = args[0].toLowerCase();

        if (subCmd.equals("tuluyen")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            boolean isMeditating = dataManager.isDangTuLuyen(p);

            if (!isMeditating) {
                dataManager.setTuLuyenMode(p, true);
                Location loc = p.getLocation();
                // Chỉ căn giữa block, không nâng Y lên nữa để tránh lơ lửng/giật
                loc.setX(loc.getBlockX() + 0.5);
                loc.setZ(loc.getBlockZ() + 0.5);
                p.teleport(loc);

                p.sendMessage("§a§l[Tu Tiên] §fNgươi đã nhập định tọa thiền. Linh khí thiên địa đang hội tụ...");
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
            sender.sendMessage("§eCảnh giới của §b" + target.getName() + "§e: §b" + canhGioi.getTenHienThi() + " §e| Tu vi: §a" + tuVi + (tiepTheo != null ? "/" + tiepTheo.getTuViYeuCau() : ""));
            return true;
        }

        if (subCmd.equals("menu")) {
            if (sender instanceof Player) MainMenu.open((Player) sender, dataManager);
            else sender.sendMessage("§cLệnh này chỉ dùng trong game.");
            return true;
        }

        // --- LỆNH LUYỆN ĐAN ---
        if (subCmd.equals("luyendan")) {
            if (sender instanceof Player) LuyenDanGUI.open((Player) sender, dataManager);
            else sender.sendMessage("§cLệnh này chỉ dùng trong game.");
            return true;
        }

        // --- LỆNH TÚI ĐỒ HƯ KHÔNG ---
        if (subCmd.equals("tuido")) {
            if (!(sender instanceof Player)) return true;
            TuiDoGUI.open((Player) sender, plugin.getTuiDoManager());
            return true;
        }

        // --- LỆNH ĐỘ KIẾP ---
        if (subCmd.equals("dokiep")) {
            if (!(sender instanceof Player)) return true;
            DotPhaGUI.open((Player) sender, plugin);
            return true;
        }

        // --- LỆNH NGỰ KIẾM (SPRINT 2 UPGRADE: tiêu Linh Lực) ---
        if (subCmd.equals("ngukiem")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            CanhGioi cg = dataManager.getCanhGioi(player);

            if (cg.ordinal() < CanhGioi.KIM_DAN.ordinal()) {
                player.sendMessage("§cCần đạt §bKim Đan §ftrở lên mới có thể ngự kiếm!");
                return true;
            }

            if (tutien.task.NguKiemTask.isFlying(player)) {
                tutien.task.NguKiemTask.stopFlight(player);
            } else {
                if (dataManager.getLinhLuc(player) < 50) {
                    player.sendMessage("§c[Ngự Kiếm] §fCần §e50 Linh Lực §fđể xuất kiếm!");
                    return true;
                }
                tutien.task.NguKiemTask.startFlight(player);
            }
            return true;
        }

        // --- LỆNH BẾ QUAN SHOP (SPRINT 2) ---
        if (subCmd.equals("bequan")) {
            if (!(sender instanceof Player)) return true;
            tutien.tutien.gui.BeQuanShopGUI.open((Player) sender, dataManager);
            return true;
        }

        // --- LỆNH CHẾ TÁC (SPRINT 3) ---
        if (subCmd.equals("chetac")) {
            if (!(sender instanceof Player)) return true;
            tutien.tutien.gui.CheGUI.open((Player) sender, dataManager, plugin.getCheManager());
            return true;
        }

        // --- LỆNH NHIỆM VỤ (SPRINT 3) ---
        if (subCmd.equals("nhiemvu")) {
            if (!(sender instanceof Player)) return true;
            tutien.tutien.gui.NhiemVuGUI.open((Player) sender, plugin.getNhiemVuManager());
            return true;
        }

        // --- LỆNH LINH THÚ VIÊN (SPRINT 4) ---
        if (subCmd.equals("linhthuvien")) {
            if (!(sender instanceof Player)) return true;
            tutien.tutien.gui.LinhThuVienGUI.open((Player) sender, dataManager, plugin.getLinhThuVienManager());
            return true;
        }

        // --- LỆNH GACHA (SPRINT 4) ---
        if (subCmd.equals("gacha")) {
            if (!(sender instanceof Player)) return true;
            tutien.tutien.gui.GachaGUI.open((Player) sender, dataManager, plugin.getGachaGUI());
            return true;
        }

        // --- LỆNH VẠN GIỚI CÁC (v2.1) ---
        if (subCmd.equals("vangioi")) {
            if (!(sender instanceof Player)) return true;
            tutien.tutien.gui.VanGioiCacGUI.open((Player) sender);
            return true;
        }

        // --- LỆNH XẾP HẠNG (v2.1) ---
        if (subCmd.equals("xephang")) {
            if (!(sender instanceof Player)) return true;
            tutien.tutien.gui.XepHangGUI.open((Player) sender, plugin);
            return true;
        }

        if (subCmd.equals("laptong")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;

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

            String tenTongMon = args[1];
            int nth = tongMonManager.getTongMonCount();
            int centerX = 10000 + (nth * 500);
            int centerZ = 10000;
            int radius = 25;

            World world = player.getWorld();
            int highestY = world.getHighestBlockYAt(centerX, centerZ);
            Location tpLoc = new Location(world, centerX + 0.5, highestY + 1, centerZ + 0.5);

            player.teleport(tpLoc);
            player.sendMessage("§6§l[Tông Môn] §eĐang khai tông lập phái, dẫn động địa mạch tạo kết giới...");

            for (int x = centerX - radius; x <= centerX + radius; x++) {
                world.getHighestBlockAt(x, centerZ - radius).getLocation().add(0, 1, 0).getBlock().setType(Material.SPRUCE_FENCE);
                world.getHighestBlockAt(x, centerZ + radius).getLocation().add(0, 1, 0).getBlock().setType(Material.SPRUCE_FENCE);
            }
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                world.getHighestBlockAt(centerX - radius, z).getLocation().add(0, 1, 0).getBlock().setType(Material.SPRUCE_FENCE);
                world.getHighestBlockAt(centerX + radius, z).getLocation().add(0, 1, 0).getBlock().setType(Material.SPRUCE_FENCE);
            }

            tongMonManager.createTongMon(player, tenTongMon, centerX, centerZ, radius);
            Bukkit.broadcastMessage("§6§l[Thiên Hạ Truyền Âm] §c" + player.getName() + " §fđã tu thành chính quả, thành lập §b§l" + tenTongMon + "§f!");
            return true;
        }

        if (subCmd.equals("tongmon")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage("§e=== §6§lQuản Lý Tông Môn §e===");
                player.sendMessage("§f/tutien tongmon moi <tên> §7- Thu nhận đệ tử");
                player.sendMessage("§f/tutien tongmon duoi <tên> §7- Trục xuất đệ tử");
                player.sendMessage("§f/tutien tongmon roi §7- Trở thành Tán Tu (Rời tông)");
                player.sendMessage("§f/tutien tongmon ve §7- Truyền tống về Tông Môn");
                return true;
            }

            String action = args[1];

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
                    player.sendMessage("§cKhông thể tìm thấy vị trí Tông Môn!");
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
                    int maxLinhLuc = dataManager.getCanhGioi(target).getMaxLinhLuc();

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
            completions.add("bequan"); completions.add("chetac"); completions.add("nhiemvu");
            completions.add("linhthuvien"); completions.add("gacha"); completions.add("vangioi");
            completions.add("xephang");
            if (sender.hasPermission("tutien.admin")) completions.add("admin");
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("xem")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            } else if (args[0].equalsIgnoreCase("tongmon")) {
                completions.add("moi"); completions.add("duoi"); completions.add("roi"); completions.add("ve");
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