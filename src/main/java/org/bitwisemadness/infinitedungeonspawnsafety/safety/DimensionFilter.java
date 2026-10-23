package org.bitwisemadness.infinitedungeonspawnsafety.safety;

/** Applies the whitelist or blacklist dimension filter. */
public final class DimensionFilter {

    private DimensionFilter() {
    }

    /**
     * Whether the module touches this dimension at all.
     *
     * <p>Dungeon dimensions are exempt in the shipped defaults simply because
     * the default list names only the overworld; a blacklist setup must name
     * the dungeon dimensions explicitly.</p>
     */
    public static boolean affects(String dimensionId, SafetyConfig config) {
        if (config == null || !config.enabled() || dimensionId == null) {
            return false;
        }
        boolean listed = config.dimensions().contains(dimensionId);
        return switch (config.dimensionMode()) {
            case WHITELIST -> listed;
            case BLACKLIST -> !listed;
        };
    }
}
