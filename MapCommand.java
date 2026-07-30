package com.rpgcore.plugin.commands;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.listeners.PlanetSpawnListener;
import com.rpgcore.plugin.planet.PlanetDef;
import com.rpgcore.plugin.util.KoreanMobNames;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.StringJoiner;

/**
 * /지도 : 현재 행성의 "지도" 정보를 보여준다.
 *   - 이 행성에 돌아다니는 몬스터 종류
 *   - 스폰 지점 기준 거리별 난이도 링 (멀수록 단계가 높음)
 *   - 내가 지금 서 있는 위치의 단계
 */
public class MapCommand implements CommandExecutor {

    private final RpgCorePlugin plugin;

    public MapCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        PlanetDef planet = plugin.getPlanetManager().getByWorld(player.getWorld());
        if (planet == null) {
            Msg.send(player, ChatColor.RED + "여기는 행성 차원이 아닙니다. /행성이동 으로 행성에 가서 사용하세요.");
            return true;
        }

        Location spawn = player.getWorld().getSpawnLocation();
        Location loc = player.getLocation();
        double dist = Math.sqrt(Math.pow(loc.getX() - spawn.getX(), 2) + Math.pow(loc.getZ() - spawn.getZ(), 2));
        int myStage = PlanetSpawnListener.stageAt(loc);

        StringJoiner mobs = new StringJoiner(", ");
        for (EntityType type : planet.getMobs()) {
            mobs.add(KoreanMobNames.of(type));
        }

        double bps = PlanetSpawnListener.BLOCKS_PER_STAGE;
        Msg.send(player, ChatColor.GOLD + "===== 🗺️ " + planet.getName() + " 행성 지도 =====");
        Msg.send(player, ChatColor.WHITE + "서식 몬스터: " + ChatColor.RED + mobs);
        Msg.send(player, ChatColor.WHITE + "난이도 링 (스폰 지점 기준 거리):");
        Msg.send(player, ChatColor.GRAY + "  0 ~ " + (int) (bps * 10) + "m → 1~10단계");
        Msg.send(player, ChatColor.GRAY + "  " + (int) (bps * 10) + " ~ " + (int) (bps * 30) + "m → 11~30단계");
        Msg.send(player, ChatColor.GRAY + "  " + (int) (bps * 30) + " ~ " + (int) (bps * 60) + "m → 31~60단계");
        Msg.send(player, ChatColor.GRAY + "  " + (int) (bps * 60) + "m 이상 → 61~99단계 (최대)");
        Msg.send(player, ChatColor.WHITE + "내 위치: " + ChatColor.YELLOW + "스폰에서 " + (int) dist + "m → " + myStage + "단계 구역");
        Msg.send(player, ChatColor.GRAY + "※ 같은 몬스터(종류+단계)는 2번 잡으면 그 뒤로 보상이 없습니다. 더 먼 곳으로 나아가세요!");
        Msg.send(player, ChatColor.GRAY + "※ 층마다 몬스터 1마리와 싸우는 /시련의탑 도 있습니다.");
        return true;
    }
}
