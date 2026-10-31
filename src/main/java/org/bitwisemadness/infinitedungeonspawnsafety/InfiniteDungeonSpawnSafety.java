package org.bitwisemadness.infinitedungeonspawnsafety;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.bitwisemadness.infinitedungeonspawnsafety.safety.SafetyConfigs;
import org.bitwisemadness.infinitedungeonspawnsafety.safety.SafetyDataReloadListener;
import org.bitwisemadness.infinitedungeonspawnsafety.safety.SpawnSafetyHandler;
import org.bitwisemadness.infinitedungeonspawnsafety.safety.StructureElevationSource;
import org.bitwisemadness.infinitedungeonspawnsafety.safety.StructureMagiculeReloadListener;
import org.slf4j.Logger;

/**
 * Infinite Dungeon Spawn Safety entry point.
 *
 * <p>Blocks natural spawns when the ambient magicule of the spawn chunk cannot
 * sustain the mob, filtered by per-category toggles and a dimension list. It
 * consumes the infinite-dungeon API for ambient magicule and entity power
 * values, and works in a pack without the core module installed.</p>
 */
@Mod(InfiniteDungeonSpawnSafety.MOD_ID)
public final class InfiniteDungeonSpawnSafety {

    public static final String MOD_ID = "infinite_dungeon_spawn_safety";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InfiniteDungeonSpawnSafety(IEventBus modBus, ModContainer container) {
        // The safety.* config file, read per spawn into the pure record.
        SafetyConfigs.register(container);
        // Datapack lists and classification overrides load through the API's
        // reload machinery on server start and on every /reload.
        NeoForge.EVENT_BUS.addListener(SafetyDataReloadListener::onAddReloadListeners);
        // Structure-elevation rows (data/<ns>/infinite_dungeon/structure_magicule/)
        // reload the same way; they lift the ambient around listed structures.
        NeoForge.EVENT_BUS.addListener(StructureMagiculeReloadListener::onAddReloadListeners);
        // Structure floors land in the chunks' Tensura storage as they load, so
        // every consumer reads one truth (see StructureElevationSource).
        NeoForge.EVENT_BUS.addListener(StructureElevationSource::onChunkLoad);
        // The natural-spawn interception itself. Only NATURAL spawns are ever
        // gated; everything else is ignored at the allowlist.
        NeoForge.EVENT_BUS.addListener(SpawnSafetyHandler::onPositionCheck);
        LOGGER.info("Infinite Dungeon Spawn Safety initialized");
    }
}
