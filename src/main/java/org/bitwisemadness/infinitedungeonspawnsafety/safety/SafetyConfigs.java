package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code safety.*} config file, bound into the pure {@link SafetyConfig}.
 *
 * <p>The keys mirror the plan's configuration table one for one; the spec
 * validates shape and range, while the record's compact constructor keeps the
 * semantic normalisation (stat name, non-negative bands) so both the file and
 * hand-built test configs resolve the same way. Values are read per spawn into
 * a fresh record, so the decision path never caches a stale snapshot.</p>
 *
 * <p>Registered as a COMMON config: the gate is pack-wide mod behaviour, its
 * keys are readable on either distribution, and COMMON is loaded at mod
 * construction so no world-load ordering can leave it unattached. Per-world
 * tuning belongs to the datapack lists, which the reload listener rebuilds on
 * every {@code /reload}.</p>
 */
public final class SafetyConfigs {

    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.BooleanValue HOSTILE;
    private static final ModConfigSpec.BooleanValue NEUTRAL;
    private static final ModConfigSpec.BooleanValue PASSIVE;
    private static final ModConfigSpec.EnumValue<DimensionMode> DIMENSION_MODE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    private static final ModConfigSpec.BooleanValue GATE_ENABLED;
    private static final ModConfigSpec.ConfigValue<String> GATE_STAT;
    private static final ModConfigSpec.DoubleValue DEVIATION_PERCENT;
    private static final ModConfigSpec.DoubleValue ATTRIBUTE_SCALE;
    private static final ModConfigSpec.DoubleValue AMBIENT_FALLBACK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("safety");
        ENABLED = builder
                .comment("Master toggle: when off the module never touches a spawn.")
                .define("enabled", true);
        HOSTILE = builder
                .comment("Hostile mobs may spawn at all. Off by default: the module's")
                .comment("purpose is keeping chosen dimensions safe.")
                .define("hostile", false);
        NEUTRAL = builder
                .comment("Neutral mobs may spawn at all.")
                .define("neutral", true);
        PASSIVE = builder
                .comment("Passive mobs may spawn at all.")
                .define("passive", true);
        builder.push("dimensions");
        DIMENSION_MODE = builder
                .comment("'whitelist' gates only the listed dimensions; 'blacklist'")
                .comment("gates every dimension except the listed ones.")
                .defineEnum("mode", DimensionMode.WHITELIST);
        DIMENSIONS = builder
                .comment("The affected (or exempt) dimension ids. The default names only")
                .comment("the overworld, so dungeon dimensions stay exempt unless a")
                .comment("blacklist setup names them explicitly.")
                .defineListAllowEmpty("list", List.of("minecraft:overworld"),
                        () -> "minecraft:overworld",
                        value -> value instanceof String);
        builder.pop();
        builder.push("magiculeGate");
        GATE_ENABLED = builder
                .comment("Whether the attribute-driven magicule gate runs. Category")
                .comment("toggles and datapack lists work either way.")
                .define("enabled", true);
        GATE_STAT = builder
                .comment("The mob stat measured against ambient magicule: 'ap' (aura),")
                .comment("'mp' (magicule), or 'ep' (stored energy).")
                .define("stat", "ep",
                        value -> value instanceof String stat
                                && List.of("ap", "mp", "ep").contains(
                                        stat.toLowerCase(Locale.ROOT)));
        DEVIATION_PERCENT = builder
                .comment("Tolerance band on the strict rule, in percent. The gate needs")
                .comment("stat * scale * (1 - deviationPercent / 100) ambient magicule.")
                .defineInRange("deviationPercent", 10.0, 0.0, 100.0);
        ATTRIBUTE_SCALE = builder
                .comment("Unit alignment between stat values and ambient magicule;")
                .comment("advanced, leave at 1 unless a provider says otherwise.")
                .defineInRange("attributeScale", 1.0, 0.0, 1000.0);
        AMBIENT_FALLBACK = builder
                .comment("The ambient value used when no provider answers. Zero blocks")
                .comment("only mobs whose stat provider supplies a positive requirement.")
                .defineInRange("ambientFallback", 0.0, 0.0, 1.0E9);
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }

    private SafetyConfigs() {
    }

    /** Registers the config file; called from the mod constructor. */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    /** The config as the pure record the decision path consumes. */
    public static SafetyConfig current() {
        return new SafetyConfig(
                ENABLED.get(),
                HOSTILE.get(),
                NEUTRAL.get(),
                PASSIVE.get(),
                DIMENSION_MODE.get(),
                new HashSet<>(DIMENSIONS.get()),
                GATE_ENABLED.get(),
                GATE_STAT.get(),
                DEVIATION_PERCENT.get(),
                ATTRIBUTE_SCALE.get(),
                AMBIENT_FALLBACK.get());
    }

    /** The spec, for anything that must pair with the registered file. */
    public static IConfigSpec spec() {
        return SPEC;
    }
}
