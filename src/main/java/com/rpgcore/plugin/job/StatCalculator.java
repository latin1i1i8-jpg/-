package com.rpgcore.plugin.job;

import com.rpgcore.plugin.data.PlayerData;

/**
 * 디스코드 봇 battle_system.py 의 공식을 그대로 옮겼습니다.
 *   xp_needed(level)      = int(50 * level^1.4)
 *   player_attack_power   = 8 + level*4 (+ 직업 보너스)
 *   player_max_hp         = 100 + level*30 (+ 직업 보너스)
 *
 * 마인크래프트는 체력이 하트(0.5당 1점) 단위라, 최종 값은 실제 적용 시 스케일 조정이 필요합니다.
 * (예: HunterListener 에서 몬스터 체력에 곱해주는 배율로 사용, 플레이어 체력은 MAX_HEALTH 속성에
 *  너무 크게 넣으면 바닐라 UI가 깨지므로 20~40 하트 사이로 별도 스케일링해서 적용하는 걸 권장합니다.)
 */
public final class StatCalculator {

    private StatCalculator() {
    }

    /** 다음 레벨업까지 필요한 누적 경험치 */
    public static long xpNeeded(int level) {
        return (long) (50 * Math.pow(level, 1.4));
    }

    /** 순수 공격력 지표 (몬스터 체력에 대응시켜 사용하는 값이지, 마인크래프트 데미지 그 자체는 아님) */
    public static double attackPower(PlayerData data) {
        double base = 8 + data.getLevel() * 4;
        JobType job = JobType.fromDisplayName(data.getJob());
        if (job != null) {
            base *= job.getMeleeMultiplier();
        }
        return base;
    }

    /** 원거리(활) 공격력 지표 */
    public static double rangedAttackPower(PlayerData data) {
        double base = 8 + data.getLevel() * 4;
        JobType job = JobType.fromDisplayName(data.getJob());
        if (job != null) {
            base *= job.getRangedMultiplier();
        }
        return base;
    }

    /** 최대 체력 지표 (하트 단위로 변환해서 MAX_HEALTH 속성에 적용) */
    public static double maxHealthPoints(PlayerData data) {
        double base = 100 + data.getLevel() * 30;
        JobType job = JobType.fromDisplayName(data.getJob());
        if (job != null) {
            base *= job.getHealthMultiplier();
        }
        return base;
    }

    /**
     * maxHealthPoints() 값을 바닐라 체력바가 감당 가능한 하트 수(최대 40하트=80체력 권장)로 변환.
     * 레벨 1~50 정도까지는 부드럽게, 그 이상은 완만하게 증가하도록 로그 스케일을 살짝 섞었습니다.
     */
    public static double scaledVanillaMaxHealth(PlayerData data) {
        double raw = maxHealthPoints(data);
        double scaled = 20 + Math.log(raw / 100.0 + 1) * 15; // 20(기본 10하트) 부터 완만히 증가
        return Math.min(80.0, Math.max(20.0, scaled)); // 10~40하트 사이로 클램프
    }
}
