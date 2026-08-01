package com.rpgcore.plugin.magic;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.job.StatCalculator;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ⭐ 마법 시스템 (new).
 *
 * - 마나: 초기 70, 레벨당 +5
 * - 쿨타임: 10초
 * - 4가지 마법: 화염, 얼음, 번개, 치유
 * - 화염구 투사체 사용 (기본 마인크래프트 이펙트)
 */
public class MagicManager {

    private final RpgCorePlugin plugin;

    /** 플레이어의 현재 마나 */
    private final Map<UUID, Integer> playerMana = new HashMap<>();

    /** 플레이어의 마법 쿨타임 (마지막 사용 시각, ms) */
    private final Map<UUID, Long> magicCooldown = new HashMap<>();

    private static final long COOLDOWN_MS = 10 * 1000; // 10초
    private static final int MANA_REGEN_TICKS = 60; // 3초 = 60틱

    public MagicManager(RpgCorePlugin plugin) {
        this.plugin = plugin;
        startManaRegeneration();
    }

    /** 3초마다 모든 플레이어에게 마나 1씩 회복 (무음) */
    private void startManaRegeneration() {
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
            plugin,
            () -> {
                for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(player.getUniqueId());
                    if (data == null) continue;

                    int currentMana = getMana(player.getUniqueId(), data);
                    int maxMana = getMaxMana(data);

                    if (currentMana < maxMana) {
                        addMana(player.getUniqueId(), 1, data);
                    }
                }
            },
            0,
            MANA_REGEN_TICKS // 3초
        );
    }

    /**
     * 플레이어의 최대 마나.
     * 초기: 70
     * 레벨당 +5
     */
    public int getMaxMana(PlayerData data) {
        return 70 + (data.getLevel() - 1) * 5;
    }

    /**
     * 플레이어의 현재 마나 (캐시, 없으면 최대값).
     */
    public int getMana(UUID uuid, PlayerData data) {
        return playerMana.getOrDefault(uuid, getMaxMana(data));
    }

    /** 마나 회복 (플레이어 회복 물약 마실 때 등) */
    public void setMana(UUID uuid, int amount, PlayerData data) {
        int max = getMaxMana(data);
        playerMana.put(uuid, Math.min(amount, max));
    }

    /** 마나 소비 */
    public void addMana(UUID uuid, int delta, PlayerData data) {
        int current = getMana(uuid, data);
        setMana(uuid, current + delta, data);
    }

    /**
     * 마법 실행 가능 여부 + 쿨타임 체크.
     */
    public boolean canUseMagic(Player player, Magic magic, PlayerData data) {
        int mana = getMana(player.getUniqueId(), data);
        long now = System.currentTimeMillis();
        long lastUse = magicCooldown.getOrDefault(player.getUniqueId(), 0L);

        if (mana < magic.getManaCost()) {
            Msg.send(player, ChatColor.RED + "마나가 부족합니다! (필요: " + magic.getManaCost() + ", 보유: " + mana + ")");
            return false;
        }
        if (now - lastUse < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - lastUse)) / 1000;
            Msg.send(player, ChatColor.RED + "쿨타임: " + remaining + "초 남음");
            return false;
        }
        return true;
    }

    /**
     * 마법 시전.
     */
    public void castMagic(Player player, Magic magic, PlayerData data) {
        // ⭐ Godswrath는 OP 전용
        if (magic == Magic.GODSWRATH) {
            if (!player.isOp()) {
                Msg.send(player, ChatColor.RED + "신의 분노는 OP 유저만 사용 가능합니다!");
                return;
            }
            // Godswrath는 마나 체크 없음
            castGodsWrath(player);
            Msg.send(player, ChatColor.RED + ChatColor.BOLD + "⭐ 신의 분노 발동!");
            return;
        }

        if (!canUseMagic(player, magic, data)) {
            return;
        }

        // 마나 소비
        addMana(player.getUniqueId(), -magic.getManaCost(), data);

        // 쿨타임 설정
        magicCooldown.put(player.getUniqueId(), System.currentTimeMillis());

        // 마법 효과
        double dmg = magic.getDamageMult() > 0 ? StatCalculator.attackPower(data) * magic.getDamageMult() / 10.0 : 0;

        switch (magic) {
            case FIRE:
                castFireball(player, dmg);
                Msg.send(player, magic.getColor() + "🔥 " + magic.getDisplayName() + " 시전! (-" + magic.getManaCost() + " 마나)");
                break;
            case FREEZE:
                castFreeze(player, dmg);
                Msg.send(player, magic.getColor() + "❄️ " + magic.getDisplayName() + " 시전! (-" + magic.getManaCost() + " 마나)");
                break;
            case LIGHTNING:
                castLightning(player, dmg);
                Msg.send(player, magic.getColor() + "⚡ " + magic.getDisplayName() + " 시전! (-" + magic.getManaCost() + " 마나)");
                break;
            case HEAL:
                castHeal(player);
                Msg.send(player, magic.getColor() + "💚 " + magic.getDisplayName() + " 시전! (-" + magic.getManaCost() + " 마나)");
                break;
            case TORNADO:
                castTornado(player, dmg);
                Msg.send(player, magic.getColor() + "🌪️ " + magic.getDisplayName() + " 시전! (-" + magic.getManaCost() + " 마나)");
                break;
            case METEOR:
                castMeteor(player, dmg);
                Msg.send(player, magic.getColor() + "🔮 " + magic.getDisplayName() + " 시전! (-" + magic.getManaCost() + " 마나)");
                break;
            case GODSWRATH:
                // 이미 위에서 처리됨
                break;
        }
    }

    /** 화염구 (기본 마인크래프트 이펙트) */
    private void castFireball(Player player, double damage) {
        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection().multiply(1.5);

        Fireball fireball = player.getWorld().spawn(loc, Fireball.class);
        fireball.setShooter(player);
        fireball.setVelocity(dir);
        fireball.setYield(0.5f); // 폭발 규모 줄임 (블록 파괴 방지)
        fireball.setIsIncendiary(true); // 화염 입힐 수 있도록

        // 태그에 데미지 저장 (MagicListener가 읽음)
        fireball.getPersistentDataContainer().set(
            plugin.getMagicDamageKey(),
            org.bukkit.persistence.PersistentDataType.DOUBLE,
            damage
        );
    }

    /** 얼음술 (장거리 판정, 자취를 통해 몬스터에 느려짐 부여) */
    private void castFreeze(Player player, double damage) {
        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection();

        // 시야 20블록 내 몬스터 찾기
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(loc, 20, 20, 20)) {
            if (entity == player || entity.getHealth() <= 0) continue;
            if (!isValidTarget(entity)) continue;

            double dist = entity.getLocation().distance(loc);
            // 시야 방향으로 대략 맞춰진 몬스터만 타격
            Vector toEntity = entity.getLocation().subtract(loc).toVector().normalize();
            if (toEntity.dot(dir) > 0.5 && dist < 20) {
                entity.damage(damage, player);
                // Paper 1.20.4: SLOWNESS 는 구버전, 대신 SLOW_FALLING 사용
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false)); // 느려짐 3초
                break; // 첫 번째 몬스터만 타격
            }
        }
    }

    /** 번개술 (플레이어 위치 근처 지면에 번개) */
    private void castLightning(Player player, double damage) {
        Location targetLoc = player.getTargetBlock(null, 30).getLocation().add(0, 1, 0);

        // 번개 시각 효과
        player.getWorld().strikeLightning(targetLoc);

        // 근처 몬스터 데미지 + 스턴
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(targetLoc, 5)) {
            if (entity == player || entity.getHealth() <= 0) continue;
            if (!isValidTarget(entity)) continue;

            entity.damage(damage, player);
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, false, false)); // 스턴 1초
        }
    }

    /** 치유술 (플레이어 체력 회복) */
    private void castHeal(Player player) {
        double max = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double heal = max * 0.3; // 최대 체력의 30%
        player.setHealth(Math.min(player.getHealth() + heal, max));
    }

    /** 회오리술 (플레이어 주변 몬스터를 밀어냄) */
    private void castTornado(Player player, double damage) {
        Location center = player.getLocation();
        
        // 15블록 반경 내 몬스터 찾기
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(center, 15)) {
            if (entity == player || entity.getHealth() <= 0) continue;
            if (!isValidTarget(entity)) continue;
            
            // 몬스터를 위로 띄우기
            Vector knockback = entity.getLocation().subtract(center).toVector().normalize().multiply(2).setY(1);
            entity.setVelocity(knockback);
            entity.damage(damage, player);
        }
    }

    /** 유성술 (플레이어 시야 방향에 큰 광역 피해) */
    private void castMeteor(Player player, double damage) {
        Location targetLoc = player.getTargetBlock(null, 30).getLocation().add(0, 5, 0);
        
        // 유성 시각 효과 (빛)
        player.getWorld().spawnParticle(
            org.bukkit.Particle.EXPLOSION_LARGE,
            targetLoc,
            10,
            2, 2, 2,
            0.5
        );
        
        // 20블록 반경 내 모든 몬스터에 데미지
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(targetLoc, 20)) {
            if (entity == player || entity.getHealth() <= 0) continue;
            if (!isValidTarget(entity)) continue;
            
            entity.damage(damage * 1.5, player); // 데미지 1.5배
        }
    }

    /** ⭐ 신의 분노 (OP 전용 - 절대 데미지) */
    private void castGodsWrath(Player player) {
        Location center = player.getLocation();
        
        // 30블록 반경 내 모든 몬스터를 즉시 제거
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(center, 30)) {
            if (entity == player || entity.getHealth() <= 0) continue;
            if (!isValidTarget(entity)) continue;
            
            // 절대 데미지 (즉시 킬)
            entity.setHealth(0);
        }
        
        // 시각 효과 (화려한 폭발)
        player.getWorld().spawnParticle(
            org.bukkit.Particle.EXPLOSION_HUGE,
            center,
            20,
            5, 5, 5,
            1.0
        );
    }

    /** 몬스터인지 확인 (플레이어는 제외) */
    private boolean isValidTarget(LivingEntity entity) {
        return !(entity instanceof Player) && entity instanceof org.bukkit.entity.Monster;
    }

    /** 플레이어 접속 해제 시 마나 저장 */
    public void onPlayerQuit(UUID uuid) {
        playerMana.remove(uuid);
        magicCooldown.remove(uuid);
    }
}
