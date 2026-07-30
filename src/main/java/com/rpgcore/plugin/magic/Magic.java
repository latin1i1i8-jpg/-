package com.rpgcore.plugin.magic;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * 마법 4가지 정의.
 *
 * 직업별 마법:
 *  - 마법사: 모두 (화염 + 얼음 + 번개 + 치유)
 *  - 전사: 번개만
 *  - 궁수: 화염만
 *  - 도적: 번개만
 */
public enum Magic {
    FIRE(
        "화염술",
        ChatColor.RED,
        Material.REDSTONE,
        4.0,      // 데미지 배율 (플레이어 공격력의 배수)
        15,       // 마나 소비
        "화염이 몬스터를 삼킨다! 화상 2초"
    ),
    FREEZE(
        "얼음술",
        ChatColor.AQUA,
        Material.SNOWBALL,
        3.0,
        15,
        "얼음이 몬스터를 얼린다! 느려짐 3초"
    ),
    LIGHTNING(
        "번개술",
        ChatColor.YELLOW,
        Material.NETHER_STAR,
        5.0,
        20,
        "번개가 몬스터를 내려찍는다! 스턴 1초"
    ),
    HEAL(
        "치유술",
        ChatColor.GREEN,
        Material.LIME_DYE,
        0.0,      // 대미지 없음 (체력 회복)
        20,
        "따뜻한 빛이 감싼다. 체력 회복!"
    );

    private final String displayName;
    private final ChatColor color;
    private final Material icon;
    private final double damageMult;
    private final int manaCost;
    private final String description;

    Magic(String displayName, ChatColor color, Material icon, double damageMult, int manaCost, String description) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.damageMult = damageMult;
        this.manaCost = manaCost;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public Material getIcon() {
        return icon;
    }

    public double getDamageMult() {
        return damageMult;
    }

    public int getManaCost() {
        return manaCost;
    }

    public String getDescription() {
        return description;
    }

    /** 해당 마법을 사용할 수 있는 직업들 */
    public boolean canUse(String job) {
        if (job == null) {
            return false;
        }
        return switch (this) {
            case FIRE, FREEZE, LIGHTNING, HEAL -> job.equals("마법사");
            case LIGHTNING -> job.equals("마법사") || job.equals("전사") || job.equals("도적");
            case FIRE -> job.equals("마법사") || job.equals("궁수");
            default -> false;
        };
    }
}
