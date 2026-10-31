package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.OptionalDouble;
import org.bitwisemadness.infinitedungeonapi.magicule.ConcentrationModifier;
import org.junit.jupiter.api.Test;

/**
 * The ambient boundary policy: a provider answer takes the modifiers, and the
 * fallback stands in only when no provider answers.
 */
class AmbientReadTest {

    @Test
    void fallbackAnswersWhenNoProviderDoes() {
        assertEquals(42.0, AmbientRead.resolve(OptionalDouble.empty(), List.of(), 42.0));
    }

    @Test
    void fallbackNeverPushesAmbientNegative() {
        assertEquals(0.0, AmbientRead.resolve(OptionalDouble.empty(), List.of(), -5.0));
    }

    @Test
    void fallbackIsNotShapedByModifiers() {
        List<ConcentrationModifier> modifiers = List.of(
                ConcentrationModifier.additive(100.0),
                ConcentrationModifier.multiplicative(10.0));
        assertEquals(3.0, AmbientRead.resolve(OptionalDouble.empty(), modifiers, 3.0));
    }

    @Test
    void providerAnswerTakesAdditiveThenMultiplicativeModifiers() {
        List<ConcentrationModifier> modifiers = List.of(
                ConcentrationModifier.additive(5.0),
                ConcentrationModifier.multiplicative(2.0));
        assertEquals(30.0, AmbientRead.resolve(OptionalDouble.of(10.0), modifiers, 0.0));
    }

    @Test
    void providerAnswerWithoutModifiersPassesThrough() {
        assertEquals(10.0, AmbientRead.resolve(OptionalDouble.of(10.0), null, 0.0));
    }
}
