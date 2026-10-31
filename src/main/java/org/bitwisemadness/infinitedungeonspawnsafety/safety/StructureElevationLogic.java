package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The structure-elevation rules as pure logic: how much magicule a chunk
 * should carry because structures have pieces near it.
 *
 * <p>Pack rule: a structure's surrounding chunks carry at least the magicule
 * baseline of the mobs related to that structure, so the rule's value for one
 * chunk is the largest elevation of any row whose radius covers it. The write
 * itself happens in the Minecraft boundary (see
 * {@code TensuraChunkMagiculeBridge}), which sets the chunk's stored
 * concentration to the higher of its current value and this floor - "equal or
 * higher", exactly as the rule reads.</p>
 *
 * <p>Pure by construction: strings, primitives and
 * {@link StructureElevationRow} records only.</p>
 */
public final class StructureElevationLogic {

    private StructureElevationLogic() {
    }

    /**
     * One structure piece near the queried chunk, as pure facts.
     *
     * @param x the piece chunk centre's world X
     * @param z the piece chunk centre's world Z
     */
    public record Piece(int x, int z) {
    }

    /**
     * The floor one chunk should carry: the largest elevation among rows whose
     * radius covers it from any known piece of their structure.
     *
     * @param rows        the loaded elevation rows, grouped by structure id
     * @param piecesByStructure known piece centres per structure id
     * @param chunkX      the queried chunk's centre X
     * @param chunkZ      the queried chunk's centre Z
     */
    public static OptionalDouble floorFor(Map<String, List<StructureElevationRow>> rows,
                                          Map<String, List<Piece>> piecesByStructure,
                                          double chunkX, double chunkZ) {
        if (rows == null || rows.isEmpty() || piecesByStructure == null || piecesByStructure.isEmpty()) {
            return OptionalDouble.empty();
        }
        double best = Double.NaN;
        for (Map.Entry<String, List<StructureElevationRow>> entry : rows.entrySet()) {
            List<Piece> pieces = piecesByStructure.get(entry.getKey());
            if (pieces == null) {
                continue;
            }
            for (StructureElevationRow row : entry.getValue()) {
                for (Piece piece : pieces) {
                    double dx = piece.x() - chunkX;
                    double dz = piece.z() - chunkZ;
                    if (dx * dx + dz * dz <= row.radius() * row.radius()
                            && (Double.isNaN(best) || row.elevation() > best)) {
                        best = row.elevation();
                    }
                }
            }
        }
        return Double.isNaN(best) ? OptionalDouble.empty() : OptionalDouble.of(best);
    }

    /**
     * The furthest radius any row reaches, or zero when no rows ship - the scan
     * bound for the Minecraft side, so an empty table costs one map lookup.
     */
    public static double maxRadius(Map<String, List<StructureElevationRow>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0.0;
        }
        double max = 0.0;
        for (List<StructureElevationRow> list : rows.values()) {
            for (StructureElevationRow row : list) {
                if (row.radius() > max) {
                    max = row.radius();
                }
            }
        }
        return max;
    }
}
