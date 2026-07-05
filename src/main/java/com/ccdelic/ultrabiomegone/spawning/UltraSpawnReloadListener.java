package com.ccdelic.ultrabiomegone.spawning;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * A datapack reload listener registered <i>after</i> Pixelmon's (our mod loads
 * {@code ordering="AFTER"} pixelmon), so in the reload apply phase it runs after Pixelmon has
 * populated {@code PixelmonSpawning} and re-registered its move skills. In its apply step (on the
 * game thread) it runs {@link SpawnDataPostProcessor#applyAll()}.
 *
 * <p>The listener has no prepare work; it simply waits on the barrier and does all its work in the
 * apply phase, which the reload manager runs on the server thread — required because the work
 * touches server state (spawner coordinator, player list).
 */
public class UltraSpawnReloadListener implements PreparableReloadListener {
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return barrier.wait(Unit.INSTANCE)
            .thenRunAsync(SpawnDataPostProcessor::applyAll, gameExecutor);
    }
}
