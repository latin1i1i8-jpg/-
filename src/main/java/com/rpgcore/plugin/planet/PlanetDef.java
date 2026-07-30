package com.rpgcore.plugin.planet;

import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.List;

/**
 * 행성 1개의 정의. config.yml 에서 로드된다.
 * 각 행성은 자기만의 월드(지도)를 가지며, 그 월드 안에서는
 * 이 행성 테마에 맞는 몬스터들만 자연 스폰되어 곳곳을 돌아다닌다.
 */
public class PlanetDef {

    private final String name;           // 예: "테라"
    private final String worldName;      // 예: "planet_terra"
    private final World.Environment environment;
    private final double mult;           // 몬스터 체력/공격력/보상 배수 (디스코드 봇의 mult)
    private final int levelRequirement;  // 이 행성에 입장 가능한 최소 레벨
    private final long travelCost;       // 이동 비용 (골드)
    private final List<EntityType> mobs; // 이 행성에서 돌아다니는 몬스터 종류

    public PlanetDef(String name, String worldName, World.Environment environment,
                     double mult, int levelRequirement, long travelCost, List<EntityType> mobs) {
        this.name = name;
        this.worldName = worldName;
        this.environment = environment;
        this.mult = mult;
        this.levelRequirement = levelRequirement;
        this.travelCost = travelCost;
        this.mobs = mobs;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public double getMult() {
        return mult;
    }

    public int getLevelRequirement() {
        return levelRequirement;
    }

    public long getTravelCost() {
        return travelCost;
    }

    public List<EntityType> getMobs() {
        return mobs;
    }
}
