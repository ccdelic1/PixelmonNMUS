package com.ccdelic.ultrabiomegone.dimension;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Belt-and-suspenders access control for the (now removed) Ultra Space dimension. Removing the
 * wormhole spawns and the {@code open_wormhole} move stops the intended entry routes; this guard
 * additionally blocks any <i>other</i> route (commands, other mods) from sending an entity to
 * {@code pixelmon:ultra_space}, and rescues any player who is already inside when the mod is first
 * installed (they'd otherwise be stranded, since their return wormhole no longer spawns).
 */
public final class UltraSpaceAccessGuard {
    private UltraSpaceAccessGuard() {}

    public static final ResourceLocation ULTRA_SPACE =
        ResourceLocation.fromNamespaceAndPath("pixelmon", "ultra_space");

    /** Cancel any travel whose destination is Ultra Space. Travel <i>out</i> of it is unaffected. */
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (Config.BLOCK_TRAVEL_TO_ULTRA_SPACE.get() && ULTRA_SPACE.equals(event.getDimension().location())) {
            event.setCanceled(true);
        }
    }

    /** If a player logs in while stuck inside Ultra Space, return them to the overworld spawn. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.RESCUE_STUCK_PLAYERS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ULTRA_SPACE.equals(player.level().dimension().location())) {
            return;
        }
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
            player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal(
            "Ultra Space has collapsed. You have been returned to the overworld."));
        UltraBiomeGone.LOGGER.info("[UltraBiomeGone] Rescued {} from the removed Ultra Space dimension",
            player.getGameProfile().getName());
    }
}
