package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SafetyConfigTest {

    @Test
    void defaultsMatchTheDocumentedSurface() {
        SafetyConfig config = SafetyConfig.defaults();

        assertTrue(config.enabled());
        assertFalse(config.hostileAllowed());
        assertTrue(config.neutralAllowed());
        assertTrue(config.passiveAllowed());
        assertEquals(DimensionMode.WHITELIST, config.dimensionMode());
        assertEquals(Set.of("minecraft:overworld"), config.dimensions());
        assertTrue(config.gateEnabled());
        assertEquals("ep", config.gateStat());
        assertEquals(10.0, config.deviationPercent());
        assertEquals(1.0, config.attributeScale());
        assertEquals(0.0, config.ambientFallback());
    }

    @Test
    void gateStatIsNormalisedToTheSupportedNames() {
        assertEquals("ap", withStat("AP").gateStat());
        assertEquals("mp", withStat(" mp ").gateStat());
        assertEquals("ep", withStat("EP").gateStat());
        assertEquals("ep", withStat("bogus").gateStat());
        assertEquals("ep", withStat(null).gateStat());
    }

    @Test
    void negativeNumbersAreClamped() {
        SafetyConfig config = new SafetyConfig(true, false, true, true,
                DimensionMode.WHITELIST, Set.of("minecraft:overworld"), true, "ep", -5.0, 1.0, -3.0);

        assertEquals(0.0, config.deviationPercent());
        assertEquals(0.0, config.ambientFallback());
    }

    @Test
    void dimensionsAreCopiedAndImmutable() {
        Set<String> mutable = new HashSet<>();
        mutable.add("minecraft:overworld");
        SafetyConfig config = new SafetyConfig(true, false, true, true,
                DimensionMode.WHITELIST, mutable, true, "ep", 10.0, 1.0, 0.0);

        mutable.add("minecraft:the_nether");

        assertEquals(Set.of("minecraft:overworld"), config.dimensions());
        assertThrows(UnsupportedOperationException.class, () -> config.dimensions().add("x:y"));
    }

    @Test
    void nullDimensionModeFallsBackToWhitelist() {
        SafetyConfig config = new SafetyConfig(true, false, true, true,
                null, Set.of("minecraft:overworld"), true, "ep", 10.0, 1.0, 0.0);

        assertEquals(DimensionMode.WHITELIST, config.dimensionMode());
    }

    @Test
    void categoryTogglesAreQueriedByName() {
        SafetyConfig config = SafetyConfig.defaults();

        assertFalse(config.categoryAllowed("HOSTILE"));
        assertTrue(config.categoryAllowed("NEUTRAL"));
        assertTrue(config.categoryAllowed("PASSIVE"));
        assertTrue(config.categoryAllowed("SOMETHING_NEW"));
        assertTrue(config.categoryAllowed(null));
    }

    private static SafetyConfig withStat(String stat) {
        return new SafetyConfig(true, false, true, true,
                DimensionMode.WHITELIST, Set.of("minecraft:overworld"), true, stat, 10.0, 1.0, 0.0);
    }
}
