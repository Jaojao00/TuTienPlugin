package tutien.alchemy.manager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tutien.alchemy.model.DanDuoc;
import tutien.alchemy.model.DanDuocEffect;
import tutien.alchemy.model.DanDuocGrade;
import tutien.core.CanhGioi;
import tutien.core.ItemKeys;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý danh mục Đan Dược từ danduoc.yml và nạp cấu hình hệ thống luyện đan.
 */
public class DanDuocManager {

    private final TuTienPlugin plugin;
    private final Map<String, DanDuoc> catalog = new ConcurrentHashMap<>();

    // Cấu hình đan độc & kháng thuốc
    private double maxToxicity = 1000.0;
    private double resistanceReductionCoeff = 0.60;
    private double minResistanceEfficiency = 0.20;

    public DanDuocManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public synchronized void loadAll() {
        if (plugin == null) {
            File resDir = new File("src/main/resources");
            if (resDir.exists()) {
                File docFile = new File(resDir, "dan_doc.yml");
                if (docFile.exists()) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(docFile);
                    this.maxToxicity = config.getDouble("dan-doc.max-toxicity", 1000.0);
                }
                File resFile = new File(resDir, "drug_resistance.yml");
                if (resFile.exists()) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(resFile);
                    this.resistanceReductionCoeff = config.getDouble("drug-resistance.reduction-coefficient", 0.60);
                    this.minResistanceEfficiency = config.getDouble("drug-resistance.min-efficiency", 0.20);
                }
            }
            return;
        }
        loadDanDocConfig();
        loadResistanceConfig();
        loadCatalog();
    }

    private void loadDanDocConfig() {
        File file = new File(plugin.getDataFolder(), "dan_doc.yml");
        if (!file.exists()) {
            plugin.saveResource("dan_doc.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.maxToxicity = config.getDouble("dan-doc.max-toxicity", 1000.0);
    }

    private void loadResistanceConfig() {
        File file = new File(plugin.getDataFolder(), "drug_resistance.yml");
        if (!file.exists()) {
            plugin.saveResource("drug_resistance.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.resistanceReductionCoeff = config.getDouble("drug-resistance.reduction-coefficient", 0.60);
        this.minResistanceEfficiency = config.getDouble("drug-resistance.min-efficiency", 0.20);
    }

    public void loadCatalog() {
        File file = new File(plugin.getDataFolder(), "danduoc.yml");
        if (!file.exists()) {
            plugin.saveResource("danduoc.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("DanDuoc");
        if (section == null) {
            section = config.getConfigurationSection("dan-duoc");
        }
        if (section == null) {
            plugin.getLogger().warning("[DanDuocManager] Khong tim thay muc DanDuoc trong danduoc.yml!");
            return;
        }

        Map<String, DanDuoc> newCatalog = new HashMap<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection pillSec = section.getConfigurationSection(key);
            if (pillSec == null) continue;

            DanDuoc pill = new DanDuoc(key);

            // Tên hiển thị
            String name = pillSec.getString("TenHienThi", pillSec.getString("display-name", key));
            pill.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            // Material
            String matName = pillSec.getString("Material", pillSec.getString("material", "POTION"));
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.POTION;
            pill.setMaterial(mat);

            // Mô tả
            String desc = pillSec.getString("MoTa", pillSec.getString("description", ""));
            pill.setMoTa(ChatColor.translateAlternateColorCodes('&', desc));

            // Cảnh giới yêu cầu
            String realmStr = pillSec.getString("CanhGioiMin", pillSec.getString("required-realm", "PHAM_NHAN"));
            try {
                pill.setRequiredRealm(CanhGioi.valueOf(realmStr.toUpperCase().trim()));
            } catch (Exception e) {
                pill.setRequiredRealm(CanhGioi.PHAM_NHAN);
            }

            // Phẩm cấp
            String gradeStr = pillSec.getString("Grade", pillSec.getString("grade", ""));
            if (gradeStr != null && !gradeStr.isEmpty()) {
                pill.setGrade(DanDuocGrade.fromString(gradeStr));
            } else {
                // Tự động suy ra phẩm cấp dựa trên cảnh giới nếu chưa khai báo
                CanhGioi cg = pill.getRequiredRealm();
                if (cg.ordinal() <= CanhGioi.LUYEN_KHI.ordinal()) pill.setGrade(DanDuocGrade.HOANG);
                else if (cg.ordinal() <= CanhGioi.TRUC_CO.ordinal()) pill.setGrade(DanDuocGrade.HUYEN);
                else if (cg.ordinal() <= CanhGioi.NGUYEN_ANH.ordinal()) pill.setGrade(DanDuocGrade.DIA);
                else if (cg.ordinal() <= CanhGioi.VAN_DINH.ordinal()) pill.setGrade(DanDuocGrade.THIEN);
                else pill.setGrade(DanDuocGrade.TIEN);
            }

            // Tỷ lệ luyện đan thành công
            pill.setSuccessRate(pillSec.getInt("TyLe", pillSec.getInt("success-rate", 60)));

            // Đan Độc
            double danDocGain = pillSec.getDouble("dan-doc.gain", pillSec.getDouble("DanDocGain", 0.0));
            if (danDocGain <= 0.0) {
                // Mặc định dựa trên phẩm cấp
                danDocGain = 5.0 * pill.getGrade().getTier();
            }
            pill.setDanDocGain(danDocGain);

            // Kháng thuốc
            String resGroup = pillSec.getString("resistance.group", pillSec.getString("ResistanceGroup", ""));
            if (resGroup == null || resGroup.isEmpty()) {
                if (key.contains("TU_KHI") || key.contains("TU_VI") || key.contains("LINH_KHI")) resGroup = "CULTIVATION";
                else if (key.contains("HOI") || key.contains("MAU") || key.contains("RECOVERY")) resGroup = "RECOVERY";
                else if (key.contains("DAN") && (key.contains("TRUC_CO") || key.contains("NGUYEN_ANH") || key.contains("HOA_THAN") || key.contains("DO_KIEP"))) resGroup = "BREAKTHROUGH";
                else if (key.contains("THE") || key.contains("COT") || key.contains("GIAP")) resGroup = "BODY";
                else if (key.contains("THAN") || key.contains("HON") || key.contains("NGO")) resGroup = "SPIRIT";
                else resGroup = "COMBAT";
            }
            pill.setResistanceGroup(resGroup);

            double resGain = pillSec.getDouble("resistance.gain", pillSec.getDouble("ResistanceGain", 0.0));
            if (resGain <= 0.0) {
                resGain = 1.0 * pill.getGrade().getTier();
            }
            pill.setResistanceGain(resGain);

            // Hiệu quả
            DanDuocEffect eff = new DanDuocEffect();
            eff.setInstantTuVi(pillSec.getInt("BonusTuVi", pillSec.getInt("effects.instant-tuvi", 0)));
            eff.setBonusDamage(pillSec.getDouble("BonusDamage", pillSec.getDouble("effects.bonus-damage", 0.0)));
            eff.setBonusHealth(pillSec.getDouble("BonusHealth", pillSec.getDouble("effects.bonus-health", 0.0)));
            eff.setInstantLinhLuc(pillSec.getInt("BonusLinhLuc", pillSec.getInt("effects.instant-linhluc", 0)));
            eff.setInstantHeal(pillSec.getDouble("BonusHeal", pillSec.getDouble("effects.instant-heal", 0.0)));
            eff.setCultivationBonus(pillSec.getDouble("effects.cultivation-bonus", 0.0));
            eff.setBreakthroughChance(pillSec.getDouble("effects.breakthrough-chance", 0.0));
            eff.setDurationSeconds(pillSec.getInt("effects.duration-seconds", pillSec.getInt("duration", 0)));
            eff.setStackPolicy(pillSec.getString("stack-policy", "REFRESH"));
            pill.setEffect(eff);

            // Nguyên liệu luyện đan
            List<String> rawIngredients = pillSec.getStringList("NguyenLieu");
            List<DanDuoc.Ingredient> ingList = new ArrayList<>();
            for (String raw : rawIngredients) {
                String[] parts = raw.split(":");
                if (parts.length >= 2) {
                    Material iMat = Material.matchMaterial(parts[0]);
                    int amount = 1;
                    try { amount = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                    String vName = parts.length >= 3 ? parts[2] : parts[0];
                    if (iMat != null) {
                        ingList.add(new DanDuoc.Ingredient(iMat, amount, vName));
                    }
                }
            }
            pill.setIngredients(ingList);

            newCatalog.put(key.toUpperCase(), pill);
        }

        catalog.clear();
        catalog.putAll(newCatalog);
        plugin.getLogger().info("[DanDuocManager] Da nap thanh cong " + catalog.size() + " loai Dan Duoc vao Catalog!");
    }

    public DanDuoc getDanDuoc(String id) {
        if (id == null) return null;
        return catalog.get(id.toUpperCase().trim());
    }

    public Map<String, DanDuoc> getCatalog() {
        return Collections.unmodifiableMap(catalog);
    }

    public double getMaxToxicity() {
        return maxToxicity;
    }

    public double getResistanceReductionCoeff() {
        return resistanceReductionCoeff;
    }

    public double getMinResistanceEfficiency() {
        return minResistanceEfficiency;
    }

    /**
     * Tạo vật phẩm Đan Dược có PDC chống giả mạo bằng đe / anvil.
     */
    public ItemStack createPillItem(String pillId, int amount) {
        DanDuoc pill = getDanDuoc(pillId);
        if (pill == null) return null;

        ItemStack item = new ItemStack(pill.getMaterial(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(pill.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("§8Phẩm cấp: " + pill.getGrade().getDisplayName());
        lore.add("§8Yêu cầu cảnh giới: §e" + pill.getRequiredRealm().getTenHienThi());
        if (!pill.getMoTa().isEmpty()) {
            lore.add("§7" + pill.getMoTa());
        }
        lore.add("§8────────────────");

        DanDuocEffect eff = pill.getEffect();
        if (eff.getInstantTuVi() > 0) lore.add("§a✦ Tăng Tu Vi: §f+" + eff.getInstantTuVi());
        if (eff.getInstantLinhLuc() > 0) lore.add("§b✦ Hồi Linh Lực: §f+" + eff.getInstantLinhLuc());
        if (eff.getBonusDamage() > 0) lore.add("§c✦ Tăng Sát Thương: §f+" + eff.getBonusDamage());
        if (eff.getBonusHealth() > 0) lore.add("§d✦ Tăng Sinh Lực: §f+" + eff.getBonusHealth());
        if (eff.getCultivationBonus() > 0) lore.add("§6✦ Hiệu Suất Tu Luyện: §f+" + (int)(eff.getCultivationBonus() * 100) + "%");

        lore.add("§8────────────────");
        lore.add("§c☠ Đan Độc: §f+" + pill.getDanDocGain());
        lore.add("§e⚖ Kháng Nhóm: §f+" + pill.getResistanceGain() + "% §7(" + pill.getResistanceGroup() + ")");
        lore.add("");
        lore.add("§e▶ Chuột phải để sử dụng");

        meta.setLore(lore);

        // Gắn PDC chống hàng giả
        ItemKeys.markMedicine(meta, pill.getId(), 1);
        meta.getPersistentDataContainer().set(ItemKeys.KEY_PILL_TUVI, org.bukkit.persistence.PersistentDataType.INTEGER, eff.getInstantTuVi());
        meta.getPersistentDataContainer().set(ItemKeys.KEY_PILL_DMG, org.bukkit.persistence.PersistentDataType.DOUBLE, eff.getBonusDamage());
        meta.getPersistentDataContainer().set(ItemKeys.KEY_PILL_HP, org.bukkit.persistence.PersistentDataType.DOUBLE, eff.getBonusHealth());

        item.setItemMeta(meta);
        return item;
    }
}
