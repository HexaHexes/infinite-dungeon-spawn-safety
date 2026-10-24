package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;
import org.bitwisemadness.infinitedungeonapi.magicule.ConcentrationModifier;

/**
 * How one spawn reads the ambient magicule, as pure logic.
 *
 * <p>The boundary policy is exact: modifiers shape what a provider answered,
 * and the configured fallback stands in only when no provider answers at all.
 * A fallback is a constant stand-in, not a provider value, so modifiers never
 * touch it; the plan states the fallback as "the ambient value when no
 * provider answers", and this class keeps that literal. A negative fallback
 * or base clamps at zero, matching the gate's own non-negative view of
 * ambient.</p>
 */
public final class AmbientRead {

    private AmbientRead() {
    }

    /**
     * Resolves one ambient reading.
     *
     * @param base      the first provider's answer, or empty when none answered
     * @param modifiers the registered modifiers, applied only to a provider answer
     * @param fallback  the configured stand-in when no provider answered
     */
    public static double resolve(OptionalDouble base,
                                 Collection<ConcentrationModifier> modifiers,
                                 double fallback) {
        if (base == null || base.isEmpty()) {
            return Math.max(0.0, fallback);
        }
        return ConcentrationModifier.applyAll(
                base.getAsDouble(), modifiers == null ? List.of() : modifiers);
    }
}
