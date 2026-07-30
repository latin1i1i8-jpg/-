package com.rpgcore.plugin.util;

import org.bukkit.entity.EntityType;

import java.util.Map;

/** 자주 쓰는 적대 몬스터의 한글 표시 이름. 없으면 영문 이름을 다듬어서 반환. */
public final class KoreanMobNames {

    private static final Map<EntityType, String> NAMES = Map.ofEntries(
            Map.entry(EntityType.ZOMBIE, "좀비"),
            Map.entry(EntityType.HUSK, "허스크"),
            Map.entry(EntityType.DROWNED, "익사체"),
            Map.entry(EntityType.SKELETON, "스켈레톤"),
            Map.entry(EntityType.STRAY, "스트레이"),
            Map.entry(EntityType.WITHER_SKELETON, "위더 스켈레톤"),
            Map.entry(EntityType.SPIDER, "거미"),
            Map.entry(EntityType.CAVE_SPIDER, "동굴 거미"),
            Map.entry(EntityType.CREEPER, "크리퍼"),
            Map.entry(EntityType.PHANTOM, "팬텀"),
            Map.entry(EntityType.WITCH, "마녀"),
            Map.entry(EntityType.BLAZE, "블레이즈"),
            Map.entry(EntityType.MAGMA_CUBE, "마그마큐브"),
            Map.entry(EntityType.ZOMBIFIED_PIGLIN, "좀비피글린"),
            Map.entry(EntityType.PIGLIN_BRUTE, "피글린 브루트"),
            Map.entry(EntityType.ENDERMAN, "엔더맨"),
            Map.entry(EntityType.EVOKER, "에보커"),
            Map.entry(EntityType.VINDICATOR, "변명자"),
            Map.entry(EntityType.RAVAGER, "라바저"),
            Map.entry(EntityType.PILLAGER, "약탈자"),
            Map.entry(EntityType.POLAR_BEAR, "북극곰"),
            Map.entry(EntityType.SLIME, "슬라임"),
            Map.entry(EntityType.SILVERFISH, "좀벌레"),
            Map.entry(EntityType.GHAST, "가스트"),
            Map.entry(EntityType.HOGLIN, "호글린"),
            Map.entry(EntityType.SHULKER, "셜커"),
            Map.entry(EntityType.GUARDIAN, "가디언"),
            Map.entry(EntityType.ELDER_GUARDIAN, "엘더 가디언")
    );

    private KoreanMobNames() {
    }

    public static String of(EntityType type) {
        String known = NAMES.get(type);
        if (known != null) {
            return known;
        }
        String name = type.name().replace('_', ' ').toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
