package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.bitwisemadness.infinitedungeonapi.magicule.ConcentrationModifier;

/**
 * The Minecraft boundary behind {@link StructureElevationLogic}: finds the
 * structure pieces near a spawn position and turns the loaded rows into
 * position-scoped elevation modifiers.
 *
 * <p>The scan walks the chunk square the loaded rows can reach
 * ({@code maxRadius}) and reads each chunk's own structure <em>references</em> -
 * which structures have pieces in that chunk - through a non-blocking
 * {@code getChunkNow}, so the query never loads or generates a chunk: it costs
 * one map lookup when no rows ship, and a bounded sweep of already-loaded
 * chunks otherwise. Every structure chunk within the radius is inside the
 * square by construction, so the sweep is complete for the rows' reach.</p>
 *
 * <p>Because membership comes from piece references rather than the start, the
 * elevation hugs the structure's whole footprint: the chunks surrounding a
 * dragon cave are elevated wherever its pieces run, and the sphere anchors on
 * the piece chunk at the queried Y so depth never splits a value.</p>
 */
public final class StructureElevationSource {

    private StructureElevationSource() {
    }

    /** The elevation modifiers active at the spawn position. */
    public static List<ConcentrationModifier> nearby(ServerLevel level, BlockPos position) {
        Map<String, List<StructureElevationRow>> rows = StructureElevation.all();
        if (rows.isEmpty()) {
            return List.of();
        }
        double maxRadius = StructureElevation.maxRadius();
        int chunkRadius = (int) Math.ceil(maxRadius / 16.0) + 1;
        int centerChunkX = position.getX() >> 4;
        int centerChunkZ = position.getZ() >> 4;

        // Structures are a datapack (dynamic) registry: resolve through the
        // level's registry access, not the static BuiltInRegistries.
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<String, List<StructureElevationLogic.Start>> starts = new HashMap<>();
        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                ChunkAccess chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue; // unloaded neighbourhood: no elevation, never a load
                }
                for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry
                        : chunk.getAllReferences().entrySet()) {
                    String id = String.valueOf(structures.getKey(entry.getKey()));
                    if (!rows.containsKey(id)) {
                        continue;
                    }
                    List<StructureElevationLogic.Start> list =
                            starts.computeIfAbsent(id, key -> new ArrayList<>());
                    entry.getValue().forEach(packed -> {
                        ChunkPos piece = new ChunkPos(packed);
                        double dx = (double) (piece.getMinBlockX() + 8) - position.getX();
                        double dz = (double) (piece.getMinBlockZ() + 8) - position.getZ();
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        if (distance <= maxRadius) {
                            list.add(new StructureElevationLogic.Start(
                                    piece.getMinBlockX() + 8, piece.getMinBlockZ() + 8,
                                    position.getY(), distance));
                        }
                    });
                }
            }
        }
        return StructureElevationLogic.modifiersFor(rows, starts);
    }
}
