package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.chunk.ChunkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The only class that touches Tensura's per-chunk magicule storage.
 *
 * <p>It exists so the write path is isolated: the caller
 * ({@link StructureElevationSource}) checks {@code ModList} for Tensura before
 * reaching here, and the JVM then loads this class on first use - in a pack
 * without Tensura it is never loaded, so the missing classes never surface.</p>
 *
 * <p>The elevation is not written as a stored value: it is installed as an
 * <b>anchored block modifier</b> in the chunk's own modifier list, exactly the
 * mechanism Tensura's own {@code ChunkMagiculeCommand} drives. Tensura applies
 * those modifiers every time it computes the chunk's concentration, so the
 * elevation survives Tensura's periodic re-initialisation and regeneration -
 * its own math re-adds our contribution instead of overwriting it.</p>
 *
 * <p>Modifiers are keyed by their anchor position: a refresh removes the
 * previous entry at the anchor and installs the current value, so repeated
 * chunk loads neither stack values nor leave stale ones behind.</p>
 */
final class TensuraChunkMagiculeBridge {

    /**
     * How far a chunk's anchor modifier reaches, in blocks. One anchor sits at
     * the chunk's centre at a mid-height Y; this distance covers the whole
     * build column from there, so no position in the chunk escapes the
     * contribution.
     */
    static final double COVER_DISTANCE = 192.0;

    /** The anchor height: mid-column, so {@link #COVER_DISTANCE} spans the full build range. */
    static final int ANCHOR_Y = 128;

    private TensuraChunkMagiculeBridge() {
    }

    /** The anchor a chunk's contribution lives at. */
    static BlockPos anchorOf(LevelChunk chunk) {
        return new BlockPos(chunk.getPos().getMinBlockX() + 8, ANCHOR_Y, chunk.getPos().getMinBlockZ() + 8);
    }

    /**
     * Installs (or refreshes) the chunk's elevation, or removes it when the
     * contribution is gone (structures vanished from the datapack).
     *
     * @return whether anything changed
     */
    static boolean refresh(LevelChunk chunk, double value) {
        ChunkStorage storage = TensuraStorages.getChunkFrom(chunk);
        if (storage == null) {
            return false;
        }
        BlockPos anchor = anchorOf(chunk);
        var existing = storage.getBlockModifiers().stream()
                .filter(modifier -> anchor.equals(modifier.pos()))
                .findFirst();
        if (!(value > 0.0)) {
            if (existing.isPresent()) {
                storage.removeBlockModifier(anchor);
                storage.markDirty();
                return true;
            }
            return false;
        }
        // The value is deterministic per chunk, so an unchanged entry means
        // nothing to do - and no dirty mark churn on every chunk load.
        if (existing.isPresent() && existing.get().modifiers().size() == 1
                && existing.get().modifiers().get(0).value() == value) {
            return false;
        }
        storage.removeBlockModifier(anchor);
        storage.addBlockModifier(anchor, value, COVER_DISTANCE);
        storage.markDirty();
        return true;
    }
}
