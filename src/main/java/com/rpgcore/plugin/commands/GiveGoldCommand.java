package com.rpgcore.plugin.commands;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GiveGoldCommand implements CommandExecutor {

    private final RpgCorePlugin plugin;

    public GiveGoldCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rpgcore.admin")) {
            Msg.send(sender, ChatColor.RED + "권한이 없습니다.");
            return true;
        }
        if (args.length < 2) {
            Msg.send(sender, ChatColor.RED + "사용법: /골드지급 <플레이어> <금액>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Msg.send(sender, ChatColor.RED + "해당 플레이어를 찾을 수 없습니다.");
            return true;
        }
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException ex) {
            Msg.send(sender, ChatColor.RED + "금액은 숫자로 입력해주세요.");
            return true;
        }
        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        data.addGold(amount);
        plugin.getDataManager().save(data);
        Msg.send(sender, ChatColor.GREEN + target.getName() + " 에게 " + amount + "G 지급 완료! (보유: " + data.getGold() + "G)");
        return true;
    }
}
