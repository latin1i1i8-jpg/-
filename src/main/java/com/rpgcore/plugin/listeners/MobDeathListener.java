package com.rpgcore.plugin.listeners;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.job.PlayerStatApplier;
import com.rpgcore.plugin.job.StatCalculator;
import com.rpgcore.plugin.mob.MobStats;
import com.rpgcore.plugin.planet.PlanetDef;
import com.rpgcore.plugin.tower.TowerManager;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/**
 * RPG 몬스터(행성 배회 몹 + 시련의 탑 몹)의 전투/처치 처리.
 *
 * - 플레이어가 RPG 몬스터를 때리면 직업/레벨 배율을 실제 데미지에 곱한다.
 * - ⭐ 반대로 몬스터가 플레이어를 때릴 때, 화살/폭발/물약처럼 공격력 속성이
 *   먹지 않는 피해에는 MobStats 가 저장해둔 배율을 곱해준다.
 *   (근접 공격은 GENERIC_ATTACK_DAMAGE 속성으로 이미 반영되어 있으므로 제외)
 * - 누구든 잡은 사람이 보상(골드/XP)을 받는다.
 * - 은퇴 시스템: 같은 몬스터(행성+종류+단계)를 2번 잡으면 그 뒤로 보상 0.
 *   단 ⭐ 시련의 탑은 예외 — 층마다 항상 보상이 나온다.
 */
public class MobDeathListener implements Listener {

    /** 같은 몬스터를 이 횟수만큼 잡으면, 그 뒤로는 보상 없음 */
    private static final int RETIRE_KILL_COUNT = 2;

    private final RpgCorePlugin plugin;

    public MobDeathListener(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    private static boolean isRpgMob(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(PlanetSpawnListener.PLANET_KEY, PersistentDataType.STRING);
    }

    /** 전투 데미지 보정 (플레이어 -> 몬스터, 몬스터 -> 플레이어 양방향) */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        // 1) 플레이어가 RPG 몬스터를 공격 -> 레벨/직업 배율
        if (event.getDamager() instanceof Player player
                && event.getEntity() instanceof LivingEntity target && isRpgMob(target)) {
            PlayerData data = plugin.getDataManager().get(player.getUniqueId());
            boolean ranged = event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE;
            double multiplier = ranged
                    ? StatCalculator.rangedAttackPower(data) / 10.0
                    : StatCalculator.attackPower(data) / 10.0;
            multiplier = Math.max(0.5, multiplier); // 최소 배율 보장
            event.setDamage(event.getDamage() * multiplier);
            return;
        }

        // 2) RPG 몬스터가 플레이어를 공격 -> 속성으로 안 되는 피해(화살/폭발/물약)에 배율 적용
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        LivingEntity attacker = resolveAttacker(event);
        if (attacker == null || !isRpgMob(attacker)) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean melee = cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK;
        if (melee) {
            return; // 근접은 공격력 속성이 이미 처리
        }
        Double mult = attacker.getPersistentDataContainer()
                .get(MobStats.DAMAGE_MULT_KEY, PersistentDataType.DOUBLE);
        if (mult != null && mult > 0 && Math.abs(mult - 1.0) > 0.01) {
            event.setDamage(event.getDamage() * mult);
        }
    }

    /** 화살/화염구를 쏜 몬스터 본체를 찾아낸다 */
    private LivingEntity resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity living) {
            return living;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isRpgMob(entity)) {
            return;
        }
        Player killer = entity.getKiller();
        if (killer == null) {
            return; // 자연사/낙사 등은 보상 없음
        }

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String planetName = pdc.get(PlanetSpawnListener.PLANET_KEY, PersistentDataType.STRING);
        String mobName = pdc.get(PlanetSpawnListener.MOB_NAME_KEY, PersistentDataType.STRING);
        Integer stageObj = pdc.get(PlanetSpawnListener.STAGE_KEY, PersistentDataType.INTEGER);
        int stage = stageObj == null ? 1 : stageObj;
        if (mobName == null) {
            mobName = planetName + " " + entity.getType().name();
        }
        Integer towerFloor = pdc.get(TowerManager.TOWER_FLOOR_KEY, PersistentDataType.INTEGER);
        Integer towerSlot = pdc.get(TowerManager.TOWER_SLOT_KEY, PersistentDataType.INTEGER);

        PlayerData data = plugin.getDataManager().get(killer.getUniqueId());
        event.setDroppedExp(0); // 바닐라 경험치 오브는 끄고 커스텀 XP만 사용

        // ---------- 시련의 탑 ----------
        if (towerFloor != null) {
            double mult = plugin.getTowerManager().getRewardMult();
            long gold = (long) (((12 * Math.pow(towerFloor, 1.25)) + 30) * mult);
            long xp = (long) ((10 * Math.pow(towerFloor, 1.32)) * mult);
            grant(killer, data, gold, xp);
            Msg.send(killer, ChatColor.GREEN + "🎉 " + towerFloor + "층 몬스터 처치! +" + gold + "G / +" + xp + "XP");
            plugin.getTowerManager().onMobKilled(killer, towerFloor, towerSlot == null ? 0 : towerSlot);
            plugin.getDataManager().save(data);
            return;
        }

        // ---------- 행성 배회 몬스터 ----------
        // 은퇴 판정: 같은 몬스터(이름+단계)를 이미 2번 잡았다면 보상 없음 (처치 자체는 자유)
        String retireKey = mobName.replace(' ', '_') + "_" + stage;
        int killCount = data.getKillCount(retireKey);
        if (killCount >= RETIRE_KILL_COUNT) {
            Msg.send(killer, ChatColor.DARK_GRAY + "…이미 " + RETIRE_KILL_COUNT
                    + "번 처치한 몬스터입니다. 보상이 지급되지 않습니다. (더 먼 곳의 강한 몬스터에 도전해보세요)");
            return;
        }
        data.addKillCount(retireKey);

        // 보상: 디스코드 봇 공식 (12*stage^1.2+25 / 10*stage^1.3) x 행성 배수(완만 압축)
        PlanetDef planet = plugin.getPlanetManager().getByName(planetName);
        double rewardMult = planet == null ? 1.0 : Math.pow(planet.getMult(), 0.5);
        long gold = (long) (((12 * Math.pow(stage, 1.2)) + 25) * rewardMult);
        long xp = (long) ((10 * Math.pow(stage, 1.3)) * rewardMult);

        int remaining = RETIRE_KILL_COUNT - data.getKillCount(retireKey);
        String retireNote = remaining <= 0 ? ChatColor.DARK_GRAY + " (이 몬스터 보상은 이제 끝!)" : "";
        Msg.send(killer, ChatColor.GREEN + "🎉 " + mobName + " (" + stage + "단계) 처치! +" + gold + "G / +" + xp + "XP" + retireNote);
        grant(killer, data, gold, xp);
        plugin.getDataManager().save(data);
    }

    /** 골드/XP 지급 + 레벨업 처리. 올라간 레벨 수를 반환 */
    private int grant(Player killer, PlayerData data, long gold, long xp) {
        data.addGold(gold);
        data.setXp(data.getXp() + xp);
        data.incrementKills();

        long needed = StatCalculator.xpNeeded(data.getLevel());
        int leveled = 0;
        // 디스코드 봇에서 겪었던 "거대 XP -> while 루프 서버 정지" 버그 방지: 반복 상한
        while (data.getXp() >= needed && leveled < 500) {
            data.setXp(data.getXp() - needed);
            data.setLevel(data.getLevel() + 1);
            leveled++;
            needed = StatCalculator.xpNeeded(data.getLevel());
        }
        if (leveled > 0) {
            PlayerStatApplier.apply(killer, data);
            Msg.send(killer, ChatColor.YELLOW + "🆙 레벨 업! Lv." + data.getLevel());
        }
        return leveled;
    }
}
