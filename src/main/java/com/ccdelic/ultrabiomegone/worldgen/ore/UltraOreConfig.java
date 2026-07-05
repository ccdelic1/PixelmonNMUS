package com.ccdelic.ultrabiomegone.worldgen.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for {@link UltraOreFeature}: which block to place and which biome family it belongs
 * to. Note the cluster <b>size</b>, <b>rarity</b>, enable flag and biome-exclusivity are NOT here —
 * those are read live from the mod config at generation time, which is the whole point (a datapack
 * JSON value would be static).
 */
public record UltraOreConfig(Block block, OreGroup group) implements FeatureConfiguration {
    public static final Codec<UltraOreConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(UltraOreConfig::block),
        OreGroup.CODEC.fieldOf("group").forGetter(UltraOreConfig::group)
    ).apply(instance, UltraOreConfig::new));
}
