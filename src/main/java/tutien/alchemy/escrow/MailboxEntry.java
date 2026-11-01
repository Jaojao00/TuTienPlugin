package tutien.alchemy.escrow;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Bản ghi vật phẩm trong Hòm Thư Đền Bù (Mailbox) có định danh giao dịch bền vững.
 * Hỗ trợ giao thức Two-Phase Claim Protocol: PENDING -> CLAIMING -> CLAIMED.
 * Đối soát receipt ID và amount thực tế trong inventory, chống nhân đôi và phục hồi an toàn sau crash.
 */
public class MailboxEntry {

    public enum MailboxStatus {
        PENDING,
        CLAIMING,
        CLAIMED
    }

    private final UUID entryId;
    private final UUID transactionId;
    private UUID claimReceiptId;
    private ItemStack item;
    private final long createdAt;
    private MailboxStatus status;

    public MailboxEntry(UUID entryId, UUID transactionId, UUID claimReceiptId, ItemStack item, long createdAt) {
        this(entryId, transactionId, claimReceiptId, item, createdAt, MailboxStatus.PENDING);
    }

    public MailboxEntry(UUID entryId, UUID transactionId, UUID claimReceiptId, ItemStack item, long createdAt, MailboxStatus status) {
        this.entryId = entryId != null ? entryId : UUID.randomUUID();
        this.transactionId = transactionId;
        this.claimReceiptId = claimReceiptId;
        this.item = item != null ? item.clone() : new ItemStack(Material.STONE);
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.status = status != null ? status : MailboxStatus.PENDING;
    }

    public UUID getEntryId() {
        return entryId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getClaimReceiptId() {
        return claimReceiptId;
    }

    public void setClaimReceiptId(UUID claimReceiptId) {
        this.claimReceiptId = claimReceiptId;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public void setItem(ItemStack item) {
        this.item = item != null ? item.clone() : new ItemStack(Material.STONE);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public MailboxStatus getStatus() {
        return status;
    }

    public void setStatus(MailboxStatus status) {
        this.status = status;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = EscrowItemSerializer.serialize(item);
        map.put("entry-id", entryId.toString());
        if (transactionId != null) map.put("transaction-id", transactionId.toString());
        if (claimReceiptId != null) map.put("claim-receipt-id", claimReceiptId.toString());
        map.put("created-at", createdAt);
        map.put("status", status.name());
        return map;
    }

    public static MailboxEntry fromMap(Map<?, ?> map) {
        if (map == null) return null;
        UUID eId = map.containsKey("entry-id") ? UUID.fromString(String.valueOf(map.get("entry-id"))) : UUID.randomUUID();
        UUID txId = map.containsKey("transaction-id") && map.get("transaction-id") != null
                ? UUID.fromString(String.valueOf(map.get("transaction-id"))) : null;
        UUID rId = map.containsKey("claim-receipt-id") && map.get("claim-receipt-id") != null
                ? UUID.fromString(String.valueOf(map.get("claim-receipt-id"))) : null;
        long cAt = map.containsKey("created-at") ? Long.parseLong(String.valueOf(map.get("created-at"))) : System.currentTimeMillis();

        MailboxStatus st = MailboxStatus.PENDING;
        if (map.containsKey("status")) {
            try {
                st = MailboxStatus.valueOf(String.valueOf(map.get("status")));
            } catch (Exception ignored) {}
        }

        ItemStack is = EscrowItemSerializer.deserialize(map);
        if (is == null) {
            // Nếu bản ghi có raw_bytes nhưng deserialize thất bại -> bản ghi bị corrupt NBT/PDC!
            // Tuyệt đối không fallback thành ItemStack trần làm mất metadata, trả về null để cách ly an toàn!
            if (map.containsKey("raw_bytes")) {
                return null;
            }
            // Chỉ bản ghi legacy (hoàn toàn không có raw_bytes) mới dùng fallback tương thích
            String matName = String.valueOf(map.get("material"));
            Material mat = Material.matchMaterial(matName);
            if (mat == null) {
                return null;
            }
            int amt = 1;
            if (map.containsKey("amount")) {
                try {
                    amt = Integer.parseInt(String.valueOf(map.get("amount")));
                } catch (Exception ignored) {}
            }
            is = new ItemStack(mat, Math.max(1, amt));
        }

        if (rId != null) {
            is = AlchemyEscrowManager.tagClaimReceipt(is, txId, rId);
        }
        return new MailboxEntry(eId, txId, rId, is, cAt, st);
    }
}
