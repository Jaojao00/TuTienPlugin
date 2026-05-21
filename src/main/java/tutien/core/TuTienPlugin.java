package tutien.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import tutien.command.TuTienCommand;
import tutien.command.TienDeCommand;
import tutien.craft.CheManager;
import tutien.dungeon.LinhThuVienManager;
import tutien.event.PlayerListener;
import tutien.event.TeleportListener;
import tutien.event.ItemListener;
import tutien.event.TongMonListener;
import tutien.event.CombatListener;
import tutien.event.AutoPickupListener;
import tutien.quest.NhiemVuManager;

import tutien.tutien.gui.*;
import tutien.task.AutoSaveTask;
import tutien.task.NguKiemTask;
import tutien.task.TuLuyenTask;
import tutien.tongmon.TongMonManager;
import tutien.inventory.TuiDoManager;

import java.io.File;

/**
 * Lớp chính của Plugin — Trái tim của hệ thống Tu Tiên.
 * PHIÊN BẢN ĐẦY ĐỦ: Tích hợp Sprint 1→4.
 *   - Sprint 1: Bug fix, async save, AutoSaveTask, đan đủ 17 cảnh giới
 *   - Sprint 2: Song Tu, Ngự Kiếm v2, Bế Quan, DotKiep cooldown
 *   - Sprint 3: Chế Tác Trang Bị, Nhiệm Vụ Hàng Ngày
 *   - Sprint 4: Linh Thú Viên, Gacha Linh Thú, Boss Event
 */
public class TuTienPlugin extends JavaPlugin {

    // === Core Managers ===
    private PlayerDataManager playerDataManager;
    private TongMonManager tongMonManager;
    private TuiDoManager tuiDoManager;
    private EconomyManager economyManager;

    // === Sprint 3 Managers ===
    private CheManager cheManager;
    private NhiemVuManager nhiemVuManager;

    // === Sprint 4 Managers ===
    private LinhThuVienManager linhThuVienManager;
    private GachaGUI gachaGUI;
    private BeQuanShopGUI beQuanShopGUI;

    // === v2.1 Managers ===
    private LeaderboardManager leaderboardManager;
    private KhoangThachGUI khoangThachGUI;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // =============================================
        // 1. KHỞI TẠO MANAGERS
        // =============================================
        this.playerDataManager = new PlayerDataManager(this);
        this.tongMonManager    = new TongMonManager(this);
        this.tuiDoManager      = new TuiDoManager(this);
        this.economyManager    = new EconomyManager(this);
        this.cheManager        = new CheManager(this);
        this.nhiemVuManager    = new NhiemVuManager(playerDataManager);
        this.linhThuVienManager = new LinhThuVienManager(this);
        this.gachaGUI          = new GachaGUI(playerDataManager);
        this.beQuanShopGUI     = new BeQuanShopGUI(playerDataManager);
        this.khoangThachGUI    = new KhoangThachGUI(playerDataManager);
        this.leaderboardManager = new LeaderboardManager(this);

        // =============================================
        // 2. ĐĂNG KÝ SỰ KIỆN
        // =============================================
        // Core events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new TongMonListener(this.tongMonManager), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new AutoPickupListener(this.tuiDoManager), this);

        // GUI Listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new VanGioiCacGUI(), this);
        getServer().getPluginManager().registerEvents(new LuyenDanGUI(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new ChonHeGUI(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new DotPhaGUI(this), this);
        getServer().getPluginManager().registerEvents(new TuiDoGUI(this.tuiDoManager), this);

        // Sprint 2-4 GUI + Systems
        getServer().getPluginManager().registerEvents(beQuanShopGUI, this);
        getServer().getPluginManager().registerEvents(new CheGUI(playerDataManager, cheManager), this);
        getServer().getPluginManager().registerEvents(new NhiemVuGUI(nhiemVuManager), this);
        getServer().getPluginManager().registerEvents(new LinhThuVienGUI(playerDataManager, linhThuVienManager), this);
        getServer().getPluginManager().registerEvents(gachaGUI, this);
        getServer().getPluginManager().registerEvents(linhThuVienManager, this); // Lắng nghe EntityDeath
        getServer().getPluginManager().registerEvents(khoangThachGUI, this);
        getServer().getPluginManager().registerEvents(new XepHangGUI(), this);

        // =============================================
        // 3. ĐĂNG KÝ LỆNH
        // =============================================
        if (getCommand("tutien") != null) {
            TuTienCommand cmd = new TuTienCommand(this);
            getCommand("tutien").setExecutor(cmd);
            getCommand("tutien").setTabCompleter(cmd);
        }
        if (getCommand("tiende") != null) {
            TienDeCommand tienDe = new TienDeCommand(this);
            getCommand("tiende").setExecutor(tienDe);
            getCommand("tiende").setTabCompleter(tienDe);
        }

        // =============================================
        // 4. PLACEHOLDER API
        // =============================================
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TuTienExpansion(this.playerDataManager).register();
            getLogger().info("[PAPI] PlaceholderAPI đã kết nối!");
        }

        // =============================================
        // 5. KHỞI CHẠY TASK
        // =============================================
        // Tu Luyện Task — mỗi 1 giây
        new TuLuyenTask(this.playerDataManager).runTaskTimer(this, 0L, 20L);

        // Ngự Kiếm Task — mỗi 2 giây (40 ticks), tiêu hao Linh Lực
        new NguKiemTask(this.playerDataManager).runTaskTimer(this, 0L, 40L);

        // AutoSave Task — mỗi 5 phút (6000 ticks) ASYNC
        new AutoSaveTask(this).runTaskTimer(this, 6000L, 6000L);

        // =============================================
        // 6. NẠP CẤU HÌNH
        // =============================================
        File canhGioiFile = new File(getDataFolder(), "canhgioi.yml");
        if (!canhGioiFile.exists()) saveResource("canhgioi.yml", false);
        FileConfiguration canhGioiConfig = YamlConfiguration.loadConfiguration(canhGioiFile);
        CanhGioi.loadFromConfig(canhGioiConfig);

        VanGioiCacGUI.loadConfig(this);
        LuyenDanGUI.loadConfig(this);

        getLogger().info("═══════════════════════════════════════════════");
        getLogger().info("   TU TIÊN PLUGIN v2.1 [COMPLETE]");
        getLogger().info("   ✅ Sprint 1-4 + Khoáng Thạch + Xếp Hạng");
        getLogger().info("   ✅ 40+ Đan Dược | Leaderboard | Gacha");
        getLogger().info("═══════════════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        // Lưu dữ liệu đồng bộ khi tắt server (chấp nhận blocking vì đây là shutdown)
        if (playerDataManager != null) {
            getServer().getOnlinePlayers().forEach(p -> playerDataManager.savePlayer(p));
        }
        if (tongMonManager != null) tongMonManager.saveData();
        if (tuiDoManager != null) tuiDoManager.saveAll();

        getLogger().info("Dữ liệu Tu Tiên đã được lưu an toàn. Tạm biệt đạo hữu!");
    }

    // =============================================
    // GETTERS
    // =============================================
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public TongMonManager getTongMonManager()        { return tongMonManager; }
    public TuiDoManager getTuiDoManager()            { return tuiDoManager; }
    public EconomyManager getEconomyManager()        { return economyManager; }
    public CheManager getCheManager()                { return cheManager; }
    public NhiemVuManager getNhiemVuManager()        { return nhiemVuManager; }
    public LinhThuVienManager getLinhThuVienManager(){ return linhThuVienManager; }
    public GachaGUI getGachaGUI()                    { return gachaGUI; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public KhoangThachGUI getKhoangThachGUI()         { return khoangThachGUI; }
}