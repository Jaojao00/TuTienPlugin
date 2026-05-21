package tutien.tutien.gui;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.core.CanhGioi;
import tutien.core.EconomyManager;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.util.*;

/**
 * BÁT QUÁI ĐAN LÒ - HỆ THỐNG LUYỆN ĐAN (DATA-DRIVEN từ danduoc.yml)
 *
 * Tính năng:
 * - Luyện đan từ nguyên liệu Minecraft thật (cấu hình trong danduoc.yml)
 * - Đan dược tăng chỉ số vĩnh viễn (Sát thương, Sinh lực, Tu Vi)
 * - Nâng cấp lò luyện (dùng Vault Economy)
 * - Tự động luyện + Tự động sử dụng đan dược
 * - Buff Đan Đạo (+25% tỷ lệ thành công)
 */
public class LuyenDanGUI implements Listener {

    public static final String GUI_NAME = "§6§l🔥 Bát Quái Đan Lò 🔥";
    private static final String RECIPE_GUI_PREFIX = "§6Công Thức: ";

    private static FileConfiguration danConfig;
    private final PlayerDataManager dataManager;

    // Cache danh sách đan dược đã parse
    private static List<DanDuocData> cachedPills = new ArrayList<>();

    // Cấu hình khu vực WorldGuard
    private static boolean regionBatBuoc = false;
    private static String regionName = "dan-lo";
    private static String regionThongBao = "§c§l[Đan Lò] §fNgươi phải đến §eĐan Phòng §fmới có thể khai lò!";
    private static boolean worldGuardEnabled = false;

    public LuyenDanGUI(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Dữ liệu 1 viên đan dược (parse từ YML)
     */
    public static class DanDuocData {
        public String key;
        public String name;
        public Material material;
        public String moTa;
        public CanhGioi canhGioiMin;
        public int tyLe;
        public double bonusDamage;
        public double bonusHealth;
        public int bonusTuVi;
        public List<IngredientData> nguyenLieu = new ArrayList<>();
    }

    public static class IngredientData {
        public Material material;
        public int amount;
        public String tenViet; // Tên tiếng Việt hiển thị trong GUI

        public IngredientData(Material mat, int amount, String tenViet) {
            this.material = mat;
            this.amount = amount;
            this.tenViet = tenViet;
        }
    }

    /**
     * Nạp cấu hình đan dược từ file danduoc.yml
     */
    public static void loadConfig(TuTienPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "danduoc.yml");
        if (!file.exists())
            plugin.saveResource("danduoc.yml", false);
        danConfig = YamlConfiguration.loadConfiguration(file);

        // Parse tất cả đan dược vào cache
        cachedPills.clear();
        ConfigurationSection danSec = danConfig.getConfigurationSection("DanDuoc");
        if (danSec == null)
            return;

        for (String key : danSec.getKeys(false)) {
            ConfigurationSection pill = danSec.getConfigurationSection(key);
            if (pill == null)
                continue;

            DanDuocData data = new DanDuocData();
            data.key = key;
            data.name = pill.getString("TenHienThi", key);
            data.material = Material.matchMaterial(pill.getString("Material", "FIREWORK_STAR"));
            if (data.material == null)
                data.material = Material.FIREWORK_STAR;
            data.moTa = pill.getString("MoTa", "");
            data.tyLe = pill.getInt("TyLe", 50);
            data.bonusDamage = pill.getDouble("BonusDamage", 0);
            data.bonusHealth = pill.getDouble("BonusHealth", 0);
            data.bonusTuVi = pill.getInt("BonusTuVi", 0);

            try {
                data.canhGioiMin = CanhGioi.valueOf(pill.getString("CanhGioiMin", "PHAM_NHAN"));
            } catch (Exception e) {
                data.canhGioiMin = CanhGioi.PHAM_NHAN;
            }

            // Parse nguyên liệu (format: "MATERIAL:SoLuong:TênViệt")
            List<String> ingredients = pill.getStringList("NguyenLieu");
            for (String ing : ingredients) {
                String[] parts = ing.split(":");
                if (parts.length >= 2) {
                    Material mat = Material.matchMaterial(parts[0]);
                    if (mat != null) {
                        int amt = Integer.parseInt(parts[1]);
                        String tenViet = parts.length >= 3 ? parts[2] : formatMaterialName(mat);
                        data.nguyenLieu.add(new IngredientData(mat, amt, tenViet));
                    }
                }
            }

            cachedPills.add(data);
        }

        // Đọc cấu hình khu vực luyện đan (WorldGuard)
        regionBatBuoc = danConfig.getBoolean("KhuVucLuyenDan.BatBuoc", false);
        regionName = danConfig.getString("KhuVucLuyenDan.TenRegion", "dan-lo");
        regionThongBao = danConfig.getString("KhuVucLuyenDan.ThongBao",
                "§c§l[Đan Lò] §fNgươi phải đến §eĐan Phòng §fmới có thể khai lò!");

        // Kiểm tra WorldGuard có tồn tại không
        worldGuardEnabled = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        if (regionBatBuoc && !worldGuardEnabled) {
            plugin.getLogger().warning("[Đan Lò] KhuVucLuyenDan.BatBuoc=true nhưng WorldGuard chưa cài! Tắt kiểm tra khu vực.");
            regionBatBuoc = false;
        }
    }

    /**
     * Kiểm tra người chơi có đang đứng trong khu vực luyện đan không (WorldGuard)
     */
    public static boolean isInDanLoRegion(Player player) {
        if (!regionBatBuoc || !worldGuardEnabled)
            return true; // Không bắt buộc → cho qua

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (regions == null)
                return false;

            ApplicableRegionSet regionSet = regions.getApplicableRegions(
                    BukkitAdapter.asBlockVector(player.getLocation()));

            for (ProtectedRegion region : regionSet) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // WorldGuard lỗi → cho qua để không chặn gameplay
            return true;
        }
        return false;
    }

    /**
     * Lấy danh sách đan dược mà người chơi có thể luyện (theo cảnh giới)
     */
    private List<DanDuocData> getAvailablePills(Player player) {
        CanhGioi cg = dataManager.getCanhGioi(player);
        List<DanDuocData> result = new ArrayList<>();
        for (DanDuocData pill : cachedPills) {
            if (cg.ordinal() >= pill.canhGioiMin.ordinal()) {
                result.add(pill);
            }
        }
        return result;
    }

    // ==========================================
    // GIAO DIỆN LÒ LUYỆN ĐAN
    // ==========================================
    public static void open(Player player, PlayerDataManager data) {
        // Kiểm tra khu vực WorldGuard trước khi mở
        if (!isInDanLoRegion(player)) {
            player.sendMessage(regionThongBao);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, GUI_NAME);

        int capLo = data.getDanLoCapDo(player);
        int soLan = data.getSoLanLuyenDan(player);
        boolean isAutoLuyen = data.isAutoLuyenDan(player);
        boolean isAutoUse = data.isAutoUseDan(player);

        // === THÔNG TIN LÒ (slot 4) ===
        ItemStack infoItem = new ItemStack(Material.BLAST_FURNACE);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§6§lBát Quái Đan Lò §e[Cấp " + capLo + "]");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8━━━━━━━━━━━━━━━━━━━━━");
        infoLore.add("§7Cấp lò: §e" + capLo);
        infoLore.add("§7Số lần đã luyện: §a" + soLan);
        infoLore.add("§7Tỷ lệ bonus (cấp lò): §a+" + ((capLo - 1) * 5) + "%");
        infoLore.add("§8━━━━━━━━━━━━━━━━━━━━━");
        infoLore.add("§f§lCHỈ SỐ ĐAN DƯỢC TÍCH LŨY:");
        infoLore.add("§7 ▪ Sát thương: §c+" + String.format("%.1f", data.getBonusDamage(player)));
        infoLore.add("§7 ▪ Sinh lực: §a+" + String.format("%.1f", data.getBonusHealth(player)));
        infoLore.add("§7 ▪ Tu vi cộng thêm: §e+" + data.getBonusTuVi(player));
        infoLore.add("§8━━━━━━━━━━━━━━━━━━━━━");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inv.setItem(4, infoItem);

        // === DANH SÁCH ĐAN DƯỢC CÓ THỂ LUYỆN ===
        CanhGioi cg = data.getCanhGioi(player);
        int[] pillSlots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 23, 24, 25, 28, 29, 30, 31, 32 };
        int idx = 0;
        for (DanDuocData pill : cachedPills) {
            if (idx >= pillSlots.length)
                break;

            boolean canCraft = cg.ordinal() >= pill.canhGioiMin.ordinal();
            ItemStack pillItem = new ItemStack(canCraft ? pill.material : Material.GRAY_DYE);
            ItemMeta pMeta = pillItem.getItemMeta();
            pMeta.setDisplayName((canCraft ? "§b" : "§8§m") + pill.name);

            List<String> pLore = new ArrayList<>();
            pLore.add("§7" + pill.moTa);
            pLore.add("");
            pLore.add("§7Sát thương: §c+" + String.format("%.1f", pill.bonusDamage));
            pLore.add("§7Sinh lực: §a+" + String.format("%.1f", pill.bonusHealth));
            pLore.add("§7Tu vi: §e+" + pill.bonusTuVi);
            pLore.add("§7Tỷ lệ: §f" + pill.tyLe + "%");
            pLore.add("§7Cảnh giới: §e" + pill.canhGioiMin.getTenHienThi() + "+");
            pLore.add("");

            // Hiển thị nguyên liệu
            pLore.add("§f§lNGUYÊN LIỆU:");
            for (IngredientData ing : pill.nguyenLieu) {
                int has = countMaterial(player, ing.material);
                String color = has >= ing.amount ? "§a" : "§c";
                pLore.add(color + " ▪ " + ing.tenViet + ": " + has + "/" + ing.amount);
            }

            if (canCraft) {
                pLore.add("");
                pLore.add("§e▶ Nhấp để luyện §f| §e▶ Shift+Nhấp x10");
            } else {
                pLore.add("");
                pLore.add("§c✘ Chưa đủ cảnh giới!");
            }

            pMeta.setLore(pLore);
            pillItem.setItemMeta(pMeta);
            inv.setItem(pillSlots[idx], pillItem);
            idx++;
        }

        // === NÚT TỰ ĐỘNG LUYỆN (slot 47) ===
        ItemStack autoLuyenBtn = new ItemStack(Material.REPEATER);
        ItemMeta alMeta = autoLuyenBtn.getItemMeta();
        alMeta.setDisplayName("§b§lTự Động Luyện Đan");
        alMeta.setLore(List.of(
                "§7Trạng thái: " + (isAutoLuyen ? "§a§l✔ BẬT" : "§c§l✘ TẮT"),
                "", "§7Khi bật, sẽ tự động luyện liên tục",
                "§7khi có đủ nguyên liệu.", "",
                "§e[!] Nhấp để chuyển đổi"));
        autoLuyenBtn.setItemMeta(alMeta);
        inv.setItem(47, autoLuyenBtn);

        // === NÚT NÂNG CẤP LÒ (slot 49) - DÙNG VAULT ===
        ItemStack upgradeBtn = new ItemStack(Material.ANVIL);
        ItemMeta ugMeta = upgradeBtn.getItemMeta();
        int nextCap = capLo + 1;
        int reqMoney = getUpgradeCost(capLo);
        int reqLuyen = getUpgradeCraftCount(capLo);

        TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
        EconomyManager eco = plugin.getEconomyManager();

        ugMeta.setDisplayName("§e§lNâng Cấp Lò Luyện");
        List<String> ugLore = new ArrayList<>();
        ugLore.add("§7Cấp: §e" + capLo + " §7→ §a" + nextCap);
        ugLore.add("");
        ugLore.add("§f§lYÊU CẦU:");
        String moneyColor = (eco.isVaultEnabled() && eco.hasMoney(player, reqMoney)) ? "§a" : "§c";
        String craftColor = soLan >= reqLuyen ? "§a" : "§c";
        ugLore.add(moneyColor + " ❖ " + eco.formatMoney(reqMoney) + " " + eco.getCurrencyName());
        ugLore.add(craftColor + " ❖ " + soLan + " / " + reqLuyen + " lần luyện");
        ugLore.add("");
        ugLore.add("§7Lợi ích: §a+5% tỷ lệ thành công");
        ugLore.add("");
        ugLore.add("§e[!] Nhấp để nâng cấp");
        ugMeta.setLore(ugLore);
        upgradeBtn.setItemMeta(ugMeta);
        inv.setItem(49, upgradeBtn);

        // === NÚT TỰ ĐỘNG SỬ DỤNG (slot 51) ===
        ItemStack autoUseBtn = new ItemStack(Material.BREWING_STAND);
        ItemMeta auMeta = autoUseBtn.getItemMeta();
        auMeta.setDisplayName("§a§lTự Động Sử Dụng Đan");
        auMeta.setLore(List.of(
                "§7Trạng thái: " + (isAutoUse ? "§a§l✔ BẬT" : "§c§l✘ TẮT"),
                "", "§7Khi bật, đan luyện xong sẽ tự",
                "§7động sử dụng (cộng chỉ số vĩnh viễn).",
                "", "§c⚠ NÊN BẬT để tránh đầy túi!", "",
                "§e[!] Nhấp để chuyển đổi"));
        autoUseBtn.setItemMeta(auMeta);
        inv.setItem(51, autoUseBtn);

        fillEmpty(inv);
        player.openInventory(inv);
    }

    // ==========================================
    // XỬ LÝ SỰ KIỆN CLICK
    // ==========================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME))
            return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        int slot = event.getRawSlot();
        if (slot >= 54)
            return;

        // NÚT TỰ ĐỘNG LUYỆN
        if (slot == 47) {
            dataManager.toggleAutoLuyenDan(player);
            player.sendMessage(
                    "§b§l[Đan Lò] §fTự động luyện: " + (dataManager.isAutoLuyenDan(player) ? "§aBẬT" : "§cTẮT"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
            open(player, dataManager);
            return;
        }

        // NÚT TỰ ĐỘNG SỬ DỤNG
        if (slot == 51) {
            dataManager.toggleAutoUseDan(player);
            player.sendMessage(
                    "§a§l[Đan Lò] §fTự động sử dụng: " + (dataManager.isAutoUseDan(player) ? "§aBẬT" : "§cTẮT"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
            open(player, dataManager);
            return;
        }

        // NÚT NÂNG CẤP LÒ (VAULT)
        if (slot == 49) {
            int capLo = dataManager.getDanLoCapDo(player);
            int reqMoney = getUpgradeCost(capLo);
            int reqLuyen = getUpgradeCraftCount(capLo);
            int soLan = dataManager.getSoLanLuyenDan(player);

            TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
            EconomyManager eco = plugin.getEconomyManager();

            if (soLan < reqLuyen) {
                player.sendMessage(
                        "§c[Đan Lò] §fChưa đủ kinh nghiệm! Cần §e" + reqLuyen + " §flần (hiện: §a" + soLan + "§f).");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            if (!eco.isVaultEnabled() || !eco.hasMoney(player, reqMoney)) {
                player.sendMessage("§c[Đan Lò] §fKhông đủ tiền! Cần §e" + eco.formatMoney(reqMoney) + "§f.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            eco.takeMoney(player, reqMoney);
            dataManager.setDanLoCapDo(player, capLo + 1);
            player.sendMessage(
                    "§6§l[Đan Lò] §fĐã nâng cấp lên §eCấp " + (capLo + 1) + "§f! Tỷ lệ §a+" + (capLo * 5) + "%§f!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            open(player, dataManager);
            return;
        }

        // CLICK VÀO ĐAN DƯỢC → LUYỆN ĐAN
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;
        if (clicked.getType() == Material.GRAY_DYE)
            return; // Chưa mở khóa
        if (clicked.getType().name().contains("STAINED_GLASS_PANE"))
            return;
        if (slot == 4)
            return; // Info

        // Tìm đan dược theo tên
        String clickedName = clicked.getItemMeta().getDisplayName();
        if (clickedName.startsWith("§b")) {
            String pillName = clickedName.substring(2);
            DanDuocData pill = findPillByName(pillName);
            if (pill == null)
                return;

            // Kiểm tra cảnh giới
            CanhGioi cg = dataManager.getCanhGioi(player);
            if (cg.ordinal() < pill.canhGioiMin.ordinal()) {
                player.sendMessage("§c[Đan Lò] §fCảnh giới không đủ!");
                return;
            }

            boolean isShift = event.getClick().isShiftClick();
            int times = isShift ? 10 : 1;
            int successCount = 0, failCount = 0;

            for (int i = 0; i < times; i++) {
                // Kiểm tra nguyên liệu
                if (!hasAllIngredients(player, pill)) {
                    if (i == 0) {
                        player.sendMessage("§c[Đan Lò] §fKhông đủ nguyên liệu để luyện §b" + pill.name + "§f!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                    break;
                }

                // Trừ nguyên liệu
                removeAllIngredients(player, pill);
                dataManager.addSoLanLuyenDan(player, 1);

                // Tính tỷ lệ thành công
                int chance = pill.tyLe;
                int capLo = dataManager.getDanLoCapDo(player);
                chance += (capLo - 1) * 5;
                if (dataManager.getHeTuLuyen(player) == HeTuLuyen.DAN_TU)
                    chance += 25;
                chance = Math.min(99, chance);

                Random r = new Random();
                if (r.nextInt(100) < chance) {
                    successCount++;
                    if (dataManager.isAutoUseDan(player)) {
                        applyPillBonus(player, pill);
                    } else {
                        player.getInventory().addItem(createPillItem(pill));
                    }
                    TuTienPlugin plugin = TuTienPlugin.getPlugin(TuTienPlugin.class);
                    if (plugin.getNhiemVuManager() != null) {
                        plugin.getNhiemVuManager().addProgress(player, tutien.quest.NhiemVuManager.NhiemVu.LUYEN_DAN, 1);
                    }
                } else {
                    failCount++;
                }
            }

            // Thông báo kết quả
            if (times > 1 && (successCount + failCount) > 0) {
                player.sendMessage("§6§l[Đan Lò] §fLuyện §b" + pill.name + " §fx" + (successCount + failCount) + ": §a"
                        + successCount + " thành công §7| §c" + failCount + " thất bại");
            } else if (successCount > 0) {
                player.sendMessage("§a§l[Đan Lò] §fLuyện §b" + pill.name + " §fthành công!");
            } else if (failCount > 0) {
                player.sendMessage("§c§l[Nổ Lò!] §fDược liệu biến thành tro bụi!");
            }

            if (successCount > 0)
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            else if (failCount > 0)
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1f);

            open(player, dataManager);
        }
    }

    // ==========================================
    // HÀM HỖ TRỢ
    // ==========================================

    private static DanDuocData findPillByName(String name) {
        for (DanDuocData pill : cachedPills) {
            if (pill.name.equals(name))
                return pill;
        }
        return null;
    }

    private void applyPillBonus(Player player, DanDuocData pill) {
        dataManager.addBonusDamage(player, pill.bonusDamage);
        dataManager.addBonusHealth(player, pill.bonusHealth);
        dataManager.addBonusTuVi(player, pill.bonusTuVi);
        dataManager.addTuVi(player, pill.bonusTuVi);
        tutien.combat.StatsManager.applyStats(player, dataManager);

        player.sendMessage("§a§l[Đan Dược] §fSử dụng §b" + pill.name + " §fthành công!");
        player.sendMessage("§7  ▪ DMG: §c+" + String.format("%.1f", pill.bonusDamage)
                + " §7| HP: §a+" + String.format("%.1f", pill.bonusHealth)
                + " §7| TV: §e+" + pill.bonusTuVi);
    }

    private static boolean hasAllIngredients(Player player, DanDuocData pill) {
        for (IngredientData ing : pill.nguyenLieu) {
            if (countMaterial(player, ing.material) < ing.amount)
                return false;
        }
        return true;
    }

    private static void removeAllIngredients(Player player, DanDuocData pill) {
        for (IngredientData ing : pill.nguyenLieu) {
            removeMaterial(player, ing.material, ing.amount);
        }
    }

    private static ItemStack createPillItem(DanDuocData pill) {
        ItemStack item = new ItemStack(pill.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b" + pill.name);
        meta.setLore(List.of(
                "§7" + pill.moTa, "",
                "§7Sát thương: §c+" + String.format("%.1f", pill.bonusDamage),
                "§7Sinh lực: §a+" + String.format("%.1f", pill.bonusHealth),
                "§7Tu vi: §e+" + pill.bonusTuVi, "",
                "§e[Chuột Phải] §fđể sử dụng"));
        item.setItemMeta(meta);
        return item;
    }

    private static int countMaterial(Player player, Material mat) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat)
                count += is.getAmount();
        }
        return count;
    }

    private static void removeMaterial(Player player, Material mat, int amount) {
        int toRemove = amount;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) {
                if (is.getAmount() > toRemove) {
                    is.setAmount(is.getAmount() - toRemove);
                    return;
                } else {
                    toRemove -= is.getAmount();
                    is.setAmount(0);
                }
            }
            if (toRemove <= 0)
                break;
        }
    }

    private static String formatMaterialName(Material mat) {
        String name = mat.name().replace("_", " ");
        String[] words = name.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty())
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static int getUpgradeCost(int level) {
        if (danConfig != null && danConfig.contains("NangCapLo." + level + ".GiaTien")) {
            return danConfig.getInt("NangCapLo." + level + ".GiaTien");
        }
        return switch (level) {
            case 1 -> 25000;
            case 2 -> 75000;
            case 3 -> 200000;
            case 4 -> 500000;
            default -> 1000000;
        };
    }

    private static int getUpgradeCraftCount(int level) {
        if (danConfig != null && danConfig.contains("NangCapLo." + level + ".SoLanLuyen")) {
            return danConfig.getInt("NangCapLo." + level + ".SoLanLuyen");
        }
        return switch (level) {
            case 1 -> 2000;
            case 2 -> 5000;
            case 3 -> 15000;
            case 4 -> 50000;
            default -> 100000;
        };
    }

    private static void fillEmpty(Inventory inv) {
        ItemStack glass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(" ");
        glass.setItemMeta(m);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, glass);
        }
    }
}