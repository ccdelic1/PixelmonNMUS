package com.ccdelic.ultrabiomegone.biome;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;

/**
 * The canonical overworld biome-family mappings that every Ultra Space biome is re-homed into.
 * Centralised here so the spawn relocations (Phase 3), loot relocations (Phase 4) and ultra-block
 * worldgen (Phase 6) all draw from the <b>same</b> picks — the spec repeatedly requires this
 * ("same biome family chosen for Xurkitree", etc.).
 *
 * <p><b>Sub-biome rule (Phase 3):</b> each relocation lists the family's <i>base</i> biome plus
 * exactly two sub-variants (deliberately not the whole family). We use only vanilla 1.21.1 biome
 * ids so the mod does not depend on BOP/BWG/Terralith being present, even though Pixelmon's own
 * {@code spawning/*} tags (the nominal "families") include those modded biomes.
 *
 * <table>
 *   <tr><th>Ultra biome</th><th>Family</th><th>base + 2 sub-variants</th></tr>
 *   <tr><td>ultra_jungle</td><td>{@link #JUNGLE}</td><td>jungle, sparse_jungle, bamboo_jungle</td></tr>
 *   <tr><td>ultra_desert</td><td>{@link #DESERT}</td><td>desert (only vanilla member — see note)</td></tr>
 *   <tr><td>ultra_plant</td><td>{@link #PLAINS}</td><td>plains, sunflower_plains, meadow</td></tr>
 *   <tr><td>ultra_crater</td><td>{@link #BADLANDS}</td><td>badlands, eroded_badlands, wooded_badlands</td></tr>
 *   <tr><td>ultra_forest</td><td>{@link #FOREST}</td><td>forest, birch_forest, flower_forest</td></tr>
 *   <tr><td>ultra_deep_sea (Nihilego exception)</td><td>{@link #SPOOKY}</td><td>dark_forest, swamp, mangrove_swamp</td></tr>
 *   <tr><td>ultra_deep_sea (loot/worldgen)</td><td>{@link #OCEANIC_TAG}</td><td>#minecraft:is_ocean</td></tr>
 * </table>
 *
 * <p><b>Desert note:</b> vanilla 1.21.1 has a single desert biome, so the DESERT family has fewer
 * than three members; per the spec's "fewer than 3 → use what exists" clause it is just
 * {@code minecraft:desert}.
 *
 * <p><b>Spooky note:</b> vanilla's dark-forest/"evil" family is effectively just
 * {@code minecraft:dark_forest} (the other members of Pixelmon's {@code roofed}/{@code evil} tags
 * are modded). To honour the base+2 pattern for Nihilego's eerie land home we round it out with the
 * two murky swamp biomes ({@code swamp}, {@code mangrove_swamp}).
 */
public final class OverworldBiomeFamilies {
    private OverworldBiomeFamilies() {}

    public static final List<String> JUNGLE = List.of(
        "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle");

    public static final List<String> DESERT = List.of(
        "minecraft:desert");

    public static final List<String> PLAINS = List.of(
        "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow");

    public static final List<String> BADLANDS = List.of(
        "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands");

    public static final List<String> FOREST = List.of(
        "minecraft:forest", "minecraft:birch_forest", "minecraft:flower_forest");

    public static final List<String> SPOOKY = List.of(
        "minecraft:dark_forest", "minecraft:swamp", "minecraft:mangrove_swamp");

    /** For loot/worldgen deep-sea mappings, which target the whole ocean family via a tag. */
    public static final String OCEANIC_TAG = "#minecraft:is_ocean";

    /**
     * Build a {@link TagEntry} list from the given biome tokens. A token beginning with {@code #}
     * becomes a tag entry (e.g. {@code #minecraft:is_ocean}); anything else becomes a direct biome
     * element (e.g. {@code minecraft:jungle}).
     */
    public static List<TagEntry> toBiomeElements(List<String> biomeTokens) {
        return biomeTokens.stream()
            .map(OverworldBiomeFamilies::toBiomeEntry)
            .toList();
    }

    private static TagEntry toBiomeEntry(String token) {
        if (token.startsWith("#")) {
            return TagEntry.tag(ResourceLocation.parse(token.substring(1)));
        }
        return TagEntry.element(ResourceLocation.parse(token));
    }
}
