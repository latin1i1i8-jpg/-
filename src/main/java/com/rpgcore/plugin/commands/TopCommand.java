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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopCommand implements CommandExecutor {

    private final RpgCorePlugin plugin;

    public TopCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Map<java.util.UUID, PlayerData> all = plugin.getDataManager().all();
        List<PlayerData> ranked = all.values().stream()
                .sorted(Comparator.comparingInt(PlayerData::getLevel).reversed()
                        .thenComparing(Comparator.comparingLong(PlayerData::getXp).reversed()))
                .limit(10)
                .collect(Collectors.toList());

        Msg.send(sender, ChatColor.GOLD + "===== 레벨 랭킹 TOP 10 =====");
        int rank = 1;
        for (PlayerData data : ranked) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(data.getUuid());
            String name = op.getName() != null ? op.getName() : data.getUuid().toString().substring(0, 8);
            Msg.send(sender, ChatColor.WHITE + "" + rank + "위. " + name
                    + ChatColor.GRAY + " - Lv." + data.getLevel() + " (" + (data.getJob() == null ? "무직" : data.getJob()) + ")");
            rank++;
        }
        if (ranked.isEmpty()) {
            Msg.send(sender, ChatColor.GRAY + "아직 기록이 없습니다.");
        }
        return true;
    }
}
