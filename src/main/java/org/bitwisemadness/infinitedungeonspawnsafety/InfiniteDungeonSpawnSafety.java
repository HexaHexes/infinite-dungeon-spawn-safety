package org.bitwisemadness.infinitedungeonspawnsafety;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
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

    public InfiniteDungeonSpawnSafety(IEventBus modBus) {
        LOGGER.info("Infinite Dungeon Spawn Safety initialized");
    }
}
