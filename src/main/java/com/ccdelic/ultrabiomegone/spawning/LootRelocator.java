package com.ccdelic.ultrabiomegone.spawning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.biome.OverworldBiomeFamilies;
import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnSet;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.collection.SpawnInfoCollection;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.items.SpawnInfoItem;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;

/**
 * Phase 4: re-homes the ultra-exclusive forage/headbutt loot entries into the overworld, keeping
 * each entry's rarity exactly as-is and only swapping its biome list. Runs <b>before</b> the
 * scrubber (like {@link UltraBeastRelocator}), so the relocated entries no longer reference ultra
 * biomes and survive the scrub. All-ultra loot entries NOT in this table (e.g. sapphire,
 * amethyst_shard, crystal, silicon, black_sludge — obtainable elsewhere) are intentionally left to
 * be deleted by the scrubber, exactly as the spec intends.
 *
 * <p>We target by item id and only touch entries whose biome list is entirely ultra, so we can
 * never accidentally rewrite a mixed/overworld entry of the same item.
 */
public final class LootRelocator {
    private LootRelocator() {}

    // ultra_forest_flower originally spanned ultra_forest + ultra_jungle -> forests + jungles.
    private static final List<String> FLOWER = concat(OverworldBiomeFamilies.FOREST, OverworldBiomeFamilies.JUNGLE);
    // Ginkgo/elm leaves ~ oak/dark-oak trees -> oak forest + dark-oak (dark) forest.
    private static final List<String> ELM_GINKGO = List.of("minecraft:forest", "minecraft:dark_forest");
    // Deep-sea items -> ocean family (loot conditions accept tags; the sub-biome rule is UB-only).
    private static final List<String> DEEP_SEA = List.of(OverworldBiomeFamilies.OCEANIC_TAG);

    /** item id -> new biome tokens. Rarity is left unchanged. */
    private static final Map<String, List<String>> RELOCATIONS = Map.ofEntries(
        // Forage — ultra_plant -> plains (the Xurkitree family).
        Map.entry("pixelmon:dubious_disc", OverworldBiomeFamilies.PLAINS),
        Map.entry("minecraft:sea_lantern", OverworldBiomeFamilies.PLAINS),
        Map.entry("pixelmon:ultra_space_stone", OverworldBiomeFamilies.PLAINS),
        // Forage — ultra_desert -> deserts.
        Map.entry("pixelmon:bright_powder", OverworldBiomeFamilies.DESERT),
        Map.entry("pixelmon:ultra_desert_sand", OverworldBiomeFamilies.DESERT),
        Map.entry("pixelmon:ultra_desert_cactus", OverworldBiomeFamilies.DESERT),
        // Forage — ultra_crater -> badlands (reusing the Celesteela family for consistency).
        Map.entry("pixelmon:aluminum_ingot", OverworldBiomeFamilies.BADLANDS),
        Map.entry("pixelmon:silver_ingot", OverworldBiomeFamilies.BADLANDS),
        Map.entry("pixelmon:platinum_ingot", OverworldBiomeFamilies.BADLANDS),
        Map.entry("minecraft:iron_ingot", OverworldBiomeFamilies.BADLANDS),
        Map.entry("minecraft:gunpowder", OverworldBiomeFamilies.BADLANDS),
        Map.entry("minecraft:yellow_concrete", OverworldBiomeFamilies.BADLANDS),
        Map.entry("minecraft:yellow_concrete_powder", OverworldBiomeFamilies.BADLANDS),
        Map.entry("minecraft:orange_concrete", OverworldBiomeFamilies.BADLANDS),
        // Forage — ultra_deep_sea -> oceanic.
        Map.entry("pixelmon:ultra_deep_stone", DEEP_SEA),
        Map.entry("pixelmon:ultra_deep_clay", DEEP_SEA),
        // Forage — ultra_forest + ultra_jungle -> forests + jungles.
        Map.entry("pixelmon:ultra_forest_flower", FLOWER),
        // Headbutt — ultra_forest ginkgo/elm leaves -> oak/dark-oak forests.
        Map.entry("pixelmon:ultra_elm_leaves", ELM_GINKGO),
        Map.entry("pixelmon:ultra_ginkgo_leaves", ELM_GINKGO),
        // Headbutt — ultra_jungle leaves -> jungles.
        Map.entry("pixelmon:ultra_jungle_leaves", OverworldBiomeFamilies.JUNGLE)
    );

    public static void run() {
        UltraBiomeGone.debugLog("LootRelocator: starting ultra loot relocation");
        int relocated = 0;
        for (List<SpawnSet> sets : PixelmonSpawning.getAll().values()) {
            for (SpawnSet set : sets) {
                if (set.spawnInfos != null) {
                    for (SpawnInfo info : set.spawnInfos) {
                        relocated += relocate(info);
                    }
                }
            }
        }
        UltraBiomeGone.LOGGER.info("[UltraBiomeGone] Relocated {} ultra-exclusive forage/headbutt loot entries to the overworld", relocated);
        if (relocated != RELOCATIONS.size()) {
            UltraBiomeGone.LOGGER.warn("[UltraBiomeGone] Expected {} loot relocations but performed {}", RELOCATIONS.size(), relocated);
        }
    }

    private static int relocate(SpawnInfo info) {
        int count = 0;
        if (info instanceof SpawnInfoCollection collection && collection.collection != null) {
            for (SpawnInfo child : collection.collection) {
                count += relocate(child);
            }
        }
        if (info instanceof SpawnInfoItem itemInfo && itemInfo.item != null && info.condition != null) {
            List<String> target = RELOCATIONS.get(itemInfo.item.itemID);
            if (target != null && isAllUltra(info.condition.biomes)) {
                info.condition.biomes.clear();
                info.condition.biomes.addAll(OverworldBiomeFamilies.toBiomeElements(target));
                count++;
            }
        }
        return count;
    }

    private static boolean isAllUltra(java.util.Set<net.minecraft.tags.TagEntry> biomes) {
        return biomes != null && !biomes.isEmpty() && biomes.stream().allMatch(UltraBiomes::isUltraBiomeToken);
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> combined = new ArrayList<>(a);
        combined.addAll(b);
        return List.copyOf(combined);
    }
}
