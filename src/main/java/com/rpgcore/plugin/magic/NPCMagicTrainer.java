package com.rpgcore.plugin.magic;

import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * ⭐ 마법 사범 NPC (무직 주민).
 *
 * - 마을의 무직 주민(WanderingTrader)을 우클릭하면 마법 배우기 GUI 열기
 * - 마법을 배우면 인벤토리에 마법 아이템 추가
 * - 마나 소비로 마법을 시전
 */
public class NPCMagicTrainer {

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final org.bukkit.NamespacedKey magicNameKey;

    public NPCMagicTrainer(org.bukkit.plugin.java.JavaPlugin plugin, org.bukkit.NamespacedKey magicNameKey) {
        this.plugin = plugin;
        this.magicNameKey = magicNameKey;
    }

    /**
     * 마법 사범 NPC인지 확인 (무직 주민, 이름 포함).
     */
    public boolean isMagicTrainer(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof WanderingTrader npc)) {
            return false;
        }
        // 무직 주민 자체가 마법 사범으로 설정됨
        return true;
    }

    /**
     * 마법 배우기 GUI 열기.
     */
    public void openMagicTrainerMenu(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 9, "⭐ 마법 배우기");

        int slot = 0;
        for (Magic magic : Magic.values()) {
            ItemStack item = new ItemStack(magic.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(magic.getColor() + magic.getDisplayName());
                meta.setLore(java.util.List.of(
                    ChatColor.GRAY + magic.getDescription(),
                    ChatColor.YELLOW + "마나 소비: " + magic.getManaCost(),
                    ChatColor.GREEN + "클릭해서 배우기"
                ));

                // 마법 이름을 저장
                meta.getPersistentDataContainer().set(
                    magicNameKey,
                    PersistentDataType.STRING,
                    magic.name()
                );
                item.setItemMeta(meta);
            }

            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
        Msg.send(player, ChatColor.GOLD + "마법 사범: 배우고 싶은 마법을 클릭하세요!");
    }
}
