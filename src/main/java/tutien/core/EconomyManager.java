package tutien.core;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Quản lý kết nối với Vault Economy.
 * Cung cấp các phương thức tiện ích để kiểm tra số dư, trừ tiền, cộng tiền.
 * Vault sẽ tự kết nối với plugin kinh tế đang chạy trên server (EssentialsX, CMI, iConomy, v.v.)
 */
public class EconomyManager {

    private final TuTienPlugin plugin;
    private Economy economy;
    private boolean vaultEnabled = false;

    public EconomyManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        setupVault();
    }

    /**
     * Kết nối với Vault Economy Provider.
     * Gọi khi plugin được bật (onEnable).
     */
    private void setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("§c[Kinh Tế] Vault không được tìm thấy! Hệ thống tiền tệ sẽ không hoạt động.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("§c[Kinh Tế] Không tìm thấy plugin kinh tế nào (EssentialsX, CMI...). Hãy cài đặt một plugin kinh tế!");
            return;
        }

        economy = rsp.getProvider();
        vaultEnabled = true;
        plugin.getLogger().info("§a[Kinh Tế] Kết nối Vault thành công! Provider: " + economy.getName());
    }

    /**
     * Kiểm tra Vault đã được kết nối chưa.
     */
    public boolean isVaultEnabled() {
        return vaultEnabled && economy != null;
    }

    /**
     * Lấy số dư tiền của người chơi.
     * @param player Người chơi cần kiểm tra
     * @return Số dư hiện tại, hoặc 0 nếu Vault chưa kết nối
     */
    public double getBalance(Player player) {
        if (!isVaultEnabled()) return 0;
        return economy.getBalance(player);
    }

    /**
     * Kiểm tra người chơi có đủ tiền hay không.
     * @param player Người chơi cần kiểm tra
     * @param amount Số tiền cần kiểm tra
     * @return true nếu đủ tiền, false nếu không
     */
    public boolean hasMoney(Player player, double amount) {
        if (!isVaultEnabled()) return false;
        return economy.has(player, amount);
    }

    /**
     * Trừ tiền của người chơi.
     * @param player Người chơi bị trừ tiền
     * @param amount Số tiền cần trừ
     * @return true nếu giao dịch thành công
     */
    public boolean takeMoney(Player player, double amount) {
        if (!isVaultEnabled()) return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Cộng tiền cho người chơi.
     * @param player Người chơi được cộng tiền
     * @param amount Số tiền cần cộng
     * @return true nếu giao dịch thành công
     */
    public boolean giveMoney(Player player, double amount) {
        if (!isVaultEnabled()) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Lấy tên đơn vị tiền tệ (số nhiều).
     * Ví dụ: "Dollars", "Linh Thạch", v.v. (tùy theo cấu hình plugin kinh tế)
     */
    public String getCurrencyName() {
        if (!isVaultEnabled()) return "Linh Thạch";
        return economy.currencyNamePlural();
    }

    /**
     * Lấy tên đơn vị tiền tệ (số ít).
     */
    public String getCurrencyNameSingular() {
        if (!isVaultEnabled()) return "Linh Thạch";
        return economy.currencyNameSingular();
    }

    /**
     * Format số tiền theo định dạng của plugin kinh tế.
     * @param amount Số tiền cần format
     * @return Chuỗi đã format (ví dụ: "1,000.00")
     */
    public String formatMoney(double amount) {
        if (!isVaultEnabled()) return String.format("%.0f", amount);
        return economy.format(amount);
    }

    /**
     * Lấy Economy provider trực tiếp (dùng cho các trường hợp đặc biệt).
     */
    public Economy getEconomy() {
        return economy;
    }
}
