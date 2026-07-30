package com.rpgcore.plugin.listeners;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.job.PlayerStatApplier;
import com.rpgcore.plugin.util.BedrockUtil;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final RpgCorePlugin plugin;

    public PlayerJoinListener(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        PlayerStatApplier.apply(player, data);

        // ⭐ 베드락(휴대폰/콘솔) 플레이어에게는 한글 입력 없이 쓸 수 있는 명령어를 안내
        if (BedrockUtil.isBedrock(player) && plugin.getConfig().getBoolean("bedrock.welcome-hint", true)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                Msg.send(player, ChatColor.AQUA + "[RpgCore] 베드락 에디션 접속을 확인했습니다.");
                Msg.send(player, ChatColor.WHITE + "한글 입력이 불편하면 영문 명령어를 쓰세요:");
                Msg.send(player, ChatColor.GRAY + "  /job  /setjob 1  /info  /planet  /warp 2  /map  /tower  /top");
            }, 40L);
        }
    }
}
