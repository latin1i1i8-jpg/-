package com.rpgcore.plugin.listeners;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.planet.PlanetDef;
import com.rpgcore.plugin.util.KoreanMobNames;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

/**
 * 행성 월드에서 몬스터가 자연 스폰될 때:
 *   1) 그 행성 테마 몬스터 목록에 없는 종류면 -> 목록에 있는 종류로 교체해서 다시 소환
 *   2) "지도" 개념: 월드 스폰 지점에서 멀어질수록 단계(1~99)가 올라가고, 그만큼 강해짐
 *   3) ⭐ 소환되는 건 전부 <b>바닐라 마인크래프트 몬스터</b>이고,
 *      config.yml 의 mob-stats + 행성 배수 + 단계로 <b>체력과 공격력만</b> 바뀝니다.
 *      (MobStats 클래스가 담당 — 커스텀 몹/모델 없음)
 *   4) RPG 몬스터로 태깅 (처치 보상/은퇴 판정용)
 */
public class PlanetSpawnListener implements Listener {

    private static final Random RANDOM = new Random();

    /** 단계 1이 올라가는 거리 간격 (블록). 40블록마다 1단계씩 상승, 약 4000블록에서 99단계 도달 */
    public static final double BLOCKS_PER_STAGE = 40.0;

    public static NamespacedKey PLANET_KEY;
    public static NamespacedKey MOB_NAME_KEY;
    public static NamespacedKey STAGE_KEY;

    private final RpgCorePlugin plugin;

    public PlanetSpawnListener(RpgCorePlugin plugin) {
        this.plugin = plugin;
        initKeys(plugin);
    }

    /**
     * 몬스터 태그용 키 초기화.
     * 시련의 탑도 같은 키를 쓰기 때문에, 등록 순서에 상관없도록
     * 플러그인 시작 직후 한 번 호출한다. (여러 번 불러도 안전)
     */
    public static void initKeys(RpgCorePlugin plugin) {
        if (PLANET_KEY == null) {
            PLANET_KEY = new NamespacedKey(plugin, "rpg_planet");
            MOB_NAME_KEY = new NamespacedKey(plugin, "rpg_mob_name");
            STAGE_KEY = new NamespacedKey(plugin, "rpg_stage");
        }
    }

    /** 스폰 지점 기준 거리 -> 단계 (지도의 난이도 링) */
    public static int stageAt(Location loc) {
        Location spawn = loc.getWorld().getSpawnLocation();
        double dist = Math.sqrt(Math.pow(loc.getX() - spawn.getX(), 2) + Math.pow(loc.getZ() - spawn.getZ(), 2));
        int stage = 1 + (int) (dist / BLOCKS_PER_STAGE);
        return Math.min(99, Math.max(1, stage));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        // 자연 스폰만 개입 (스폰 알/명령어/플러그인 소환은 건드리지 않음 — 탑 몬스터도 여기 안 걸림)
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }
        PlanetDef planet = plugin.getPlanetManager().getByWorld(event.getLocation().getWorld());
        if (planet == null || planet.getMobs().isEmpty()) {
            return; // 행성 월드가 아니면 바닐라 그대로
        }
        if (!(event.getEntity() instanceof Monster)) {
            return; // 동물 등 평화 몹은 그대로 둠
        }

        List<EntityType> pool = planet.getMobs();
        EntityType chosen = pool.get(RANDOM.nextInt(pool.size()));

        if (event.getEntityType() != chosen) {
            // 다른 종류면 원래 스폰은 취소하고 테마 몬스터로 교체 소환
            event.setCancelled(true);
            LivingEntity mob = (LivingEntity) event.getLocation().getWorld().spawnEntity(event.getLocation(), chosen);
            decorate(mob, planet);
        } else {
            decorate((LivingEntity) event.getEntity(), planet);
        }
    }

    private void decorate(LivingEntity mob, PlanetDef planet) {
        int stage = stageAt(mob.getLocation());

        // ⭐ 바닐라 몹의 체력/공격력만 교체 (config.yml -> mob-stats)
        plugin.getMobStats().apply(mob, planet.getMult(), stage);

        String mobName = planet.getName() + " " + KoreanMobNames.of(mob.getType());
        mob.setCustomName("§c[" + planet.getName() + " " + stage + "단계] " + KoreanMobNames.of(mob.getType()));
        mob.setCustomNameVisible(true);
        mob.setRemoveWhenFarAway(true); // 멀어지면 자연 소멸 (서버 렉 방지)
        mob.getPersistentDataContainer().set(PLANET_KEY, PersistentDataType.STRING, planet.getName());
        mob.getPersistentDataContainer().set(MOB_NAME_KEY, PersistentDataType.STRING, mobName);
        mob.getPersistentDataContainer().set(STAGE_KEY, PersistentDataType.INTEGER, stage);
    }
}
