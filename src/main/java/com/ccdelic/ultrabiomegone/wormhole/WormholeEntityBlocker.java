package com.ccdelic.ultrabiomegone.wormhole;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.pixelmonmod.pixelmon.entities.WormholeEntity;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Belt-and-suspenders guarantee that no Ultra Wormhole can ever appear, regardless of source
 * (the external move, commands, other mods, or any code path we didn't anticipate): cancel any
 * {@link WormholeEntity} being added to a level. Combined with {@link WormholeMoveSkillDisabler}
 * this makes the "no wormholes" requirement airtight even if the move-skill replacement's timing
 * were ever off.
 */
public final class WormholeEntityBlocker {
    private WormholeEntityBlocker() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        try {
            if (!Config.BLOCK_WORMHOLE_ENTITIES.get()) {
                return;
            }
            if (event.getEntity() instanceof WormholeEntity) {
                event.setCanceled(true);
            }
        } catch (Exception e) {
            UltraBiomeGone.LOGGER.warn("[PixelmonNMUS] WormholeEntityBlocker error: {}", e.getMessage());
        }
    }
}