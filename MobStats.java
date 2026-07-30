package com.rpgcore.plugin.mob;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;

/**
 * ⭐ "원래 있던 마인크래프트 몬스터의 체력과 공격력만 바꾸는" 시스템.
 *
 * 커스텀 몹/모델을 쓰지 않고, 바닐라 좀비·스켈레톤·크리퍼 등을 그대로 소환한 뒤
 * config.yml 의 mob-stats 값으로 체력(GENERIC_MAX_HEALTH)과
 * 공격력(GENERIC_ATTACK_DAMAGE)만 덮어씁니다.
 *
 * 문제: 마인크래프트에서 아래 피해는 공격력 속성을 따르지 않습니다.
 *   - 크리퍼 폭발
 *   - 블레이즈/가스트 화염구
 *   - 스켈레톤/약탈자 화살·화살촉
 *   - 마녀 물약, 에보커 송곳니 등
 * 해결: 계산한 "공격력 배율"을 몹에 태그로 저장해두고,
 *       그 몹(또는 그 몹이 쏜 투사체)이 플레이어를 때릴 때
 *       MobDeathListener 에서 실제 피해에 곱해줍니다.
 */
public class MobStats {

    /** 근접이 아닌 피해(화살/폭발/물약)에 곱할 배율을 몹에 저장하는 키 */
    public static NamespacedKey DAMAGE_MULT_KEY;

    /**
     * 공격력 속성이 없거나 실제 피해가 속성과 무관한 몹들의 "바닐라 기준 피해".
     * config 의 damage 값이 이 값의 몇 배인지로 배율을 계산합니다.
     * (예: CREEPER damage: 44.0 -> 22.0 의 2배 -> 폭발 피해 2배)
     */
    private static final Map<EntityType, Double> VANILLA_REFERENCE = new EnumMap<>(EntityType.class);

    static {
        VANILLA_REFERENCE.put(EntityType.CREEPER, 22.0);      // 폭발 (근거리 기준)
        VANILLA_REFERENCE.put(EntityType.BLAZE, 5.0);         // 화염구
        VANILLA_REFERENCE.put(EntityType.GHAST, 6.0);         // 화염구
        VANILLA_REFERENCE.put(EntityType.SKELETON, 4.0);      // 화살
        VANILLA_REFERENCE.put(EntityType.STRAY, 4.0);         // 화살
        VANILLA_REFERENCE.put(EntityType.PILLAGER, 5.0);      // 쇠뇌
        VANILLA_REFERENCE.put(EntityType.WITCH, 6.0);         // 물약
        VANILLA_REFERENCE.put(EntityType.EVOKER, 6.0);        // 송곳니
        VANILLA_REFERENCE.put(EntityType.SHULKER, 4.0);       // 셜커 탄
        VANILLA_REFERENCE.put(EntityType.GUARDIAN, 6.0);      // 레이저
        VANILLA_REFERENCE.put(EntityType.ELDER_GUARDIAN, 8.0);
        VANILLA_REFERENCE.put(EntityType.WITHER, 8.0);
        VANILLA_REFERENCE.put(EntityType.ENDER_DRAGON, 10.0);
    }

    private final JavaPlugin plugin;
    private final Map<EntityType, Double> baseHealth = new EnumMap<>(EntityType.class);
    private final Map<EntityType, Double> baseDamage = new EnumMap<>(EntityType.class);

    private double healthCap = 800.0;
    private double damageCap = 30.0;
    private double healthPerStage = 0.30;
    private double damagePerStage = 0.06;
    private double planetHealthWeight = 1.2;
    private double planetDamageWeight = 0.5;

    public MobStats(JavaPlugin plugin) {
        this.plugin = plugin;
        DAMAGE_MULT_KEY = new NamespacedKey(plugin, "rpg_damage_mult");
    }

    public void load() {
        baseHealth.clear();
        baseDamage.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("mob-stats");
        if (root == null) {
            plugin.getLogger().warning("config.yml 에 mob-stats 섹션이 없습니다. 기본값을 사용합니다.");
            return;
        }
        healthCap = root.getDouble("health-cap", healthCap);
        damageCap = root.getDouble("damage-cap", damageCap);
        healthPerStage = root.getDouble("health-per-stage", healthPerStage);
        damagePerStage = root.getDouble("damage-per-stage", damagePerStage);
        planetHealthWeight = root.getDouble("planet-health-weight", planetHealthWeight);
        planetDamageWeight = root.getDouble("planet-damage-weight", planetDamageWeight);

        ConfigurationSection base = root.getConfigurationSection("base");
        if (base != null) {
            for (String key : base.getKeys(false)) {
                EntityType type;
                try {
                    type = EntityType.valueOf(key.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("[mob-stats] 알 수 없는 몬스터 타입: " + key);
                    continue;
                }
                ConfigurationSection sec = base.getConfigurationSection(key);
                if (sec == null) {
                    continue;
                }
                if (sec.isSet("health")) {
                    baseHealth.put(type, sec.getDouble("health"));
                }
                if (sec.isSet("damage")) {
                    baseDamage.put(type, sec.getDouble("damage"));
                }
            }
        }
        plugin.getLogger().info("[RpgCore] 몬스터 스탯 " + baseHealth.size() + "종 로드 완료 (체력/공격력 커스텀)");
    }

    /**
     * 바닐라 몬스터 1마리에 체력/공격력을 적용한다.
     *
     * @param mob   방금 소환된 바닐라 몬스터
     * @param mult  행성 배수 (탑에서는 층 배수)
     * @param stage 단계 (행성=거리, 탑=층)
     * @return 실제 적용된 최종 체력
     */
    public double apply(LivingEntity mob, double mult, int stage) {
        return apply(mob, mult, stage, 1.0, 1.0);
    }

    /**
     * 보스처럼 추가 보정이 필요한 경우.
     *
     * @param hpBonus  체력 추가 배율 (상한도 같은 비율로 늘어남)
     * @param dmgBonus 공격력 추가 배율
     */
    public double apply(LivingEntity mob, double mult, int stage, double hpBonus, double dmgBonus) {
        // mult=1.0(테라) & stage=1 이면 배율이 정확히 1.0 -> config 의 base 값이 그대로 적용된다.
        // 즉 "1단계 · 테라 기준 체력/공격력" = config 에 적은 숫자.
        double safeMult = Math.max(1.0, mult);
        int steps = Math.max(0, stage - 1);
        double planetHpScale = 1.0 + Math.log(safeMult) * planetHealthWeight;
        double planetDmgScale = 1.0 + Math.log(safeMult) * planetDamageWeight;
        double stageHpScale = 1.0 + steps * healthPerStage;
        double stageDmgScale = 1.0 + steps * damagePerStage;

        // ----- 체력 -----
        AttributeInstance healthAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double finalHp = 20.0;
        if (healthAttr != null) {
            double base = baseHealth.getOrDefault(mob.getType(), healthAttr.getBaseValue());
            finalHp = Math.max(1.0, Math.min(healthCap * hpBonus, base * planetHpScale * stageHpScale * hpBonus));
            healthAttr.setBaseValue(finalHp);
            mob.setHealth(finalHp);
        }

        // ----- 공격력 (근접) -----
        AttributeInstance damageAttr = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        Double configuredDamage = baseDamage.get(mob.getType());
        double scale = planetDmgScale * stageDmgScale;

        if (damageAttr != null) {
            double base = configuredDamage != null ? configuredDamage : damageAttr.getBaseValue();
            double finalDamage = Math.max(0.5, Math.min(damageCap * dmgBonus, base * scale * dmgBonus));
            damageAttr.setBaseValue(finalDamage);
        }

        // ----- 공격력 (화살/폭발/물약 등 속성으로 안 되는 피해) -----
        // 바닐라 기준 피해 대비 몇 배인지 = ratio, 여기에 행성/단계 배율을 곱해 저장.
        double ratio = 1.0;
        if (configuredDamage != null) {
            Double reference = VANILLA_REFERENCE.get(mob.getType());
            if (reference == null && damageAttr != null) {
                reference = damageAttr.getDefaultValue();
            }
            if (reference != null && reference > 0) {
                ratio = configuredDamage / reference;
            }
        }
        double rangedMult = Math.max(0.1, ratio * scale * dmgBonus);
        // 상한: 배율 피해도 damage-cap 기준으로 과하게 커지지 않도록 제한
        rangedMult = Math.min(rangedMult, Math.max(1.0, damageCap / 4.0));
        mob.getPersistentDataContainer().set(DAMAGE_MULT_KEY, PersistentDataType.DOUBLE, rangedMult);

        return finalHp;
    }

    public double getHealthCap() {
        return healthCap;
    }

    public double getDamageCap() {
        return damageCap;
    }
}
