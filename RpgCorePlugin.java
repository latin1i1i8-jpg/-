package com.rpgcore.plugin;

import com.rpgcore.plugin.commands.GiveGoldCommand;
import com.rpgcore.plugin.commands.InfoCommand;
import com.rpgcore.plugin.commands.JobCommand;
import com.rpgcore.plugin.commands.MapCommand;
import com.rpgcore.plugin.commands.PlanetCommand;
import com.rpgcore.plugin.commands.TopCommand;
import com.rpgcore.plugin.commands.TowerCommand;
import com.rpgcore.plugin.data.DataManager;
import com.rpgcore.plugin.listeners.MobDeathListener;
import com.rpgcore.plugin.listeners.PlanetSpawnListener;
import com.rpgcore.plugin.listeners.PlayerJoinListener;
import com.rpgcore.plugin.mob.MobStats;
import com.rpgcore.plugin.planet.PlanetManager;
import com.rpgcore.plugin.tower.TowerListener;
import com.rpgcore.plugin.tower.TowerManager;
import com.rpgcore.plugin.tower.VoidGenerator;
import com.rpgcore.plugin.util.BedrockUtil;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RpgCorePlugin extends JavaPlugin {

    private DataManager dataManager;
    private PlanetManager planetManager;
    private MobStats mobStats;
    private TowerManager towerManager;

    /** 탑 안에서 로그아웃한 플레이어를 다음 접속 때 돌려보낼 좌표 */
    private final Map<UUID, Location> pendingTowerExit = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 베드락(Geyser) 관련 옵션
        Msg.stripEmoji = getConfig().getBoolean("bedrock.strip-emoji", true);

        // 몬스터 태그 키를 가장 먼저 초기화 (행성/탑이 공용으로 사용)
        PlanetSpawnListener.initKeys(this);

        this.dataManager = new DataManager(this);
        this.dataManager.load();

        // ⭐ 바닐라 몬스터의 체력/공격력 설정 로드
        this.mobStats = new MobStats(this);
        this.mobStats.load();

        // 행성 차원들 로드/생성 (config.yml 의 planets 섹션 기반)
        this.planetManager = new PlanetManager(this);
        this.planetManager.load();

        // ⭐ 시련의 탑 (돌 탑 + 한 층에 몬스터 1마리)
        this.towerManager = new TowerManager(this);
        this.towerManager.load();

        register("직업", new JobCommand(this));
        register("전직", new JobCommand(this));
        register("내정보", new InfoCommand(this));
        register("랭킹", new TopCommand(this));
        register("골드지급", new GiveGoldCommand(this));
        register("행성", new PlanetCommand(this));
        register("행성이동", new PlanetCommand(this));
        register("지도", new MapCommand(this));
        register("시련의탑", new TowerCommand(this));

        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlanetSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new TowerListener(this), this);

        // 탑 진행 점검 (0.5초마다) — 위층에 올라가면 그 층 몬스터 1마리 소환
        getServer().getScheduler().runTaskTimer(this, () -> towerManager.tick(), 40L, 10L);

        // 5분마다 자동 저장 (서버 다운 시 데이터 손실 최소화)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> dataManager.saveAll(), 6000L, 6000L);

        getLogger().info("RpgCore 활성화 완료! (행성 차원 + 바닐라 몬스터 스탯 조정 + 시련의 탑)");
        if (BedrockUtil.isFloodgateInstalled()) {
            getLogger().info("베드락 에디션 크로스플레이 지원 중 (Geyser/Floodgate 감지)");
        } else {
            getLogger().info("베드락으로도 접속시키려면 Geyser-Spigot + Floodgate 플러그인을 넣으세요. (README 참고)");
        }
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("plugin.yml 에 명령어가 없습니다: " + name);
            return;
        }
        command.setExecutor(executor);
    }

    /**
     * 시련의 탑 월드는 지형이 없는 빈 월드여야 한다.
     * (bukkit.yml 등에서 월드를 미리 불러오는 경우에도 안전하도록 여기서 지정)
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (towerManager != null && worldName.equals(towerManager.getWorldName())) {
            return new VoidGenerator();
        }
        if (worldName.equals(getConfig().getString("tower.world", "trial_tower"))) {
            return new VoidGenerator();
        }
        return null;
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("RpgCore 비활성화 — 데이터 저장 완료");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public PlanetManager getPlanetManager() {
        return planetManager;
    }

    public MobStats getMobStats() {
        return mobStats;
    }

    public TowerManager getTowerManager() {
        return towerManager;
    }

    public Map<UUID, Location> getPendingTowerExit() {
        return pendingTowerExit;
    }
}
