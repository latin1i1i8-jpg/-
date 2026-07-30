package com.rpgcore.plugin.commands;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.job.StatCalculator;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InfoCommand implements CommandExecutor {

    private final RpgCorePlugin plugin;

    public InfoCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        long needed = StatCalculator.xpNeeded(data.getLevel());

        Msg.send(player, ChatColor.GOLD + "===== " + player.getName() + " 의 정보 =====");
        Msg.send(player, ChatColor.WHITE + "레벨: " + ChatColor.YELLOW + data.getLevel()
                + ChatColor.GRAY + "  (XP " + data.getXp() + "/" + needed + ")");
        Msg.send(player, ChatColor.WHITE + "직업: " + ChatColor.AQUA + (data.getJob() == null ? "무직 (/직업)" : data.getJob()));
        Msg.send(player, ChatColor.WHITE + "골드: " + ChatColor.GOLD + data.getGold() + "G");
        Msg.send(player, ChatColor.WHITE + "현재 행성: " + ChatColor.GREEN + (data.getPlanet() == null ? "테라 (기본)" : data.getPlanet()) + ChatColor.GRAY + " (처치 수 " + data.getKills() + ")");
        Msg.send(player, ChatColor.WHITE + "공격력 지표: " + ChatColor.RED + String.format("%.1f", StatCalculator.attackPower(data)));
        Msg.send(player, ChatColor.WHITE + "시련의 탑 최고 기록: " + ChatColor.LIGHT_PURPLE + data.getTowerBest() + "층");
        return true;
    }
}
