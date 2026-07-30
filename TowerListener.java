package com.rpgcore.plugin.tower;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * 시련의 탑 부가 처리.
 *  - 탑 안 블록 파괴/설치 금지 (돌 벽을 뚫고 층을 건너뛰는 것 방지, 관리자는 예외)
 *  - 탑에서 죽으면 회차 종료 (다시 들어가면 1층부터)
 *  - 탑 안에서 로그아웃 -> 다음 접속 때 원래 있던 곳으로 돌려보냄
 */
public class TowerListener implements Listener {

    private final RpgCorePlugin plugin;

    public TowerListener(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        TowerManager tower = plugin.getTowerManager();
        if (!tower.isProtectBlocks() || !tower.isTowerWorld(event.getBlock().getWorld())) {
            return;
        }
        if (event.getPlayer().hasPermission("rpgcore.admin")) {
            return;
        }
        event.setCancelled(true);
        Msg.send(event.getPlayer(), ChatColor.RED + "탑의 돌벽은 부술 수 없습니다. 몬스터를 잡아 천장을 여세요!");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        TowerManager tower = plugin.getTowerManager();
        if (!tower.isProtectBlocks() || !tower.isTowerWorld(event.getBlock().getWorld())) {
            return;
        }
        if (event.getPlayer().hasPermission("rpgcore.admin")) {
            return;
        }
        event.setCancelled(true);
        Msg.send(event.getPlayer(), ChatColor.RED + "탑 안에서는 블록을 놓을 수 없습니다.");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        TowerManager tower = plugin.getTowerManager();
        if (!tower.isTowerWorld(event.getEntity().getWorld())) {
            return;
        }
        Msg.send(event.getEntity(), ChatColor.RED + "시련의 탑에서 쓰러졌습니다. 다시 도전하면 1층부터 시작합니다.");
        tower.exit(event.getEntity(), false);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        TowerManager tower = plugin.getTowerManager();
        if (tower.isTowerWorld(event.getRespawnLocation().getWorld())) {
            // 탑 월드에서 리스폰되지 않도록 밖으로
            Location out = plugin.getPendingTowerExit().remove(event.getPlayer().getUniqueId());
            if (out == null) {
                for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                    if (!tower.isTowerWorld(world)) {
                        out = world.getSpawnLocation();
                        break;
                    }
                }
            }
            if (out != null) {
                event.setRespawnLocation(out);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTowerManager().handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Location out = plugin.getPendingTowerExit().remove(event.getPlayer().getUniqueId());
        if (out != null && plugin.getTowerManager().isTowerWorld(event.getPlayer().getWorld())) {
            event.getPlayer().teleport(out);
            Msg.send(event.getPlayer(), ChatColor.GRAY + "탑 도전이 초기화되어 밖으로 이동했습니다. (/시련의탑 으로 다시 도전)");
        }
    }
}
