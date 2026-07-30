package com.rpgcore.plugin.job;

import com.rpgcore.plugin.data.PlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * 레벨업/전직 등으로 스탯이 바뀔 때, 실제 플레이어의 GENERIC_MAX_HEALTH 속성에 반영한다.
 * (공격력은 속성이 아니라 전투 이벤트에서 배율로 직접 계산하므로 여기서 다루지 않음 — HunterListener 참고)
 */
public final class PlayerStatApplier {

    private PlayerStatApplier() {
    }

    public static void apply(Player player, PlayerData data) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr == null) {
            return;
        }
        double newMax = StatCalculator.scaledVanillaMaxHealth(data);
        healthAttr.setBaseValue(newMax);
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }
}
