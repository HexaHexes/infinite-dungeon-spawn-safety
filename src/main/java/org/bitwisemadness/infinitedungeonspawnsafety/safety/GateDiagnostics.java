package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Locale;

/**
 * Why a spawn was allowed, blocked, or never evaluated, as pure text.
 *
 * <p>Kept free of Minecraft types so the wording is unit-testable. The handler
 * logs block messages and evaluated-spawn traces at debug level, which keeps
 * the default experience quiet until an operator asks for it deliberately;
 * the never-evaluated cases (spawn type, dimension filter, exemption) go to
 * trace, because those fire for attempts server-wide and must not turn a
 * debug session in one dimension into a firehose from every other (issue
 * #65).</p>
 */
public final class GateDiagnostics {

    private GateDiagnostics() {
    }

    /**
     * A block message for a rule that needs no magicule numbers, such as a
     * switched-off category or a datapack list match.
     */
    public static String blockMessage(GateReason reason,
                                      String entityId,
                                      String dimensionId,
                                      String categoryName) {
        return String.format(Locale.ROOT,
                "Blocked natural spawn: entity=%s dimension=%s category=%s reason=%s",
                orUnknown(entityId), orUnknown(dimensionId), orUnknown(categoryName), reason);
    }

    /**
     * A block message for the magicule gate, carrying the numbers the gate
     * compared and the ambient's origin, so the requirement can be tuned from
     * the log alone and a constant stand-in is never mistaken for a provider
     * answer.
     */
    public static String blockMessage(GateReason reason,
                                      String entityId,
                                      String dimensionId,
                                      String categoryName,
                                      AmbientRead.Reading ambient,
                                      double required,
                                      double stat) {
        return String.format(Locale.ROOT,
                "Blocked natural spawn: entity=%s dimension=%s category=%s reason=%s"
                        + " ambient=%.2f source=%s modifiers=%d required=%.2f stat=%.2f",
                orUnknown(entityId), orUnknown(dimensionId), orUnknown(categoryName), reason,
                ambient == null ? 0.0 : ambient.value(),
                ambient == null ? AmbientRead.Source.FALLBACK : ambient.source(),
                ambient == null ? 0 : ambient.modifierCount(),
                required, stat);
    }

    /**
     * The trace of an evaluated spawn the gate ALLOWED - the line that answers
     * "why did nothing block here", with the same facts a block carries
     * (issue #65: allowing paths used to log nothing at all).
     */
    public static String evaluationMessage(GateReason reason,
                                           String entityId,
                                           String dimensionId,
                                           String categoryName,
                                           AmbientRead.Reading ambient,
                                           double required,
                                           double stat) {
        return String.format(Locale.ROOT,
                "Allowed natural spawn: entity=%s dimension=%s category=%s reason=%s"
                        + " ambient=%.2f source=%s modifiers=%d required=%.2f stat=%.2f",
                orUnknown(entityId), orUnknown(dimensionId), orUnknown(categoryName), reason,
                ambient == null ? 0.0 : ambient.value(),
                ambient == null ? AmbientRead.Source.FALLBACK : ambient.source(),
                ambient == null ? 0 : ambient.modifierCount(),
                required, stat);
    }

    /**
     * The trace of a spawn the module never evaluated - a switch thrown before
     * any gate ran, named so "the gate was never asked" reads differently from
     * "the gate allowed it" (issue #65).
     */
    public static String ignoredMessage(String dimensionId,
                                        String spawnType,
                                        String why) {
        return String.format(Locale.ROOT,
                "Spawn not evaluated: dimension=%s spawnType=%s why=%s",
                orUnknown(dimensionId), orUnknown(spawnType), orUnknown(why));
    }

    private static String orUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
