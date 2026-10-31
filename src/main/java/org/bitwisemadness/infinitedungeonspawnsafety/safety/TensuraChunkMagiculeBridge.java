package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.chunk.ChunkStorage;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The only class that touches Tensura's per-chunk magicule storage.
 *
 * <p>It exists so the write path is isolated: the caller
 * ({@link StructureElevationSource}) checks {@code ModList} for Tensura before
 * reaching here, and the JVM then loads this class on first use - in a pack
 * without Tensura it is never loaded, so the missing classes never surface.</p>
 *
 * <p>The write mirrors what Tensura's own {@code ChunkMagiculeCommand} does:
 * resolve the chunk's storage, raise the stored concentration and its ceiling,
 * and mark the storage dirty so the change persists. Setting the ceiling too
 * keeps the value stable - Tensura's regeneration tick moves the stored
 * magicule toward the ceiling, so a floor written only on the current value
 * would decay back to the pre-structure baseline.</p>
 */
final class TensuraChunkMagiculeBridge {

    private TensuraChunkMagiculeBridge() {
    }

    /**
     * Raises the chunk's stored magicule concentration to at least {@code floor}.
     *
     * @return whether anything was written
     */
    static boolean raiseTo(LevelChunk chunk, double floor) {
        ChunkStorage storage = TensuraStorages.getChunkFrom(chunk);
        if (storage == null || !(floor > 0.0)) {
            return false;
        }
        boolean changed = false;
        if (storage.getMagicule() < floor) {
            storage.setMagicule(floor);
            changed = true;
        }
        if (storage.getMaxMagicule() < floor) {
            storage.setMaxMagicule(floor);
            changed = true;
        }
        if (changed) {
            storage.markDirty();
        }
        return changed;
    }
}
