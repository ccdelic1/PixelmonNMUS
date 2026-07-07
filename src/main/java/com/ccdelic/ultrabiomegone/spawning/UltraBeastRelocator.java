package com.ccdelic.ultrabiomegone.spawning;

import java.util.List;
import java.util.Map;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.biome.OverworldBiomeFamilies;
import com.ccdelic.ultrabiomegone.config.Config;
import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnSet;
import com.pixelmonmod.pixelmon.api.spawning.conditions.SpawnCondition;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;

/**
 * Phase 3: relocates the 12 Ultra Space spawn sets (10 Ultra Beasts + the alter Porygon line) into
 * overworld biomes. Runs <b>before</b> {@link UltraSpaceScrubber}, so the sets still hold their
 * original ultra data when we rewrite them; afterwards they reference only overworld biomes, so the
 * scrubber leaves them untouched.
 *
 * <p>For every spawn entry in each targeted set we keep ALL attributes verbatim (level range,
 * {@code stringLocationTypes}, times, {@code ultrabeast} tag, {@code palette:alter} specs) and only:
 * <ol>
 *   <li>replace the biome list with the mapped family's base biome + 2 sub-variants
 *       ({@link OverworldBiomeFamilies}), and</li>
 *   <li>divide the rarity by 3 (0.5 &rarr; 0.1667 for UBs; Porygon 10.0 &rarr; 3.333;
 *       Porygon-Z 0.5 &rarr; 0.1667).</li>
 * </ol>
 * Naganadel is intentionally absent (evolution only, no spawn entry).
 */
public final class UltraBeastRelocator {
    private UltraBeastRelocator() {}

    /** Set id -> mapped overworld biome family (base + 2 sub-variants). */
    private static final Map<String, List<String>> RELOCATIONS = Map.ofEntries(
        // Nihilego: EXCEPTION — spooky dark/dark-oak forests, not oceans.
        Map.entry("Nihilego", OverworldBiomeFamilies.SPOOKY),
        // Jungle Ultra Beasts.
        Map.entry("Buzzwole", OverworldBiomeFamilies.JUNGLE),
        Map.entry("Poipole", OverworldBiomeFamilies.JUNGLE),
        // Desert Ultra Beasts.
        Map.entry("Pheromosa", OverworldBiomeFamilies.DESERT),
        Map.entry("Guzzlord", OverworldBiomeFamilies.DESERT),
        Map.entry("Stakataka", OverworldBiomeFamilies.DESERT),
        // Plains ("electric plant" open landscape) — Xurkitree + alter Porygon line.
        Map.entry("Xurkitree", OverworldBiomeFamilies.PLAINS),
        Map.entry("Porygon", OverworldBiomeFamilies.PLAINS),
        Map.entry("PorygonZ", OverworldBiomeFamilies.PLAINS),
        // Crater -> barren badlands.
        Map.entry("Celesteela", OverworldBiomeFamilies.BADLANDS),
        // Forest Ultra Beasts.
        Map.entry("Kartana", OverworldBiomeFamilies.FOREST),
        Map.entry("Blacephalon", OverworldBiomeFamilies.FOREST)
    );

    public static void run() {
        UltraBiomeGone.debugLog("UltraBeastRelocator: starting Ultra Beast / Porygon relocation");
        final float rarityDivisor = (float) (double) Config.RELOCATION_RARITY_DIVISOR.get();
        UltraBiomeGone.debugLog("UltraBeastRelocator: rarity divisor = {}", rarityDivisor);
        int relocatedSets = 0;
        int relocatedEntries = 0;
        for (List<SpawnSet> sets : PixelmonSpawning.getAll().values()) {
            for (SpawnSet set : sets) {
                List<String> family = RELOCATIONS.get(set.id);
                if (family == null || set.spawnInfos == null) {
                    continue;
                }
                UltraBiomeGone.debugLog("UltraBeastRelocator: relocating spawn set '{}' -> {} overworld biomes", set.id, family.size());
                boolean touched = false;
                for (SpawnInfo info : set.spawnInfos) {
                    relocate(info, family, rarityDivisor);
                    relocatedEntries++;
                    touched = true;
                }
                if (touched) {
                    relocatedSets++;
                }
            }
        }
        UltraBiomeGone.LOGGER.info(
            "[UltraBiomeGone] Relocated {} Ultra Beast / alter-Porygon spawn sets ({} entries) to the overworld",
            relocatedSets, relocatedEntries);
        if (relocatedSets != RELOCATIONS.size()) {
            UltraBiomeGone.LOGGER.warn(
                "[UltraBiomeGone] Expected to relocate {} sets but found {} — some ids may not have loaded",
                RELOCATIONS.size(), relocatedSets);
        }
    }

    private static void relocate(SpawnInfo info, List<String> family, float rarityDivisor) {
        if (info.condition == null) {
            info.condition = new SpawnCondition();
        }
        info.condition.biomes.clear();
        info.condition.biomes.addAll(OverworldBiomeFamilies.toBiomeElements(family));
        info.rarity = info.rarity / rarityDivisor;
    }
}
