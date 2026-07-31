package com.rpgcore.plugin.listeners;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.job.PlayerStatApplier;
import com.rpgcore.plugin.util.BedrockUtil;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

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

        // ⭐ 마나 초기화 (처음 접속하면 최대값)
        plugin.getMagicManager().setMana(player.getUniqueId(), plugin.getMagicManager().getMaxMana(data), data);

        // ⭐ 시작 아이템: 첫 접속 시만 지급 (한 번만!)
        if (!data.hasReceivedStarterItems()) {
            ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
            ItemStack stick = new ItemStack(Material.STICK);

            player.getInventory().addItem(ironSword, stick);
            data.setReceivedStarterItems(true);

            Msg.send(player, ChatColor.GOLD + "🎮 RpgCore에 오신 것을 환영합니다! 철검으로 공격, 막대기로 마법을 시전하세요!");
            Msg.send(player, ChatColor.YELLOW + "💡 무직 주민에게 가서 마법을 배우세요!");
        }

        // ⭐ 스폰 포인트로 텔레포트 (처음 접속 시만)
        if (!player.hasPlayedBefore()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                // config.yml에서 스폰 좌표 읽기
                String world = plugin.getConfig().getString("spawn.world", "world");
                double x = plugin.getConfig().getDouble("spawn.x", 100);
                double y = plugin.getConfig().getDouble("spawn.y", 65);
                double z = plugin.getConfig().getDouble("spawn.z", 200);
                float yaw = (float) plugin.getConfig().getDouble("spawn.yaw", 0);
                float pitch = (float) plugin.getConfig().getDouble("spawn.pitch", 0);

                org.bukkit.World w = plugin.getServer().getWorld(world);
                if (w != null) {
                    Location spawnLoc = new Location(w, x, y, z, yaw, pitch);
                    player.teleport(spawnLoc);
                    Msg.send(player, ChatColor.AQUA + "🏘️ 마을에 오신 것을 환영합니다!");
                }
            }, 20L); // 1초 후
        }

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
