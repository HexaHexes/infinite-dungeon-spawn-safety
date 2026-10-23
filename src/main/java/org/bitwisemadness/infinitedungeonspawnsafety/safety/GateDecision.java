package org.bitwisemadness.infinitedungeonspawnsafety.safety;

/**
 * The outcome of one spawn decision.
 *
 * @param allowed whether the spawn may proceed
 * @param reason  why, for diagnostics
 */
public record GateDecision(boolean allowed, GateReason reason) {

    public static GateDecision allow(GateReason reason) {
        return new GateDecision(true, reason);
    }

    public static GateDecision block(GateReason reason) {
        return new GateDecision(false, reason);
    }
}
