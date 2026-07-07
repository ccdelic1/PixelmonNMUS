package com.ccdelic.ultrabiomegone.spawning;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.pixelmonmod.pixelmon.api.spawning.AbstractSpawner;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;

/**
 * Rebuilds Pixelmon's spawner caches after the loaded spawn data has been mutated.
 *
 * <p>Why this is needed: each spawner caches its {@link com.pixelmonmod.pixelmon.api.spawning.SpawnInfo}s
 * indexed by biome ({@code cacheMap}/{@code tagCacheMap}/{@code anyCacheSet} on the preset builders),
 * so mutating a SpawnInfo's biomes after the cache is built would leave the cache pointing at stale
 * biome keys. {@link PixelmonSpawning#initialize()} rebuilds those caches — but its
 * {@code setupCache()} only clears {@code cacheMap}, <b>not</b> {@code tagCacheMap} or
 * {@code anyCacheSet}, so calling it twice would <i>duplicate</i> tag-based and any-biome entries.
 * We therefore clear all three cache collections on every preset builder ourselves first.
 *
 * <p>After re-initialising, if a spawner coordinator is already running (i.e. this is a runtime
 * {@code /reload}, not first server start), we rebuild it via {@code startTrackingSpawner()} so the
 * live per-player tracking spawners and the regular trigger spawners pick up the fresh caches. On
 * first server start the coordinator is null and {@code Pixelmon.onServerStart} starts it after us.
 */
public final class SpawnDataRebuilder {
    private SpawnDataRebuilder() {}

    /** All preset builders that {@link PixelmonSpawning#initialize()} feeds/caches. */
    private static AbstractSpawner.SpawnerBuilder<?>[] presets() {
        return new AbstractSpawner.SpawnerBuilder<?>[] {
            PixelmonSpawning.trackingSpawnerPreset,
            PixelmonSpawning.legendarySpawnerPreset,
            PixelmonSpawning.megaBossSpawnerPreset,
            PixelmonSpawning.rockSmashPreset,
            PixelmonSpawning.headbuttPreset,
            PixelmonSpawning.fishingPreset,
            PixelmonSpawning.sweetScentPreset,
            PixelmonSpawning.seaweedPreset,
            PixelmonSpawning.pixelmonGrassPreset,
            PixelmonSpawning.pixelmonDoubleGrassPreset,
            PixelmonSpawning.caverockPreset,
            PixelmonSpawning.foragePreset,
            PixelmonSpawning.curryPreset
        };
    }

    public static void rebuild() {
        UltraBiomeGone.debugLog("SpawnDataRebuilder: starting cache rebuild");

        // 1. Clear ALL three cache collections on every preset (setupCache() only clears cacheMap).
        for (AbstractSpawner.SpawnerBuilder<?> preset : presets()) {
            preset.cacheMap.clear();
            preset.tagCacheMap.clear();
            preset.anyCacheSet.clear();
        }
        UltraBiomeGone.debugLog("SpawnDataRebuilder: cleared all preset caches (cacheMap, tagCacheMap, anyCacheSet)");

        // 2. Rebuild spawner instances + caches from the (now mutated) spawn set lists.
        UltraBiomeGone.debugLog("SpawnDataRebuilder: calling PixelmonSpawning.initialize()");
        PixelmonSpawning.initialize();

        // 3. If the server is already running, rebuild the coordinator so live spawners use the
        //    fresh caches. On first startup the coordinator is null; Pixelmon starts it after us.
        if (PixelmonSpawning.coordinator != null) {
            UltraBiomeGone.debugLog("SpawnDataRebuilder: coordinator exists — calling startTrackingSpawner()");
            PixelmonSpawning.startTrackingSpawner();
            UltraBiomeGone.LOGGER.info("[UltraBiomeGone] Rebuilt spawner caches and restarted the tracking coordinator");
        } else {
            UltraBiomeGone.LOGGER.info("[UltraBiomeGone] Rebuilt spawner caches (coordinator not yet started)");
        }
    }
}
