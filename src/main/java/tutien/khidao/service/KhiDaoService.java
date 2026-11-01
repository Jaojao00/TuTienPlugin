package tutien.khidao.service;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tutien.core.ItemKeys;
import tutien.core.PlayerDataManager;
import tutien.core.TuTienPlugin;
import tutien.craft.CheManager;
import tutien.element.ElementRelationService;
import tutien.khidao.model.*;

import java.util.*;

/**
 * Service cốt lõi của Khí Đạo & Trang Bị (Phase 4):
 * Quản lý Rèn 8 bậc, Khảm Nạm Linh Thạch Ngũ Hành, Thức Tỉnh & Nuôi Dưỡng Khí Linh,
 * Tính toán Tương Sinh/Tương Khắc Ngũ Hành trong Combat.
 */
public class KhiDaoService {

    private final TuTienPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ElementRelationService elementRelationService;
    private static final Random random = new Random();

    // Cooldown hộ chủ (UUID -> Timestamp kết thúc)
    private final Map<UUID, Long> hoChuCooldowns = new HashMap<>();

    public KhiDaoService(TuTienPlugin plugin, PlayerDataManager dataManager, ElementRelationService elementRelationService) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.elementRelationService = (elementRelationService != null) ? elementRelationService : new ElementRelationService();
    }

    public ElementRelationService getElementRelationService() {
        return elementRelationService;
    }

    /**
     * Rèn trang bị theo công thức và cấp Lò Rèn (8 phẩm cấp tu tiên)
     */
    public ItemStack rollCraftEquipment(CheManager.CheRecipe recipe, int capChe) {
        EquipTier[] tiers = EquipTier.values();
        int[] weights = new int[tiers.length];

        // Trọng số cơ bản
        for (int i = 0; i < tiers.length; i++) {
            weights[i] = tiers[i].getBaseWeight();
        }

        // Cấp lò rèn càng cao -> Giảm tỷ lệ Phàm Khí, tăng mạnh tỷ lệ Bảo Khí, Cổ Bảo và Linh Bảo
        int reduction = Math.min(40, capChe * 3);
        weights[0] = Math.max(10, weights[0] - reduction);
        int bonusPerTier = reduction / (tiers.length - 1);
        for (int i = 1; i < tiers.length; i++) {
            weights[i] += bonusPerTier;
        }

        int totalWeight = 0;
        for (int w : weights) totalWeight += w;

        int roll = random.nextInt(Math.max(1, totalWeight));
        int cumulative = 0;
        EquipTier selectedTier = EquipTier.PHAM_KHI;
        for (int i = 0; i < tiers.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                selectedTier = tiers[i];
                break;
            }
        }

        ItemStack item = new ItemStack(recipe.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(selectedTier.getDisplayName() + " " + recipe.displayName);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.KEY_ITEM_TYPE, PersistentDataType.STRING, ItemKeys.TYPE_EQUIP);
        pdc.set(ItemKeys.KEY_EQUIP_TIER, PersistentDataType.INTEGER, selectedTier.getLevel());

        double scale = recipe.tierScale;
        if (recipe.type.equals("Kiếm")) {
            double dmg = Math.round((selectedTier.getBaseDmgBonus()) * scale * 10.0) / 10.0;
            pdc.set(ItemKeys.KEY_PILL_DMG, PersistentDataType.DOUBLE, dmg);

            AttributeModifier dmgMod = new AttributeModifier(
                    UUID.randomUUID(), "tutien_sword_dmg", dmg,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, dmgMod);
        } else if (recipe.type.equals("Giáp")) {
            double hp = Math.round((selectedTier.getBaseHpBonus()) * scale * 10.0) / 10.0;
            double armor = Math.round((selectedTier.getBaseArmorBonus()) * scale * 10.0) / 10.0;
            pdc.set(ItemKeys.KEY_PILL_HP, PersistentDataType.DOUBLE, hp);

            AttributeModifier hpMod = new AttributeModifier(
                    UUID.randomUUID(), "tutien_armor_hp", hp,
                    AttributeModifier.Operation.ADD_NUMBER, recipe.slot);
            AttributeModifier armorMod = new AttributeModifier(
                    UUID.randomUUID(), "tutien_armor_def", armor,
                    AttributeModifier.Operation.ADD_NUMBER, recipe.slot);
            meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, hpMod);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorMod);
        } else {
            // Phụ Kiện Hộ Mệnh
            double bonusHp = selectedTier.getBaseHpBonus() * 0.8;
            double bonusDmg = selectedTier.getBaseDmgBonus() * 0.6;
            pdc.set(ItemKeys.KEY_PILL_HP, PersistentDataType.DOUBLE, bonusHp);
            pdc.set(ItemKeys.KEY_PILL_DMG, PersistentDataType.DOUBLE, bonusDmg);
        }

        item.setItemMeta(meta);

        // Khởi tạo hồ sơ Phase 4 (sockets và profile)
        EquipProfile profile = new EquipProfile(selectedTier);
        profile.applyToItemStack(item);

        return item;
    }

    /**
     * Khai mở thêm ô khảm (đục lỗ)
     */
    public boolean unlockSocket(ItemStack equipItem) {
        EquipProfile profile = EquipProfile.fromItemStack(equipItem);
        if (profile == null) return false;

        boolean success = profile.unlockNextSocket();
        if (success) {
            profile.applyToItemStack(equipItem);
        }
        return success;
    }

    /**
     * Khảm Linh Thạch vào trang bị
     */
    public boolean inlayGem(ItemStack equipItem, ItemStack gemItem) {
        EquipProfile profile = EquipProfile.fromItemStack(equipItem);
        if (profile == null) return false;

        LinhThachData gemData = LinhThachData.fromItemStack(gemItem);
        if (gemData == null) return false;

        boolean success = profile.inlayGem(gemData);
        if (success) {
            profile.applyToItemStack(equipItem);
            gemItem.setAmount(gemItem.getAmount() - 1);
        }
        return success;
    }

    /**
     * Tháo Linh Thạch ra khỏi trang bị và trả về viên đá
     */
    public LinhThachData extractGem(ItemStack equipItem, int slotIndex) {
        EquipProfile profile = EquipProfile.fromItemStack(equipItem);
        if (profile == null) return null;

        LinhThachData extracted = profile.extractGem(slotIndex);
        if (extracted != null) {
            profile.applyToItemStack(equipItem);
        }
        return extracted;
    }

    /**
     * Thức tỉnh Khí Linh độc lập (Chỉ khả dụng từ bậc Cổ Bảo trở lên)
     */
    public boolean awakenKhiLinh(ItemStack equipItem, String spiritName, String element, KhiLinhSkill skill) {
        EquipProfile profile = EquipProfile.fromItemStack(equipItem);
        if (profile == null) return false;

        // Kiểm tra phẩm cấp
        if (!profile.getTier().isAllowSpirit()) {
            return false;
        }

        // Đã có khí linh rồi thì không thức tỉnh đè
        if (profile.hasKhiLinh()) {
            return false;
        }

        KhiLinhData kl = new KhiLinhData(spiritName, element, 1, 0, skill);
        profile.setKhiLinh(kl);
        profile.applyToItemStack(equipItem);
        return true;
    }

    /**
     * Bồi dưỡng linh tính Khí Linh
     */
    public boolean nourishKhiLinh(ItemStack equipItem, int soulExp) {
        EquipProfile profile = EquipProfile.fromItemStack(equipItem);
        if (profile == null || !profile.hasKhiLinh()) return false;

        boolean leveledUp = profile.getKhiLinh().addSoul(soulExp);
        profile.applyToItemStack(equipItem);
        return true;
    }

    // === KẾT QUẢ GIAO CHIẾN COMBAT ===

    public static class CombatAttackResult {
        public double finalDamage;
        public double elementalBonus;
        public double lifestealAmount;
        public boolean slowApplied;
        public boolean burnApplied;
        public boolean swordAuraTriggered;
        public double relationMultiplier;

        public CombatAttackResult(double finalDamage, double elementalBonus, double lifestealAmount,
                                  boolean slowApplied, boolean burnApplied, boolean swordAuraTriggered,
                                  double relationMultiplier) {
            this.finalDamage = finalDamage;
            this.elementalBonus = elementalBonus;
            this.lifestealAmount = lifestealAmount;
            this.slowApplied = slowApplied;
            this.burnApplied = burnApplied;
            this.swordAuraTriggered = swordAuraTriggered;
            this.relationMultiplier = relationMultiplier;
        }
    }

    public static class CombatDefenseResult {
        public double modifiedDamage;
        public double reflectDamage;
        public boolean hoChuTriggered;

        public CombatDefenseResult(double modifiedDamage, double reflectDamage, boolean hoChuTriggered) {
            this.modifiedDamage = modifiedDamage;
            this.reflectDamage = reflectDamage;
            this.hoChuTriggered = hoChuTriggered;
        }
    }

    /**
     * Tính toán tương sinh tương khắc Ngũ Hành chuyên dụng trong Giao Chiến (Combat):
     * - Ta khắc Địch (vd Thủy khắc Hỏa): Sát thương x1.50 (+50%)
     * - Bị Địch khắc (vd Thổ khắc Thủy): Sát thương x0.60 (-40%)
     * - Địch sinh Ta: Sát thương x1.25 (+25%)
     * - Ta sinh Địch: Sát thương x0.80 (-20%)
     */
    public double getCombatRelation(String weaponElement, String victimElement) {
        if (weaponElement == null || victimElement == null) return 1.0;
        Optional<tutien.element.NguHanh> wOpt = tutien.element.NguHanh.fromString(weaponElement);
        Optional<tutien.element.NguHanh> vOpt = tutien.element.NguHanh.fromString(victimElement);
        if (wOpt.isPresent() && vOpt.isPresent()) {
            tutien.element.NguHanh w = wOpt.get();
            tutien.element.NguHanh v = vOpt.get();
            if (w == v) return 1.0;
            if (w.isKhac(v)) return 1.50; // Khắc chế đối thủ
            if (v.isKhac(w)) return 0.60; // Bị đối thủ khắc chế
            if (v.isSinh(w)) return 1.25; // Địch sinh ta
            if (w.isSinh(v)) return 0.80; // Ta sinh địch
        }
        return 1.0;
    }

    /**
     * Xử lý sát thương khi người chơi tấn công mục tiêu (Tính toán Ngũ Hành, Ngọc Khảm & Khí Linh)
     */
    public CombatAttackResult processAttack(Player attacker, Entity victim, double baseDamage, String victimElement) {
        ItemStack weapon = (attacker != null && attacker.getInventory() != null)
                ? attacker.getInventory().getItemInMainHand() : null;
        EquipProfile profile = EquipProfile.fromItemStack(weapon);

        double elementalPower = 0.0;
        double lifesteal = 0.0;
        boolean slow = false;
        boolean burn = false;
        boolean swordAura = false;
        double relationMulti = 1.0;

        if (profile != null) {
            // 1. Tính toán sát thương nguyên tố từ từng viên ngọc khảm
            for (LinhThachData gem : profile.getInlaidGems()) {
                double gemPower = gem.getElementalPower();
                String gemElem = gem.getType().name();

                // Áp dụng tương sinh / tương khắc chiến đấu với mục tiêu
                if (victimElement != null && !victimElement.isBlank()) {
                    double rel = getCombatRelation(gemElem, victimElement);
                    gemPower *= rel;
                    relationMulti = Math.max(relationMulti, rel);
                }

                elementalPower += gemPower;

                // Hiệu ứng đặc biệt
                if (gem.getType() == LinhThachType.MOC) {
                    lifesteal += gem.getSpecialRatio();
                } else if (gem.getType() == LinhThachType.THUY) {
                    slow = true;
                } else if (gem.getType() == LinhThachType.HOA) {
                    burn = true;
                }
            }

            // 2. Kích hoạt kỹ năng Khí Linh
            if (profile.hasKhiLinh()) {
                KhiLinhData kl = profile.getKhiLinh();
                if (kl.getSkill() == KhiLinhSkill.KIEM_KHI_TRAC_VIET) {
                    // 25% phóng kiếm khí gây thêm 150% sát thương ngũ hành
                    if (random.nextInt(100) < 25) {
                        swordAura = true;
                        elementalPower += (baseDamage + elementalPower) * 1.5;
                    }
                } else if (kl.getSkill() == KhiLinhSkill.LINH_BA_CUONG_NO) {
                    // Tăng thêm 50% nếu quái có nguyên tố hoặc tên Boss
                    if (victim != null && (victimElement != null || victim.getName().contains("Boss"))) {
                        elementalPower *= 1.5;
                    }
                }
            }
        }

        double finalDamage = baseDamage + elementalPower;
        double lifestealAmount = finalDamage * Math.min(0.5, lifesteal);

        return new CombatAttackResult(finalDamage, elementalPower, lifestealAmount, slow, burn, swordAura, relationMulti);
    }

    /**
     * Xử lý phòng thủ khi người chơi bị tấn công (Phản đòn hệ Thổ & Hộ Chủ Khí Linh)
     */
    public CombatDefenseResult processDefense(Player victim, Entity damager, double incomingDamage) {
        double reflectRatio = 0.0;
        boolean hoChuTriggered = false;
        double currentHp = victim.getHealth();
        double maxHp = victim.getMaxHealth();

        // Quét toàn bộ trang bị giáp của người chơi
        for (ItemStack armor : victim.getInventory().getArmorContents()) {
            if (armor == null) continue;
            EquipProfile profile = EquipProfile.fromItemStack(armor);
            if (profile == null) continue;

            reflectRatio += profile.getReflectRatio();

            // Kích hoạt Hộ Chủ Quang Hoàn khi máu dưới 30%
            if (profile.hasKhiLinh()) {
                KhiLinhData kl = profile.getKhiLinh();
                if (kl.getSkill() == KhiLinhSkill.HO_CHU_HOANG_KIM) {
                    if (currentHp / maxHp <= 0.30) {
                        long now = System.currentTimeMillis();
                        long cdUntil = hoChuCooldowns.getOrDefault(victim.getUniqueId(), 0L);
                        if (now >= cdUntil) {
                            hoChuTriggered = true;
                            hoChuCooldowns.put(victim.getUniqueId(), now + 45000L); // 45s hồi chiêu
                        }
                    }
                }
            }
        }

        double modifiedDamage = incomingDamage;
        if (hoChuTriggered) {
            // Giảm 40% sát thương nhận vào
            modifiedDamage *= 0.60;
        }

        double reflectDamage = modifiedDamage * Math.min(0.50, reflectRatio);

        return new CombatDefenseResult(modifiedDamage, reflectDamage, hoChuTriggered);
    }
}
