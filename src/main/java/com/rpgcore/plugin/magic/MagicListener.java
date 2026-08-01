package com.rpgcore.plugin.magic;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * ⭐ 마법 시스템 리스너 (new).
 *
 * - 지팡이(STICK) 우클릭 → 마법 메뉴 열기
 * - 화염구 히트 → 데미지 (MagicManager에 저장된 배율 적용)
 */
public class MagicListener implements Listener {

    private final RpgCorePlugin plugin;

    public MagicListener(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 지팡이 우클릭 → 마법 선택 GUI.
     */
    @EventHandler
    public void onStickClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack hand = event.getItem();
        if (hand == null || hand.getType() != Material.STICK) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        String job = data.getJob();

        event.setCancelled(true);
        openMagicMenu(player, job);
    }

    /**
     * 마법 선택 GUI (인벤토리).
     */
    private void openMagicMenu(Player player, String job) {
        Inventory inv = player.isOp() ? 
            plugin.getServer().createInventory(null, 18, "⭐ 마법 선택 (OP)") :
            plugin.getServer().createInventory(null, 9, "마법 선택");

        int slot = 0;
        for (Magic magic : Magic.values()) {
            if (!magic.canUse(job)) {
                continue; // 이 직업이 사용할 수 없는 마법
            }

            ItemStack item = new ItemStack(magic.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(magic.getColor() + magic.getDisplayName());
                meta.setLore(java.util.List.of(
                    ChatColor.GRAY + "마나 소비: " + magic.getManaCost(),
                    ChatColor.GRAY + magic.getDescription()
                ));
                // 마법 이름을 ItemMeta에 저장
                meta.getPersistentDataContainer().set(
                    plugin.getMagicNameKey(),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    magic.name()
                );
                item.setItemMeta(meta);
            }

            inv.setItem(slot++, item);
        }

        // ⭐ OP 유저만 신의 분노 추가
        if (player.isOp()) {
            ItemStack godswrath = new ItemStack(Material.DEBUG_STICK);
            ItemMeta meta = godswrath.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "⭐ 신의 분노");
                meta.setLore(java.util.List.of(
                    ChatColor.RED + "무한한 파괴의 마법",
                    ChatColor.YELLOW + "마나 소비: 없음",
                    ChatColor.GOLD + "절대 세력을 펼쳐라!"
                ));
                meta.getPersistentDataContainer().set(
                    plugin.getMagicNameKey(),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    Magic.GODSWRATH.name()
                );
                godswrath.setItemMeta(meta);
            }
            inv.setItem(slot, godswrath);
        }

        player.openInventory(inv);
    }

    /**
     * 마법 메뉴에서 아이템 클릭 → 마법 시전.
     */
    @EventHandler
    public void onMagicMenuClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("마법 선택")) {
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
            org.bukkit.persistence.PersistentDataType.STRING
        );
        if (magicName == null) {
            return;
        }

        Magic magic = Magic.valueOf(magicName);
        player.closeInventory();
        plugin.getMagicManager().castMagic(player, magic, data);
    }

    /**
     * 화염구가 몬스터에 맞음 → 추가 데미지.
     */
    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Fireball fireball)) {
            return;
        }
        if (!(fireball.getShooter() instanceof Player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        Double magicDamage = fireball.getPersistentDataContainer().get(
            plugin.getMagicDamageKey(),
            PersistentDataType.DOUBLE
        );
        if (magicDamage == null || magicDamage <= 0) {
            return;
        }

        // 기본 화염구 데미지는 이미 적용됨, 추가 데미지만 더하기
        event.setDamage(event.getDamage() + magicDamage);
    }
}
