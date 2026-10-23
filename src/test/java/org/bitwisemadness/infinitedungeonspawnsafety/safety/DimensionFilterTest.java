package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DimensionFilterTest {

    private static SafetyConfig config(DimensionMode mode, Set<String> dimensions, boolean enabled) {
        return new SafetyConfig(enabled, false, true, true, mode, dimensions, true, "ep", 10.0, 1.0, 0.0);
    }

    @Test
    void whitelistAffectsOnlyListedDimensions() {
        SafetyConfig whitelist = config(DimensionMode.WHITELIST, Set.of("minecraft:overworld"), true);

        assertTrue(DimensionFilter.affects("minecraft:overworld", whitelist));
        assertFalse(DimensionFilter.affects("minecraft:the_nether", whitelist));
        assertFalse(DimensionFilter.affects("infinite_dungeon:infinite_dungeon", whitelist));
    }

    @Test
    void blacklistAffectsEverythingExceptListedDimensions() {
        SafetyConfig blacklist = config(DimensionMode.BLACKLIST,
                Set.of("infinite_dungeon:infinite_dungeon"), true);

        assertTrue(DimensionFilter.affects("minecraft:overworld", blacklist));
        assertTrue(DimensionFilter.affects("minecraft:the_nether", blacklist));
        assertFalse(DimensionFilter.affects("infinite_dungeon:infinite_dungeon", blacklist));
    }

    @Test
    void theDefaultConfigurationExemptsDungeonDimensions() {
        SafetyConfig defaults = SafetyConfig.defaults();

        assertTrue(DimensionFilter.affects("minecraft:overworld", defaults));
        assertFalse(DimensionFilter.affects("infinite_dungeon:infinite_dungeon", defaults));
    }

    @Test
    void switchedOffMeansNoDimensionIsAffected() {
        SafetyConfig off = config(DimensionMode.BLACKLIST, Set.of(), false);

        assertFalse(DimensionFilter.affects("minecraft:overworld", off));
    }

    @Test
    void nullInputsAreSafe() {
        assertFalse(DimensionFilter.affects(null, SafetyConfig.defaults()));
        assertFalse(DimensionFilter.affects("minecraft:overworld", null));
    }
}
