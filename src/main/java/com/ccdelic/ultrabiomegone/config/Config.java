package com.ccdelic.ultrabiomegone.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All tunables for PixelmonNMUS ("No More Ultra Space"). Every behaviour the mod adds is gated by a
 * flag here, and every hard-coded number (rarity divisor, beast-chest rate, horde chance, …) is a
 * config value, so the whole mod can be tuned or selectively disabled without a rebuild. This is a
 * COMMON config (loaded early on both sides, before spawn post-processing and worldgen run).
 *
 * <p>Handlers read these values live, so most changes take effect on a config reload / next
 * {@code /reload} (spawn data) or next world generation (ore clusters). The datapack-driven pieces
 * (ore-cluster JSONs, crafting recipes, the replacement research quest) are not represented here —
 * those are tuned by editing/overriding the datapack JSONs.
 */
public final class Config {
    private Config() {}

    public static final ModConfigSpec SPEC;

    // --- Wormhole / Ultra Space access ---
    public static final ModConfigSpec.BooleanValue DISABLE_WORMHOLE_SPAWNS;
    public static final ModConfigSpec.BooleanValue DISABLE_OPEN_WORMHOLE_MOVE;
    public static final ModConfigSpec.BooleanValue BLOCK_WORMHOLE_ENTITIES;
    public static final ModConfigSpec.BooleanValue BLOCK_TRAVEL_TO_ULTRA_SPACE;
    public static final ModConfigSpec.BooleanValue RESCUE_STUCK_PLAYERS;

    // --- Spawn data ---
    public static final ModConfigSpec.BooleanValue SCRUB_ULTRA_SPAWNS;
    public static final ModConfigSpec.BooleanValue RELOCATE_ULTRA_BEASTS;
    public static final ModConfigSpec.DoubleValue RELOCATION_RARITY_DIVISOR;

    // --- Loot ---
    public static final ModConfigSpec.BooleanValue RELOCATE_LOOT;

    // --- Beast Ball PokéChest ---
    public static final ModConfigSpec.BooleanValue BEAST_CHEST_ENABLED;

    // --- Ultra terrain-block ore clusters ---
    public static final ModConfigSpec.BooleanValue ORE_ENABLED;
    public static final ModConfigSpec.BooleanValue ORE_BIOME_EXCLUSIVE;
    public static final ModConfigSpec.IntValue ORE_RARITY;
    public static final ModConfigSpec.IntValue ORE_CLUSTER_SIZE;

    // --- Overworld horde battles ---
    public static final ModConfigSpec.BooleanValue HORDE_ENABLED;
    public static final ModConfigSpec.DoubleValue HORDE_CHANCE;
    public static final ModConfigSpec.IntValue HORDE_MAX_EXTRA;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Wormhole shutdown and Ultra Space access control").push("wormhole");
        DISABLE_WORMHOLE_SPAWNS = b
            .comment("Remove both Ultra Wormhole spawn entries from the loaded spawn data (no wormholes spawn).")
            .define("disableWormholeSpawns", true);
        DISABLE_OPEN_WORMHOLE_MOVE = b
            .comment("Neutralise the 'open_wormhole' external move (Palkia/Lunala/Solgaleo/Arceus/Necrozma): it messages the player and opens no wormhole, with no cooldown burn.")
            .define("disableOpenWormholeMove", true);
        BLOCK_WORMHOLE_ENTITIES = b
            .comment("Belt-and-suspenders: cancel any WormholeEntity from being added to a level, from any source.")
            .define("blockWormholeEntities", true);
        BLOCK_TRAVEL_TO_ULTRA_SPACE = b
            .comment("Cancel any entity travel whose destination is pixelmon:ultra_space (commands, other mods, etc.).")
            .define("blockTravelToUltraSpace", true);
        RESCUE_STUCK_PLAYERS = b
            .comment("Teleport a player who logs in inside Ultra Space back to the overworld spawn.")
            .define("rescueStuckPlayers", true);
        b.pop();

        b.comment("Spawn-data changes (applied to the live loaded spawn sets on every server start / reload)").push("spawns");
        SCRUB_ULTRA_SPAWNS = b
            .comment("Delete ultra-exclusive spawn pools and strip ultra biomes out of mixed pools (so no spawn references an ultra biome).")
            .define("scrubUltraSpawns", true);
        RELOCATE_ULTRA_BEASTS = b
            .comment("Relocate the 12 Ultra Beast / alter-Porygon spawn sets into their mapped overworld biomes.")
            .define("relocateUltraBeasts", true);
        RELOCATION_RARITY_DIVISOR = b
            .comment("Divide the relocated spawns' rarity by this (spec default 3.0 -> UB 0.5 becomes ~0.1667).")
            .defineInRange("relocationRarityDivisor", 3.0D, 0.0001D, 1000.0D);
        b.pop();

        b.comment("Forage / headbutt ultra-exclusive loot relocation").push("loot");
        RELOCATE_LOOT = b
            .comment("Relocate the ultra-exclusive forage/headbutt loot entries into their mapped overworld biomes (rarities unchanged).")
            .define("relocateLoot", true);
        b.pop();

        b.comment("Overworld Beast Ball PokéChest (rolls the untouched ultraSpace loot pool)").push("beast_chest");
        BEAST_CHEST_ENABLED = b
            .comment("Place one Beast Ball chest on the ground at each naturally-generated PokéStop (way_point) structure.",
                     "Idempotent: never adds a second chest to a PokéStop that already has one, and retroactively",
                     "adds chests to PokéStops in already-generated chunks when they reload.")
            .define("enabled", true);
        b.pop();

        b.comment("Overworld horde battles").push("horde");
        HORDE_ENABLED = b
            .comment("Allow aggressive-initiated overworld wild battles to escalate into hordes.")
            .define("enabled", true);
        HORDE_CHANCE = b
            .comment("Chance (0..1) that a qualifying aggressive-initiated overworld wild battle becomes a horde. Spec default 0.02.")
            .defineInRange("chance", 0.02D, 0.0D, 1.0D);
        HORDE_MAX_EXTRA = b
            .comment("Maximum extra wild Pokémon that join the horde. Spec default 4.")
            .defineInRange("maxExtra", 4, 0, 20);
        b.pop();

        b.comment("Underground ore-style clusters of the ultra terrain blocks (space stone, deep sea",
                  "stone/clay/gravel, desert sand/gilded sand/sandstone)").push("ore_clusters");
        ORE_ENABLED = b
            .comment("Master switch for generating ultra terrain-block clusters underground.")
            .define("enabled", true);
        ORE_BIOME_EXCLUSIVE = b
            .comment("If true (default), each block only clusters under its matching biome family:",
                     "  ultra_space_stone -> plains/sunflower_plains/meadow",
                     "  ultra_deep_* -> oceans",
                     "  ultra_desert_* -> deserts",
                     "If false, every cluster can generate under ANY overworld biome.")
            .define("biomeExclusive", true);
        ORE_RARITY = b
            .comment("Cluster rarity: roughly 1 cluster attempt succeeds per N chunks. Lower = more common.")
            .defineInRange("rarity", 8, 1, 10000);
        ORE_CLUSTER_SIZE = b
            .comment("Cluster (blob) size — how many blocks each cluster tries to place (vanilla granite is 64).")
            .defineInRange("clusterSize", 32, 1, 64);
        b.pop();

        SPEC = b.build();
    }
}
