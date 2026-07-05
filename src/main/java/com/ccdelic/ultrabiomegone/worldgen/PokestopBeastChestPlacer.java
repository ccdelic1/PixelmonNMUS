package com.ccdelic.ultrabiomegone.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.pixelmonmod.pixelmon.blocks.tileentity.PokeChestTileEntity;
import com.pixelmonmod.pixelmon.blocks.tileentity.PokeStopTileEntity;
import com.pixelmonmod.pixelmon.init.registry.BlockRegistration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Places exactly one Beast Ball chest ({@code pixelmon:beast_chest}, which rolls the untouched
 * {@code ultraSpace} loot pool) on the ground beside each naturally-generated PokéStop.
 *
 * <p>PokéStops are Pixelmon's {@code way_point} jigsaw structures; there is no per-structure
 * generation event, so we hook {@link ChunkEvent.Load}: when a chunk containing a
 * {@link PokeStopTileEntity} is loaded, we place a beast chest next to it. The work is:
 * <ul>
 *   <li><b>deferred</b> to the server thread ({@code server.execute}) so we never mutate a chunk
 *       mid-load;</li>
 *   <li><b>idempotent</b> — we skip any PokéStop that already has a beast chest within a small
 *       radius, so reloading a chunk never adds a second one. This also means PokéStops that
 *       generated before the mod was installed get a chest the first time their chunk reloads.</li>
 * </ul>
 * PokéStop blocks have no survival crafting recipe (creative/command only), so this cannot be farmed
 * by placing PokéStops. Disable via {@code beast_chest.enabled}.
 */
public final class PokestopBeastChestPlacer {
    private PokestopBeastChestPlacer() {}

    /** Radius (blocks) around a PokéStop checked for an existing beast chest before placing. */
    private static final int DEDUPE_RADIUS = 1;

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!Config.BEAST_CHEST_ENABLED.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        List<BlockPos> pokestops = null;
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (chunk.getBlockEntity(pos) instanceof PokeStopTileEntity) {
                if (pokestops == null) {
                    pokestops = new ArrayList<>(1);
                }
                pokestops.add(pos.immutable());
            }
        }
        if (pokestops == null) {
            return;
        }
        final List<BlockPos> found = pokestops;
        // Defer off the chunk-load callback onto the server thread.
        level.getServer().execute(() -> placeAll(level, found));
    }

    private static void placeAll(ServerLevel level, List<BlockPos> pokestops) {
        Block beastChest = BlockRegistration.BEAST_CHEST.value();
        for (BlockPos pokestop : pokestops) {
            // Confirm the PokéStop is still there (chunk could have changed).
            if (!(level.getBlockEntity(pokestop) instanceof PokeStopTileEntity)) {
                continue;
            }
            if (hasBeastChestNearby(level, pokestop, beastChest)) {
                continue;
            }
            BlockPos spot = findGroundSpot(level, pokestop);
            if (spot == null) {
                continue;
            }
            level.setBlock(spot, beastChest.defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(spot) instanceof PokeChestTileEntity chest) {
                chest.enableConfigSettings();
            }
            UltraBiomeGone.LOGGER.debug("[PixelmonNMUS] Placed beast chest at {} beside PokéStop {}", spot, pokestop);
        }
    }

    private static boolean hasBeastChestNearby(ServerLevel level, BlockPos center, Block beastChest) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -DEDUPE_RADIUS; dx <= DEDUPE_RADIUS; dx++) {
            for (int dy = -DEDUPE_RADIUS; dy <= DEDUPE_RADIUS; dy++) {
                for (int dz = -DEDUPE_RADIUS; dz <= DEDUPE_RADIUS; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(cursor).is(beastChest)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Four diagonal offsets reused in findGroundSpot. */
    private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    /**
     * Finds an air/replaceable spot on solid ground next to the PokéStop, at the PokéStop's own
     * height or one below (the structure floor). Prefers the four cardinal neighbours, then diagonals.
     */
    private static BlockPos findGroundSpot(ServerLevel level, BlockPos pokestop) {
        for (int dy = 0; dy >= -1; dy--) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = pokestop.relative(dir).above(dy);
                if (isPlaceable(level, candidate)) {
                    return candidate;
                }
            }
            for (int[] diag : DIAGONALS) {
                BlockPos candidate = pokestop.offset(diag[0], dy, diag[1]);
                if (isPlaceable(level, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** True if {@code pos} is replaceable (air/plants) and the block beneath can support a chest. */
    private static boolean isPlaceable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced()) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), Direction.UP);
    }
}
