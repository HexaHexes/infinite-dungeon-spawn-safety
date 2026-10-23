package org.bitwisemadness.infinitedungeonspawnsafety.safety;

/** How the dimension list is interpreted. */
public enum DimensionMode {

    /** Only the listed dimensions are affected. */
    WHITELIST,

    /** Every dimension except the listed ones is affected. */
    BLACKLIST
}
