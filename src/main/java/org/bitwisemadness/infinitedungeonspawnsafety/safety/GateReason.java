package org.bitwisemadness.infinitedungeonspawnsafety.safety;

/** Why a spawn was allowed or blocked, for diagnostics and tests. */
public enum GateReason {

    /** The module does not touch this dimension or is switched off. */
    NOT_APPLICABLE,

    /** Only natural spawns are gated; this was a spawner, structure, or other spawn. */
    NOT_NATURAL,

    /** The mob's category is switched off outright. */
    CATEGORY_DISABLED,

    /** The mob's category is allowed and the magicule rules do not apply. */
    CATEGORY_ALLOWED,

    /** The gate is switched off by config. */
    GATE_DISABLED,

    /** The mob has no usable stat, so no requirement can be formed. */
    NO_REQUIREMENT,

    /** The ambient magicule sustains the mob. */
    SUSTAINED,

    /** The ambient magicule is too low for the mob. */
    INSUFFICIENT_MAGICULE
}
