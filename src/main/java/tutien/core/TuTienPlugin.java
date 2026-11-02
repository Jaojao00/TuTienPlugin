package tutien.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import tutien.alchemy.manager.DanDuocManager;
import tutien.alchemy.service.DanDocService;
import tutien.alchemy.service.DanDuocService;
import tutien.alchemy.service.DrugResistanceService;
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
    private tutien.element.LinhCanManager linhCanManager;
    private tutien.cultivation.manager.CongPhapManager congPhapManager;
    private tutien.cultivation.service.CongPhapService congPhapService;
    private tutien.cultivation.gui.CongPhapGUIConfig congPhapGUIConfig;
    private tutien.cultivation.service.CongPhapSkillService congPhapSkillService;

    // === Alchemy (Phase 3) ===
    private DanDuocManager danDuocManager;
    private DanDocService danDocService;
    private DrugResistanceService drugResistanceService;
    private DanDuocService danDuocService;
    private tutien.alchemy.service.DetoxService detoxService;
    private tutien.alchemy.manager.AlchemyManager alchemyManager;
    private tutien.alchemy.escrow.AlchemyEscrowManager alchemyEscrowManager;
    private tutien.alchemy.service.AlchemyService alchemyService;

    // === BTTH Alchemy (Đấu Phá Thương Khung) ===
    private tutien.alchemy.btth.service.SoulControlService btthSoulService;
    private tutien.alchemy.btth.service.AlchemistRankService btthRankService;
    private tutien.alchemy.btth.service.AlchemyFlameService btthFlameService;
    private tutien.alchemy.btth.service.AlchemyCauldronService btthCauldronService;
    private tutien.alchemy.btth.service.BTTHFormulaManager btthFormulaManager;
    private tutien.alchemy.btth.service.BTTHProcessController btthProcessController;

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

    // === Phase 4 Managers & Services ===
    private tutien.khidao.service.KhiDaoService khiDaoService;

    // === Phase 5: Linh Địa & Phụ Bản ===
    private tutien.linhdia.DongPhuManager dongPhuManager;
    private tutien.dungeon.bicanh.BiCanhManager biCanhManager;
    private tutien.dungeon.boss.WorldBossManager worldBossManager;

    // === Phase 6: Thiên Kiếp & Xã Hội ===
    private tutien.thienkiep.ThienKiepService thienKiepService;
    private tutien.social.DaoLuService daoLuService;
    private tutien.tongmon.TangKinhCacService tangKinhCacService;
    private tutien.tongmon.TongMonWarManager tongMonWarManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // =============================================
        // 1. KHỞI TẠO MANAGERS
        // =============================================
        this.playerDataManager = new PlayerDataManager(this);
        this.linhCanManager    = new tutien.element.LinhCanManager(this);
        this.congPhapManager   = new tutien.cultivation.manager.CongPhapManager(this);
        this.congPhapManager.load();
        this.congPhapService   = new tutien.cultivation.service.CongPhapService(this.congPhapManager);
        this.congPhapGUIConfig = new tutien.cultivation.gui.CongPhapGUIConfig(this);
        this.congPhapGUIConfig.load();
        this.congPhapSkillService = new tutien.cultivation.service.CongPhapSkillService(this);
        this.congPhapSkillService.load();
        this.danDuocManager    = new DanDuocManager(this);
        this.danDocService     = new DanDocService(this.playerDataManager, this.danDuocManager);
        this.drugResistanceService = new DrugResistanceService(this.playerDataManager, this.danDuocManager);
        this.danDuocService    = new DanDuocService(this.playerDataManager, this.danDuocManager, this.danDocService, this.drugResistanceService);
        this.detoxService      = new tutien.alchemy.service.DetoxService(this, this.playerDataManager, this.danDocService, this.danDuocManager);
        this.alchemyManager    = new tutien.alchemy.manager.AlchemyManager(this);
        this.alchemyEscrowManager = new tutien.alchemy.escrow.AlchemyEscrowManager(this);
        this.alchemyService    = new tutien.alchemy.service.AlchemyService(this, this.playerDataManager, this.alchemyManager, this.danDuocManager, this.alchemyEscrowManager);
        this.btthSoulService   = new tutien.alchemy.btth.service.SoulControlService(this.playerDataManager);
        this.btthRankService   = new tutien.alchemy.btth.service.AlchemistRankService(this.btthSoulService);
        this.btthFlameService  = new tutien.alchemy.btth.service.AlchemyFlameService();
        this.btthCauldronService = new tutien.alchemy.btth.service.AlchemyCauldronService();
        this.btthFormulaManager = new tutien.alchemy.btth.service.BTTHFormulaManager();
        this.btthProcessController = new tutien.alchemy.btth.service.BTTHProcessController(
                this, this.btthSoulService, this.btthRankService, this.btthFlameService,
                this.btthCauldronService, this.btthFormulaManager, this.alchemyEscrowManager);
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
        this.khiDaoService     = new tutien.khidao.service.KhiDaoService(this, this.playerDataManager, new tutien.element.ElementRelationService());

        // Phase 5 & 6
        this.dongPhuManager    = new tutien.linhdia.DongPhuManager(this);
        this.biCanhManager     = new tutien.dungeon.bicanh.BiCanhManager(this, this.playerDataManager);
        this.worldBossManager  = new tutien.dungeon.boss.WorldBossManager(this, this.playerDataManager);
        this.thienKiepService  = new tutien.thienkiep.ThienKiepService(this, this.playerDataManager);
        this.daoLuService      = new tutien.social.DaoLuService(this);
        this.tangKinhCacService = new tutien.tongmon.TangKinhCacService(this);
        this.tongMonWarManager = new tutien.tongmon.TongMonWarManager(this);

        // =============================================
        // 2. ĐĂNG KÝ SỰ KIỆN
        // =============================================
        // Core events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this, this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new TongMonListener(this.tongMonManager), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this.playerDataManager, this.khiDaoService), this);
        getServer().getPluginManager().registerEvents(new AutoPickupListener(this.tuiDoManager), this);

        // GUI Listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new VanGioiCacGUI(), this);
        getServer().getPluginManager().registerEvents(new LuyenDanGUI(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new ChonHeGUI(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new DotPhaGUI(this), this);
        getServer().getPluginManager().registerEvents(new NhapDaoGUI(this), this);
        getServer().getPluginManager().registerEvents(new TuiDoGUI(this.tuiDoManager), this);
        getServer().getPluginManager().registerEvents(new tutien.cultivation.gui.CongPhapGUIListener(this), this);

        // Sprint 2-4 GUI + Systems
        getServer().getPluginManager().registerEvents(beQuanShopGUI, this);
        getServer().getPluginManager().registerEvents(new CheGUI(playerDataManager, cheManager), this);
        getServer().getPluginManager().registerEvents(new NhiemVuGUI(nhiemVuManager), this);
        getServer().getPluginManager().registerEvents(new LinhThuVienGUI(playerDataManager, linhThuVienManager), this);
        getServer().getPluginManager().registerEvents(gachaGUI, this);
        getServer().getPluginManager().registerEvents(linhThuVienManager, this); // Lắng nghe EntityDeath
        getServer().getPluginManager().registerEvents(khoangThachGUI, this);
        getServer().getPluginManager().registerEvents(new XepHangGUI(), this);
        getServer().getPluginManager().registerEvents(new tutien.alchemy.gui.AlchemyMiniGameGUI(this.alchemyService), this);
        getServer().getPluginManager().registerEvents(new tutien.alchemy.btth.gui.BTTHAlchemyGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new tutien.cultivation.listener.BiTichListener(this), this);
        getServer().getPluginManager().registerEvents(new tutien.integration.mythicmobs.MythicMobDropListener(this), this);

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

        // Hóa Độc Task (Phase 3.2) — mỗi 1 giây (20 ticks)
        new tutien.alchemy.task.DetoxTask(this.detoxService, this.danDocService).runTaskTimer(this, 20L, 20L);

        // Luyện Đan Hỏa Hầu Task (Phase 3.3) — mỗi 1 giây (20 ticks)
        new tutien.alchemy.task.AlchemyTask(this.alchemyService).runTaskTimer(this, 20L, 20L);

        // Bí Cảnh Cleanup Task (Phase 5) — mỗi 20 giây (400 ticks)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (biCanhManager != null) {
                biCanhManager.cleanupExpiredSessions();
            }
        }, 400L, 400L);

        // =============================================
        // 6. NẠP CẤU HÌNH
        // =============================================
        File canhGioiFile = new File(getDataFolder(), "canhgioi.yml");
        if (!canhGioiFile.exists()) saveResource("canhgioi.yml", false);
        FileConfiguration canhGioiConfig = YamlConfiguration.loadConfiguration(canhGioiFile);
        CanhGioi.loadFromConfig(canhGioiConfig);

        if (!new File(getDataFolder(), "gui_congphap.yml").exists()) saveResource("gui_congphap.yml", false);
        if (!new File(getDataFolder(), "gui_thamngo.yml").exists()) saveResource("gui_thamngo.yml", false);
        if (!new File(getDataFolder(), "cultivation.yml").exists()) saveResource("cultivation.yml", false);
        if (!new File(getDataFolder(), "skills.yml").exists()) saveResource("skills.yml", false);
        if (!new File(getDataFolder(), "dan_doc.yml").exists()) saveResource("dan_doc.yml", false);
        if (!new File(getDataFolder(), "drug_resistance.yml").exists()) saveResource("drug_resistance.yml", false);
        if (!new File(getDataFolder(), "detox.yml").exists()) saveResource("detox.yml", false);
        if (!new File(getDataFolder(), "luyen_dan.yml").exists()) saveResource("luyen_dan.yml", false);

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
        if (alchemyService != null) {
            alchemyService.onDisable();
        }

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
    public tutien.element.LinhCanManager getLinhCanManager() { return linhCanManager; }
    public tutien.cultivation.manager.CongPhapManager getCongPhapManager() { return congPhapManager; }
    public tutien.cultivation.service.CongPhapService getCongPhapService() { return congPhapService; }
    public tutien.cultivation.gui.CongPhapGUIConfig getCongPhapGUIConfig() { return congPhapGUIConfig; }
    public tutien.cultivation.service.CongPhapSkillService getCongPhapSkillService() { return congPhapSkillService; }
    public DanDuocManager getDanDuocManager() { return danDuocManager; }
    public DanDocService getDanDocService() { return danDocService; }
    public DrugResistanceService getDrugResistanceService() { return drugResistanceService; }
    public DanDuocService getDanDuocService() { return danDuocService; }
    public tutien.alchemy.service.DetoxService getDetoxService() { return detoxService; }
    public tutien.alchemy.manager.AlchemyManager getAlchemyManager() { return alchemyManager; }
    public tutien.alchemy.escrow.AlchemyEscrowManager getAlchemyEscrowManager() { return alchemyEscrowManager; }
    public tutien.alchemy.service.AlchemyService getAlchemyService() { return alchemyService; }
    public tutien.khidao.service.KhiDaoService getKhiDaoService() { return khiDaoService; }

    // Phase 5 & 6 Getters
    public tutien.linhdia.DongPhuManager getDongPhuManager() { return dongPhuManager; }
    public tutien.dungeon.bicanh.BiCanhManager getBiCanhManager() { return biCanhManager; }
    public tutien.dungeon.boss.WorldBossManager getWorldBossManager() { return worldBossManager; }
    public tutien.thienkiep.ThienKiepService getThienKiepService() { return thienKiepService; }
    public tutien.social.DaoLuService getDaoLuService() { return daoLuService; }
    public tutien.tongmon.TangKinhCacService getTangKinhCacService() { return tangKinhCacService; }
    public tutien.tongmon.TongMonWarManager getTongMonWarManager() { return tongMonWarManager; }
    public tutien.alchemy.btth.service.SoulControlService getBtthSoulService() { return btthSoulService; }
    public tutien.alchemy.btth.service.AlchemistRankService getBtthRankService() { return btthRankService; }
    public tutien.alchemy.btth.service.AlchemyFlameService getBtthFlameService() { return btthFlameService; }
    public tutien.alchemy.btth.service.AlchemyCauldronService getBtthCauldronService() { return btthCauldronService; }
    public tutien.alchemy.btth.service.BTTHFormulaManager getBtthFormulaManager() { return btthFormulaManager; }
    public tutien.alchemy.btth.service.BTTHProcessController getBtthProcessController() { return btthProcessController; }
}