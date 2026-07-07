package com.ccdelic.ultrabiomegone.worldgen;

import java.util.function.Supplier;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the mod's custom worldgen feature types. The configured/placed features that use them
 * are shipped as datapack JSON referencing {@code ultrabiomegone:beast_chest}.
 *
 * <p>Ultra terrain-block geodes use vanilla {@code minecraft:geode} type — no custom feature
 * registration needed; those are pure datapack JSON.
 */
public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, UltraBiomeGone.MOD_ID);

    public static final Supplier<BeastChestFeature> BEAST_CHEST =
        FEATURES.register("beast_chest", BeastChestFeature::new);

    public static void register(IEventBus modEventBus) {
        UltraBiomeGone.debugLog("ModFeatures: registering beast_chest feature type");
        FEATURES.register(modEventBus);
        UltraBiomeGone.debugLog("ModFeatures: beast_chest feature type registered successfully");
    }
}
