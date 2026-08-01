package com.rpgcore.plugin.magic;

import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * ⭐ 마법 선생 NPC (보라색 옷 입은 사서 주민).
 *
 * - 사서(Librarian) 주민을 우클릭하면 마법 배우기 GUI 열기
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
     * 마법 선생 NPC인지 확인 (보라색 옷 사서 주민).
     */
    public boolean isMagicTrainer(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Villager villager)) {
            return false;
        }
        // 사서(Librarian)만 마법 선생
        return villager.getProfession() == Villager.Profession.LIBRARIAN;
    }

    /**
     * 마법 배우기 GUI 열기.
     */
    public void openMagicTrainerMenu(Player player) {
        // 6개 마법 (Godswrath 제외) - 54 슬롯 인벤토리
        Inventory inv = plugin.getServer().createInventory(null, 9, "⭐ 마법 배우기");

        int slot = 0;
        for (Magic magic : Magic.values()) {
            // Godswrath는 GUI에 표시 안 함 (OP 전용)
            if (magic == Magic.GODSWRATH) {
                continue;
            }

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
        Msg.send(player, ChatColor.GOLD + "마법 선생: 배우고 싶은 마법을 클릭하세요!");
    }
}
