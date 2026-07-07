package com.ccdelic.ultrabiomegone.spawning;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccdelic.ultrabiomegone.UltraBiomeGone;
import com.pixelmonmod.pixelmon.api.npc.NPCPreset;
import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnSet;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.collection.SpawnInfoCollection;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.npcs.SpawnInfoNPC;
import com.pixelmonmod.pixelmon.init.registry.PixelmonRegistry;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Removes invalid NPC trainer-preset spawn entries from loaded spawn data.
 *
 * <p>Pixelmon 9.3.16 can load a trainer spawn entry whose preset id does not
 * exist in the NPC preset registry. That entry later logs
 * "Invalid NPC preset provided" and can cascade into a client-side NPE during
 * entity animation init while chunks are being explored. We sanitize these
 * entries out of the live spawn data during our reload pass.
 */
public final class NpcPresetSanitizer {
    private NpcPresetSanitizer() {}

    /**
     * Runtime-confirmed broken preset id observed in 9.3.16 logs.
     */
    private static final ResourceLocation KNOWN_BROKEN_PRESET =
        ResourceLocation.parse("pixelmon:trainers/femalescientist");

    public static int run() {
        Set<ResourceLocation> validPresets = getValidNpcPresets();
        boolean haveRegistry = validPresets != null;

        int removed = 0;
        Map<String, List<SpawnSet>> all = PixelmonSpawning.getAll();
        for (List<SpawnSet> sets : all.values()) {
            for (SpawnSet set : sets) {
                if (set == null || set.spawnInfos == null) {
                    continue;
                }
                removed += removeInvalidFromList(set.spawnInfos, validPresets, haveRegistry);
            }
        }

        if (removed > 0) {
            UltraBiomeGone.LOGGER.warn(
                "[PixelmonNMUS] Removed {} invalid NPC preset spawn entr{} during spawn-data sanitization",
                removed, removed == 1 ? "y" : "ies");
        } else {
            UltraBiomeGone.debugLog("NpcPresetSanitizer: no invalid NPC preset spawn entries found");
        }
        return removed;
    }

    private static int removeInvalidFromList(List<SpawnInfo> infos, Set<ResourceLocation> validPresets, boolean haveRegistry) {
        int removed = 0;
        for (int i = infos.size() - 1; i >= 0; i--) {
            SpawnInfo info = infos.get(i);
            if (info instanceof SpawnInfoCollection collection && collection.collection != null) {
                removed += removeInvalidFromList(collection.collection, validPresets, haveRegistry);
            }

            if (isInvalidNpcPreset(info, validPresets, haveRegistry)) {
                infos.remove(i);
                removed++;
            }
        }
        return removed;
    }

    private static boolean isInvalidNpcPreset(SpawnInfo info, Set<ResourceLocation> validPresets, boolean haveRegistry) {
        if (!(info instanceof SpawnInfoNPC npcInfo) || npcInfo.preset == null) {
            return false;
        }

        ResourceLocation preset = npcInfo.preset;

        // Always remove known bad preset from this Pixelmon version.
        if (KNOWN_BROKEN_PRESET.equals(preset)) {
            UltraBiomeGone.debugLog("NpcPresetSanitizer: removing known broken NPC preset {}", preset);
            return true;
        }

        // If registry is available, remove any preset that doesn't exist.
        if (haveRegistry && !validPresets.contains(preset)) {
            UltraBiomeGone.debugLog("NpcPresetSanitizer: removing missing NPC preset {}", preset);
            return true;
        }

        return false;
    }

    private static Set<ResourceLocation> getValidNpcPresets() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return null;
            }
            Registry<NPCPreset> registry = server.registryAccess().registry(PixelmonRegistry.NPC_PRESET_REGISTRY).orElse(null);
            if (registry == null) {
                return null;
            }
            return new HashSet<>(registry.keySet());
        } catch (Throwable t) {
            UltraBiomeGone.LOGGER.warn("[PixelmonNMUS] Could not resolve NPC preset registry for sanitization", t);
            return null;
        }
    }
}
