package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.bitwisemadness.infinitedungeonapi.magicule.ConcentrationModifier;
import org.bitwisemadness.infinitedungeonapi.magicule.RadiusScope;
import org.junit.jupiter.api.Test;

class StructureElevationLogicTest {

    @Test
    void emptyRowsYieldNothing() {
        assertTrue(StructureElevationLogic.modifiersFor(Map.of(), Map.of()).isEmpty());
        assertEquals(0.0, StructureElevationLogic.maxRadius(Map.of()));
    }

    @Test
    void aStartWithinRadiusLifts() {
        Map<String, List<StructureElevationRow>> rows = Map.of(
                "iceandfire:fire_dragon_cave", List.of(new StructureElevationRow(
                        "iceandfire:fire_dragon_cave", 256.0, 800000.0)));
        Map<String, List<StructureElevationLogic.Start>> starts = Map.of(
                "iceandfire:fire_dragon_cave",
                List.of(new StructureElevationLogic.Start(16, 16, 64, 100.0)));

        List<ConcentrationModifier> modifiers = StructureElevationLogic.modifiersFor(rows, starts);

        assertEquals(1, modifiers.size());
        ConcentrationModifier modifier = modifiers.get(0);
        assertEquals(800000.0, modifier.amount());
        assertTrue(!modifier.multiplicative());
        RadiusScope scope = modifier.scope();
        assertEquals(16, scope.x());
        assertEquals(16, scope.z());
        assertEquals(64, scope.y());
        assertEquals(256.0, scope.radius());
        assertTrue(scope.contains(16, 64, 16), "the centre is inside");
        assertTrue(scope.contains(16 + 256, 64, 16), "the boundary counts as inside");
    }

    @Test
    void aStartBeyondTheRadiusDoesNotLift() {
        Map<String, List<StructureElevationRow>> rows = Map.of(
                "tensura:catacombs", List.of(new StructureElevationRow(
                        "tensura:catacombs", 64.0, 120000.0)));
        Map<String, List<StructureElevationLogic.Start>> starts = Map.of(
                "tensura:catacombs",
                List.of(new StructureElevationLogic.Start(0, 0, 0, 65.0)));

        assertTrue(StructureElevationLogic.modifiersFor(rows, starts).isEmpty());
    }

    @Test
    void aStructureWithoutStartsContributesNothing() {
        Map<String, List<StructureElevationRow>> rows = Map.of(
                "irons_spellbooks:pyromancer_tower", List.of(new StructureElevationRow(
                        "irons_spellbooks:pyromancer_tower", 128.0, 12000.0)));

        assertTrue(StructureElevationLogic.modifiersFor(rows, Map.of()).isEmpty());
    }

    @Test
    void multipleRowsForOneStructureAllApply() {
        Map<String, List<StructureElevationRow>> rows = Map.of(
                "iceandfire:dragon_cave", List.of(
                        new StructureElevationRow("iceandfire:dragon_cave", 256.0, 800000.0),
                        new StructureElevationRow("iceandfire:dragon_cave", 96.0, 100000.0)));
        Map<String, List<StructureElevationLogic.Start>> starts = Map.of(
                "iceandfire:dragon_cave",
                List.of(new StructureElevationLogic.Start(0, 0, 0, 8.0)));

        List<ConcentrationModifier> modifiers = StructureElevationLogic.modifiersFor(rows, starts);

        assertEquals(2, modifiers.size());
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
