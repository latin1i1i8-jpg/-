package com.rpgcore.plugin.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * players.yml 에 모든 플레이어의 RPG 데이터를 저장/로드한다.
 * 디스코드 봇의 gacha_data.json 저장 방식과 같은 역할.
 */
public class DataManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private FileConfiguration config;

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "players.yml 생성 실패", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        cache.clear();
        if (config.isConfigurationSection("players")) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                PlayerData data = new PlayerData(uuid);
                String path = "players." + key + ".";
                data.setLevel(config.getInt(path + "level", 1));
                data.setXp(config.getLong(path + "xp", 0));
                data.setGold(config.getLong(path + "gold", 0));
                data.setJob(config.getString(path + "job", null));
                data.setStage(config.getInt(path + "stage", 1));
                data.setPlanet(config.getString(path + "planet", null));
                data.setTowerBest(config.getInt(path + "tower-best", 0));
                data.setReceivedStarterItems(config.getBoolean(path + "received-starter-items", false)); // ⭐ 스타터 아이템
                if (config.isConfigurationSection(path + "killcounts")) {
                    for (String mobKey : config.getConfigurationSection(path + "killcounts").getKeys(false)) {
                        int count = config.getInt(path + "killcounts." + mobKey, 0);
                        for (int i = 0; i < count; i++) {
                            data.addKillCount(mobKey);
                        }
                    }
                }
                cache.put(uuid, data);
            }
        }
        plugin.getLogger().info("[RpgCore] 플레이어 데이터 " + cache.size() + "명 로드 완료");
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            writeToConfig(data);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "players.yml 저장 실패", e);
        }
    }

    /** 개별 플레이어만 즉시 저장 (레벨업/골드 변화 등 중요 이벤트 직후 호출용) */
    public void save(PlayerData data) {
        writeToConfig(data);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "players.yml 저장 실패", e);
        }
    }

    private void writeToConfig(PlayerData data) {
        String path = "players." + data.getUuid() + ".";
        config.set(path + "level", data.getLevel());
        config.set(path + "xp", data.getXp());
        config.set(path + "gold", data.getGold());
        config.set(path + "job", data.getJob());
        config.set(path + "stage", data.getStage());
        config.set(path + "planet", data.getPlanet());
        config.set(path + "tower-best", data.getTowerBest());
        config.set(path + "received-starter-items", data.hasReceivedStarterItems()); // ⭐ 스타터 아이템
        config.set(path + "killcounts", null); // 기존 섹션 초기화 후 다시 기록
        for (Map.Entry<String, Integer> entry : data.getKillCounts().entrySet()) {
            config.set(path + "killcounts." + entry.getKey(), entry.getValue());
        }
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public Map<UUID, PlayerData> all() {
        return cache;
    }
}
