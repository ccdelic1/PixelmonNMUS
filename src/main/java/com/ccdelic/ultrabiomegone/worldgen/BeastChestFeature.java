package com.ccdelic.ultrabiomegone.worldgen;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places Beast Ball PokéChests in the overworld, similar to how Pixelmon's
 * {@code PokeChestFeature} places normal/ultra/master chests.
 *
 * <p>Uses the {@code pixelmon:beast_chest} block (which internally uses the
 * {@code EnumPokeChestType.BEASTBALL} type and rolls from the untouched
 * {@code ultraSpace} loot pool).
 *
 * <p>Placement is gated by {@link Config#BEAST_CHEST_ENABLED} and follows
 * the surface placement pattern from Pixelmon's own chest feature.
 */
public class BeastChestFeature extends Feature<NoneFeatureConfiguration> {

    private static final String BEAST_CHEST_ID = "pixelmon:beast_chest";

    public BeastChestFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!Config.BEAST_CHEST_ENABLED.get()) {
            return false;
        }

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // Config-driven rarity: 1-in-N chunks per placement attempt. Rolled here (not in the
        // placed-feature JSON) so BEAST_CHEST_RARITY is actually respected — previously the rate
        // was hard-coded in the JSON's rarity_filter and this config value did nothing. Roll first
        // so the vast majority of chunks bail out before any heightmap/surface work.
        int rarity = Config.BEAST_CHEST_RARITY.get();
        if (rarity > 1 && random.nextInt(rarity) != 0) {
            return false;
        }

        // Pick a random position within the chunk
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        int x = random.nextInt(16) + chunkX * 16;
        int z = random.nextInt(16) + chunkZ * 16;

        // Use WORLD_SURFACE_WG (noise-based, safe during worldgen) instead of MOTION_BLOCKING
        int y = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).getY();

        // Walk down past leaves/logs/air to find solid ground
        BlockState underBlockState;
        for (underBlockState = level.getBlockState(new BlockPos(x, y - 1, z));
             underBlockState.is(BlockTags.LEAVES) || underBlockState.is(BlockTags.LOGS) || underBlockState.isAir();
             underBlockState = level.getBlockState(new BlockPos(x, y - 1, z))) {
            if (--y <= 0) {
                return false;
            }
        }

        // Don't place on lava or water
        if (underBlockState.is(Blocks.LAVA) || underBlockState.is(Blocks.WATER)) {
            return false;
        }

        BlockPos placePos = new BlockPos(x, y, z);

        // Resolve the beast chest block from Pixelmon's registry
        Block beastChestBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(BEAST_CHEST_ID));
        if (beastChestBlock == Blocks.AIR) {
            UltraBiomeGone.LOGGER.warn("[PixelmonNMUS] BeastChestFeature: could not find pixelmon:beast_chest in registry");
            return false;
        }

        level.setBlock(placePos, beastChestBlock.defaultBlockState(), 18);
        UltraBiomeGone.debugLog("BeastChestFeature: placed beast chest at {}", placePos.toShortString());

        return true;
    }
}
