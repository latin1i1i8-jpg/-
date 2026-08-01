package com.rpgcore.plugin.magic;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * ⭐ 마법 선생(보라색 사서 주민) 상호작용 처리.
 *
 * - 보라색 주민 우클릭 → 마법 배우기 GUI
 * - 마법 선택 → 에메랄드 5개 소비 → 배우기 완료!
 */
public class NPCMagicListener implements Listener {

    private final RpgCorePlugin plugin;
    private final NPCMagicTrainer trainer;

    public NPCMagicListener(RpgCorePlugin plugin, NPCMagicTrainer trainer) {
        this.plugin = plugin;
        this.trainer = trainer;
    }

    /**
     * 보라색 주민(사서) 우클릭 → 마법 배우기 GUI.
     */
    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        if (!trainer.isMagicTrainer(event.getRightClicked())) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        trainer.openMagicTrainerMenu(player);
    }

    /**
     * 마법 학습 GUI에서 마법 선택 → 에메랄드 5개 소비 + 마법 습득.
     */
    @EventHandler
    public void onLearnMagic(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("마법 배우기")) {
            return;
        }
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }

        String magicName = meta.getPersistentDataContainer().get(
            plugin.getMagicNameKey(),
            PersistentDataType.STRING
        );
        if (magicName == null) {
            return;
        }

        Magic magic = Magic.valueOf(magicName);
        player.closeInventory();

        // ⭐ 에메랄드 5개 필요
        int emeraldCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.EMERALD) {
                emeraldCount += item.getAmount();
            }
        }

        if (emeraldCount < 5) {
            Msg.send(player, ChatColor.RED + "에메랄드가 부족합니다! (필요: 5개, 보유: " + emeraldCount + "개)");
            return;
        }

        // 에메랄드 5개 제거
        int toRemove = 5;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.EMERALD && toRemove > 0) {
                if (item.getAmount() >= toRemove) {
                    item.setAmount(item.getAmount() - toRemove);
                    toRemove = 0;
                    break;
                } else {
                    toRemove -= item.getAmount();
                    item.setAmount(0);
                }
            }
        }

        // 마법 학습 메시지
        Msg.send(player, magic.getColor() + "✨ " + magic.getDisplayName() + "을(를) 배웠습니다! (-에메랄드 5개)");
        Msg.send(player, ChatColor.YELLOW + "이제 막대기를 우클릭하면 " + magic.getDisplayName() + "을 사용할 수 있습니다!");
    }
}
