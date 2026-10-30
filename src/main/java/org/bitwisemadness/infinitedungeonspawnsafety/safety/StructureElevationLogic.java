package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bitwisemadness.infinitedungeonapi.magicule.ConcentrationModifier;
import org.bitwisemadness.infinitedungeonapi.magicule.RadiusScope;

/**
 * The structure-elevation rules as pure logic.
 *
 * <p>Given the loaded rows and the structure starts found near a queried
 * position, this decides which API {@link ConcentrationModifier}s apply and
 * where: one additive, position-scoped modifier per (row, start) pair whose
 * start lies within the row's radius. The scope centres on the structure's
 * start chunk with the queried position's Y, so the model is deliberately
 * horizontal - "the chunks surrounding a structure" - and depth never excludes
 * a spawn at the same X/Z.</p>
 *
 * <p>Boundary included, matching {@link RadiusScope#contains}: a start exactly
 * radius blocks away still contributes.</p>
 */
public final class StructureElevationLogic {

    private StructureElevationLogic() {
    }

    /**
     * One structure start near the query, as pure facts.
     *
     * @param x        the start chunk centre's world X
     * @param z        the start chunk centre's world Z
     * @param y        the vertical anchor for the sphere (the queried Y)
     * @param distance the horizontal distance from the query to the start
     */
    public record Start(int x, int z, int y, double distance) {
    }

    /**
     * Builds the modifiers for one query.
     *
     * @param rows             the loaded elevation rows
     * @param startsByStructure nearby starts per structure id
     */
    public static List<ConcentrationModifier> modifiersFor(
            Map<String, List<StructureElevationRow>> rows,
            Map<String, List<Start>> startsByStructure) {
        if (rows == null || rows.isEmpty() || startsByStructure == null || startsByStructure.isEmpty()) {
            return List.of();
        }
        List<ConcentrationModifier> modifiers = new ArrayList<>();
        for (Map.Entry<String, List<StructureElevationRow>> entry : rows.entrySet()) {
            List<Start> starts = startsByStructure.get(entry.getKey());
            if (starts == null) {
                continue;
            }
            for (StructureElevationRow row : entry.getValue()) {
                for (Start start : starts) {
                    if (start.distance() <= row.radius()) {
                        modifiers.add(ConcentrationModifier.additive(row.elevation(),
                                RadiusScope.around(start.x(), start.y(), start.z(), row.radius())));
                    }
                }
            }
        }
        return List.copyOf(modifiers);
    }

    /**
     * The furthest radius any row reaches, or zero when no rows ship - the
     * scan bound for the MC side, so an empty table costs one map lookup.
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
