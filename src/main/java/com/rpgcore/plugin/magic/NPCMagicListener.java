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
 * ⭐ 마법 사범(무직 주민) 상호작용 처리.
 *
 * - 무직 주민 우클릭 → 마법 배우기 GUI
 * - 마법 선택 → 마나 소비 후 마법 배우기 (막대기에 자동 포함)
 */
public class NPCMagicListener implements Listener {

    private final RpgCorePlugin plugin;
    private final NPCMagicTrainer trainer;

    public NPCMagicListener(RpgCorePlugin plugin, NPCMagicTrainer trainer) {
        this.plugin = plugin;
        this.trainer = trainer;
    }

    /**
     * 무직 주민 우클릭 → 마법 배우기 GUI.
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
     * 마법 학습 GUI에서 마법 선택 → 마나 소비 + 마법 습득.
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

        // 마나 확인
        int mana = plugin.getMagicManager().getMana(player.getUniqueId(), data);
        if (mana < magic.getManaCost()) {
            Msg.send(player, ChatColor.RED + "마나가 부족합니다! (필요: " + magic.getManaCost() + ", 보유: " + mana + ")");
            return;
        }

        // 마나 소비
        plugin.getMagicManager().addMana(player.getUniqueId(), -magic.getManaCost(), data);

        // 마법 학습 메시지
        Msg.send(player, magic.getColor() + "✨ " + magic.getDisplayName() + "를 배웠습니다! (-" + magic.getManaCost() + " 마나)");
        Msg.send(player, ChatColor.YELLOW + "이제 막대기를 우클릭하면 " + magic.getDisplayName() + "을 사용할 수 있습니다!");
    }
}
