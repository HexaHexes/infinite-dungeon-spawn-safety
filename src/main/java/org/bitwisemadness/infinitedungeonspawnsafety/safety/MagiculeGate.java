package org.bitwisemadness.infinitedungeonspawnsafety.safety;

/**
 * The magicule gate as pure math.
 *
 * <p>Rules, in order:</p>
 * <ol>
 *   <li>only natural spawns are ever gated;</li>
 *   <li>a switched-off category blocks outright;</li>
 *   <li>with the gate disabled, an allowed category passes;</li>
 *   <li>a mob with no usable stat has no requirement and passes;</li>
 *   <li>otherwise the spawn passes when the ambient magicule reaches
 *       {@code stat * attributeScale * (1 - deviationPercent / 100)}.</li>
 * </ol>
 *
 * <p>The gate never guesses: values arrive from the API's services, and the
 * caller supplies the ambient fallback the same way.</p>
 */
public final class MagiculeGate {

    private MagiculeGate() {
    }

    /**
     * Evaluates one spawn.
     *
     * @param config       the resolved safety config
     * @param categoryName the mob's category name (the API's {@code SpawnCategory} name)
     * @param naturalSpawn whether this is a natural spawn
     * @param ambient      the ambient magicule at the spawn position
     * @param statValue    the mob's own gate stat value
     */
    public static GateDecision evaluate(SafetyConfig config,
                                        String categoryName,
                                        boolean naturalSpawn,
                                        double ambient,
                                        double statValue) {
        if (config == null || !config.enabled()) {
            return GateDecision.allow(GateReason.NOT_APPLICABLE);
        }
        if (!naturalSpawn) {
            return GateDecision.allow(GateReason.NOT_NATURAL);
        }
        if (!config.categoryAllowed(categoryName)) {
            return GateDecision.block(GateReason.CATEGORY_DISABLED);
        }
        if (!config.gateEnabled()) {
            return GateDecision.allow(GateReason.GATE_DISABLED);
        }
        if (!(statValue > 0.0)) {
            return GateDecision.allow(GateReason.NO_REQUIREMENT);
        }

        double required = requiredMagicule(config, statValue);
        double available = Math.max(0.0, ambient);
        return available >= required
                ? GateDecision.allow(GateReason.SUSTAINED)
                : GateDecision.block(GateReason.INSUFFICIENT_MAGICULE);
    }

    /** The ambient value a mob of this stat needs to spawn. */
    public static double requiredMagicule(SafetyConfig config, double statValue) {
        double scale = config.attributeScale();
        double tolerance = 1.0 - config.deviationPercent() / 100.0;
        return statValue * scale * Math.max(0.0, tolerance);
    }
}
