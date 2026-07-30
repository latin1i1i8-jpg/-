package com.rpgcore.plugin.planet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * config.yml 의 planets 섹션을 읽어 행성들을 로드하고,
 * 각 행성에 해당하는 월드가 없으면 생성한다.
 *
 * ⭐ 지도(맵)는 서버 주인이 직접 만듭니다:
 *   - 처음 실행하면 행성별 월드가 자동 생성됩니다 (planet_terra, planet_ignis, ...)
 *   - 그 월드에 들어가서 원하는 대로 지형/건축물을 꾸미면 그게 곧 그 행성의 지도가 됩니다
 *   - 또는 이미 만들어둔 맵 폴더를 월드 이름에 맞게 서버 폴더에 넣으면 그 맵을 그대로 사용합니다
 */
public class PlanetManager {

    private final JavaPlugin plugin;
    private final Map<String, PlanetDef> byName = new LinkedHashMap<>();
    private final Map<String, PlanetDef> byWorld = new LinkedHashMap<>();

    public PlanetManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        byName.clear();
        byWorld.clear();
        plugin.saveDefaultConfig();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("planets");
        if (root == null) {
            plugin.getLogger().warning("config.yml 에 planets 섹션이 없습니다!");
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            String worldName = sec.getString("world", "planet_" + key);
            World.Environment env;
            try {
                env = World.Environment.valueOf(sec.getString("environment", "NORMAL").toUpperCase());
            } catch (IllegalArgumentException ex) {
                env = World.Environment.NORMAL;
            }
            List<EntityType> mobs = new ArrayList<>();
            for (String mobName : sec.getStringList("mobs")) {
                try {
                    mobs.add(EntityType.valueOf(mobName.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("[" + key + "] 알 수 없는 몬스터 타입: " + mobName);
                }
            }
            PlanetDef def = new PlanetDef(
                    sec.getString("name", key),
                    worldName,
                    env,
                    sec.getDouble("mult", 1.0),
                    sec.getInt("level-requirement", 1),
                    sec.getLong("travel-cost", 0),
                    mobs
            );
            byName.put(def.getName(), def);
            byWorld.put(worldName, def);

            // 월드가 아직 없으면 생성 (이미 있으면 그대로 로드 — 직접 만든 지도 존중)
            if (Bukkit.getWorld(worldName) == null) {
                plugin.getLogger().info("행성 월드 생성/로드 중: " + worldName + " (" + def.getName() + ")");
                new WorldCreator(worldName).environment(env).createWorld();
            }
        }
        plugin.getLogger().info("행성 " + byName.size() + "개 로드 완료: " + String.join(", ", byName.keySet()));
    }

    public PlanetDef getByName(String name) {
        return byName.get(name);
    }

    public PlanetDef getByWorld(World world) {
        return byWorld.get(world.getName());
    }

    public Map<String, PlanetDef> all() {
        return byName;
    }
}
