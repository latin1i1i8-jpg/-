package com.rpgcore.plugin.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * ⭐ 플레이어 죽음 이벤트.
 * - 막대기(마법) + 철검은 죽어도 드롭되지 않음 (계속 소유)
 */
public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        // 드롭 목록에서 막대기 & 철검 제거 (플레이어가 계속 소유)
        event.getDrops().removeIf(item ->
            item.getType() == Material.STICK || item.getType() == Material.IRON_SWORD
        );

        // 부활 후 막대기 & 철검이 없으면 다시 지급
        player.getServer().getScheduler().runTaskLater(
            player.getServer().getPluginManager().getPlugin("RpgCore"),
            () -> {
                if (!player.isOnline()) return;

                boolean hasStick = player.getInventory().contains(Material.STICK);
                boolean hasSword = player.getInventory().contains(Material.IRON_SWORD);

                if (!hasStick) {
                    player.getInventory().addItem(new ItemStack(Material.STICK));
                }
                if (!hasSword) {
                    player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
                }
            },
            1L // 1틱 후
        );
    }
}
