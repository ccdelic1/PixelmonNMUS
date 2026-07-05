package com.ccdelic.ultrabiomegone.spawning;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.ccdelic.ultrabiomegone.wormhole.WormholeMoveSkillDisabler;

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

    public static void applyAll() {
        try {
            // Phase 3 — relocate the 12 Ultra Beast / alter-Porygon spawn sets to the overworld.
            if (Config.RELOCATE_ULTRA_BEASTS.get()) {
                UltraBeastRelocator.run();
            }
            // Phase 4 — relocate the forage/headbutt ultra loot entries to the overworld.
            if (Config.RELOCATE_LOOT.get()) {
                LootRelocator.run();
            }

            // Phase 1 — kill wormholes, delete remaining ultra-exclusive pools, scrub mixed pools.
            // (Self-gates on the wormhole/scrub config flags.)
            UltraSpaceScrubber.run();

            // Rebuild all spawner caches so /checkspawn and live spawning reflect the mutated data.
            // Only needed if some spawn-mutating step is enabled.
            if (Config.RELOCATE_ULTRA_BEASTS.get() || Config.RELOCATE_LOOT.get()
                || Config.SCRUB_ULTRA_SPAWNS.get() || Config.DISABLE_WORMHOLE_SPAWNS.get()) {
                SpawnDataRebuilder.rebuild();
            }

            // Phase 2 — disable the open_wormhole external move. Runs here (after the rebuild, at the
            // tail of our LOWEST-priority reload listener) so it is after Pixelmon's drops loader has
            // re-registered the default move skills on this same reload.
            if (Config.DISABLE_OPEN_WORMHOLE_MOVE.get()) {
                WormholeMoveSkillDisabler.disable();
            }
        } catch (Throwable t) {
            UltraBiomeGone.LOGGER.error("[UltraBiomeGone] Failed to post-process spawn data", t);
        }
    }
}
