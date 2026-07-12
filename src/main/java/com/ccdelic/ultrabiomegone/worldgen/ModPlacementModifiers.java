package com.ccdelic.ultrabiomegone.worldgen;

import java.util.function.Supplier;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the mod's custom worldgen {@link PlacementModifierType}s. Currently just
 * {@code ultrabiomegone:geode_config}, the config-aware rarity gate used by the ultra geode
 * {@code placed_feature} JSONs (see {@link GeodeConfigPlacement}).
 */
public final class ModPlacementModifiers {
    private ModPlacementModifiers() {}

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
        DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, UltraBiomeGone.MOD_ID);

    public static final Supplier<PlacementModifierType<GeodeConfigPlacement>> GEODE_CONFIG =
        PLACEMENT_MODIFIERS.register("geode_config", () -> () -> GeodeConfigPlacement.CODEC);

    public static void register(IEventBus modEventBus) {
        UltraBiomeGone.debugLog("ModPlacementModifiers: registering geode_config placement modifier type");
        PLACEMENT_MODIFIERS.register(modEventBus);
        UltraBiomeGone.debugLog("ModPlacementModifiers: geode_config placement modifier type registered successfully");
    }
}
