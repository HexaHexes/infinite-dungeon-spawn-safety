package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructureElevationLogicTest {

    private static Map<String, List<StructureElevationRow>> rows(String structure, double radius, double elevation) {
        return Map.of(structure, List.of(new StructureElevationRow(structure, radius, elevation)));
    }

    @Test
    void emptyRowsYieldNothing() {
        assertTrue(StructureElevationLogic.floorFor(Map.of(), Map.of(), 0, 0).isEmpty());
        assertEquals(0.0, StructureElevationLogic.maxRadius(Map.of()));
    }

    @Test
    void aPieceWithinRadiusSetsTheFloor() {
        Map<String, List<StructureElevationRow>> rows =
                rows("iceandfire:fire_dragon_cave", 256.0, 800000.0);
        Map<String, List<StructureElevationLogic.Piece>> pieces = Map.of(
                "iceandfire:fire_dragon_cave", List.of(new StructureElevationLogic.Piece(16, 16)));

        assertEquals(800000.0,
                StructureElevationLogic.floorFor(rows, pieces, 16, 16).getAsDouble());
        // Boundary counts as inside, matching RadiusScope's contract.
        assertEquals(800000.0,
                StructureElevationLogic.floorFor(rows, pieces, 16 + 256, 16).getAsDouble());
    }

    @Test
    void aPieceBeyondTheRadiusSetsNothing() {
        Map<String, List<StructureElevationRow>> rows =
                rows("tensura:catacombs", 64.0, 120000.0);
        Map<String, List<StructureElevationLogic.Piece>> pieces = Map.of(
                "tensura:catacombs", List.of(new StructureElevationLogic.Piece(0, 0)));

        assertTrue(StructureElevationLogic.floorFor(rows, pieces, 65, 0).isEmpty());
    }

    @Test
    void structuresWithoutPiecesSetNothing() {
        Map<String, List<StructureElevationRow>> rows =
                rows("irons_spellbooks:pyromancer_tower", 128.0, 12000.0);

        assertTrue(StructureElevationLogic.floorFor(rows, Map.of(), 0, 0).isEmpty());
    }

    @Test
    void theLargestCoveringRowWins() {
        Map<String, List<StructureElevationRow>> rows = Map.of(
                "iceandfire:dragon_cave", List.of(
                        new StructureElevationRow("iceandfire:dragon_cave", 256.0, 800000.0),
                        new StructureElevationRow("iceandfire:dragon_cave", 96.0, 100000.0)));
        Map<String, List<StructureElevationLogic.Piece>> pieces = Map.of(
                "iceandfire:dragon_cave", List.of(new StructureElevationLogic.Piece(0, 0)));

        assertEquals(800000.0,
                StructureElevationLogic.floorFor(rows, pieces, 8, 0).getAsDouble());
    }

    @Test
    void maxRadiusTracksTheWidestRow() {
        Map<String, List<StructureElevationRow>> rows = Map.of(
                "a:tower", List.of(new StructureElevationRow("a:tower", 64.0, 1.0)),
                "b:cave", List.of(new StructureElevationRow("b:cave", 512.0, 2.0)));

        assertEquals(512.0, StructureElevationLogic.maxRadius(rows));
    }

    @Test
    void invalidRowsAreRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StructureElevationRow("", 10.0, 1.0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StructureElevationRow("a:b", -1.0, 1.0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StructureElevationRow("a:b", 10.0, Double.NaN));
    }
}
