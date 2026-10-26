package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Locale;

/**
 * Why a spawn was blocked, as pure text.
 *
 * <p>Kept free of Minecraft types so the wording is unit-testable. The handler
 * logs the result at debug level, which keeps the default experience quiet:
 * nothing is emitted for a passing gate or an untouched spawn, so an operator
 * asks for debug logging deliberately when hunting a blocked spawn.</p>
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
     * compared so the requirement can be tuned from the log alone.
     */
    public static String blockMessage(GateReason reason,
                                      String entityId,
                                      String dimensionId,
                                      String categoryName,
                                      double ambient,
                                      double required,
                                      double stat) {
        return String.format(Locale.ROOT,
                "Blocked natural spawn: entity=%s dimension=%s category=%s reason=%s"
                        + " ambient=%.2f required=%.2f stat=%.2f",
                orUnknown(entityId), orUnknown(dimensionId), orUnknown(categoryName), reason,
                ambient, required, stat);
    }

    private static String orUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
