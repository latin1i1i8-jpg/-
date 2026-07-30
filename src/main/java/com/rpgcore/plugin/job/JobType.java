package com.rpgcore.plugin.job;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 직업 정의. 디스코드 봇의 CLASSES 딕셔너리(전사/마법사/거너/네크로맨서/성기사/흑마법사)를
 * 마인크래프트 실시간 전투 상황에 맞게 재해석했습니다.
 *
 * - 전사: 근접 공격력 +20%, 최대 체력 +20%
 * - 마법사: 활(투사체) 피해 +25%, 최대 체력 -10% (물리 방어 약함)
 * - 궁수: 활 피해 +35%, 화살 소모 25% 확률로 없음 (탄약 절약)
 * - 도적: 이동속도 +10%, 치명타(구르기 직후 첫 타격) 피해 +30%
 */
public enum JobType {
    WARRIOR("전사", "근접 피해 +20%, 최대 체력 +20%", 1.20, 1.0, 1.20),
    MAGE("마법사", "원거리(화살) 피해 +25%, 최대 체력 -10%", 1.0, 1.25, 0.90),
    ARCHER("궁수", "원거리(화살) 피해 +35%", 1.0, 1.35, 1.0),
    ROGUE("도적", "근접 피해 +10%, 최대 체력 +5%", 1.10, 1.0, 1.05);

    private final String displayName;
    private final String description;
    private final double meleeMultiplier;
    private final double rangedMultiplier;
    private final double healthMultiplier;

    JobType(String displayName, String description, double meleeMultiplier, double rangedMultiplier, double healthMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.meleeMultiplier = meleeMultiplier;
        this.rangedMultiplier = rangedMultiplier;
        this.healthMultiplier = healthMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getMeleeMultiplier() {
        return meleeMultiplier;
    }

    public double getRangedMultiplier() {
        return rangedMultiplier;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    /** 한글 이름 -> JobType. 못 찾으면 null. */
    public static JobType fromDisplayName(String name) {
        for (JobType type : values()) {
            if (type.displayName.equals(name)) {
                return type;
            }
        }
        return null;
    }

    /** /직업 명령어에 목록을 보여줄 때 사용 (입력 순서 보존) */
    public static Map<String, JobType> asDisplayMap() {
        Map<String, JobType> map = new LinkedHashMap<>();
        for (JobType type : values()) {
            map.put(type.displayName, type);
        }
        return map;
    }
}
