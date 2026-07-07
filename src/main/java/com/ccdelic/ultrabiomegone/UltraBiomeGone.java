package com.ccdelic.ultrabiomegone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ccdelic.ultrabiomegone.battle.HordeBattleListener;
import com.ccdelic.ultrabiomegone.config.Config;
import com.ccdelic.ultrabiomegone.dimension.UltraSpaceAccessGuard;
import com.ccdelic.ultrabiomegone.spawning.UltraSpawnReloadListener;
import com.ccdelic.ultrabiomegone.worldgen.ModFeatures;
import com.ccdelic.ultrabiomegone.wormhole.WormholeMoveSkillDisabler;
import com.pixelmonmod.pixelmon.Pixelmon;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Entry point for <b>PixelmonNMUS</b> ("No More Ultra Space") — a Pixelmon side-mod.
 *
 * <p>This mod removes the Ultra Space dimension from play and redistributes everything
 * worth having from it into the overworld. See {@code plan.md} / {@code ultraModTemplate.md}
 * for the full specification. Phases are wired in from their own packages:
 * <ul>
 *   <li>{@code spawning} — SpawnSet post-processor (kill wormholes + ultra pools, relocate UBs/loot)</li>
 *   <li>{@code wormhole} — disable the {@code open_wormhole} external move + dimension-travel guard</li>
 *   <li>{@code worldgen} — ultra terrain geodes + beast chest generation (datapack + code)</li>
 *   <li>{@code battle} — overworld horde battles</li>
 *   <li>{@code config} — every behaviour/number above is tunable via {@link Config}</li>
 * </ul>
 *
 * <p>The mod id ({@code ultrabiomegone}) must match {@code mod_id} in {@code gradle.properties}
 * and the {@code modId} in {@code neoforge.mods.toml}. The display name is "PixelmonNMUS".
 */
@Mod(UltraBiomeGone.MOD_ID)
public class UltraBiomeGone {
    public static final String MOD_ID = "ultrabiomegone";
    public static final Logger LOGGER = LoggerFactory.getLogger("PixelmonNMUS");

    /** Set to true after config is registered; {@link #debugLog} gates on it to avoid reading unloaded config. */
    private static volatile boolean configReady = false;

    public UltraBiomeGone(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("[PixelmonNMUS] Constructing — Ultra Space redistribution mod loading");
        modEventBus.addListener(this::commonSetup);

        // Register custom worldgen feature types (ultra geodes, beast chests).
        debugLog("Registering custom worldgen feature types (ultra_geode, beast_chest)");
        ModFeatures.register(modEventBus);

        // Register the config so every behaviour/number is tunable (see Config).
        // Custom file name so it generates as PixelmonNMUS.toml (not <modid>-common.toml).
        debugLog("Registering COMMON config as PixelmonNMUS.toml");
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "PixelmonNMUS.toml");

        // Game-bus listeners. The spawn-data post-processor is registered at LOWEST priority so our
        // reload listener is added AFTER Pixelmon's, and therefore applies after Pixelmon has loaded
        // its spawn data (and re-registered its move skills) on every server start and /reload.
        IEventBus gameBus = NeoForge.EVENT_BUS;
        debugLog("Registering NeoForge EVENT_BUS listeners: reload (LOWEST), travel guard, login guard, entity join");
        gameBus.addListener(EventPriority.LOWEST, UltraBiomeGone::onAddReloadListeners);
        gameBus.addListener(UltraSpaceAccessGuard::onTravel);
        gameBus.addListener(UltraSpaceAccessGuard::onLogin);
        // WormholeEntityBlocker is NOT registered here — the DimensionsConfig.ultraSpace=false
        // override (applied during spawn post-processing) prevents wormholes from spawning
        // naturally via SpawnInfoWormhole.fits().  Registering an EntityJoinLevelEvent handler
        // that fires on every entity addition can interfere with chunk loading on worker threads.
        // gameBus.addListener(WormholeEntityBlocker::onEntityJoin);
        // Battle events (BattleStartedEvent / BattleEndEvent) are posted on Pixelmon's own bus,
        // not the NeoForge bus, so the horde listener registers there.
        debugLog("Registering HordeBattleListener on Pixelmon.EVENT_BUS");
        Pixelmon.EVENT_BUS.register(HordeBattleListener.class);
        debugLog("[PixelmonNMUS] Construction complete — all listeners registered");
    }

    private static void onAddReloadListeners(final AddReloadListenerEvent event) {
        debugLog("AddReloadListenerEvent fired — registering UltraSpawnReloadListener (LOWEST priority)");
        event.addListener(new UltraSpawnReloadListener());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        configReady = true;
        debugLog("FMLCommonSetupEvent fired — config is now loaded");
        LOGGER.info("[PixelmonNMUS] Common setup complete");
    }

    /**
     * Logs a formatted debug message when {@code debugLogging} is enabled in the mod config.
     * Prefixes every message with {@code [PixelmonNMUS]} for consistent console filtering.
     *
     * <p>Before the config is registered (during early construction), all messages are logged
     * unconditionally since there are only a handful and they help diagnose startup issues.
     * After config registration, messages are gated on {@code debug.debugLogging}.
     */
    public static void debugLog(String format, Object... args) {
        if (!configReady || Config.DEBUG_LOGGING.get()) {
            LOGGER.info("[PixelmonNMUS] " + format, args);
        }
    }
}
