package tutien.cultivation.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Trình quản lý nạp và xác thực cấu hình YAML cho hệ thống GUI Công Pháp & Tham Ngộ (Phase 2.4).
 * Hỗ trợ nạp nóng không cần khởi động lại máy chủ (reload an toàn).
 */
public class CongPhapGUIConfig {

    private final TuTienPlugin plugin;

    // GUI Công Pháp
    private String congPhapTitle = "§8✦ Tàng Kinh Các - Công Pháp ✦";
    private int congPhapSize = 54;
    private int congPhapPrimarySlot = 4;
    private List<Integer> congPhapAuxiliarySlots = Arrays.asList(11, 13, 15);
    private int congPhapListStart = 19;
    private int congPhapListEnd = 34;
    private int congPhapPrevPageSlot = 47;
    private int congPhapNextPageSlot = 51;
    private int congPhapOpenComprehensionSlot = 49;
    private int congPhapBackSlot = 45;

    // GUI Tham Ngộ
    private String thamNgoTitle = "§8✦ Tọa Thiền Tham Ngộ Khẩu Quyết ✦";
    private int thamNgoSize = 45;
    private int thamNgoCurrentTechSlot = 13;
    private int thamNgoProgressBarSlot = 22;
    private int thamNgoDaoNgo10Slot = 29;
    private int thamNgoDaoNgo50Slot = 31;
    private int thamNgoToggleSlot = 33;
    private int thamNgoBackSlot = 40;

    private String progressSymbolFilled = "■";
    private String progressSymbolEmpty = "□";
    private int progressLength = 20;
    private String progressColorFilled = "§a";
    private String progressColorEmpty = "§7";

    public CongPhapGUIConfig(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Nạp cấu hình từ disk (hoặc resources).
     */
    public void load() {
        loadCongPhapGUI();
        loadThamNgoGUI();
    }

    private void loadCongPhapGUI() {
        File file = new File(plugin.getDataFolder(), "gui_congphap.yml");
        if (!file.exists()) {
            try { plugin.saveResource("gui_congphap.yml", false); }
            catch (Exception ignored) {}
        }

        FileConfiguration config;
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            InputStream in = plugin.getResource("gui_congphap.yml");
            if (in == null) return;
            config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        String title = config.getString("gui.title", congPhapTitle);
        int size = config.getInt("gui.size", congPhapSize);
        if (size <= 0 || size % 9 != 0 || size > 54) size = 54;

        this.congPhapTitle = title.replace('&', '§');
        this.congPhapSize = size;

        this.congPhapPrimarySlot = clampSlot(config.getInt("gui.slots.primary", 4), size);
        List<Integer> aux = config.getIntegerList("gui.slots.auxiliary");
        if (!aux.isEmpty()) {
            List<Integer> valid = new ArrayList<>();
            for (int s : aux) if (s >= 0 && s < size) valid.add(s);
            if (!valid.isEmpty()) this.congPhapAuxiliarySlots = valid;
        }

        this.congPhapListStart = clampSlot(config.getInt("gui.slots.technique-list.start", 19), size);
        this.congPhapListEnd = clampSlot(config.getInt("gui.slots.technique-list.end", 34), size);
        this.congPhapPrevPageSlot = clampSlot(config.getInt("gui.slots.previous-page", 47), size);
        this.congPhapNextPageSlot = clampSlot(config.getInt("gui.slots.next-page", 51), size);
        this.congPhapOpenComprehensionSlot = clampSlot(config.getInt("gui.slots.open-comprehension", 49), size);
        this.congPhapBackSlot = clampSlot(config.getInt("gui.slots.back-main-menu", 45), size);
    }

    private void loadThamNgoGUI() {
        File file = new File(plugin.getDataFolder(), "gui_thamngo.yml");
        if (!file.exists()) {
            try { plugin.saveResource("gui_thamngo.yml", false); }
            catch (Exception ignored) {}
        }

        FileConfiguration config;
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            InputStream in = plugin.getResource("gui_thamngo.yml");
            if (in == null) return;
            config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        String title = config.getString("gui.title", thamNgoTitle);
        int size = config.getInt("gui.size", thamNgoSize);
        if (size <= 0 || size % 9 != 0 || size > 54) size = 45;

        this.thamNgoTitle = title.replace('&', '§');
        this.thamNgoSize = size;

        this.thamNgoCurrentTechSlot = clampSlot(config.getInt("gui.slots.current-technique", 13), size);
        this.thamNgoProgressBarSlot = clampSlot(config.getInt("gui.slots.progress-bar", 22), size);
        this.thamNgoDaoNgo10Slot = clampSlot(config.getInt("gui.slots.consume-daongo-10", 29), size);
        this.thamNgoDaoNgo50Slot = clampSlot(config.getInt("gui.slots.consume-daongo-50", 31), size);
        this.thamNgoToggleSlot = clampSlot(config.getInt("gui.slots.toggle-meditation", 33), size);
        this.thamNgoBackSlot = clampSlot(config.getInt("gui.slots.back-congphap", 40), size);

        this.progressSymbolFilled = config.getString("gui.progress-bar.symbol-filled", "■");
        this.progressSymbolEmpty = config.getString("gui.progress-bar.symbol-empty", "□");
        this.progressLength = Math.max(5, Math.min(30, config.getInt("gui.progress-bar.length", 20)));
        this.progressColorFilled = config.getString("gui.progress-bar.color-filled", "§a").replace('&', '§');
        this.progressColorEmpty = config.getString("gui.progress-bar.color-empty", "§7").replace('&', '§');
    }

    /**
     * Tạo chuỗi thanh tiến độ Unicode trực quan.
     */
    public String renderProgressBar(double percent) {
        double clamped = Math.max(0.0, Math.min(100.0, percent));
        int filledCount = (int) Math.round((clamped / 100.0) * progressLength);
        int emptyCount = progressLength - filledCount;

        StringBuilder sb = new StringBuilder();
        sb.append(progressColorFilled);
        for (int i = 0; i < filledCount; i++) sb.append(progressSymbolFilled);
        sb.append(progressColorEmpty);
        for (int i = 0; i < emptyCount; i++) sb.append(progressSymbolEmpty);
        sb.append(" §f").append(String.format("%.1f%%", clamped));
        return sb.toString();
    }

    public static Material safeMaterial(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) return fallback;
        Material mat = Material.matchMaterial(name.trim().toUpperCase());
        return mat != null ? mat : fallback;
    }

    private static int clampSlot(int slot, int maxExclusive) {
        if (slot < 0) return 0;
        if (slot >= maxExclusive) return maxExclusive - 1;
        return slot;
    }

    // Getters
    public String getCongPhapTitle() { return congPhapTitle; }
    public int getCongPhapSize() { return congPhapSize; }
    public int getCongPhapPrimarySlot() { return congPhapPrimarySlot; }
    public List<Integer> getCongPhapAuxiliarySlots() { return congPhapAuxiliarySlots; }
    public int getCongPhapListStart() { return congPhapListStart; }
    public int getCongPhapListEnd() { return congPhapListEnd; }
    public int getCongPhapPrevPageSlot() { return congPhapPrevPageSlot; }
    public int getCongPhapNextPageSlot() { return congPhapNextPageSlot; }
    public int getCongPhapOpenComprehensionSlot() { return congPhapOpenComprehensionSlot; }
    public int getCongPhapBackSlot() { return congPhapBackSlot; }

    public String getThamNgoTitle() { return thamNgoTitle; }
    public int getThamNgoSize() { return thamNgoSize; }
    public int getThamNgoCurrentTechSlot() { return thamNgoCurrentTechSlot; }
    public int getThamNgoProgressBarSlot() { return thamNgoProgressBarSlot; }
    public int getThamNgoDaoNgo10Slot() { return thamNgoDaoNgo10Slot; }
    public int getThamNgoDaoNgo50Slot() { return thamNgoDaoNgo50Slot; }
    public int getThamNgoToggleSlot() { return thamNgoToggleSlot; }
    public int getThamNgoBackSlot() { return thamNgoBackSlot; }
}
