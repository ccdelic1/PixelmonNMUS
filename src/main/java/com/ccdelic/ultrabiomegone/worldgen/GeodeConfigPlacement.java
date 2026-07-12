package com.ccdelic.ultrabiomegone.worldgen;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * A config-driven replacement for the vanilla {@code minecraft:rarity_filter} on the ultra
 * terrain-block geodes. The geodes themselves are plain vanilla {@code minecraft:geode} features
 * generated straight from datapack JSON, so their master switch ({@link Config#GEODE_ENABLED}) and
 * rarity ({@link Config#GEODE_RARITY}) can't be read from the JSON — this placement modifier reads
 * them at worldgen time instead. Swapped in for the hard-coded {@code rarity_filter} in each geode
 * {@code placed_feature} JSON so those two config values actually take effect (previously they were
 * declared but never consulted, and the real 1-in-24 rate was frozen in the JSON).
 *
 * <p>Extends {@link PlacementFilter}, which turns {@link #shouldPlace} into the standard
 * "emit this position or nothing" placement behaviour.
 */
public final class GeodeConfigPlacement extends PlacementFilter {

    public static final GeodeConfigPlacement INSTANCE = new GeodeConfigPlacement();

    /** No fields to serialise — the behaviour is entirely config-driven, so this is a unit codec. */
    public static final MapCodec<GeodeConfigPlacement> CODEC = MapCodec.unit(() -> INSTANCE);

    private GeodeConfigPlacement() {}

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!Config.GEODE_ENABLED.get()) {
            return false;
        }
        int rarity = Config.GEODE_RARITY.get();
        // 1-in-N chunks per placement attempt. rarity <= 1 means "every attempt".
        if (rarity > 1 && random.nextInt(rarity) != 0) {
            return false;
        }
        UltraBiomeGone.debugLog("GeodeConfigPlacement: geode placement passed at {}", pos.toShortString());
        return true;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.GEODE_CONFIG.get();
    }
}
