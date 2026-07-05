package com.ccdelic.ultrabiomegone.battle;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.ccdelic.ultrabiomegone.config.Config;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.pixelmonmod.pixelmon.api.battles.BattleType;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent;
import com.pixelmonmod.pixelmon.api.pokemon.species.aggression.Aggression;
import com.pixelmonmod.pixelmon.api.spawning.AbstractSpawner;
import com.pixelmonmod.pixelmon.api.spawning.SpawnAction;
import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnLocation;
import com.pixelmonmod.pixelmon.api.spawning.SpawnerCoordinator;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnActionPokemon;
import com.pixelmonmod.pixelmon.api.util.WeightedSet;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import com.pixelmonmod.pixelmon.battles.api.BattleBuilder;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRuleSet;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.BattleAI;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import com.pixelmonmod.pixelmon.spawning.PlayerTrackingSpawner;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Phase 7: ports Ultra Space's horde battles to the overworld. Modelled on
 * {@code UltraSpaceListener.onBattleStart}/{@code onBattleEnd}, but gated on our own conditions
 * rather than the ultra dimension configs:
 * <ul>
 *   <li>the battle is in the <b>overworld</b>;</li>
 *   <li>a 1v1 wild-vs-player battle;</li>
 *   <li>the initiating wild Pokémon's {@link Aggression} is <b>AGGRESSIVE</b>; and</li>
 *   <li>a <b>2%</b> roll succeeds (vs Ultra Space's 40%).</li>
 * </ul>
 * When it fires, up to 4 extra aggressive wild Pokémon are spawned around the player and folded into
 * a {@link BattleType#HORDE} battle. The filler Pokémon are tagged {@code REMOVE_AFTER_BATTLE} and
 * cleaned up in {@link #onBattleEnd}.
 *
 * <p><b>Bus note:</b> {@code BattleStartedEvent}/{@code BattleEndEvent} are posted on
 * {@code Pixelmon.EVENT_BUS} (not the NeoForge bus), so this listener is registered there.
 */
public final class HordeBattleListener {
    private HordeBattleListener() {}

    @SubscribeEvent
    public static void onBattleStart(BattleStartedEvent.Pre event) {
        if (!Config.HORDE_ENABLED.get()) {
            return;
        }
        BattleParticipant player = findPlayer(event);
        if (player == null || player.getPlayer() == null) {
            return;
        }
        // Gate 1: overworld only.
        if (player.getPlayer().level().dimension() != Level.OVERWORLD) {
            return;
        }
        // Gate 2: a plain 1v1.
        if (event.getTeamOne().length > 1 || event.getTeamTwo().length > 1) {
            return;
        }
        WildPixelmonParticipant wildPokemon = findWildPokemon(event);
        if (wildPokemon == null || !(wildPokemon.getEntity() instanceof PixelmonEntity entity)) {
            return;
        }
        // Gate 3: the initiating wild Pokémon is AGGRESSIVE.
        if (entity.getAggression() != Aggression.AGGRESSIVE) {
            return;
        }
        // Gate 4: configurable roll (default 2%) and there aren't already extra participants.
        if (event.getBattleController().participants.size() > 2 || !RandomHelper.getRandomChance(Config.HORDE_CHANCE.get())) {
            return;
        }
        if (PixelmonSpawning.coordinator == null) {
            return;
        }
        AbstractSpawner spawner = PixelmonSpawning.coordinator.getSpawner(player.getName().getString());
        if (!(spawner instanceof PlayerTrackingSpawner pSpawner)) {
            return;
        }

        List<BattleParticipant> newWildPokemon = Lists.newArrayList();
        newWildPokemon.add(new WildPixelmonParticipant(entity));

        // Cancel the vanilla 1v1 start; we assemble and start a HORDE battle asynchronously instead,
        // mirroring UltraSpaceListener's spawn pipeline.
        event.setCanceled(true);
        pSpawner.getTrackedBlockCollection(player.getPlayer(), 0.0F, 0.0F,
                pSpawner.horizontalSliceRadius, pSpawner.verticalSliceRadius, 0, 0)
            .thenApplyAsync(blockCollection -> {
                List<SpawnLocation> spawnLocations = pSpawner.spawnLocationCalculator.calculateSpawnableLocations(blockCollection);
                WeightedSet<Pair<SpawnInfo, SpawnLocation>> possibleSpawns = WeightedSet.newWeightedSet();
                for (SpawnLocation spawnLocation : spawnLocations) {
                    List<SpawnInfo> spawns = spawner.getSuitableSpawns(spawnLocation);
                    for (SpawnInfo spawn : spawns) {
                        possibleSpawns.add(spawn.rarity, Pair.of(spawn, spawnLocation));
                    }
                }
                return possibleSpawns.isEffectivelyEmpty() ? null : possibleSpawns;
            }, SpawnerCoordinator.PROCESSOR)
            .thenAcceptAsync(possibleSpawns -> {
                if (possibleSpawns == null) {
                    return;
                }
                int maxExtra = Config.HORDE_MAX_EXTRA.get();
                for (int i = 0; i < maxExtra; i++) {
                    Pair<SpawnInfo, SpawnLocation> spawnAction = possibleSpawns.get();
                    SpawnAction<?> action = spawnAction.getFirst().construct(spawner, spawnAction.getSecond());
                    if (action instanceof SpawnActionPokemon pokemonAction) {
                        PixelmonEntity spawnedEntity = pokemonAction.getOrCreateEntity();
                        spawner.tweaks.forEach(tweak -> tweak.doTweak(spawner, (SpawnAction<? extends Entity>) action));
                        SpawnLocation loc = spawnAction.getSecond();
                        spawnedEntity.setPos(loc.location.pos.getX() + 0.5, loc.location.pos.getY(), loc.location.pos.getZ() + 0.5);
                        loc.location.world.addFreshEntity(spawnedEntity);
                        spawnedEntity.addTag("REMOVE_AFTER_BATTLE");
                        spawner.spawnedTracker.addEntity(spawnedEntity);
                        WildPixelmonParticipant nextParticipant = new WildPixelmonParticipant(spawnedEntity);
                        nextParticipant.setBattleAI(BattleAI.AGGRESSIVE);
                        newWildPokemon.add(nextParticipant);
                    }
                }
                BattleController controller = BattleBuilder.builder()
                    .rules(BattleRuleSet.AG)
                    .teamOne(newWildPokemon)
                    .teamTwo(player)
                    .setBattleType(BattleType.HORDE)
                    .start(player.getPlayer().registryAccess())
                    .join();
                if (controller != null) {
                    for (BattleParticipant battleParticipant : newWildPokemon) {
                        battleParticipant.bc = controller;
                        for (PixelmonWrapper pixelmonWrapper : battleParticipant.allPokemon) {
                            PixelmonEntity pixelmonEntity = pixelmonWrapper.getEntity();
                            if (pixelmonEntity != null) {
                                pixelmonEntity.battleController = controller;
                            }
                        }
                    }
                    controller.sendToAll(wildPokemon.getName() + " summoned some nearby allies to assist them");
                }
            }, ServerLifecycleHooks.getCurrentServer())
            .orTimeout(15, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                if (!(ex instanceof java.util.concurrent.TimeoutException)) {
                    UltraBiomeGone.LOGGER.warn("[PixelmonNMUS] Horde battle spawn failed", ex);
                }
                return null;
            });
    }

    @SubscribeEvent
    public static void onBattleEnd(BattleEndEvent event) {
        for (BattleParticipant participant : event.getBattleController().participants) {
            if (participant.isWild() && participant.getEntity() != null
                && participant.getEntity().getTags().contains("REMOVE_AFTER_BATTLE")) {
                participant.getEntity().remove(RemovalReason.DISCARDED);
            }
        }
    }

    private static BattleParticipant findPlayer(BattleStartedEvent event) {
        for (BattleParticipant p : event.getTeamOne()) {
            if (p.isPlayer()) {
                return p;
            }
        }
        for (BattleParticipant p : event.getTeamTwo()) {
            if (p.isPlayer()) {
                return p;
            }
        }
        return null;
    }

    private static WildPixelmonParticipant findWildPokemon(BattleStartedEvent event) {
        for (BattleParticipant p : event.getTeamOne()) {
            if (p.isWild()) {
                return (WildPixelmonParticipant) p;
            }
        }
        for (BattleParticipant p : event.getTeamTwo()) {
            if (p.isWild()) {
                return (WildPixelmonParticipant) p;
            }
        }
        return null;
    }
}
