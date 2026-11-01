package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.OptionalDouble;
import org.bitwisemadness.infinitedungeonapi.magicule.ConcentrationModifier;
import org.junit.jupiter.api.Test;

/**
 * The ambient boundary policy: a provider answer takes the modifiers, the
 * fallback stands in only when no provider answers, and the reading always
 * says which of the two it was (issue #65's source attribution).
 */
class AmbientReadTest {

    @Test
    void fallbackAnswersWhenNoProviderDoes() {
        AmbientRead.Reading reading = AmbientRead.resolve(OptionalDouble.empty(), List.of(), 42.0);
        assertEquals(42.0, reading.value());
        assertEquals(AmbientRead.Source.FALLBACK, reading.source());
        assertEquals(0, reading.modifierCount());
    }

    @Test
    void fallbackNeverPushesAmbientNegative() {
        assertEquals(0.0, AmbientRead.resolve(OptionalDouble.empty(), List.of(), -5.0).value());
    }

    @Test
    void fallbackIsNotShapedByModifiers() {
        List<ConcentrationModifier> modifiers = List.of(
                ConcentrationModifier.additive(100.0),
                ConcentrationModifier.multiplicative(10.0));
        AmbientRead.Reading reading = AmbientRead.resolve(OptionalDouble.empty(), modifiers, 3.0);
        assertEquals(3.0, reading.value());
        // Modifiers shape provider answers only - the fallback never counts any.
        assertEquals(0, reading.modifierCount());
        assertEquals(AmbientRead.Source.FALLBACK, reading.source());
    }

    @Test
    void providerAnswerTakesAdditiveThenMultiplicativeModifiers() {
        List<ConcentrationModifier> modifiers = List.of(
                ConcentrationModifier.additive(5.0),
                ConcentrationModifier.multiplicative(2.0));
        AmbientRead.Reading reading =
                AmbientRead.resolve(OptionalDouble.of(10.0), modifiers, 0.0);
        assertEquals(30.0, reading.value());
        assertEquals(AmbientRead.Source.PROVIDER, reading.source());
        assertEquals(2, reading.modifierCount());
    }

    @Test
    void providerAnswerWithoutModifiersPassesThrough() {
        AmbientRead.Reading reading = AmbientRead.resolve(OptionalDouble.of(10.0), null, 0.0);
        assertEquals(10.0, reading.value());
        assertEquals(AmbientRead.Source.PROVIDER, reading.source());
        assertEquals(0, reading.modifierCount());
    }
}
