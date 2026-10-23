package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The resolved safety configuration, as pure data.
 *
 * <p>Kept free of Minecraft types so the whole decision path is unit-testable;
 * the game-facing code reads config values into this record. Category toggles
 * are queried by name so they line up with the API's spawn categories without
 * duplicating that enum here.</p>
 *
 * @param enabled           master toggle
 * @param hostileAllowed    hostiles may spawn at all
 * @param neutralAllowed    neutral mobs may spawn
 * @param passiveAllowed    passive mobs may spawn
 * @param dimensionMode     whitelist or blacklist
 * @param dimensions        the affected (or exempt) dimension ids
 * @param gateEnabled       whether the magicule gate runs
 * @param gateStat          {@code ap}, {@code mp}, or {@code ep}
 * @param deviationPercent  tolerance band on the strict rule, in percent
 * @param attributeScale    unit alignment between ambient magicule and the stat
 * @param ambientFallback   ambient value when no provider answers
 */
public record SafetyConfig(
        boolean enabled,
        boolean hostileAllowed,
        boolean neutralAllowed,
        boolean passiveAllowed,
        DimensionMode dimensionMode,
        Set<String> dimensions,
        boolean gateEnabled,
        String gateStat,
        double deviationPercent,
        double attributeScale,
        double ambientFallback) {

    /** The shipped defaults: overworld-only, hostiles off, gate on with EP. */
    public static SafetyConfig defaults() {
        return new SafetyConfig(
                true,
                false,
                true,
                true,
                DimensionMode.WHITELIST,
                Set.of("minecraft:overworld"),
                true,
                "ep",
                10.0,
                1.0,
                0.0);
    }

    public SafetyConfig {
        if (dimensionMode == null) {
            dimensionMode = DimensionMode.WHITELIST;
        }
        dimensions = dimensions == null ? Set.of() : Set.copyOf(dimensions);
        gateStat = normaliseStat(gateStat);
        deviationPercent = Math.max(0.0, deviationPercent);
        ambientFallback = Math.max(0.0, ambientFallback);
    }

    /** Whether the named spawn category may spawn at all. */
    public boolean categoryAllowed(String categoryName) {
        if (categoryName == null) {
            return true;
        }
        return switch (categoryName.toUpperCase(Locale.ROOT)) {
            case "HOSTILE" -> hostileAllowed;
            case "NEUTRAL" -> neutralAllowed;
            case "PASSIVE" -> passiveAllowed;
            default -> true;
        };
    }

    private static String normaliseStat(String stat) {
        String value = stat == null ? "" : stat.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "ap", "mp", "ep" -> value;
            default -> "ep";
        };
    }

    /** The optional stat name, for callers that must distinguish "unset". */
    public Optional<String> gateStatName() {
        return Optional.ofNullable(gateStat);
    }
}
