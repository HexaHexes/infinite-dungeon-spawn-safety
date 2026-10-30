package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The loaded structure-elevation rows, replaced wholesale on every datapack
 * reload.
 *
 * <p>Mirrors {@link SafetyTables}: one volatile immutable snapshot so the
 * spawn path never reads a half-applied load. Multiple namespaces' files
 * accumulate - each pack contributes its own structures rather than replacing
 * another's.</p>
 */
public final class StructureElevation {

    private static volatile Map<String, List<StructureElevationRow>> rows = Map.of();
    private static volatile double maxRadius;

    private StructureElevation() {
    }

    /** Replaces the table; rows are grouped by structure id. */
    public static void set(Map<String, List<StructureElevationRow>> entries) {
        Map<String, List<StructureElevationRow>> copy = new TreeMap<>();
        if (entries != null) {
            entries.forEach((structure, list) ->
                    copy.put(structure, List.copyOf(list)));
        }
        rows = Collections.unmodifiableMap(copy);
        maxRadius = StructureElevationLogic.maxRadius(copy);
    }

    /** The rows grouped by structure id; empty when no pack shipped any. */
    public static Map<String, List<StructureElevationRow>> all() {
        return rows;
    }

    /** The furthest radius any row reaches; zero when none ship. */
    public static double maxRadius() {
        return maxRadius;
    }

    /** Clears the table; used on reload teardown and in tests. */
    public static void clear() {
        rows = Map.of();
        maxRadius = 0.0;
    }
}
