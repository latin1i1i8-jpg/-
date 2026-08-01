package com.rpgcore.plugin.commands;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /시련의탑            -> 탑 1층 입장 (별칭: /tower, /탑)
 * /시련의탑 나가기      -> 탑 밖으로 (별칭: exit, out, leave)
 * /시련의탑 정보        -> 규칙/기록 확인 (별칭: info)
 */
public class TowerCommand implements CommandExecutor {

    private final RpgCorePlugin plugin;

    public TowerCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("나가기") || sub.equals("퇴장") || sub.equals("exit") || sub.equals("out") || sub.equals("leave")) {
                plugin.getTowerManager().exit(player, true);
                return true;
            }
            if (sub.equals("정보") || sub.equals("info") || sub.equals("help")) {
                plugin.getTowerManager().showInfo(player);
                return true;
            }
            Msg.send(player, ChatColor.RED + "사용법: /시련의탑 [나가기|정보]");
            return true;
        }

        plugin.getTowerManager().enter(player);
        return true;
    }
}
