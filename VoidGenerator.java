package com.rpgcore.plugin.tower;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * 시련의 탑 전용 "빈 월드" 생성기.
 *
 * 지형을 아무것도 만들지 않으므로(=허공), 그 위에 우리가 돌로 탑만 쌓습니다.
 * ChunkGenerator 의 기본 동작이 "빈 청크"이기 때문에 generateNoise 등은
 * 일부러 재정의하지 않고, 지형/구조물/장식/몹 생성만 전부 끕니다.
 */
public class VoidGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, 70, 0.5);
    }
}
