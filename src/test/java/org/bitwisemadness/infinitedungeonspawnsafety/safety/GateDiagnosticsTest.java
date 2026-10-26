package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The block message wording: every fact present, missing ones tolerated. */
class GateDiagnosticsTest {

    @Test
    void categoryMessageNamesEveryFact() {
        String message = GateDiagnostics.blockMessage(
                GateReason.CATEGORY_DISABLED, "minecraft:zombie", "minecraft:overworld", "HOSTILE");
        assertTrue(message.contains("minecraft:zombie"), message);
        assertTrue(message.contains("minecraft:overworld"), message);
        assertTrue(message.contains("HOSTILE"), message);
        assertTrue(message.contains(GateReason.CATEGORY_DISABLED.name()), message);
    }

    @Test
    void magiculeMessageCarriesTheNumbers() {
        assertEquals(
                "Blocked natural spawn: entity=minecraft:sheep dimension=minecraft:overworld"
                        + " category=PASSIVE reason=INSUFFICIENT_MAGICULE ambient=0.00 required=90.00 stat=100.00",
                GateDiagnostics.blockMessage(GateReason.INSUFFICIENT_MAGICULE,
                        "minecraft:sheep", "minecraft:overworld", "PASSIVE", 0.0, 90.0, 100.0));
    }

    @Test
    void missingFactsReadAsUnknown() {
        String message = GateDiagnostics.blockMessage(
                GateReason.BLACKLISTED, null, "  ", null);
        assertTrue(message.contains("entity=unknown"), message);
        assertTrue(message.contains("dimension=unknown"), message);
        assertTrue(message.contains("category=unknown"), message);
    }
}
