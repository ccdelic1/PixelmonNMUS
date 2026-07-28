package com.ccdelic.ultrabiomegone.spawning;

import java.util.Set;

import net.minecraft.tags.TagEntry;

/**
 * Constants and classification helpers for the six Ultra Space biomes, the
 * {@code pixelmon:spawning/ultra_space} biome tag, and the {@code pixelmon:ultra_space}
 * dimension.
 *
 * <p>Spawn data references Ultra Space three ways (all handled by {@link UltraSpaceScrubber}):
 * <ol>
 *   <li>a direct biome id (e.g. {@code pixelmon:ultra_forest}) in {@code condition.biomes};</li>
 *   <li>the tag ref {@code #pixelmon:spawning/ultra_space} in {@code condition.biomes};</li>
 *   <li>the dimension {@code pixelmon:ultra_space} in {@code condition.dimensions}.</li>
 * </ol>
 *
 * <p>We classify {@link TagEntry}s via {@link TagEntry#toString()} (vanilla-public), which emits
 * {@code #} for tags and a trailing {@code ?} for optional entries — so we never depend on
 * Pixelmon-added accessor methods that aren't on the compile-time classpath.
 */
public final class UltraBiomes {
    private UltraBiomes() {}

    /** The Ultra Space dimension id, as it appears in {@code condition.dimensions}. */
    public static final String ULTRA_SPACE_DIMENSION = "pixelmon:ultra_space";

    /** The Pixelmon biome tag grouping all six ultra biomes. */
    public static final String ULTRA_SPACE_TAG = "pixelmon:spawning/ultra_space";

    /** The six ultra biome ids (tag file {@code data/pixelmon/tags/worldgen/biome/spawning/ultra_space.json}). */
    public static final Set<String> ULTRA_BIOME_IDS = Set.of(
        "pixelmon:ultra_forest",
        "pixelmon:ultra_deep_sea",
        "pixelmon:ultra_jungle",
        "pixelmon:ultra_plant",
        "pixelmon:ultra_crater",
        "pixelmon:ultra_desert"
    );

    /**
     * Normalises a {@link TagEntry#toString()} token: strips a leading {@code #} (tag marker) and a
     * trailing {@code ?} (optional marker), returning the bare id and whether it was a tag.
     */
    private static String bareId(String token) {
        String s = token;
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.endsWith("?")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** @return true if a {@code condition.biomes} token refers to Ultra Space (a ultra biome id or the ultra tag). */
    public static boolean isUltraBiomeToken(TagEntry entry) {
        if (entry == null) {
            return false;
        }
        String token = entry.toString();
        boolean tag = token.startsWith("#");
        String id = bareId(token);
        if (tag) {
            return ULTRA_SPACE_TAG.equals(id);
        }
        return ULTRA_BIOME_IDS.contains(id);
    }

    /** @return true if a {@code condition.dimensions} entry is the Ultra Space dimension. */
    public static boolean isUltraDimension(String dimension) {
        return ULTRA_SPACE_DIMENSION.equals(dimension);
    }
}
