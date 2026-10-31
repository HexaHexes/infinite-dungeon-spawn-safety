package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructureElevationLogicTest {

    private static Map<String, List<StructureElevationRow>> rows(String structure, double elevation) {
        return Map.of(structure, List.of(new StructureElevationRow(structure, elevation)));
    }

    private static Map<String, List<StructureElevationLogic.Piece>> pieces(String structure, int x, int z) {
        return Map.of(structure, List.of(new StructureElevationLogic.Piece(x, z)));
    }

    @Test
    void reachScalesWithTheBaseline() {
        // sqrt(baseline) * 0.35, clamped to [48, 320].
        assertEquals(49.497, StructureElevationLogic.radiusFor(20000.0), 0.01);
        assertEquals(207.062, StructureElevationLogic.radiusFor(350000.0), 0.01);
        assertEquals(313.049, StructureElevationLogic.radiusFor(800000.0), 0.01);
        assertEquals(320.0, StructureElevationLogic.radiusFor(2000000.0), 0.001, "huge baselines cap");
        assertEquals(48.0, StructureElevationLogic.radiusFor(1000.0), 0.001, "tiny baselines stay local");
    }

    @Test
    void valuesRadiateOutwards() {
        double baseline = 120000.0;
        double radius = StructureElevationLogic.radiusFor(baseline);

        double centre = StructureElevationLogic.valueAt(baseline, 0.0, radius);
        double halfway = StructureElevationLogic.valueAt(baseline, radius / 2.0, radius);
        double edge = StructureElevationLogic.valueAt(baseline, radius, radius);

        assertEquals(baseline * 1.5, centre, 0.001, "the centre sits above the baseline");
        assertEquals(baseline * 1.25, halfway, 0.001);
        assertEquals(baseline, edge, 0.001, "the edge sits exactly on the baseline, per the rule");
        assertTrue(centre > halfway && halfway > edge);
    }

    @Test
    void variationIsDeterministicAndUpwardOnly() {
        double a = StructureElevationLogic.variationFor("iceandfire:hydra_cave", 10, 20);
        double b = StructureElevationLogic.variationFor("iceandfire:hydra_cave", 10, 20);
        double c = StructureElevationLogic.variationFor("iceandfire:hydra_cave", 11, 20);

        assertEquals(a, b, "same inputs, same factor");
        assertTrue(a >= 1.0 && a <= 1.12, "never below one, so the baseline floor holds");
        assertTrue(a != c, "neighbouring chunks differ");
    }

    @Test
    void aChunkInsideTheReachReceivesARadiatedValue() {
        Map<String, List<StructureElevationRow>> rows = rows("tensura:catacombs", 120000.0);
        Map<String, List<StructureElevationLogic.Piece>> pieces = pieces("tensura:catacombs", 0, 0);

        // The 120k baseline reaches ~121 blocks, so 96 blocks is still inside.
        double near = StructureElevationLogic.contributionFor(rows, pieces, 0, 0).getAsDouble();
        double far = StructureElevationLogic.contributionFor(rows, pieces, 96, 0).getAsDouble();

        assertTrue(near > far, "closer chunks read higher");
        assertTrue(far >= 120000.0, "the value never dips below the baseline inside the reach");
    }

    @Test
    void aChunkBeyondTheReachReceivesNothing() {
        Map<String, List<StructureElevationRow>> rows = rows("tensura:catacombs", 120000.0);
        Map<String, List<StructureElevationLogic.Piece>> pieces = pieces("tensura:catacombs", 0, 0);

        assertTrue(StructureElevationLogic.contributionFor(rows, pieces, 1000, 0).isEmpty());
    }

    @Test
    void structuresWithoutPiecesContributeNothing() {
        Map<String, List<StructureElevationRow>> rows = rows("irons_spellbooks:pyromancer_tower", 12000.0);

        assertTrue(StructureElevationLogic.contributionFor(rows, Map.of(), 0, 0).isEmpty());
    }

    @Test
    void theStrongestReachingStructureWins() {
        Map<String, List<StructureElevationRow>> rows = Map.of(
                "a:tower", List.of(new StructureElevationRow("a:tower", 12000.0)),
                "b:cave", List.of(new StructureElevationRow("b:cave", 800000.0)));
        Map<String, List<StructureElevationLogic.Piece>> pieces = Map.of(
                "a:tower", List.of(new StructureElevationLogic.Piece(0, 0)),
                "b:cave", List.of(new StructureElevationLogic.Piece(32, 0)));

        assertTrue(StructureElevationLogic.contributionFor(rows, pieces, 16, 0).getAsDouble()
                > 800000.0);
    }

    @Test
    void maxRadiusTracksTheStrongestBaseline() {
        Map<String, List<StructureElevationRow>> rows = Map.of(
                "a:tower", List.of(new StructureElevationRow("a:tower", 1000.0)),
                "b:cave", List.of(new StructureElevationRow("b:cave", 800000.0)));

        assertEquals(313.0, StructureElevationLogic.maxRadius(rows), 0.5);
    }

    @Test
    void invalidRowsAreRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StructureElevationRow("", 1000.0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StructureElevationRow("a:b", -1.0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StructureElevationRow("a:b", Double.NaN));
    }
}
