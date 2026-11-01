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
 *
 * <p>The reading carries its {@link Source} so diagnostics can name where the
 * value came from - the difference between "the world's provider answered"
 * and "a constant stood in" is exactly what an operator hunting the gate needs
 * to see in the log (issue #65).</p>
 */
public final class AmbientRead {

    /** Where an ambient value came from. */
    public enum Source {

        /** A registered provider answered the position. */
        PROVIDER,

        /** No provider answered; the configured constant stood in. */
        FALLBACK
    }

    /**
     * One resolved ambient reading: the value the gate compares, its origin,
     * and how many concentration modifiers shaped a provider answer (always
     * zero for the fallback - modifiers never touch the constant).
     */
    public record Reading(double value, Source source, int modifierCount) {
    }

    private AmbientRead() {
    }

    /**
     * Resolves one ambient reading.
     *
     * @param base      the first provider's answer, or empty when none answered
     * @param modifiers the registered modifiers, applied only to a provider answer
     * @param fallback  the configured stand-in when no provider answered
     */
    public static Reading resolve(OptionalDouble base,
                                  Collection<ConcentrationModifier> modifiers,
                                  double fallback) {
        if (base == null || base.isEmpty()) {
            return new Reading(Math.max(0.0, fallback), Source.FALLBACK, 0);
        }
        Collection<ConcentrationModifier> safe =
                modifiers == null ? List.of() : modifiers;
        return new Reading(
                ConcentrationModifier.applyAll(base.getAsDouble(), safe),
                Source.PROVIDER,
                safe.size());
    }
}
