package com.ccdelic.ultrabiomegone.spawning;

import java.util.List;
import java.util.Map;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnSet;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.collection.SpawnInfoCollection;
import com.pixelmonmod.pixelmon.api.spawning.conditions.SpawnCondition;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;

/**
 * Phase 1: the central SpawnSet post-processor that removes all Ultra Space references from the
 * loaded spawn data. Operates in place on the live {@link SpawnSet} objects held by
 * {@link PixelmonSpawning}, so {@code /wiki} and {@code /checkspawn} (which read the live data)
 * reflect the changes. Must be followed by a spawner cache rebuild (see
 * {@link com.ccdelic.ultrabiomegone.spawning.SpawnDataRebuilder}).
 *
 * <p>For each {@link SpawnInfo} (recursing into {@link SpawnInfoCollection} loot children):
 * <ul>
 *   <li><b>wormhole entries</b> ({@code typeID == "wormhole"}) are removed outright — this kills
 *       both the ultra-space and the overworld wormhole spawns;</li>
 *   <li><b>exclusively-ultra entries</b> (positive {@code condition} restricts to ultra biomes or
 *       the ultra dimension only) are removed — these are the fossil/trade-evo/UB pools that
 *       intentionally lose their wild spawns (the 12 relocated spawns are re-homed <i>before</i>
 *       this runs, in later phases, so by the time we get here they no longer reference ultra);</li>
 *   <li><b>mixed entries</b> have every ultra biome id, the {@code #pixelmon:spawning/ultra_space}
 *       tag ref, and the {@code pixelmon:ultra_space} dimension stripped from all of their
 *       conditions/anticonditions, leaving everything else intact.</li>
 * </ul>
 */
public final class UltraSpaceScrubber {
    private UltraSpaceScrubber() {}

    /** Result counters for logging/verification. */
    public record Result(int wormholesRemoved, int exclusiveRemoved, int mixedScrubbed) {}

    public static Result run() {
        int[] counts = new int[3]; // [wormholes, exclusive, mixed]
        Map<String, List<SpawnSet>> all = PixelmonSpawning.getAll();
        for (List<SpawnSet> sets : all.values()) {
            for (SpawnSet set : sets) {
                processSet(set, counts);
            }
        }
        Result result = new Result(counts[0], counts[1], counts[2]);
        UltraBiomeGone.LOGGER.info(
            "[UltraBiomeGone] Scrubbed spawn data: removed {} wormhole entries, {} ultra-exclusive entries, scrubbed {} mixed entries",
            result.wormholesRemoved(), result.exclusiveRemoved(), result.mixedScrubbed());
        return result;
    }

    /** Process a single set in place: prune ultra entries and scrub ultra refs. */
    public static void processSet(SpawnSet set, int[] counts) {
        if (set != null && set.spawnInfos != null) {
            set.spawnInfos.removeIf(info -> processInfo(info, counts));
        }
    }

    /**
     * @return true if this SpawnInfo should be removed from its parent list.
     */
    private static boolean processInfo(SpawnInfo info, int[] counts) {
        boolean killWormholes = Config.DISABLE_WORMHOLE_SPAWNS.get();
        boolean scrubUltra = Config.SCRUB_ULTRA_SPAWNS.get();

        // Recurse into loot collections first: prune ultra children in place.
        if (scrubUltra && info instanceof SpawnInfoCollection collection && collection.collection != null) {
            collection.collection.removeIf(child -> processInfo(child, counts));
        }

        // Wormhole entries always go (both the ultra-space and overworld variants).
        if (killWormholes && "wormhole".equals(info.typeID)) {
            counts[0]++;
            return true;
        }

        if (!scrubUltra) {
            return false;
        }

        // Exclusively-ultra by positive condition -> remove entirely.
        if (isExclusivelyUltra(info.condition)) {
            counts[1]++;
            return true;
        }

        // Otherwise scrub ultra references out of every condition, keeping the entry intact.
        boolean scrubbed = scrubCondition(info.condition);
        scrubbed |= scrubCondition(info.anticondition);
        if (info.compositeCondition != null) {
            if (info.compositeCondition.conditions != null) {
                for (SpawnCondition c : info.compositeCondition.conditions) {
                    scrubbed |= scrubCondition(c);
                }
            }
            if (info.compositeCondition.anticonditions != null) {
                for (SpawnCondition c : info.compositeCondition.anticonditions) {
                    scrubbed |= scrubCondition(c);
                }
            }
        }
        if (scrubbed) {
            counts[2]++;
        }
        return false;
    }

    /**
     * A condition is "exclusively ultra" (its positive spawn constraint can only be met in Ultra
     * Space) when it restricts to the ultra dimension only, or its biome list is non-empty and every
     * biome entry is an ultra biome / the ultra tag.
     */
    private static boolean isExclusivelyUltra(SpawnCondition c) {
        if (c == null) {
            return false;
        }
        // Dimension restricted to ultra space only.
        if (c.dimensions != null && !c.dimensions.isEmpty()
            && c.dimensions.stream().allMatch(UltraBiomes::isUltraDimension)) {
            return true;
        }
        // Biome list entirely ultra.
        if (c.biomes != null && !c.biomes.isEmpty()
            && c.biomes.stream().allMatch(UltraBiomes::isUltraBiomeToken)) {
            return true;
        }
        return false;
    }

    /**
     * Removes ultra biome refs from {@code condition.biomes} and the ultra dimension from
     * {@code condition.dimensions} (rebuilding {@code cachedDimensions} to match). Never empties a
     * mixed list to nothing — callers only reach here for non-exclusive conditions.
     *
     * @return true if anything was removed.
     */
    private static boolean scrubCondition(SpawnCondition c) {
        if (c == null) {
            return false;
        }
        boolean changed = false;
        if (c.biomes != null) {
            changed |= c.biomes.removeIf(UltraBiomes::isUltraBiomeToken);
        }
        if (c.dimensions != null && c.dimensions.removeIf(UltraBiomes::isUltraDimension)) {
            changed = true;
            // Keep the transient dimension cache consistent with the mutated list.
            c.cachedDimensions.clear();
            for (String dim : c.dimensions) {
                c.cachedDimensions.add(ResourceLocation.parse(dim));
            }
        }
        return changed;
    }

    /** Exposed for tests / debugging: classify a single biome token. */
    static boolean isUltra(TagEntry entry) {
        return UltraBiomes.isUltraBiomeToken(entry);
    }
}
