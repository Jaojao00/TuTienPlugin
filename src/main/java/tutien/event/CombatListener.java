package tutien.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import tutien.combat.StatsManager;
import tutien.core.CanhGioi;
import tutien.core.HeTuLuyen;
import tutien.core.PlayerDataManager;
import tutien.tutien.gui.GachaGUI;

import java.util.Set;

/**
 * Lớp xử lý các hiệu ứng đặc biệt của từng hệ, Cảnh Giới và Linh Thú khi giao chiến.
 * v2.3 Skyblock Edition:
 *   - Giảm sát thương nhận vào (Bảo Vệ Hộ Thể) theo CanhGioi (từ 2% ở Luyện Khí đến 50% ở Tiên Đế).
 *   - Đồng bộ % buff của Hệ Tu Luyện (Thể Tu, Ma Tu, Kiếm Tu, Yêu Tu).
 *   - Bổ sung hiệu ứng trợ chiến của Linh Thú: Huyết Phù Quy (-15% sát thương nhận), Thiên Mãu Hổ (+20% sát thương gây ra).
 */
public class CombatListener implements Listener {

    private final PlayerDataManager dataManager;
    private final tutien.khidao.service.KhiDaoService khiDaoService;

    public CombatListener(PlayerDataManager dataManager) {
        this(dataManager, null);
    }

    public CombatListener(PlayerDataManager dataManager, tutien.khidao.service.KhiDaoService khiDaoService) {
        this.dataManager = dataManager;
        this.khiDaoService = khiDaoService;
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {

        // --- XỬ LÝ KHI NGƯỜI CHƠI BỊ TẤN CÔNG ---
        if (event.getEntity() instanceof Player victim) {
            CanhGioi cgVictim = dataManager.getCanhGioi(victim);
            HeTuLuyen heVictim = dataManager.getHeTuLuyen(victim);
            Set<GachaGUI.LinhThu> victimPets = StatsManager.getActivePets(victim);

            double dmgMultiplier = 1.0;

            // BẢO VỆ HỘ THỂ BẨM SINH THEO CẢNH GIỚI (Damage Reduction)
            double reduction = cgVictim.getDamageReduction();
            if (reduction > 0) {
                dmgMultiplier *= (1.0 - reduction);
            }

            // THỂ TU: Giảm 10% sát thương nhận vào
            if (heVictim == HeTuLuyen.THE_TU) {
                dmgMultiplier *= 0.90;
            }

            // MA TU: Nhận thêm 20% sát thương (đổi lại +30% công kích)
            if (heVictim == HeTuLuyen.MA_TU) {
                dmgMultiplier *= 1.20;
            }

            // LINH THÚ HUYẾT PHÙ QUY: +15% Phòng thủ (Giảm 15% sát thương nhận vào)
            if (victimPets.contains(GachaGUI.LinhThu.HUYET_PHU_QUY)) {
                dmgMultiplier *= 0.85;
            }

            double dmg = event.getDamage() * dmgMultiplier;

            // KHÍ ĐẠO: Xử lý giáp khảm nạm (Phản đòn Thổ & Hộ Chủ Khí Linh)
            if (khiDaoService != null) {
                tutien.khidao.service.KhiDaoService.CombatDefenseResult defRes =
                        khiDaoService.processDefense(victim, event.getDamager(), dmg);
                dmg = defRes.modifiedDamage;
                if (defRes.hoChuTriggered) {
                    victim.sendMessage("§6[Khí Linh] §eHộ Chủ Quang Hoàn kích hoạt! Giảm 40% sát thương và tạo kết giới hộ thể!");
                }
                if (defRes.reflectDamage > 0 && event.getDamager() instanceof org.bukkit.entity.LivingEntity damagerLiving) {
                    damagerLiving.damage(defRes.reflectDamage);
                }
            }

            // BÍ CẢNH: Ghi nhận sát thương gánh chịu (Tank damage tracking - Phase 5)
            try {
                tutien.core.TuTienPlugin plugin = tutien.core.TuTienPlugin.getPlugin(tutien.core.TuTienPlugin.class);
                if (plugin != null && plugin.getBiCanhManager() != null && plugin.getBiCanhManager().isPlayerInsideBiCanh(victim)) {
                    plugin.getBiCanhManager().recordDamageTanked(victim.getUniqueId(), dmg);
                }
            } catch (Throwable ignored) {}

            event.setDamage(dmg);
        }

        // --- XỬ LÝ KHI NGƯỜI CHƠI TẤN CÔNG ---
        if (event.getDamager() instanceof Player attacker) {
            HeTuLuyen heAttacker = dataManager.getHeTuLuyen(attacker);
            Set<GachaGUI.LinhThu> attackerPets = StatsManager.getActivePets(attacker);
            String mainHandName = attacker.getInventory().getItemInMainHand().getType().name();

            double atkMultiplier = 1.0;

            // KIẾM TU: +20% sát thương khi cầm Kiếm
            if (heAttacker == HeTuLuyen.KIEM_TU && mainHandName.contains("SWORD")) {
                atkMultiplier *= 1.20;
            }

            // LINH THÚ THIÊN MÃU HỔ: +20% sát thương cận chiến
            if (attackerPets.contains(GachaGUI.LinhThu.THIEN_MAU_HO)) {
                atkMultiplier *= 1.20;
            }

            // ĐẠO LỮ SONG TU: +15% sát thương khi kề vai tác chiến cùng Đạo Lữ (Phase 6)
            try {
                tutien.core.TuTienPlugin plugin = tutien.core.TuTienPlugin.getPlugin(tutien.core.TuTienPlugin.class);
                if (plugin != null && plugin.getDaoLuService() != null) {
                    atkMultiplier *= plugin.getDaoLuService().getPartnerCombatBonus(attacker);
                }
            } catch (Throwable ignored) {}

            double baseDmg = event.getDamage() * atkMultiplier;

            // KHÍ ĐẠO: Xử lý vũ khí khảm nạm Ngũ Hành & Kỹ năng Khí Linh
            if (khiDaoService != null) {
                String victimElem = resolveEntityElement(event.getEntity());
                tutien.khidao.service.KhiDaoService.CombatAttackResult atkRes =
                        khiDaoService.processAttack(attacker, event.getEntity(), baseDmg, victimElem);

                baseDmg = atkRes.finalDamage;

                if (atkRes.swordAuraTriggered) {
                    attacker.sendMessage("§6[Khí Linh] §eKiếm Khí Trác Tuyệt bộc phát! Phóng xuất chùm kiếm khí hủy diệt!");
                }

                if (atkRes.lifestealAmount > 0 && attacker.getHealth() < attacker.getMaxHealth()) {
                    attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + atkRes.lifestealAmount));
                }

                if (atkRes.burnApplied && event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                    target.setFireTicks(60); // 3 giây thiêu đốt
                }

                if (atkRes.slowApplied && event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
                }
            }

            // BÍ CẢNH & WORLD BOSS DAMAGE TRACKING (Phase 5)
            // Ghi nhận sát thương THỰC TẾ sau toàn bộ hiệu ứng công kích, vũ khí và phòng thủ
            try {
                tutien.core.TuTienPlugin plugin = tutien.core.TuTienPlugin.getPlugin(tutien.core.TuTienPlugin.class);
                if (plugin != null) {
                    // Kiểm tra đúng vùng Bí Cảnh (không chỉ UUID trong session)
                    if (plugin.getBiCanhManager() != null && plugin.getBiCanhManager().isPlayerInsideBiCanh(attacker)) {
                        plugin.getBiCanhManager().recordDamage(attacker.getUniqueId(), baseDmg);
                    }
                    // Kiểm tra Boss Thế Giới (cả Entity UUID lẫn spawnId)
                    if (plugin.getWorldBossManager() != null && plugin.getWorldBossManager().isWorldBoss(event.getEntity())) {
                        plugin.getWorldBossManager().recordBossDamage(attacker.getUniqueId(), baseDmg, event.getEntity());
                    }
                }
            } catch (Throwable ignored) {}

            event.setDamage(baseDmg);
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            // YÊU TU: Hồi phục tự nhiên × 2
            if (dataManager.getHeTuLuyen(player) == HeTuLuyen.YEU_TU) {
                event.setAmount(event.getAmount() * 2.0);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (event == null || event.getEntity() == null) return;
        try {
            tutien.core.TuTienPlugin plugin = tutien.core.TuTienPlugin.getPlugin(tutien.core.TuTienPlugin.class);
            if (plugin != null) {
                if (plugin.getWorldBossManager() != null) {
                    plugin.getWorldBossManager().handleEntityDeath(event.getEntity());
                }
                Player killer = event.getEntity().getKiller();
                if (killer != null && plugin.getBiCanhManager() != null && plugin.getBiCanhManager().isPlayerInsideBiCanh(killer)) {
                    plugin.getBiCanhManager().recordKill(killer.getUniqueId());
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Xác định nguyên tố Ngũ Hành của một mục tiêu (Boss, Mob, hoặc Người Chơi)
     */
    public String resolveEntityElement(Entity entity) {
        return resolveEntityElement(entity, this.dataManager);
    }

    public static String resolveEntityElement(Entity entity, PlayerDataManager dm) {
        if (entity == null) return null;
        if (entity instanceof Player p && dm != null) {
            // Đọc hệ linh căn chính của người chơi
            tutien.element.LinhCanData lcd = dm.getLinhCanData(p);
            if (lcd != null && lcd.getPrimaryElement() != null) {
                return lcd.getPrimaryElement();
            }
        }
        // Nhận diện theo tên quái hoặc chủng loại
        String name = entity.getType().name();
        if (name.contains("BLAZE") || name.contains("MAGMA") || name.contains("GHAST")) {
            return "HOA";
        } else if (name.contains("DROWNED") || name.contains("GUARDIAN") || name.contains("SQUID")) {
            return "THUY";
        } else if (name.contains("IRON_GOLEM")) {
            return "KIM";
        } else if (name.contains("CREEPER") || name.contains("SPIDER")) {
            return "MOC";
        } else if (name.contains("ZOMBIE") || name.contains("SKELETON") || name.contains("HUSK")) {
            return "THO";
        }
        return null;
    }
}
