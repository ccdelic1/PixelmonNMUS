package com.ccdelic.ultrabiomegone.worldgen.ore;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * The overworld biome family an ultra terrain block naturally clusters under, and the test for
 * whether a given biome belongs to it. Used by {@link UltraOreFeature} to enforce the (configurable)
 * biome-exclusive behaviour: when {@code ore_clusters.biomeExclusive} is true, a block only generates
 * under its family; when false, the feature skips this check and generates in any overworld biome.
 */
public enum OreGroup implements StringRepresentable {
    PLAINS("plains") {
        @Override
        public boolean matches(Holder<Biome> biome) {
            return biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS) || biome.is(Biomes.MEADOW);
        }
    },
    OCEAN("ocean") {
        @Override
        public boolean matches(Holder<Biome> biome) {
            return biome.is(BiomeTags.IS_OCEAN);
        }
    },
    DESERT("desert") {
        @Override
        public boolean matches(Holder<Biome> biome) {
            return biome.is(Biomes.DESERT);
        }
    };

    public static final Codec<OreGroup> CODEC = StringRepresentable.fromEnum(OreGroup::values);

    private final String name;

    OreGroup(String name) {
        this.name = name;
    }

    public abstract boolean matches(Holder<Biome> biome);

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
