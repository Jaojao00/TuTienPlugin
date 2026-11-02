package tutien.integration.mythicmobs;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * Cơ chế thi triển kỹ năng dự phòng thuần Bukkit (Fallback) khi MythicMobs vắng mặt (Phase 2.4):
 * Không tạo hiệu ứng chung chung mà tôn trọng cấu hình chi tiết của từng kỹ năng trong skills.yml.
 * Hỗ trợ các dạng: BUFF_DEFENSE, AREA_KNOCKBACK, INVULNERABILITY, PROJECTILE_DAMAGE,
 * LIGHTNING_STRIKE, EXPLOSION_AOE, HEAL_AND_CLEANSE, TELEPORT_DASH, TRUE_DAMAGE_AOE, SWORD_SLASH_AOE.
 */
public class BukkitSkillFallback {

    public static boolean execute(Player caster, ConfigurationSection fallbackSec) {
        if (caster == null || fallbackSec == null) return false;

        boolean enabled = fallbackSec.getBoolean("enabled", false);
        if (!enabled) return false;

        String type = fallbackSec.getString("type", "BUFF_DEFENSE").toUpperCase();
        String particleName = fallbackSec.getString("particle", "VILLAGER_HAPPY");
        String soundName = fallbackSec.getString("sound", "ENTITY_PLAYER_LEVELUP");

        Particle particle;
        try { particle = Particle.valueOf(particleName.toUpperCase()); }
        catch (Exception e) { particle = Particle.CRIT; }

        Sound sound;
        try { sound = Sound.valueOf(soundName.toUpperCase()); }
        catch (Exception e) { sound = Sound.ENTITY_PLAYER_LEVELUP; }

        Location loc = caster.getLocation();

        switch (type) {
            case "BUFF_DEFENSE" -> {
                int duration = fallbackSec.getInt("duration-seconds", 8) * 20;
                caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 1));
                caster.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration / 2, 0));
                caster.getWorld().spawnParticle(particle, loc.clone().add(0, 1.0, 0), 20, 0.5, 0.5, 0.5, 0.05);
                caster.playSound(loc, sound, 1.0f, 1.2f);
                return true;
            }

            case "AREA_KNOCKBACK" -> {
                double range = fallbackSec.getDouble("range-blocks", 5.0);
                double damage = fallbackSec.getDouble("damage", 15.0);
                Collection<Entity> nearby = caster.getWorld().getNearbyEntities(loc, range, range, range);
                for (Entity e : nearby) {
                    if (e instanceof Monster monster && !e.getUniqueId().equals(caster.getUniqueId())) {
                        Vector knockback = monster.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5).setY(0.4);
                        monster.setVelocity(knockback);
                        monster.damage(damage, caster);
                        monster.getWorld().spawnParticle(Particle.CRIT, monster.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                caster.getWorld().spawnParticle(particle, loc.clone().add(0, 0.5, 0), 30, range / 2, 0.2, range / 2, 0.1);
                caster.playSound(loc, sound, 1.0f, 1.0f);
                return true;
            }

            case "INVULNERABILITY" -> {
                int duration = fallbackSec.getInt("duration-seconds", 4) * 20;
                caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 4)); // Kháng sát thương cấp 5
                caster.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration * 2, 1));
                caster.getWorld().spawnParticle(particle, loc.clone().add(0, 1.0, 0), 25, 0.6, 0.8, 0.6, 0.05);
                caster.playSound(loc, sound, 1.0f, 1.0f);
                return true;
            }

            case "PROJECTILE_DAMAGE" -> {
                double damage = fallbackSec.getDouble("damage", 25.0);
                double maxDistance = 20.0;
                RayTraceResult trace = caster.getWorld().rayTraceEntities(
                        caster.getEyeLocation(),
                        caster.getLocation().getDirection(),
                        maxDistance,
                        0.5,
                        e -> !e.getUniqueId().equals(caster.getUniqueId()) && e instanceof Damageable
                );

                Vector dir = caster.getLocation().getDirection().normalize().multiply(0.5);
                Location step = caster.getEyeLocation().clone();
                for (int i = 0; i < 30; i++) {
                    step.add(dir);
                    caster.getWorld().spawnParticle(particle, step, 2, 0.02, 0.02, 0.02, 0.01);
                }

                if (trace != null && trace.getHitEntity() instanceof Damageable target) {
                    target.damage(damage, caster);
                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
                }
                caster.playSound(loc, sound, 1.0f, 1.5f);
                return true;
            }

            case "LIGHTNING_STRIKE" -> {
                double range = fallbackSec.getDouble("range-blocks", 10.0);
                double damage = fallbackSec.getDouble("damage", 30.0);
                Collection<Entity> nearby = caster.getWorld().getNearbyEntities(loc, range, range, range);
                for (Entity e : nearby) {
                    if (e instanceof Damageable target && !e.getUniqueId().equals(caster.getUniqueId())) {
                        caster.getWorld().strikeLightningEffect(target.getLocation());
                        target.damage(damage, caster);
                    }
                }
                caster.playSound(loc, sound, 1.0f, 1.0f);
                return true;
            }

            case "EXPLOSION_AOE" -> {
                double range = fallbackSec.getDouble("range-blocks", 6.0);
                double damage = fallbackSec.getDouble("damage", 40.0);
                caster.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                caster.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 50, range / 2, 0.5, range / 2, 0.1);
                Collection<Entity> nearby = caster.getWorld().getNearbyEntities(loc, range, range, range);
                for (Entity e : nearby) {
                    if (e instanceof Damageable target && !e.getUniqueId().equals(caster.getUniqueId())) {
                        target.damage(damage, caster);
                        target.setFireTicks(100);
                    }
                }
                caster.playSound(loc, sound, 1.2f, 0.8f);
                return true;
            }

            case "HEAL_AND_CLEANSE" -> {
                double healAmount = fallbackSec.getDouble("heal-amount", 10.0);
                double maxHealth = 20.0;
                if (caster.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    maxHealth = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                }
                caster.setHealth(Math.min(maxHealth, caster.getHealth() + healAmount));
                caster.removePotionEffect(PotionEffectType.POISON);
                caster.removePotionEffect(PotionEffectType.WITHER);
                caster.removePotionEffect(PotionEffectType.SLOWNESS);
                caster.getWorld().spawnParticle(particle, loc.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
                caster.playSound(loc, sound, 1.0f, 1.2f);
                return true;
            }

            case "TELEPORT_DASH" -> {
                double distance = fallbackSec.getDouble("distance-blocks", 8.0);
                Vector dir = caster.getLocation().getDirection().normalize().multiply(distance);
                Location dest = caster.getLocation().add(dir);
                if (dest.getBlock().isPassable()) {
                    caster.teleport(dest);
                }
                caster.getWorld().strikeLightningEffect(caster.getLocation());
                caster.getWorld().spawnParticle(Particle.FLASH, caster.getLocation(), 1);
                caster.playSound(loc, sound, 1.0f, 1.5f);
                return true;
            }

            case "TRUE_DAMAGE_AOE" -> {
                double range = fallbackSec.getDouble("range-blocks", 8.0);
                double damage = fallbackSec.getDouble("damage", 50.0);
                caster.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.5, 0), 80, range / 2, 0.4, range / 2, 0.05);
                Collection<Entity> nearby = caster.getWorld().getNearbyEntities(loc, range, range, range);
                for (Entity e : nearby) {
                    if (e instanceof Damageable target && !e.getUniqueId().equals(caster.getUniqueId())) {
                        target.damage(damage, caster);
                        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                caster.playSound(loc, sound, 1.0f, 0.9f);
                return true;
            }

            case "SWORD_SLASH_AOE" -> {
                double range = fallbackSec.getDouble("range-blocks", 6.0);
                double damage = fallbackSec.getDouble("damage", 35.0);
                caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1.0, 0), 15, range / 3, 0.2, range / 3, 0.1);
                Collection<Entity> nearby = caster.getWorld().getNearbyEntities(loc, range, range, range);
                for (Entity e : nearby) {
                    if (e instanceof Damageable target && !e.getUniqueId().equals(caster.getUniqueId())) {
                        target.damage(damage, caster);
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
                    }
                }
                caster.playSound(loc, sound, 1.0f, 1.2f);
                return true;
            }

            default -> {
                return false;
            }
        }
    }
}
