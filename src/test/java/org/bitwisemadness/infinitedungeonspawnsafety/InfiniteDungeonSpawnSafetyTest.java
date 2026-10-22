package org.bitwisemadness.infinitedungeonspawnsafety;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Placeholder JUnit suite. The CI {@code junit} check asserts a non-zero test
 * count so it cannot pass vacuously; the gate's pure math follows in its own
 * test classes.
 */
class InfiniteDungeonSpawnSafetyTest {

    @Test
    void modIdIsALowercasePathSegment() {
        assertTrue(InfiniteDungeonSpawnSafety.MOD_ID.matches("[a-z][a-z0-9_]{2,63}"),
                "mod id must be a lowercase resource location path segment");
    }
}
