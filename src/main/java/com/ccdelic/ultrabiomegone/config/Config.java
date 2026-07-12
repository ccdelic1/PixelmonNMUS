package com.ccdelic.ultrabiomegone.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All tunables for PixelmonNMUS ("No More Ultra Space"). Every behaviour the mod adds is gated by a
 * flag here, and every hard-coded number (rarity divisor, geode rarity, beast-chest rate, horde chance, …) is a
 * config value, so the whole mod can be tuned or selectively disabled without a rebuild. This is a
 * COMMON config (loaded early on both sides, before spawn post-processing and worldgen run).
 *
 * <p>Handlers read these values live, so most changes take effect on a config reload / next
 * {@code /reload} (spawn data) or next world generation (geodes, beast chests). The geode and
 * beast-chest master switches and rarities are enforced in code (a custom placement modifier and the
 * beast-chest feature read them at worldgen time), so they only affect newly generated chunks. The
 * remaining datapack-driven pieces (the geode block/structure definitions, crafting recipes, and the
 * replacement research quest) are not represented here — those are tuned by editing/overriding the
 * datapack JSONs.
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
    public static final ModConfigSpec.BooleanValue EXEMPT_ULTRA_BEASTS_FROM_LEVEL_GATE;

    // --- Loot ---
    public static final ModConfigSpec.BooleanValue RELOCATE_LOOT;

    // --- Ultra terrain-block geodes (hijack Pixelmon's crystal geode structure) ---
    public static final ModConfigSpec.BooleanValue GEODE_ENABLED;
    public static final ModConfigSpec.IntValue GEODE_RARITY;

    // --- Overworld beast chest generation ---
    public static final ModConfigSpec.BooleanValue BEAST_CHEST_ENABLED;
    public static final ModConfigSpec.IntValue BEAST_CHEST_RARITY;

    // --- Debug logging ---
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;

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
            .comment("Relocate the 10 Ultra Beast spawn sets into their mapped overworld biomes.")
            .define("relocateUltraBeasts", true);
        RELOCATION_RARITY_DIVISOR = b
            .comment("Divide the relocated spawns' rarity by this (default 2.0 -> UB 0.5 becomes 0.25).")
            .defineInRange("relocationRarityDivisor", 2.0D, 0.0001D, 1000.0D);
        EXEMPT_ULTRA_BEASTS_FROM_LEVEL_GATE = b
            .comment("Exempt Ultra Beasts (only them) from Pixelmon's party-level spawn gate, so they can appear",
                     "even for low-level players. With Pixelmon's 'spawnLevelsCloserToPlayerLevels' on (its default),",
                     "a spawn is normally blocked unless your highest party Pokemon is at least the spawn's minLevel;",
                     "UBs spawn at level 60 (Poipole 40), which would otherwise hide them from early-game players.")
            .define("exemptUltraBeastsFromLevelGate", true);
        b.pop();

        b.comment("Forage / headbutt ultra-exclusive loot relocation").push("loot");
        RELOCATE_LOOT = b
            .comment("Relocate the ultra-exclusive forage/headbutt loot entries into their mapped overworld biomes (rarities unchanged).")
            .define("relocateLoot", true);
        b.pop();

        b.comment("Debug logging: when enabled, every action the mod takes is logged to the console",
                  "for troubleshooting silent crashes and verifying mod behaviour.").push("debug");
        DEBUG_LOGGING = b
            .comment("Print a log line for every single thing this mod does (spawn mutations, event gates, dimension guards, etc.).",
                     "Off by default; enable when debugging suspected mod-related crashes.")
            .define("debugLogging", false);
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

        b.comment("Ultra terrain-block geodes: hijacks Pixelmon's crystal geode structure to generate",
                  "ultra space / deep / desert blocks as the inner layer of overworld geodes.",
                  "Plains family → ultra_space_stone, Oceans → ultra_deep_stone, Deserts → ultra_desert_sand.").push("ultra_geodes");
        GEODE_ENABLED = b
            .comment("Master switch for generating ultra terrain-block geodes in the overworld.")
            .define("enabled", true);
        GEODE_RARITY = b
            .comment("Geode rarity: 1-in-N chunks per placement attempt (matching Pixelmon's crystal geode at 24). Lower = more common.")
            .defineInRange("rarity", 24, 1, 10000);
        b.pop();

        b.comment("Overworld beast chest generation: places Beast Ball PokéChests around the world",
                  "like Pixelmon's pokeloot chests, using the untouched ultraSpace loot pool.").push("beast_chests");
        BEAST_CHEST_ENABLED = b
            .comment("Master switch for generating beast chests in the overworld.")
            .define("enabled", true);
        BEAST_CHEST_RARITY = b
            .comment("Beast chest rarity: 1-in-N chunks per placement attempt. Lower = more common.")
            .defineInRange("rarity", 1200, 1, 10000);
        b.pop();

        SPEC = b.build();
    }
}
