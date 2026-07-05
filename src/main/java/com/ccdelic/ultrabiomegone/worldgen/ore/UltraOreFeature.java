package com.ccdelic.ultrabiomegone.worldgen.ore;

import java.util.Optional;

import com.ccdelic.ultrabiomegone.config.Config;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

/**
 * A config-driven ore-cluster feature. Everything the user can tune lives in the mod config and is
 * read here at generation time:
 * <ul>
 *   <li>{@code ore_clusters.enabled} — master on/off;</li>
 *   <li>{@code ore_clusters.biomeExclusive} — when true, only generate under the block's
 *       {@link OreGroup} family (space stone→plains, deep→oceans, sand→deserts); when false,
 *       generate in any overworld biome;</li>
 *   <li>{@code ore_clusters.rarity} — 1-in-N per placement attempt;</li>
 *   <li>{@code ore_clusters.clusterSize} — blob size (delegated to the vanilla ore algorithm).</li>
 * </ul>
 * The placed feature (datapack) just supplies the per-chunk attempt(s), height band and the overworld
 * {@code biome} filter; this feature makes the actual decisions and delegates the blob placement to
 * vanilla {@link Feature#ORE} with an {@link OreConfiguration} built from the configured size.
 */
public class UltraOreFeature extends Feature<UltraOreConfig> {
    /**
     * Y at which we sample the surface biome. Cave biomes (dripstone/lush/deep_dark) only occupy an
     * underground band; well above it the biome source returns the true surface biome. We sample the
     * cluster's own x/z column (same chunk) at this height — crucially WITHOUT calling getHeight(),
     * whose in_square-offset lookups can reach into a neighbouring chunk and deadlock worldgen.
     */
    private static final int SURFACE_SAMPLE_Y = 100;

    public UltraOreFeature(Codec<UltraOreConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<UltraOreConfig> context) {
        if (!Config.ORE_ENABLED.get()) {
            return false;
        }
        UltraOreConfig config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        if (Config.ORE_BIOME_EXCLUSIVE.get()) {
            // Check the SURFACE biome above the cluster, not the biome at the (underground) origin —
            // underground the biome is a 3D cave biome (dripstone/lush/deep_dark), never plains/ocean.
            // Sample the same x/z column high above the cave band (no getHeight -> no worldgen hang).
            Holder<Biome> surfaceBiome = level.getBiome(new BlockPos(origin.getX(), SURFACE_SAMPLE_Y, origin.getZ()));
            if (!config.group().matches(surfaceBiome)) {
                return false;
            }
        }

        int rarity = Config.ORE_RARITY.get();
        if (rarity > 1 && context.random().nextInt(rarity) != 0) {
            return false;
        }

        int size = Math.min(64, Math.max(1, Config.ORE_CLUSTER_SIZE.get()));
        OreConfiguration oreConfig = new OreConfiguration(
            new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD),
            config.block().defaultBlockState(),
            size);

        // Delegate the actual blob placement to the vanilla ore algorithm.
        return Feature.ORE.place(new FeaturePlaceContext<>(
            Optional.empty(),
            level,
            context.chunkGenerator(),
            context.random(),
            origin,
            oreConfig));
    }
}
