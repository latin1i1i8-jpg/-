package com.rpgcore.plugin.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 유저 1명의 RPG 데이터.
 * 디스코드 봇의 rpg 딕셔너리(level/xp/gold/job/stage)를 그대로 옮긴 개념입니다.
 */
public class PlayerData {

    private final UUID uuid;
    private int level = 1;
    private long xp = 0;
    private long gold = 0;
    private String job = null;      // 예: "전사", "마법사", "궁수", "도적" (null = 무직)
    private int stage = 1;          // 도전 가능한 최고 단계 (사냥으로 진행)
    private int kills = 0;
    private String planet = null;   // 마지막으로 이동한 행성 이름 (정보용)
    private int towerBest = 0;      // 시련의 탑 최고 도달 층
    private boolean receivedStarterItems = false; // ⭐ 처음 접속 시 아이템 받았는지
    private final Map<String, Integer> killCounts = new HashMap<>();  // 몬스터별 처치 횟수 (은퇴 판정)

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public long getGold() {
        return gold;
    }

    public void setGold(long gold) {
        this.gold = Math.max(0, gold);
    }

    public void addGold(long amount) {
        setGold(this.gold + amount);
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public int getKills() {
        return kills;
    }

    public void incrementKills() {
        this.kills++;
    }

    public String getPlanet() {
        return planet;
    }

    public void setPlanet(String planet) {
        this.planet = planet;
    }

    /** 시련의 탑 최고 기록(층) */
    public int getTowerBest() {
        return towerBest;
    }

    public void setTowerBest(int towerBest) {
        this.towerBest = Math.max(0, towerBest);
    }

    /** 특정 몬스터(키)의 누적 처치 횟수 */
    public int getKillCount(String mobKey) {
        return killCounts.getOrDefault(mobKey, 0);
    }

    public void addKillCount(String mobKey) {
        killCounts.merge(mobKey, 1, Integer::sum);
    }

    public Map<String, Integer> getKillCounts() {
        return killCounts;
    }

    /** ⭐ 처음 접속 시 아이템 받았는지 */
    public boolean hasReceivedStarterItems() {
        return receivedStarterItems;
    }

    public void setReceivedStarterItems(boolean received) {
        this.receivedStarterItems = received;
    }
}
