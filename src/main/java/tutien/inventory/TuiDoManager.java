package tutien.inventory;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tutien.core.TuTienPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * HỆ THỐNG TÚI ĐỒ HƯ KHÔNG (PHIÊN BẢN HOÀN CHỈNH)
 * - Phân loại vật phẩm thông minh theo 8 nhóm (Giáp, Kiếm, Linh Thú, Thần Thông, Trang Sức, Phụ Kiện, Đan Dược, Linh Tinh)
 * - Sức chứa tối đa 300 món/ô
 * - Bán nhanh vật phẩm đổi Linh Thạch (Emerald)
 * - Tự động cất đồ khi AFK
 */
public class TuiDoManager {

    private final TuTienPlugin plugin;
    // Cấu trúc: UUID -> (Chuỗi ID tự tạo -> StoredItem chứa vật phẩm mẫu và số lượng)
    private final Map<UUID, Map<String, StoredItem>> storageMap = new HashMap<>();
    private final Map<UUID, Boolean> autoPickupMap = new HashMap<>();

    // Giới hạn số lượng tối đa mỗi ô chứa
    public static final int MAX_STACK_SIZE = 300;

    public TuiDoManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public static class StoredItem {
        public ItemStack sample;
        public int amount;
        public ItemCategory category;

        public StoredItem(ItemStack sample, int amount) {
            this.sample = sample;
            this.amount = amount;
            this.category = classifyItem(sample);
        }
    }

    // ==========================================
    // HỆ THỐNG PHÂN LOẠI VẬT PHẨM THÔNG MINH
    // ==========================================

    /**
     * 8 nhóm vật phẩm chính trong túi đồ
     */
    public enum ItemCategory {
        GIAP("§9⛨ Giáp", "§9", 0),           // Trang bị phòng thủ
        KIEM("§c⚔ Kiếm", "§c", 1),            // Vũ khí chính
        LINH_THU("§d🐾 Linh Thú", "§d", 2),   // Pet hỗ trợ
        THAN_THONG("§5✦ Thần Thông", "§5", 3), // Kỹ năng đặc biệt
        TRANG_SUC("§e✧ Trang Sức", "§e", 4),   // Bùa, vòng, nhẫn, dây chuyền
        PHU_KIEN("§b❖ Phụ Kiện", "§b", 5),     // Pháp bảo, vật tổ
        DAN_DUOC("§a✿ Đan Dược", "§a", 6),     // Thuốc, linh dược
        LINH_TINH("§7◆ Linh Tinh", "§7", 7);   // Nguyên liệu, vật phẩm sự kiện

        private final String displayName;
        private final String colorCode;
        private final int sortOrder;

        ItemCategory(String displayName, String colorCode, int sortOrder) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.sortOrder = sortOrder;
        }

        public String getDisplayName() { return displayName; }
        public String getColorCode() { return colorCode; }
        public int getSortOrder() { return sortOrder; }
    }

    /**
     * Phân loại tự động vật phẩm dựa trên tên hiển thị và loại Material
     */
    public static ItemCategory classifyItem(ItemStack item) {
        if (item == null) return ItemCategory.LINH_TINH;

        String materialName = item.getType().name().toLowerCase();
        String displayName = "";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
        }

        // --- KIẾM / VŨ KHÍ ---
        if (materialName.contains("sword") || materialName.contains("axe") || materialName.contains("trident")
                || displayName.contains("kiếm") || displayName.contains("đao") || displayName.contains("thương")
                || displayName.contains("cung") || displayName.contains("giáo") || displayName.contains("búa")
                || displayName.contains("vũ khí") || displayName.contains("trảm")) {
            return ItemCategory.KIEM;
        }

        // --- GIÁP / PHÒNG THỦ ---
        if (materialName.contains("helmet") || materialName.contains("chestplate")
                || materialName.contains("leggings") || materialName.contains("boots")
                || materialName.contains("shield") || materialName.contains("armor")
                || displayName.contains("giáp") || displayName.contains("mũ") || displayName.contains("nón")
                || displayName.contains("khiên") || displayName.contains("áo") || displayName.contains("quần")
                || displayName.contains("giày") || displayName.contains("giáp")) {
            return ItemCategory.GIAP;
        }

        // --- LINH THÚ ---
        if (displayName.contains("linh thú") || displayName.contains("thú cưng")
                || displayName.contains("thú hoang") || displayName.contains("yêu thú")
                || displayName.contains("hồ ly") || displayName.contains("rồng")
                || displayName.contains("phượng hoàng") || displayName.contains("kỳ lân")
                || materialName.contains("spawn_egg")) {
            return ItemCategory.LINH_THU;
        }

        // --- THẦN THÔNG / KỸ NĂNG ---
        if (displayName.contains("thần thông") || displayName.contains("bí kíp")
                || displayName.contains("tâm pháp") || displayName.contains("công pháp")
                || displayName.contains("quyết") || displayName.contains("thuật")
                || displayName.contains("chiêu thức") || displayName.contains("cổ thư")
                || materialName.contains("book") || materialName.contains("enchanted_book")) {
            return ItemCategory.THAN_THONG;
        }

        // --- TRANG SỨC ---
        if (displayName.contains("bùa hộ mệnh") || displayName.contains("bùa") || displayName.contains("vòng tay")
                || displayName.contains("nhẫn") || displayName.contains("dây chuyền")
                || displayName.contains("hộ phù") || displayName.contains("trang sức")
                || displayName.contains("vương miện") || displayName.contains("tiara")) {
            return ItemCategory.TRANG_SUC;
        }

        // --- PHỤ KIỆN ---
        if (displayName.contains("pháp bảo") || displayName.contains("vật tổ")
                || displayName.contains("phụ kiện") || displayName.contains("bảo vật")
                || displayName.contains("hộ thân") || displayName.contains("linh bảo")
                || displayName.contains("bảo đá") || displayName.contains("ngọc")
                || materialName.contains("totem") || materialName.contains("nether_star")) {
            return ItemCategory.PHU_KIEN;
        }

        // --- ĐAN DƯỢC ---
        if (displayName.contains("đan") || displayName.contains("dược")
                || displayName.contains("thuốc") || displayName.contains("linh dược")
                || displayName.contains("linh thảo") || displayName.contains("tiên dược")
                || materialName.contains("potion") || materialName.contains("firework_star")) {
            return ItemCategory.DAN_DUOC;
        }

        // --- MẶC ĐỊNH: LINH TINH ---
        return ItemCategory.LINH_TINH;
    }

    // ==========================================
    // HÀM TẠO ID VÀ QUẢN LÝ FILE
    // ==========================================

    /**
     * Hàm Lõi: Tự động tạo ID từ Tên hiển thị của vật phẩm
     */
    public static String generateId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return item != null ? item.getType().name().toLowerCase() : "air";
        }
        // Xóa màu và các ký tự đặc biệt để làm key lưu vào YML
        return ChatColor.stripColor(item.getItemMeta().getDisplayName())
                .replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }

    private File getFile(UUID uuid) {
        File folder = new File(plugin.getDataFolder(), "tuido");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, uuid.toString() + ".yml");
    }

    // ==========================================
    // TẢI / LƯU DỮ LIỆU
    // ==========================================

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        File file = getFile(uuid);
        Map<String, StoredItem> items = new HashMap<>();
        boolean autoPickup = false;

        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            autoPickup = config.getBoolean("AutoPickup", false);

            if (config.contains("Items")) {
                for (String id : config.getConfigurationSection("Items").getKeys(false)) {
                    ItemStack sample = config.getItemStack("Items." + id + ".sample");
                    int amount = config.getInt("Items." + id + ".amount");
                    if (sample != null) {
                        items.put(id, new StoredItem(sample, amount));
                    }
                }
            }
        }
        storageMap.put(uuid, items);
        autoPickupMap.put(uuid, autoPickup);
    }

    public void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!storageMap.containsKey(uuid)) return;

        File file = getFile(uuid);
        FileConfiguration config = new YamlConfiguration();

        config.set("AutoPickup", autoPickupMap.getOrDefault(uuid, false));
        Map<String, StoredItem> items = storageMap.get(uuid);
        for (Map.Entry<String, StoredItem> entry : items.entrySet()) {
            config.set("Items." + entry.getKey() + ".sample", entry.getValue().sample);
            config.set("Items." + entry.getKey() + ".amount", entry.getValue().amount);
        }

        try { config.save(file); } catch (IOException ignored) {}

        storageMap.remove(uuid);
        autoPickupMap.remove(uuid);
    }

    public void saveAll() {
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            savePlayer(p);
        }
    }

    // ==========================================
    // TƯƠNG TÁC VẬT PHẨM
    // ==========================================

    /**
     * Thêm vật phẩm vào túi. Giới hạn 300/ô.
     * @return Số lượng thực sự đã thêm được (phần dư sẽ bị trả về)
     */
    public int addItem(Player player, ItemStack itemToAdd, int amountToAdd) {
        UUID uuid = player.getUniqueId();
        Map<String, StoredItem> items = storageMap.computeIfAbsent(uuid, k -> new HashMap<>());
        String id = generateId(itemToAdd);

        if (items.containsKey(id)) {
            StoredItem stored = items.get(id);
            int canAdd = MAX_STACK_SIZE - stored.amount;
            if (canAdd <= 0) return 0; // Ô đã đầy

            int actualAdd = Math.min(canAdd, amountToAdd);
            stored.amount += actualAdd;
            return actualAdd;
        } else {
            int actualAdd = Math.min(MAX_STACK_SIZE, amountToAdd);
            ItemStack sample = itemToAdd.clone();
            sample.setAmount(1); // Lưu mẫu 1 cái để copy ra dùng sau này
            items.put(id, new StoredItem(sample, actualAdd));
            return actualAdd;
        }
    }

    /**
     * Rút vật phẩm ra khỏi túi
     */
    public boolean removeItem(Player player, String id, int amountToRem) {
        UUID uuid = player.getUniqueId();
        Map<String, StoredItem> items = storageMap.get(uuid);
        if (items == null || !items.containsKey(id)) return false;

        StoredItem stored = items.get(id);
        if (stored.amount >= amountToRem) {
            stored.amount -= amountToRem;
            if (stored.amount <= 0) items.remove(id);
            return true;
        }
        return false;
    }

    /**
     * Lấy danh sách vật phẩm trong túi
     */
    public Map<String, StoredItem> getItems(Player player) {
        return storageMap.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    /**
     * Lấy danh sách vật phẩm đã được sắp xếp theo nhóm
     */
    public List<Map.Entry<String, StoredItem>> getSortedItems(Player player) {
        Map<String, StoredItem> items = getItems(player);
        List<Map.Entry<String, StoredItem>> sorted = new ArrayList<>(items.entrySet());
        sorted.sort(Comparator.comparingInt(a -> a.getValue().category.getSortOrder()));
        return sorted;
    }

    /**
     * Lấy danh sách vật phẩm theo nhóm cụ thể
     */
    public List<Map.Entry<String, StoredItem>> getItemsByCategory(Player player, ItemCategory category) {
        List<Map.Entry<String, StoredItem>> result = new ArrayList<>();
        for (Map.Entry<String, StoredItem> entry : getItems(player).entrySet()) {
            if (entry.getValue().category == category) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Xóa sạch toàn bộ vật phẩm trong túi (dùng cho Bán Tất Cả)
     * @return Tổng số vật phẩm đã bán
     */
    public int clearItems(Player player) {
        UUID uuid = player.getUniqueId();
        if (storageMap.containsKey(uuid)) {
            Map<String, StoredItem> items = storageMap.get(uuid);
            int totalCount = 0;
            for (StoredItem si : items.values()) {
                totalCount += si.amount;
            }
            items.clear();
            return totalCount;
        }
        return 0;
    }

    /**
     * Tính giá bán của vật phẩm dựa trên loại nhóm
     */
    public static int getSellPrice(StoredItem storedItem) {
        return switch (storedItem.category) {
            case KIEM -> 25;       // Kiếm: 25 Linh Thạch
            case GIAP -> 20;       // Giáp: 20 Linh Thạch
            case LINH_THU -> 50;   // Linh thú: 50 Linh Thạch
            case THAN_THONG -> 30; // Thần thông: 30 Linh Thạch
            case TRANG_SUC -> 15;  // Trang sức: 15 Linh Thạch
            case PHU_KIEN -> 15;   // Phụ kiện: 15 Linh Thạch
            case DAN_DUOC -> 10;   // Đan dược: 10 Linh Thạch
            case LINH_TINH -> 5;   // Linh tinh: 5 Linh Thạch
        };
    }

    /**
     * Đếm tổng số vật phẩm trong túi
     */
    public int getTotalItemCount(Player player) {
        int total = 0;
        for (StoredItem si : getItems(player).values()) {
            total += si.amount;
        }
        return total;
    }

    /**
     * Đếm số loại vật phẩm (ô đang chiếm)
     */
    public int getUsedSlots(Player player) {
        return getItems(player).size();
    }

    // ==========================================
    // QUẢN LÝ TỰ ĐỘNG CẤT ĐỒ (AFK)
    // ==========================================

    public boolean isAutoPickup(Player player) {
        return autoPickupMap.getOrDefault(player.getUniqueId(), false);
    }

    public void toggleAutoPickup(Player player) {
        boolean currentState = isAutoPickup(player);
        autoPickupMap.put(player.getUniqueId(), !currentState);
        player.sendMessage("§b§l[Túi Đồ] §fTự động cất đồ: " + (!currentState ? "§aBẬT" : "§cTẮT"));
    }
}