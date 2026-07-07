package com.ccdelic.ultrabiomegone.spawning;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.ccdelic.ultrabiomegone.wormhole.WormholeMoveSkillDisabler;
import com.pixelmonmod.pixelmon.api.config.DimensionsConfig;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;

import java.lang.reflect.Field;

/**
 * Single entry point that applies every Ultra-Space spawn-data transformation, in the correct
 * order, then rebuilds the spawner caches. Runs on the server thread after Pixelmon has finished
 * (re)loading its spawn data (see {@link UltraSpawnReloadListener}), so it runs on first server
 * start <i>and</i> on every {@code /reload} — ultra data can never be resurrected.
 *
 * <p><b>Ordering matters.</b> Relocations (Phases 3–4) must run <i>before</i> the scrubber so that
 * the relocated entries have already had their ultra biomes swapped for overworld ones — the
 * scrubber then sees no ultra reference on them and leaves them alone, while still deleting the
 * remaining ultra-exclusive pools and cleaning the mixed pools.
 */
public final class SpawnDataPostProcessor {
    private SpawnDataPostProcessor() {}

    /**
     * Sets Pixelmon's {@code DimensionsConfig.ultraSpace} to {@code false} via reflection.
     * Called from {@link #applyAll()} which runs after Pixelmon has finished its YAML config
     * loading, so this override cannot be overwritten by a subsequent config reload.
     */
    private static void disableUltraSpaceConfig() {
        try {
            DimensionsConfig dimConfig = PixelmonConfigProxy.getDimensions();
            if (dimConfig == null) {
                UltraBiomeGone.LOGGER.warn("[PixelmonNMUS] getDimensions() returned null — cannot disable Ultra Space");
                return;
            }
            if (!dimConfig.isUltraSpace()) {
                return; // already disabled
            }
            Field ultraSpaceField = DimensionsConfig.class.getDeclaredField("ultraSpace");
            ultraSpaceField.setAccessible(true);
            ultraSpaceField.setBoolean(dimConfig, false);
            UltraBiomeGone.LOGGER.info("[PixelmonNMUS] Set DimensionsConfig.ultraSpace = false");
        } catch (NoSuchFieldException e) {
            UltraBiomeGone.LOGGER.warn("[PixelmonNMUS] DimensionsConfig.ultraSpace field not found", e);
        } catch (Exception e) {
            UltraBiomeGone.LOGGER.warn("[PixelmonNMUS] Could not disable Ultra Space config", e);
        }
    }

    public static void applyAll() {
        UltraBiomeGone.debugLog("SpawnDataPostProcessor: applyAll() starting");
        try {
            // Disable Ultra Space wormholes via Pixelmon's own config flag.  This runs here
            // (after Pixelmon's config reload) instead of during FMLCommonSetupEvent because
            // Pixelmon's YAML config reload during startup would overwrite any earlier change.
            disableUltraSpaceConfig();

            // Phase 3 — relocate the 12 Ultra Beast / alter-Porygon spawn sets to the overworld.
            if (Config.RELOCATE_ULTRA_BEASTS.get()) {
                UltraBiomeGone.debugLog("SpawnDataPostProcessor: Phase 3 — running UltraBeastRelocator");
                UltraBeastRelocator.run();
            } else {
                UltraBiomeGone.debugLog("SpawnDataPostProcessor: Phase 3 skipped (relocateUltraBeasts=false)");
            }
            // Phase 4 — relocate the forage/headbutt ultra loot entries to the overworld.
            if (Config.RELOCATE_LOOT.get()) {
                UltraBiomeGone.debugLog("SpawnDataPostProcessor: Phase 4 — running LootRelocator");
                LootRelocator.run();
            } else {
                UltraBiomeGone.debugLog("SpawnDataPostProcessor: Phase 4 skipped (relocateLoot=false)");
            }

            // Safety pass — remove invalid NPC trainer presets that can crash clients when spawned.
            UltraBiomeGone.debugLog("SpawnDataPostProcessor: safety pass — running NpcPresetSanitizer");
            NpcPresetSanitizer.run();

            // Phase 1 — scrub remaining ultra-exclusive pools and strip ultra biomes from mixed pools.
            // (Wormholes are NOT removed here — they are disabled safely via Pixelmon's
            // DimensionsConfig.ultraSpace flag set during mod init. See UltraBiomeGone.commonSetup().)
            UltraBiomeGone.debugLog("SpawnDataPostProcessor: Phase 1 — running UltraSpaceScrubber (self-gated)");
            UltraSpaceScrubber.run();

            // Rebuild all spawner caches so /checkspawn and live spawning reflect the mutated data.
            // Only needed if some spawn-mutating step is enabled.
            boolean needRebuild = Config.RELOCATE_ULTRA_BEASTS.get() || Config.RELOCATE_LOOT.get()
                || Config.SCRUB_ULTRA_SPAWNS.get();
            if (needRebuild) {
                UltraBiomeGone.debugLog("SpawnDataPostProcessor: spawn data was mutated — running SpawnDataRebuilder");
                SpawnDataRebuilder.rebuild();
            } else {
                UltraBiomeGone.debugLog("SpawnDataPostProcessor: no spawn data mutations enabled, skipping rebuild");
            }

            // Phase 2 — disable the open_wormhole external move. Runs here (after the rebuild, at the
            // tail of our LOWEST-priority reload listener) so it is after Pixelmon's drops loader has
            // re-registered the default move skills on this same reload.
            if (Config.DISABLE_OPEN_WORMHOLE_MOVE.get()) {
                UltraBiomeGone.debugLog("SpawnDataPostProcessor: Phase 2 — running WormholeMoveSkillDisabler");
                WormholeMoveSkillDisabler.disable();
            } else {
                UltraBiomeGone.debugLog("SpawnDataPostProcessor: Phase 2 skipped (disableOpenWormholeMove=false)");
            }
            UltraBiomeGone.debugLog("SpawnDataPostProcessor: applyAll() complete");
        } catch (Throwable t) {
            UltraBiomeGone.LOGGER.error("[UltraBiomeGone] Failed to post-process spawn data", t);
            UltraBiomeGone.debugLog("SpawnDataPostProcessor: applyAll() FAILED with exception: {}", t.getMessage());
        }
    }
}
