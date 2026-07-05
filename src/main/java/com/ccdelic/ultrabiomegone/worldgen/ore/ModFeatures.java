package com.ccdelic.ultrabiomegone.worldgen.ore;

import java.util.function.Supplier;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the mod's custom worldgen feature type. The configured/placed features that use it are
 * shipped as datapack JSON referencing {@code ultrabiomegone:ultra_ore}.
 */
public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, UltraBiomeGone.MOD_ID);

    public static final Supplier<UltraOreFeature> ULTRA_ORE =
        FEATURES.register("ultra_ore", () -> new UltraOreFeature(UltraOreConfig.CODEC));

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
