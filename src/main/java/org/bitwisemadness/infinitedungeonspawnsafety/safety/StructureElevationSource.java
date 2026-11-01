package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.fml.ModList;

/**
 * The Minecraft boundary behind {@link StructureElevationLogic}: when a chunk
 * loads, every loaded chunk near a listed structure gets its elevation
 * installed as a Tensura block modifier, so Tensura's own concentration math
 * carries the value from then on.
 *
 * <p>This is the permanent form of the rule - the modifier is an input to
 * Tensura's computation, not a stored value it later re-initialises away, and
 * it is saved with the chunk's data. Values radiate from the structure's
 * pieces (nearest chunks highest, the edge of the reach on the baseline) with
 * a deterministic per-chunk variation, and the reach scales with the baseline
 * (see {@link StructureElevationLogic}).</p>
 *
 * <p>The scan walks the chunk square the rows can reach and reads each
 * <em>loaded</em> chunk's own structure references through a non-blocking
 * {@code getChunkNow}, so the query never loads or generates a chunk. Every
 * load re-evaluates its whole loaded neighbourhood - installing, refreshing or
 * removing the modifier - so the field converges whichever order the
 * structure's pieces and its surrounding chunks load in. Inert without
 * Tensura.</p>
 */
public final class StructureElevationSource {

    private StructureElevationSource() {
    }

    /** The chunk-load hook; registered on the NeoForge event bus. */
    public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getChunk() instanceof LevelChunk chunk) {
            applyAround(level, chunk);
        }
    }

    /** Applies the floors around a newly loaded chunk, if Tensura is present. */
    public static void applyAround(ServerLevel level, LevelChunk loaded) {
        Map<String, List<StructureElevationRow>> rows = StructureElevation.all();
        if (rows.isEmpty() || !tensuraLoaded()) {
            return;
        }
        double maxRadius = StructureElevation.maxRadius();
        int chunkRadius = (int) Math.ceil(maxRadius / 16.0) + 1;
        int centerChunkX = loaded.getPos().x;
        int centerChunkZ = loaded.getPos().z;

        // Structures are a datapack (dynamic) registry: resolve through the
        // level's registry access, not the static BuiltInRegistries.
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<String, List<StructureElevationLogic.Piece>> pieces = new HashMap<>();
        List<LevelChunk> neighbourhood = new ArrayList<>();
        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue; // unloaded neighbourhood: nothing to write, never a load
                }
                neighbourhood.add(chunk);
                for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry
                        : chunk.getAllReferences().entrySet()) {
                    String id = String.valueOf(structures.getKey(entry.getKey()));
                    if (!rows.containsKey(id)) {
                        continue;
                    }
                    List<StructureElevationLogic.Piece> list =
                            pieces.computeIfAbsent(id, key -> new ArrayList<>());
                    entry.getValue().forEach(packed -> {
                        ChunkPos piece = new ChunkPos(packed);
                        list.add(new StructureElevationLogic.Piece(
                                piece.getMinBlockX() + 8, piece.getMinBlockZ() + 8));
                    });
                }
            }
        }
        for (LevelChunk chunk : neighbourhood) {
            OptionalDouble value = StructureElevationLogic.contributionFor(rows, pieces,
                    chunk.getPos().getMinBlockX() + 8, chunk.getPos().getMinBlockZ() + 8);
            TensuraChunkMagiculeBridge.refresh(chunk, value.orElse(0.0));
        }
    }

    private static boolean tensuraLoaded() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("tensura");
    }
}
