package tutien.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import tutien.command.TuTienCommand;
import tutien.command.TienDeCommand;
import tutien.event.PlayerListener;
import tutien.event.TeleportListener;
import tutien.event.ItemListener;
import tutien.event.TongMonListener;
import tutien.event.CombatListener;
import tutien.event.AutoPickupListener; // [THÊM MỚI] Bắt sự kiện nhặt đồ

import tutien.tutien.gui.GUIListener;
import tutien.tutien.gui.LuyenDanGUI;
import tutien.tutien.gui.ChonHeGUI;
import tutien.tutien.gui.VanGioiCacGUI;
import tutien.tutien.gui.DotPhaGUI;
import tutien.tutien.gui.TuiDoGUI; // [THÊM MỚI] Giao diện Túi Đồ Hư Không

import tutien.task.TuLuyenTask;
import tutien.tongmon.TongMonManager;
import tutien.inventory.TuiDoManager; // [THÊM MỚI] Quản lý dữ liệu túi đồ
import tutien.core.EconomyManager; // [THÊM MỚI] Quản lý tiền tệ Vault

import java.io.File;

/**
 * Lớp chính của Plugin - Trái tim của hệ thống Tu Tiên.
 * ĐÃ THANH TẨY: Loại bỏ hoàn toàn Vòng sáng, Ngự kiếm và Hệ thống kỹ năng chủ
 * động.
 * NHIỆM VỤ: Quản lý Tu Vi, Đột Phá, Cảnh Giới, Túi Đồ Ảo và xuất dữ liệu.
 */
public class TuTienPlugin extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private TongMonManager tongMonManager;
    private TuiDoManager tuiDoManager; // Biến quản lý túi đồ
    private EconomyManager economyManager; // Biến quản lý kinh tế Vault

    @Override
    public void onEnable() {
        // Tạo thư mục plugin nếu chưa có
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 1. Khởi tạo các Quản lý dữ liệu (Managers)
        this.playerDataManager = new PlayerDataManager(this);
        this.tongMonManager = new TongMonManager(this);
        this.tuiDoManager = new TuiDoManager(this); // Khởi tạo Không Gian Ảo
        this.economyManager = new EconomyManager(this); // Kết nối Vault Economy

        // 2. Đăng ký các Sự kiện cốt lõi (Listeners)
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new TongMonListener(this.tongMonManager), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new AutoPickupListener(this.tuiDoManager), this); // Lắng nghe
                                                                                                        // nhặt đồ

        // Đăng ký toàn bộ hệ thống Giao Diện (GUI)
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new VanGioiCacGUI(), this);
        getServer().getPluginManager().registerEvents(new LuyenDanGUI(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new ChonHeGUI(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new DotPhaGUI(this), this);
        getServer().getPluginManager().registerEvents(new TuiDoGUI(this.tuiDoManager), this); // Đăng ký GUI Túi Đồ

        // 3. Đăng ký Lệnh (Command)
        if (getCommand("tutien") != null) {
            TuTienCommand commandLogic = new TuTienCommand(this);
            getCommand("tutien").setExecutor(commandLogic);
            getCommand("tutien").setTabCompleter(commandLogic);
        }

        if (getCommand("tiende") != null) {
            TienDeCommand tienDeLogic = new TienDeCommand(this);
            getCommand("tiende").setExecutor(tienDeLogic);
            getCommand("tiende").setTabCompleter(tienDeLogic);
        }

        // 4. Kết nối với PlaceholderAPI (Xuất dữ liệu cho MMOCore / MythicMobs)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new tutien.core.TuTienExpansion(this.playerDataManager).register();
        }

        // 5. Khởi chạy Tác vụ Tọa Thiền (Không còn hiệu ứng hạt)
        new TuLuyenTask(this.playerDataManager).runTaskTimer(this, 0L, 20L);

        // 6. Nạp cấu hình Cảnh Giới từ file YML
        File canhGioiFile = new File(getDataFolder(), "canhgioi.yml");
        if (!canhGioiFile.exists()) {
            saveResource("canhgioi.yml", false);
        }
        FileConfiguration canhGioiConfig = YamlConfiguration.loadConfiguration(canhGioiFile);
        tutien.core.CanhGioi.loadFromConfig(canhGioiConfig);

        // 7. Nạp cấu hình Vạn Giới Các & Đan Dược từ file YML
        VanGioiCacGUI.loadConfig(this);
        LuyenDanGUI.loadConfig(this);

        getLogger().info("=========================================");
        getLogger().info("  TU TIÊN PLUGIN [CORE ONLY] SẴN SÀNG!   ");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            getServer().getOnlinePlayers().forEach(player -> playerDataManager.savePlayer(player));
        }
        if (tongMonManager != null) {
            tongMonManager.saveData();
        }
        if (tuiDoManager != null) {
            tuiDoManager.saveAll(); // Lưu dữ liệu toàn bộ túi đồ người chơi
        }

        getLogger().info("Dữ liệu Tu Tiên đã được lưu an toàn. Tạm biệt đạo hữu!");
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TongMonManager getTongMonManager() {
        return tongMonManager;
    }

    public TuiDoManager getTuiDoManager() {
        return tuiDoManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}