package tutien.alchemy.escrow;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Bản ghi giao dịch Ký Quỹ Escrow bền vững trên ổ đĩa.
 */
public class EscrowTransaction {

    private final UUID transactionId;
    private final UUID playerId;
    private final String recipeId;
    private EscrowState state;
    private final long createdAt;
    private long updatedAt;

    private String inventoryBeforeHash;
    private String inventoryAfterHash;
    private UUID claimReceiptId;
    private UUID refundReceiptId;
    private String reconciliationNote;

    private final List<ItemStack> lockedItems = new ArrayList<>();
    private final List<ItemStack> outputItems = new ArrayList<>();

    public EscrowTransaction(UUID transactionId, UUID playerId, String recipeId,
                             EscrowState state, List<ItemStack> lockedItems) {
        this.transactionId = transactionId != null ? transactionId : UUID.randomUUID();
        this.playerId = playerId;
        this.recipeId = recipeId;
        this.state = state != null ? state : EscrowState.PREPARED;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;

        if (lockedItems != null) {
            for (ItemStack is : lockedItems) {
                if (is != null && is.getType() != Material.AIR) {
                    this.lockedItems.add(is.clone());
                }
            }
        }
    }

    public EscrowTransaction(UUID transactionId, UUID playerId, String recipeId,
                             EscrowState state, long createdAt, long updatedAt,
                             List<ItemStack> lockedItems, List<ItemStack> outputItems) {
        this.transactionId = transactionId;
        this.playerId = playerId;
        this.recipeId = recipeId;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        if (lockedItems != null) {
            for (ItemStack is : lockedItems) {
                if (is != null && is.getType() != Material.AIR) {
                    this.lockedItems.add(is.clone());
                }
            }
        }
        if (outputItems != null) {
            for (ItemStack is : outputItems) {
                if (is != null && is.getType() != Material.AIR) {
                    this.outputItems.add(is.clone());
                }
            }
        }
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public EscrowState getState() {
        return state;
    }

    public void setState(EscrowState state) {
        this.state = state;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public List<ItemStack> getLockedItems() {
        return Collections.unmodifiableList(lockedItems);
    }

    public void setLockedItems(List<ItemStack> items) {
        this.lockedItems.clear();
        if (items != null) {
            for (ItemStack is : items) {
                if (is != null && is.getType() != Material.AIR) {
                    this.lockedItems.add(is.clone());
                }
            }
        }
        this.updatedAt = System.currentTimeMillis();
    }

    public String getInventoryBeforeHash() {
        return inventoryBeforeHash;
    }

    public void setInventoryBeforeHash(String inventoryBeforeHash) {
        this.inventoryBeforeHash = inventoryBeforeHash;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getInventoryAfterHash() {
        return inventoryAfterHash;
    }

    public void setInventoryAfterHash(String inventoryAfterHash) {
        this.inventoryAfterHash = inventoryAfterHash;
        this.updatedAt = System.currentTimeMillis();
    }

    public UUID getClaimReceiptId() {
        return claimReceiptId;
    }

    public void setClaimReceiptId(UUID claimReceiptId) {
        this.claimReceiptId = claimReceiptId;
        this.updatedAt = System.currentTimeMillis();
    }

    public UUID getRefundReceiptId() {
        return refundReceiptId;
    }

    public void setRefundReceiptId(UUID refundReceiptId) {
        this.refundReceiptId = refundReceiptId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getReconciliationNote() {
        return reconciliationNote;
    }

    public void setReconciliationNote(String reconciliationNote) {
        this.reconciliationNote = reconciliationNote;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<ItemStack> getOutputItems() {
        return Collections.unmodifiableList(outputItems);
    }

    public void setOutputItems(List<ItemStack> outputs) {
        this.outputItems.clear();
        if (outputs != null) {
            for (ItemStack is : outputs) {
                if (is != null && is.getType() != Material.AIR) {
                    this.outputItems.add(is.clone());
                }
            }
        }
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Ghi transaction vào YAML ConfigurationSection
     */
    public void toSection(ConfigurationSection sec) {
        sec.set("player-id", playerId.toString());
        sec.set("recipe-id", recipeId);
        sec.set("state", state.name());
        sec.set("created-at", createdAt);
        sec.set("updated-at", updatedAt);

        if (inventoryBeforeHash != null) sec.set("inventory-before-hash", inventoryBeforeHash);
        if (inventoryAfterHash != null) sec.set("inventory-after-hash", inventoryAfterHash);
        if (claimReceiptId != null) sec.set("claim-receipt-id", claimReceiptId.toString());
        if (refundReceiptId != null) sec.set("refund-receipt-id", refundReceiptId.toString());
        if (reconciliationNote != null) sec.set("reconciliation-note", reconciliationNote);

        // Lưu danh sách nguyên liệu bảo toàn 100% ItemMeta & PDC
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (ItemStack is : lockedItems) {
            itemMaps.add(EscrowItemSerializer.serialize(is));
        }
        sec.set("locked-items", itemMaps);

        // Lưu danh sách thành phẩm bảo toàn 100% ItemMeta & PDC
        List<Map<String, Object>> outMaps = new ArrayList<>();
        for (ItemStack is : outputItems) {
            outMaps.add(EscrowItemSerializer.serialize(is));
        }
        sec.set("output-items", outMaps);
    }

    /**
     * Tái tạo transaction từ YAML ConfigurationSection
     */
    public static EscrowTransaction fromSection(UUID txId, ConfigurationSection sec) {
        if (sec == null) return null;

        UUID pId = UUID.fromString(sec.getString("player-id"));
        String rId = sec.getString("recipe-id");
        EscrowState st = EscrowState.valueOf(sec.getString("state", "PREPARED"));
        long cAt = sec.getLong("created-at", System.currentTimeMillis());
        long uAt = sec.getLong("updated-at", cAt);

        boolean corruptItemDetected = false;
        List<ItemStack> locked = new ArrayList<>();
        List<?> rawLocked = sec.getList("locked-items");
        if (rawLocked != null) {
            for (Object obj : rawLocked) {
                if (obj instanceof Map<?, ?> map) {
                    ItemStack is = EscrowItemSerializer.deserialize(map);
                    if (is != null) {
                        locked.add(is);
                    } else {
                        corruptItemDetected = true;
                    }
                }
            }
        }

        List<ItemStack> outputs = new ArrayList<>();
        List<?> rawOut = sec.getList("output-items");
        if (rawOut != null) {
            for (Object obj : rawOut) {
                if (obj instanceof Map<?, ?> map) {
                    ItemStack is = EscrowItemSerializer.deserialize(map);
                    if (is != null) {
                        outputs.add(is);
                    } else {
                        corruptItemDetected = true;
                    }
                }
            }
        }

        if (corruptItemDetected) {
            st = EscrowState.NEEDS_RECONCILIATION;
        }

        EscrowTransaction tx = new EscrowTransaction(txId, pId, rId, st, cAt, uAt, locked, outputs);
        if (corruptItemDetected) {
            tx.setReconciliationNote("Phát hiện vật phẩm bị lỗi deserialization (raw_bytes/NBT corrupt). Cách ly giao dịch sang NEEDS_RECONCILIATION để bảo vệ tài sản.");
        }
        if (sec.contains("inventory-before-hash")) {
            tx.setInventoryBeforeHash(sec.getString("inventory-before-hash"));
        }
        if (sec.contains("inventory-after-hash")) {
            tx.setInventoryAfterHash(sec.getString("inventory-after-hash"));
        }
        if (sec.contains("claim-receipt-id")) {
            try {
                tx.setClaimReceiptId(UUID.fromString(sec.getString("claim-receipt-id")));
            } catch (Exception ignored) {}
        }
        if (sec.contains("refund-receipt-id")) {
            try {
                tx.setRefundReceiptId(UUID.fromString(sec.getString("refund-receipt-id")));
            } catch (Exception ignored) {}
        }
        if (sec.contains("reconciliation-note")) {
            tx.setReconciliationNote(sec.getString("reconciliation-note"));
        }
        return tx;
    }

    private static Map<String, Object> serializeItem(ItemStack item) {
        return EscrowItemSerializer.serialize(item);
    }

    private static ItemStack deserializeItem(Map<?, ?> map) {
        return EscrowItemSerializer.deserialize(map);
    }
}
