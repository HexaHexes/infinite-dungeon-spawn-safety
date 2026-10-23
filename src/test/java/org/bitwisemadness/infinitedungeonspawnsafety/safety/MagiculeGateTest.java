package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class MagiculeGateTest {

    private static final String NEUTRAL = "NEUTRAL";
    private static final String HOSTILE = "HOSTILE";

    private static SafetyConfig config(boolean enabled, boolean gateEnabled, double deviation) {
        return new SafetyConfig(enabled, false, true, true,
                DimensionMode.WHITELIST, Set.of("minecraft:overworld"), gateEnabled, "ep", deviation, 1.0, 0.0);
    }

    @Test
    void switchedOffConfigAllowsEverything() {
        GateDecision decision = MagiculeGate.evaluate(config(false, true, 10.0), HOSTILE, true, 0.0, 1000.0);

        assertTrue(decision.allowed());
        assertEquals(GateReason.NOT_APPLICABLE, decision.reason());
    }

    @Test
    void nonNaturalSpawnsAreNeverGated() {
        GateDecision decision = MagiculeGate.evaluate(config(true, true, 10.0), HOSTILE, false, 0.0, 1000.0);

        assertTrue(decision.allowed());
        assertEquals(GateReason.NOT_NATURAL, decision.reason());
    }

    @Test
    void disabledCategoryBlocksBeforeTheGateRuns() {
        GateDecision decision = MagiculeGate.evaluate(config(true, true, 10.0), HOSTILE, true, 99999.0, 1.0);

        assertFalse(decision.allowed());
        assertEquals(GateReason.CATEGORY_DISABLED, decision.reason());
    }

    @Test
    void disabledGateAllowsAnAllowedCategory() {
        GateDecision decision = MagiculeGate.evaluate(config(true, false, 10.0), NEUTRAL, true, 0.0, 1000.0);

        assertTrue(decision.allowed());
        assertEquals(GateReason.GATE_DISABLED, decision.reason());
    }

    @Test
    void mobsWithoutAStatHaveNoRequirement() {
        for (double stat : new double[] {0.0, -5.0}) {
            GateDecision decision = MagiculeGate.evaluate(config(true, true, 10.0), NEUTRAL, true, 0.0, stat);
            assertTrue(decision.allowed());
            assertEquals(GateReason.NO_REQUIREMENT, decision.reason());
        }
    }

    @Test
    void sustainedWhenAmbientReachesTheRequirement() {
        // 1400 * 1.0 * 0.9 = 1260.
        GateDecision exact = MagiculeGate.evaluate(config(true, true, 10.0), NEUTRAL, true, 1260.0, 1400.0);
        assertTrue(exact.allowed());
        assertEquals(GateReason.SUSTAINED, exact.reason());

        GateDecision plenty = MagiculeGate.evaluate(config(true, true, 10.0), NEUTRAL, true, 5000.0, 1400.0);
        assertTrue(plenty.allowed());
        assertEquals(GateReason.SUSTAINED, plenty.reason());
    }

    @Test
    void blockedWhenAmbientIsBelowTheRequirement() {
        GateDecision decision = MagiculeGate.evaluate(config(true, true, 10.0), NEUTRAL, true, 1259.0, 1400.0);

        assertFalse(decision.allowed());
        assertEquals(GateReason.INSUFFICIENT_MAGICULE, decision.reason());
    }

    @Test
    void theDeviationBandLoosensTheRequirement() {
        assertEquals(1400.0, MagiculeGate.requiredMagicule(config(true, true, 0.0), 1400.0), 1.0e-9);
        assertEquals(1260.0, MagiculeGate.requiredMagicule(config(true, true, 10.0), 1400.0), 1.0e-9);
        assertEquals(700.0, MagiculeGate.requiredMagicule(config(true, true, 50.0), 1400.0), 1.0e-9);
    }

    @Test
    void attributeScaleAlignsTheUnits() {
        SafetyConfig scaled = new SafetyConfig(true, false, true, true,
                DimensionMode.WHITELIST, Set.of("minecraft:overworld"), true, "ep", 0.0, 2.0, 0.0);

        assertEquals(2800.0, MagiculeGate.requiredMagicule(scaled, 1400.0), 1.0e-9);
        assertFalse(MagiculeGate.evaluate(scaled, NEUTRAL, true, 1400.0, 1400.0).allowed());
    }

    @Test
    void negativeAmbientIsTreatedAsEmpty() {
        GateDecision decision = MagiculeGate.evaluate(config(true, true, 10.0), NEUTRAL, true, -50.0, 10.0);

        assertFalse(decision.allowed());
        assertEquals(GateReason.INSUFFICIENT_MAGICULE, decision.reason());
    }

    @Test
    void nullCategoryIsNeverCategoryBlocked() {
        GateDecision decision = MagiculeGate.evaluate(config(true, true, 10.0), null, true, 100.0, 10.0);

        assertTrue(decision.allowed());
        assertEquals(GateReason.SUSTAINED, decision.reason());
    }
}
