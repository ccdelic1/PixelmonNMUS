package com.ccdelic.ultrabiomegone.spawning;

import java.util.List;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.pixelmonmod.pixelmon.api.spawning.AbstractSpawner;
import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnLocation;
import com.pixelmonmod.pixelmon.api.spawning.SpawnerCondition;
import com.pixelmonmod.pixelmon.spawning.PlayerBasedLevels;

/**
 * Exempts <b>only</b> Ultra Beast spawns from Pixelmon's {@link PlayerBasedLevels} party-level gate,
 * leaving that gate fully intact for every other Pokémon.
 *
 * <p><b>The gate.</b> When Pixelmon's {@code spawnLevelsCloserToPlayerLevels} config is on (its
 * default), {@link PlayerBasedLevels} is registered as a {@code SpawnerCondition} whose
 * {@code fits(...)} rejects any spawn whose {@code minLevel} exceeds the player's highest party
 * level. Ultra Beasts spawn at level 60 (Poipole 40), so an early-game player would never see them
 * in their relocated overworld biomes. We subclass {@link PlayerBasedLevels} and short-circuit
 * {@code fits} to {@code true} for UB spawns (identified by the {@code ultrabeast} spawn tag every
 * UB set carries), delegating to the stock logic for everything else — so the level tweak that
 * scales normal spawns and the gate that blocks over-levelled ones both stay in force.
 *
 * <p><b>How it's installed.</b> {@link PlayerBasedLevels} is a single shared instance that Pixelmon
 * adds to every spawner preset's {@code conditions} list, and {@code SpawnerBuilder.apply()} shares
 * that list by reference with each built (and per-player) spawner. So replacing the stock entry with
 * an instance of this class, in place, propagates to all current and future spawners. Because this
 * class {@code instanceof PlayerBasedLevels}, Pixelmon's {@code containsA(..., PlayerBasedLevels)}
 * guard on later reloads sees it as already-present and never re-adds the stock gate — the swap is
 * idempotent. Only the {@code conditions} entry is replaced; the identical stock instance in the
 * {@code tweaks} list (which scales normal spawn levels, and is a no-op for the UBs' fixed level 60)
 * is left untouched.
 */
public final class UltraBeastLevelExemption extends PlayerBasedLevels {

    /** The spawn tag every Ultra Beast set carries (see {@code data/pixelmon/spawning/standard/*.set.json}). */
    private static final String ULTRA_BEAST_TAG = "ultrabeast";

    @Override
    public boolean fits(AbstractSpawner spawner, SpawnInfo spawnInfo, SpawnLocation spawnLocation) {
        if (spawnInfo != null && spawnInfo.tags != null && spawnInfo.tags.contains(ULTRA_BEAST_TAG)) {
            return true; // UBs bypass the party-level gate entirely
        }
        return super.fits(spawner, spawnInfo, spawnLocation);
    }

    /**
     * Replaces the stock {@link PlayerBasedLevels} condition on every given spawner preset with an
     * instance of this class. Gated by {@link Config#EXEMPT_ULTRA_BEASTS_FROM_LEVEL_GATE}. Safe to
     * call on every reload: it only matches the exact stock class, so re-running it is a no-op once
     * our subclass is already in place, and it simply finds nothing when the gate is globally off
     * (Pixelmon's {@code spawnLevelsCloserToPlayerLevels = false}).
     */
    public static void install(AbstractSpawner.SpawnerBuilder<?>[] presets) {
        if (!Config.EXEMPT_ULTRA_BEASTS_FROM_LEVEL_GATE.get()) {
            UltraBiomeGone.debugLog("UltraBeastLevelExemption: disabled in config, leaving the stock party-level gate in place");
            return;
        }
        int replaced = 0;
        for (AbstractSpawner.SpawnerBuilder<?> preset : presets) {
            List<SpawnerCondition> conditions = preset.conditions;
            if (conditions == null) {
                continue;
            }
            for (int i = 0; i < conditions.size(); i++) {
                SpawnerCondition c = conditions.get(i);
                // Exact-class match: replace only the stock gate, and skip our own subclass so
                // re-running this (on every /reload) stays idempotent.
                if (c != null && c.getClass() == PlayerBasedLevels.class) {
                    conditions.set(i, new UltraBeastLevelExemption());
                    replaced++;
                }
            }
        }
        if (replaced > 0) {
            UltraBiomeGone.LOGGER.info(
                "[UltraBiomeGone] Exempted Ultra Beasts from the party-level spawn gate on {} spawner preset(s)", replaced);
        } else {
            UltraBiomeGone.debugLog("UltraBeastLevelExemption: no stock PlayerBasedLevels condition found "
                + "(already exempted this session, or Pixelmon's spawnLevelsCloserToPlayerLevels is off)");
        }
    }
}
