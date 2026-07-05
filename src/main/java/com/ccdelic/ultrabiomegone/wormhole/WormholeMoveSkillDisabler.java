package com.ccdelic.ultrabiomegone.wormhole;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.pixelmonmod.pixelmon.api.moveskills.MoveSkill;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Phase 2: neutralises the {@code open_wormhole} external move (usable by Palkia, Lunala, Solgaleo,
 * Arceus, Necrozma). That move builds and spawns a {@code WormholeEntity} directly, bypassing the
 * spawn-set system — so scrubbing spawn data (Phase 1) does not stop it.
 *
 * <p>Rather than removing the skill (which would make its button silently vanish for those Pokémon)
 * we look up the already-registered skill and <b>overwrite its no-target behaviour in place</b>.
 * This keeps the skill's identity — able-specs, name, sprite, cooldown metadata — intact, so the
 * button still appears, but using it now just messages the player and opens no wormhole. The
 * replacement returns a cooldown of {@code 0}: per {@code MoveSkill.onUsed}, a non-positive return
 * applies no cooldown and consumes no PP, so the player isn't punished with an 18000-tick burn.
 *
 * <p>Must run after Pixelmon's drops reload task re-registers the default move skills
 * ({@code CommonProxy.loadDefaultMoveSkills}); it is invoked from
 * {@link com.ccdelic.ultrabiomegone.spawning.SpawnDataPostProcessor} whose reload listener is added
 * at {@code LOWEST} priority (after all of Pixelmon's).
 */
public final class WormholeMoveSkillDisabler {
    private WormholeMoveSkillDisabler() {}

    private static final String SKILL_ID = "open_wormhole";

    public static void disable() {
        MoveSkill skill = MoveSkill.getMoveSkillByID(SKILL_ID);
        if (skill == null) {
            UltraBiomeGone.LOGGER.warn("[UltraBiomeGone] open_wormhole move skill not found — nothing to disable");
            return;
        }
        skill.behaviourNoTarget(pixelmon -> {
            LivingEntity owner = pixelmon.getOwner();
            if (owner instanceof Player player) {
                player.sendSystemMessage(Component.literal(
                    "The wormhole fizzles out — Ultra Space has collapsed and can no longer be opened."));
            }
            return 0; // no wormhole, no cooldown, no PP consumed
        });
        UltraBiomeGone.LOGGER.info("[UltraBiomeGone] Disabled the open_wormhole external move (behaviour replaced with player feedback)");
    }
}
