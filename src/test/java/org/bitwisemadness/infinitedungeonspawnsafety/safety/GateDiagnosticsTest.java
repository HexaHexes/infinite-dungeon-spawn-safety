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
                        + " category=PASSIVE reason=INSUFFICIENT_MAGICULE"
                        + " ambient=0.00 source=FALLBACK modifiers=0 required=90.00 stat=100.00",
                GateDiagnostics.blockMessage(GateReason.INSUFFICIENT_MAGICULE,
                        "minecraft:sheep", "minecraft:overworld", "PASSIVE",
                        new AmbientRead.Reading(0.0, AmbientRead.Source.FALLBACK, 0),
                        90.0, 100.0));
    }

    @Test
    void magiculeMessageNamesAProviderAnswer() {
        String message = GateDiagnostics.blockMessage(GateReason.INSUFFICIENT_MAGICULE,
                "minecraft:zombie", "minecraft:overworld", "HOSTILE",
                new AmbientRead.Reading(12.5, AmbientRead.Source.PROVIDER, 2),
                90.0, 100.0);
        assertTrue(message.contains("ambient=12.50"), message);
        assertTrue(message.contains("source=PROVIDER"), message);
        assertTrue(message.contains("modifiers=2"), message);
    }

    @Test
    void evaluationMessageCarriesTheSameFactsAsABlock() {
        assertEquals(
                "Allowed natural spawn: entity=minecraft:zombie dimension=minecraft:overworld"
                        + " category=HOSTILE reason=SUSTAINED"
                        + " ambient=150.00 source=PROVIDER modifiers=1 required=90.00 stat=100.00",
                GateDiagnostics.evaluationMessage(GateReason.SUSTAINED,
                        "minecraft:zombie", "minecraft:overworld", "HOSTILE",
                        new AmbientRead.Reading(150.0, AmbientRead.Source.PROVIDER, 1),
                        90.0, 100.0));
    }

    @Test
    void ignoredMessageNamesTheSwitch() {
        assertEquals(
                "Spawn not evaluated: dimension=minecraft:the_nether spawnType=MONSTER"
                        + " why=dimension-not-affected",
                GateDiagnostics.ignoredMessage(
                        "minecraft:the_nether", "MONSTER", "dimension-not-affected"));
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
