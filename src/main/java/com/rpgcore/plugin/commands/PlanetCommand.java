package com.rpgcore.plugin.commands;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.planet.PlanetDef;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /행성 (=/planet)              -> 행성 목록 (이동 조건 포함)
 * /행성이동 <이름|번호> (=/warp) -> 조건 검사 후 해당 행성 차원으로 텔레포트
 *
 * ⭐ 베드락 배려: 번호로도 이동 가능 (/warp 2)
 */
public class PlanetCommand implements CommandExecutor {

    private final RpgCorePlugin plugin;

    public PlanetCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        if (command.getName().equals("행성")) {
            listPlanets(player);
            return true;
        }

        // /행성이동
        if (args.length < 1) {
            Msg.send(player, ChatColor.RED + "사용법: /행성이동 <행성이름 또는 번호>  (예: /행성이동 이그니스, /행성이동 2)");
            listPlanets(player);
            return true;
        }

        List<PlanetDef> ordered = new ArrayList<>(plugin.getPlanetManager().all().values());
        PlanetDef target = null;
        try {
            int index = Integer.parseInt(args[0].trim());
            if (index >= 1 && index <= ordered.size()) {
                target = ordered.get(index - 1);
            }
        } catch (NumberFormatException ignored) {
            // 이름으로 찾기
        }
        if (target == null) {
            target = plugin.getPlanetManager().getByName(String.join(" ", args).trim());
        }
        if (target == null) {
            Msg.send(player, ChatColor.RED + "그런 행성은 없습니다. /행성 으로 목록을 확인하세요.");
            return true;
        }

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data.getLevel() < target.getLevelRequirement()) {
            Msg.send(player, ChatColor.RED + "레벨이 부족합니다. " + target.getName()
                    + " 행성은 Lv." + target.getLevelRequirement() + " 이상부터 이동 가능합니다. (현재 Lv." + data.getLevel() + ")");
            return true;
        }
        if (data.getGold() < target.getTravelCost()) {
            Msg.send(player, ChatColor.RED + "골드가 부족합니다. 이동 비용: " + target.getTravelCost() + "G (보유: " + data.getGold() + "G)");
            return true;
        }
        World world = Bukkit.getWorld(target.getWorldName());
        if (world == null) {
            Msg.send(player, ChatColor.RED + "행성 차원이 아직 준비되지 않았습니다. 서버 관리자에게 문의하세요.");
            return true;
        }

        // 탑 안에 있었다면 도전을 정리하고 나간다
        if (plugin.getTowerManager().isTowerWorld(player.getWorld())) {
            plugin.getTowerManager().exit(player, false);
        }

        data.addGold(-target.getTravelCost());
        data.setPlanet(target.getName());
        plugin.getDataManager().save(data);

        Location spawn = world.getSpawnLocation();
        player.teleport(spawn);
        Msg.send(player, ChatColor.GREEN + "🚀 " + target.getName() + " 행성에 도착했습니다!"
                + (target.getTravelCost() > 0 ? " (이동비 " + target.getTravelCost() + "G)" : ""));
        Msg.send(player, ChatColor.GRAY + "스폰에서 멀어질수록 몬스터가 강해집니다. /지도 로 난이도 링을 확인하세요.");
        return true;
    }

    private void listPlanets(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        PlanetDef current = plugin.getPlanetManager().getByWorld(player.getWorld());
        Msg.send(player, ChatColor.GOLD + "===== 🪐 행성 목록 =====");
        int index = 1;
        for (PlanetDef p : plugin.getPlanetManager().all().values()) {
            String mark;
            if (current != null && current.getName().equals(p.getName())) {
                mark = ChatColor.AQUA + " ← 현재 위치";
            } else if (data.getLevel() >= p.getLevelRequirement()) {
                mark = ChatColor.GREEN + " (이동 가능)";
            } else {
                mark = ChatColor.DARK_GRAY + " (Lv." + p.getLevelRequirement() + " 필요)";
            }
            Msg.send(player, ChatColor.WHITE + "" + index + ". " + p.getName()
                    + ChatColor.GRAY + " · 배수 x" + (long) p.getMult()
                    + " · 이동비 " + p.getTravelCost() + "G" + mark);
            index++;
        }
        Msg.send(player, ChatColor.GRAY + "/행성이동 <이름 또는 번호> 로 이동  (베드락: /warp 2)");
    }
}
