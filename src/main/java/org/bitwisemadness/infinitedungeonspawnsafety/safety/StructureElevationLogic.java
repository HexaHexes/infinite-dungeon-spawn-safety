package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The structure-elevation rules as pure logic: how far a structure reaches and
 * how much magicule each affected chunk carries.
 *
 * <p>The pack rule is the floor: a structure's surrounding chunks carry at
 * least the EP baseline of the mobs related to it. On top of that floor the
 * values radiate - chunks closest to the structure sit above the baseline and
 * the edge of the reach sits exactly on it - with a small deterministic
 * variation per chunk so the field is organic rather than a perfect circle.
 * The reach itself scales with the baseline: concentrated structures reach
 * further.</p>
 *
 * <ul>
 *   <li><b>Reach</b>: {@code radius = clamp(sqrt(baseline) * 0.35, 48, 320)}
 *       blocks - 20,000 reaches 49 blocks, 350,000 reaches 207, 800,000 caps
 *       at 320.</li>
 *   <li><b>Radiate</b>: {@code value = baseline * (1 + 0.5 * (1 - d / r))} -
 *       the nearest chunk reads 1.5x the baseline, the edge reads exactly the
 *       baseline.</li>
 *   <li><b>Variation</b>: a deterministic per-chunk factor of plus or minus
 *       12 percent, hashed from the structure id and the chunk position, so
 *       worlds are reproducible while the field stays irregular.</li>
 * </ul>
 *
 * <p>Pure by construction: strings, primitives and {@link StructureElevationRow}
 * records only.</p>
 */
public final class StructureElevationLogic {

    /** Radius scaling: blocks of reach per sqrt of baseline. */
    private static final double RADIUS_PER_SQRT = 0.35;

    /** Reach bounds in blocks: small structures still touch their neighbours, huge ones stay local. */
    private static final double MIN_RADIUS = 48.0;

    private static final double MAX_RADIUS = 320.0;

    /** How much the nearest chunk exceeds the baseline (1.0 would be flat). */
    private static final double FALLOFF_BOOST = 0.5;

    /**
     * Peak per-chunk variation, as a fraction of the falling value. Applied
     * upward only - a downward factor at the rim would dip below the baseline
     * the rule guarantees.
     */
    private static final double VARIATION = 0.12;

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

    /** The reach a baseline earns, in blocks. */
    public static double radiusFor(double baseline) {
        if (!(baseline > 0.0)) {
            return MIN_RADIUS;
        }
        double scaled = Math.sqrt(baseline) * RADIUS_PER_SQRT;
        return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, scaled));
    }

    /** The value at {@code distance} blocks from the structure's nearest piece. */
    public static double valueAt(double baseline, double distance, double radius) {
        double t = radius <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, distance / radius));
        return baseline * (1.0 + FALLOFF_BOOST * (1.0 - t));
    }

    /** The deterministic per-chunk variation factor, in {@code [1, 1 + VARIATION]}. */
    public static double variationFor(String structure, int chunkX, int chunkZ) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < structure.length(); i++) {
            h = (h ^ structure.charAt(i)) * 0x100000001b3L;
        }
        h = (h ^ chunkX) * 0x100000001b3L;
        h = (h ^ chunkZ) * 0x100000001b3L;
        // splitmix64 finaliser, then map to [0, 1).
        long z = h + 0x9e3779b97f4a7c15L;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        z = z ^ (z >>> 31);
        double unit = (z >>> 11) * 0x1.0p-53;
        return 1.0 + VARIATION * unit;
    }

    /**
     * The contribution one chunk should carry: the highest radiating value any
     * known structure piece reaches it with, or empty when nothing reaches.
     *
     * @param rows               the loaded elevation rows, grouped by structure id
     * @param piecesByStructure  known piece centres per structure id
     * @param chunkX             the queried chunk's centre X
     * @param chunkZ             the queried chunk's centre Z
     */
    public static OptionalDouble contributionFor(Map<String, List<StructureElevationRow>> rows,
                                                 Map<String, List<Piece>> piecesByStructure,
                                                 int chunkX, int chunkZ) {
        if (rows == null || rows.isEmpty() || piecesByStructure == null || piecesByStructure.isEmpty()) {
            return OptionalDouble.empty();
        }
        double best = Double.NaN;
        for (Map.Entry<String, List<StructureElevationRow>> entry : rows.entrySet()) {
            List<Piece> pieces = piecesByStructure.get(entry.getKey());
            if (pieces == null) {
                continue;
            }
            double nearest = Double.MAX_VALUE;
            for (Piece piece : pieces) {
                double dx = piece.x() - chunkX;
                double dz = piece.z() - chunkZ;
                nearest = Math.min(nearest, Math.sqrt(dx * dx + dz * dz));
            }
            for (StructureElevationRow row : entry.getValue()) {
                double radius = radiusFor(row.elevation());
                if (nearest > radius) {
                    continue;
                }
                double value = valueAt(row.elevation(), nearest, radius)
                        * variationFor(entry.getKey(), chunkX, chunkZ);
                if (Double.isNaN(best) || value > best) {
                    best = value;
                }
            }
        }
        return Double.isNaN(best) ? OptionalDouble.empty() : OptionalDouble.of(best);
    }

    /** The furthest reach any row earns, or zero when no rows ship. */
    public static double maxRadius(Map<String, List<StructureElevationRow>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0.0;
        }
        double max = 0.0;
        for (List<StructureElevationRow> list : rows.values()) {
            for (StructureElevationRow row : list) {
                max = Math.max(max, radiusFor(row.elevation()));
            }
        }
        return max;
    }
}
