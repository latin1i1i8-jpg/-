package com.rpgcore.plugin.tower;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.listeners.PlanetSpawnListener;
import com.rpgcore.plugin.util.KoreanMobNames;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * ⭐ 시련의 탑.
 *
 * 규칙 (요청 사항 그대로):
 *   - 탑은 <b>돌</b>로 쌓여 있다 (벽 STONE, 바닥/천장 STONE_BRICKS — config 에서 변경 가능)
 *   - 한 층에 몬스터가 <b>딱 1마리</b>만 나온다
 *   - 그 1마리를 잡으면 천장의 돌 뚜껑이 열리고, 사다리로 위층에 올라간다
 *   - 위층으로 갈수록 그 1마리가 강해진다 (층 = 단계, boss-every 층마다 보스급)
 *
 * 구조:
 *   - 탑 전용 빈 월드(trial_tower)에 플레이어별로 X축 256블록 간격의 "슬롯"을 배정
 *     → 여러 명이 동시에 각자의 탑을 올라도 서로 방해받지 않음
 *   - 층은 미리 다 짓지 않고, 올라갈 때 다음 층만 미리 지어서 서버 부담을 줄임
 *   - 죽거나 나가면 그 회차는 초기화(뚜껑 다시 닫힘)되고 다시 1층부터. 최고 기록은 저장됨
 */
public class TowerManager {

    /** 이 몹이 탑 몬스터임을 표시하고, 몇 층인지 저장 */
    public static NamespacedKey TOWER_FLOOR_KEY;
    /** 어느 슬롯(플레이어 탑)의 몹인지 */
    public static NamespacedKey TOWER_SLOT_KEY;

    private static final int BASE_Y = 64;         // 1층 바닥 높이
    private static final int FLOOR_HEIGHT = 6;    // 한 층 높이 (바닥1 + 내부4~5 + 위층 바닥이 천장)
    private static final int SLOT_SPACING = 256;  // 플레이어별 탑 간격

    private static final Random RANDOM = new Random();

    private final RpgCorePlugin plugin;

    private String worldName = "trial_tower";
    private int floors = 50;
    private int roomSize = 9;
    private double baseMult = 1.0;
    private double multPerFloor = 0.45;
    private double rewardMult = 1.5;
    private int bossEvery = 10;
    private double bossHealthMult = 2.5;
    private double bossDamageMult = 1.0;
    private boolean protectBlocks = true;
    private Material wallBlock = Material.STONE;
    private Material floorBlock = Material.STONE_BRICKS;
    private Material gateBlock = Material.STONE;
    private Material lightBlock = Material.GLOWSTONE;
    private final List<EntityType> mobs = new ArrayList<>();
    private final List<EntityType> bossMobs = new ArrayList<>();

    /** 진행 중인 도전 (플레이어별) */
    private final Map<UUID, Run> runs = new HashMap<>();
    /** 슬롯 번호 -> 사용 중인 플레이어 */
    private final Map<Integer, UUID> slotOwners = new HashMap<>();

    public TowerManager(RpgCorePlugin plugin) {
        this.plugin = plugin;
        TOWER_FLOOR_KEY = new NamespacedKey(plugin, "rpg_tower_floor");
        TOWER_SLOT_KEY = new NamespacedKey(plugin, "rpg_tower_slot");
    }

    /** 한 플레이어의 도전 상태 */
    private static class Run {
        final int slot;
        int floor = 0;                          // 현재 층 (0 = 아직 입장 전)
        final Set<Integer> slabsBuilt = new HashSet<>();
        final Set<Integer> floorsBuilt = new HashSet<>();
        final Set<Integer> gatesOpen = new HashSet<>();
        final Map<Integer, UUID> floorMobs = new HashMap<>();
        Location returnLocation;

        Run(int slot) {
            this.slot = slot;
        }
    }

    // ==========================================================
    // 설정 로드 / 월드 준비
    // ==========================================================

    public void load() {
        var cfg = plugin.getConfig();
        worldName = cfg.getString("tower.world", worldName);
        floors = Math.max(1, cfg.getInt("tower.floors", floors));
        roomSize = Math.max(5, cfg.getInt("tower.room-size", roomSize));
        baseMult = cfg.getDouble("tower.base-mult", baseMult);
        multPerFloor = cfg.getDouble("tower.mult-per-floor", multPerFloor);
        rewardMult = cfg.getDouble("tower.reward-mult", rewardMult);
        bossEvery = Math.max(0, cfg.getInt("tower.boss-every", bossEvery));
        bossHealthMult = cfg.getDouble("tower.boss-health-mult", bossHealthMult);
        bossDamageMult = cfg.getDouble("tower.boss-damage-mult", bossDamageMult);
        protectBlocks = cfg.getBoolean("tower.protect-blocks", protectBlocks);
        wallBlock = material(cfg.getString("tower.wall-block", "STONE"), Material.STONE);
        floorBlock = material(cfg.getString("tower.floor-block", "STONE_BRICKS"), Material.STONE_BRICKS);
        gateBlock = material(cfg.getString("tower.gate-block", "STONE"), Material.STONE);
        lightBlock = material(cfg.getString("tower.light-block", "GLOWSTONE"), Material.GLOWSTONE);

        mobs.clear();
        for (String name : cfg.getStringList("tower.mobs")) {
            EntityType type = entityType(name);
            if (type != null) {
                mobs.add(type);
            }
        }
        if (mobs.isEmpty()) {
            mobs.add(EntityType.ZOMBIE);
        }
        bossMobs.clear();
        for (String name : cfg.getStringList("tower.boss-mobs")) {
            EntityType type = entityType(name);
            if (type != null) {
                bossMobs.add(type);
            }
        }
        if (bossMobs.isEmpty()) {
            bossMobs.addAll(mobs);
        }

        ensureWorld();
        plugin.getLogger().info("[RpgCore] 시련의 탑 준비 완료 — " + floors + "층, 월드: " + worldName);
    }

    private Material material(String name, Material fallback) {
        Material m = Material.matchMaterial(name == null ? "" : name.toUpperCase());
        if (m == null || !m.isBlock()) {
            plugin.getLogger().warning("[tower] 사용할 수 없는 블록: " + name + " -> " + fallback);
            return fallback;
        }
        return m;
    }

    private EntityType entityType(String name) {
        try {
            return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("[tower] 알 수 없는 몬스터 타입: " + name);
            return null;
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public boolean isProtectBlocks() {
        return protectBlocks;
    }

    public double getRewardMult() {
        return rewardMult;
    }

    public int getFloors() {
        return floors;
    }

    public boolean isTowerWorld(World world) {
        return world != null && world.getName().equals(worldName);
    }

    /** 탑 월드를 만들고(없으면) 규칙을 설정한다 */
    public World ensureWorld() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = new WorldCreator(worldName)
                    .environment(World.Environment.NORMAL)
                    .generator(new VoidGenerator())
                    .createWorld();
        }
        if (world == null) {
            plugin.getLogger().severe("[tower] 탑 월드 생성 실패: " + worldName);
            return null;
        }
        // 탑 안에서는 자연 스폰 금지 (한 층에 1마리만 나오게 하려면 필수)
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setDifficulty(Difficulty.NORMAL); // 몬스터가 피해를 주도록
        world.setTime(6000L);
        world.setStorm(false);
        return world;
    }

    // ==========================================================
    // 좌표 계산
    // ==========================================================

    private int outerSize() {
        return roomSize + 2; // 벽 두께 1
    }

    private int originX(int slot) {
        return slot * SLOT_SPACING;
    }

    private int originZ() {
        return 0;
    }

    /** 해당 층의 바닥 Y */
    private int floorY(int floor) {
        return BASE_Y + (floor - 1) * FLOOR_HEIGHT;
    }

    /** 천장 뚜껑(=위층으로 가는 구멍)의 X */
    private int hatchX(int slot) {
        return originX(slot) + outerSize() / 2;
    }

    /** 천장 뚜껑의 Z (북쪽 벽 바로 앞 = 사다리 설치 위치) */
    private int hatchZ() {
        return originZ() + 1;
    }

    /** 해당 층에서 플레이어가 서는 위치 */
    private Location playerSpot(World world, int slot, int floor) {
        return new Location(world,
                hatchX(slot) + 0.5,
                floorY(floor) + 1,
                originZ() + 2.5,
                180f, 0f);
    }

    /** 해당 층에서 몬스터가 나오는 위치 (방 반대쪽) */
    private Location mobSpot(World world, int slot, int floor) {
        return new Location(world,
                originX(slot) + outerSize() / 2.0,
                floorY(floor) + 1,
                originZ() + outerSize() - 2.5);
    }

    /** Y 좌표로 몇 층인지 계산 */
    public int floorAt(Location loc) {
        int f = (loc.getBlockY() - BASE_Y) / FLOOR_HEIGHT + 1;
        return Math.max(1, Math.min(floors, f));
    }

    // ==========================================================
    // 건설
    // ==========================================================

    /** 바닥/천장 판 1장. (아래층의 뚜껑 구멍 포함) */
    private void buildSlab(World world, Run run, int slabFloor) {
        if (!run.slabsBuilt.add(slabFloor)) {
            return; // 이미 지음
        }
        int x0 = originX(run.slot);
        int z0 = originZ();
        int y = floorY(slabFloor);
        int outer = outerSize();

        for (int dx = 0; dx < outer; dx++) {
            for (int dz = 0; dz < outer; dz++) {
                world.getBlockAt(x0 + dx, y, z0 + dz).setType(floorBlock, false);
            }
        }
        // 조명 (천장에 박아 넣기 — 위층 바닥이면서 아래층 천장)
        int[][] lights = {{2, 2}, {2, outer - 3}, {outer - 3, 2}, {outer - 3, outer - 3}};
        for (int[] p : lights) {
            world.getBlockAt(x0 + p[0], y, z0 + p[1]).setType(lightBlock, false);
        }

        // 이 판은 (slabFloor - 1)층의 천장이다 -> 그 층의 뚜껑을 돌로 막아둔다
        int below = slabFloor - 1;
        if (below >= 1 && below < floors) {
            world.getBlockAt(hatchX(run.slot), y, hatchZ()).setType(gateBlock, false);
            run.gatesOpen.remove(below);
        }
    }

    /** 한 층(벽 + 사다리)을 짓고, 천장까지 확보한다 */
    private void buildFloor(World world, Run run, int floor) {
        if (floor < 1 || floor > floors) {
            return;
        }
        buildSlab(world, run, floor);          // 이 층 바닥
        if (run.floorsBuilt.contains(floor)) {
            buildSlab(world, run, floor + 1);  // 천장은 항상 확보
            return;
        }
        run.floorsBuilt.add(floor);

        int x0 = originX(run.slot);
        int z0 = originZ();
        int outer = outerSize();
        int baseY = floorY(floor);

        for (int dy = 1; dy < FLOOR_HEIGHT; dy++) {
            int y = baseY + dy;
            for (int dx = 0; dx < outer; dx++) {
                for (int dz = 0; dz < outer; dz++) {
                    boolean edge = dx == 0 || dz == 0 || dx == outer - 1 || dz == outer - 1;
                    Block block = world.getBlockAt(x0 + dx, y, z0 + dz);
                    block.setType(edge ? wallBlock : Material.AIR, false);
                }
            }
        }

        // 위층으로 가는 사다리 (북쪽 벽에 붙임)
        for (int dy = 1; dy < FLOOR_HEIGHT; dy++) {
            placeLadder(world, hatchX(run.slot), baseY + dy, hatchZ());
        }

        buildSlab(world, run, floor + 1); // 천장 (= 위층 바닥, 뚜껑은 닫힌 상태)
    }

    private void placeLadder(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.LADDER, false);
        BlockData data = block.getBlockData();
        if (data instanceof Ladder ladder) {
            ladder.setFacing(BlockFace.SOUTH); // 북쪽 벽(z0)에 붙은 사다리
            block.setBlockData(ladder, false);
        }
    }

    // ==========================================================
    // 입장 / 퇴장 / 초기화
    // ==========================================================

    private int assignSlot(UUID uuid) {
        for (Map.Entry<Integer, UUID> entry : slotOwners.entrySet()) {
            if (uuid.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        int slot = 0;
        while (slotOwners.containsKey(slot)) {
            slot++;
        }
        slotOwners.put(slot, uuid);
        return slot;
    }

    /** 탑 입장 — 항상 1층부터 새 회차 시작 */
    public void enter(Player player) {
        World world = ensureWorld();
        if (world == null) {
            Msg.send(player, ChatColor.RED + "탑 월드를 준비할 수 없습니다. 서버 관리자에게 문의하세요.");
            return;
        }
        if (isTowerWorld(player.getWorld())) {
            Msg.send(player, ChatColor.YELLOW + "이미 탑 안에 있습니다. 나가려면 /시련의탑 나가기");
            return;
        }

        Run run = runs.get(player.getUniqueId());
        if (run == null) {
            run = new Run(assignSlot(player.getUniqueId()));
            runs.put(player.getUniqueId(), run);
        } else {
            resetRun(world, run); // 이전 회차 흔적(열린 뚜껑/남은 몹) 정리
        }
        run.returnLocation = player.getLocation().clone();
        run.floor = 1;

        buildFloor(world, run, 1);
        buildFloor(world, run, 2); // 다음 층 미리 준비

        player.teleport(playerSpot(world, run.slot, 1));

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        Msg.send(player, ChatColor.GOLD + "===== 🗼 시련의 탑 =====");
        Msg.send(player, ChatColor.WHITE + "총 " + floors + "층. 한 층에 몬스터가 " + ChatColor.RED + "1마리" + ChatColor.WHITE + "씩 나옵니다.");
        Msg.send(player, ChatColor.WHITE + "그 1마리를 잡으면 천장의 돌 뚜껑이 열리고 사다리로 위층에 올라갑니다.");
        Msg.send(player, ChatColor.GRAY + "최고 기록: " + data.getTowerBest() + "층  |  나가기: /시련의탑 나가기");
        Msg.title(player, ChatColor.GOLD + "1층", ChatColor.WHITE + "몬스터를 처치하세요");

        spawnFloorMob(world, player, run, 1);
    }

    /** 탑에서 나가기 (회차 종료) */
    public void exit(Player player, boolean announce) {
        Run run = runs.get(player.getUniqueId());
        World world = Bukkit.getWorld(worldName);
        Location back = null;
        if (run != null) {
            back = run.returnLocation;
            if (world != null) {
                resetRun(world, run);
            }
            run.floor = 0;
        }
        if (isTowerWorld(player.getWorld())) {
            player.teleport(back != null ? back : safeExitLocation());
        }
        if (announce) {
            Msg.send(player, ChatColor.YELLOW + "시련의 탑에서 나왔습니다. 다시 들어가면 1층부터 시작합니다.");
        }
    }

    private Location safeExitLocation() {
        for (World world : Bukkit.getWorlds()) {
            if (!isTowerWorld(world)) {
                return world.getSpawnLocation();
            }
        }
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    /** 회차 초기화: 남은 몹 제거 + 열린 뚜껑 다시 닫기 */
    private void resetRun(World world, Run run) {
        for (UUID mobId : new ArrayList<>(run.floorMobs.values())) {
            Entity entity = Bukkit.getEntity(mobId);
            if (entity != null) {
                entity.remove();
            }
        }
        run.floorMobs.clear();

        for (Integer floor : new ArrayList<>(run.gatesOpen)) {
            int y = floorY(floor) + FLOOR_HEIGHT;
            world.getBlockAt(hatchX(run.slot), y, hatchZ()).setType(gateBlock, false);
        }
        run.gatesOpen.clear();
    }

    /** 플레이어가 접속을 끊었을 때 정리 */
    public void handleQuit(Player player) {
        Run run = runs.get(player.getUniqueId());
        if (run == null) {
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            resetRun(world, run);
        }
        run.floor = 0;
        // 탑 안에서 로그아웃했다면 다음 접속 때 밖으로 돌려보낼 좌표 저장
        if (isTowerWorld(player.getWorld()) && run.returnLocation != null) {
            plugin.getPendingTowerExit().put(player.getUniqueId(), run.returnLocation.clone());
        }
    }

    // ==========================================================
    // 층 진행
    // ==========================================================

    /** 그 층에 몬스터 1마리를 소환한다 */
    private void spawnFloorMob(World world, Player player, Run run, int floor) {
        UUID existing = run.floorMobs.get(floor);
        if (existing != null) {
            Entity entity = Bukkit.getEntity(existing);
            if (entity != null && !entity.isDead()) {
                return; // 이미 그 층 몬스터가 살아있음
            }
        }

        boolean boss = bossEvery > 0 && floor % bossEvery == 0;
        List<EntityType> pool = boss ? bossMobs : mobs;
        EntityType type = pool.get(RANDOM.nextInt(pool.size()));

        Entity spawned = world.spawnEntity(mobSpot(world, run.slot, floor), type);
        if (!(spawned instanceof LivingEntity mob)) {
            spawned.remove();
            return;
        }

        double mult = baseMult + floor * multPerFloor;
        // 보스는 배수를 키우는 대신 체력/공격력에 별도 배율을 곱한다 (급격한 난이도 점프 방지)
        plugin.getMobStats().apply(mob, mult, floor,
                boss ? bossHealthMult : 1.0,
                boss ? bossDamageMult : 1.0);

        String korean = KoreanMobNames.of(type);
        String label = boss
                ? "§4[" + floor + "층 보스] " + korean
                : "§c[" + floor + "층] " + korean;
        mob.setCustomName(label);
        mob.setCustomNameVisible(true);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        if (mob instanceof Mob aggressive) {
            aggressive.setTarget(player);
        }

        // 보상/전투 처리는 MobDeathListener 가 공용으로 담당하므로 태그를 붙여준다
        mob.getPersistentDataContainer().set(PlanetSpawnListener.PLANET_KEY, PersistentDataType.STRING, "시련의 탑");
        mob.getPersistentDataContainer().set(PlanetSpawnListener.MOB_NAME_KEY, PersistentDataType.STRING, "시련의 탑 " + korean);
        mob.getPersistentDataContainer().set(PlanetSpawnListener.STAGE_KEY, PersistentDataType.INTEGER, floor);
        mob.getPersistentDataContainer().set(TOWER_FLOOR_KEY, PersistentDataType.INTEGER, floor);
        mob.getPersistentDataContainer().set(TOWER_SLOT_KEY, PersistentDataType.INTEGER, run.slot);

        run.floorMobs.put(floor, mob.getUniqueId());

        Msg.send(player, ChatColor.RED + "▶ " + floor + "층 몬스터 등장: " + ChatColor.WHITE + korean
                + (boss ? ChatColor.DARK_RED + " (보스!)" : "")
                + ChatColor.GRAY + " [체력 " + (int) mob.getHealth() + "]");
    }

    /** 탑 몬스터가 죽었을 때 (MobDeathListener 가 호출) */
    public void onMobKilled(Player killer, int floor, int slot) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        UUID owner = slotOwners.get(slot);
        Player target = owner != null ? Bukkit.getPlayer(owner) : killer;
        if (target == null) {
            target = killer;
        }
        Run run = runs.get(target.getUniqueId());
        if (run == null) {
            return;
        }
        run.floorMobs.remove(floor);
        openGate(world, run, floor, target);
    }

    /** 뚜껑 열기 + 기록 갱신 */
    private void openGate(World world, Run run, int floor, Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (floor > data.getTowerBest()) {
            data.setTowerBest(floor);
            plugin.getDataManager().save(data);
        }

        if (floor >= floors) {
            Msg.send(player, ChatColor.GOLD + "🏆 시련의 탑 " + floors + "층 전부 정복! 최상층을 클리어했습니다!");
            Msg.title(player, ChatColor.GOLD + "탑 정복!", ChatColor.WHITE + "" + floors + "층 클리어");
            exit(player, false);
            return;
        }

        int y = floorY(floor) + FLOOR_HEIGHT;
        world.getBlockAt(hatchX(run.slot), y, hatchZ()).setType(Material.AIR, false);
        placeLadder(world, hatchX(run.slot), y, hatchZ());
        run.gatesOpen.add(floor);

        buildFloor(world, run, floor + 1); // 위층 미리 준비

        Msg.send(player, ChatColor.GREEN + "✔ " + floor + "층 클리어! 천장이 열렸습니다 — 사다리를 타고 "
                + (floor + 1) + "층으로 올라가세요.");
    }

    /**
     * 주기적 점검 (RpgCorePlugin 의 반복 작업이 호출).
     *  - 플레이어가 위층으로 올라갔으면 그 층의 몬스터 1마리를 소환
     *  - 몹이 사라졌는데 뚜껑이 닫혀 있으면 열어줘서 갇히는 상황 방지
     */
    public void tick() {
        World world = Bukkit.getWorld(worldName);
        if (world == null || runs.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Run> entry : runs.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Run run = entry.getValue();
            if (player == null || !player.isOnline() || !isTowerWorld(player.getWorld()) || run.floor <= 0) {
                continue;
            }

            int current = floorAt(player.getLocation());
            if (current > run.floor) {
                run.floor = current;
                buildFloor(world, run, current);
                buildFloor(world, run, current + 1);
                Msg.title(player, ChatColor.GOLD + "" + current + "층", ChatColor.WHITE + "몬스터를 처치하세요");
                spawnFloorMob(world, player, run, current);
                continue;
            }

            // 안전장치: 그 층 몹이 없고 뚜껑도 닫혀 있으면 다시 1마리 소환
            UUID mobId = run.floorMobs.get(run.floor);
            boolean alive = false;
            if (mobId != null) {
                Entity entity = Bukkit.getEntity(mobId);
                alive = entity != null && !entity.isDead();
            }
            if (!alive && !run.gatesOpen.contains(run.floor)) {
                spawnFloorMob(world, player, run, run.floor);
            }
        }
    }

    /** /시련의탑 정보 */
    public void showInfo(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        Run run = runs.get(player.getUniqueId());
        Msg.send(player, ChatColor.GOLD + "===== 🗼 시련의 탑 정보 =====");
        Msg.send(player, ChatColor.WHITE + "총 층수: " + ChatColor.YELLOW + floors + "층");
        Msg.send(player, ChatColor.WHITE + "규칙: 한 층에 몬스터 1마리 → 처치하면 천장이 열림 → 사다리로 위층");
        Msg.send(player, ChatColor.WHITE + "보스 층: " + (bossEvery > 0 ? bossEvery + "층마다" : "없음"));
        Msg.send(player, ChatColor.WHITE + "내 최고 기록: " + ChatColor.GREEN + data.getTowerBest() + "층");
        Msg.send(player, ChatColor.WHITE + "현재 도전 층: " + ChatColor.AQUA
                + (run != null && run.floor > 0 ? run.floor + "층" : "도전 중 아님"));
        Msg.send(player, ChatColor.GRAY + "입장: /시련의탑  ·  나가기: /시련의탑 나가기");
    }
}
