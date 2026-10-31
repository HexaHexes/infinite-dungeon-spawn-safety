package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.bitwisemadness.infinitedungeonapi.event.SpawnGateDecisionEvent;
import org.bitwisemadness.infinitedungeonapi.magicule.AmbientMagicule;
import org.bitwisemadness.infinitedungeonapi.spawn.MobClassification;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;
import org.bitwisemadness.infinitedungeonapi.stats.EntityPowerStats;
import org.bitwisemadness.infinitedungeonapi.stats.StatKind;
import org.slf4j.Logger;

/**
 * The natural-spawn interception, wired to the game event bus.
 *
 * <p>Listens to {@link MobSpawnEvent.PositionCheck}, which vanilla's
 * {@code NaturalSpawner} posts through the API's spawn hook after the mob is
 * constructed and positioned but before {@code finalizeSpawn} and before the
 * entity is added to the world. Setting {@code Result.FAIL} rejects the spawn
 * right there; attributes are readable because the mob already exists. The
 * event also fires for the spawner path, so the API's own
 * {@code NaturalSpawns} allowlist is consulted first and everything outside
 * it - spawners, chunk generation, commands, eggs - passes through untouched.</p>
 *
 * <p>The decision order mirrors the plan exactly: spawn type, then the
 * dimension filter, then the category toggle, then the datapack lists, then
 * the magicule gate. Before any rejection lands, the API's cancellable
 * {@link SpawnGateDecisionEvent} is posted so a pack or integration can
 * override the block; a veto leaves the spawn alone. Every value comes from
 * the API's services or the config, so with no provider registered the gate is
 * a no-op rather than a guess.</p>
 */
public final class SpawnSafetyHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SpawnSafetyHandler() {
    }

    /** One spawn's position check, on the server event bus. */
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!MobClassification.isGatedSpawn(event.getSpawnType())) {
            return;
        }
        Mob mob = event.getEntity();
        SafetyConfig config = SafetyConfigs.current();
        // The event always comes from a ServerLevel; the accessor is a cast
        // away from it, and the ambient provider needs the level proper.
        ServerLevel level = event.getLevel().getLevel();
        String dimensionId = level.dimension().location().toString();
        if (!DimensionFilter.affects(dimensionId, config)) {
            return;
        }
        BlockPos position = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        String entityId = entityId(mob);
        SpawnCategory category = MobClassification.categoryOf(mob);
        String categoryName = category.name();
        if (SafetyExemptions.contains(entityId)) {
            // exempt.json: the module never touches this entity - no category
            // toggle, list, dimension filter or magicule requirement applies.
            // A quiet allow, exactly like a passing gate.
            return;
        }
        if (!config.categoryAllowed(categoryName)) {
            block(event, level, mob, category, position, 0.0, 0.0,
                    GateDiagnostics.blockMessage(
                            GateReason.CATEGORY_DISABLED, entityId, dimensionId, categoryName));
            return;
        }
        GateDecision list = listDecision(category, entityId);
        if (!list.allowed()) {
            block(event, level, mob, category, position, 0.0, 0.0,
                    GateDiagnostics.blockMessage(
                            list.reason(), entityId, dimensionId, categoryName));
            return;
        }
        double ambient = ambient(level, position, config);
        double stat = statValue(mob, config);
        GateDecision decision = MagiculeGate.evaluate(config, categoryName, true, ambient, stat);
        if (!decision.allowed()) {
            block(event, level, mob, category, position, ambient,
                    MagiculeGate.requiredMagicule(config, stat),
                    GateDiagnostics.blockMessage(decision.reason(), entityId, dimensionId,
                            categoryName, ambient, MagiculeGate.requiredMagicule(config, stat), stat));
        }
    }

    /** Applies the loaded datapack lists, after the category toggle. */
    private static GateDecision listDecision(SpawnCategory category, String entityId) {
        CategoryList list = SafetyTables.get(category).orElse(null);
        if (list == null) {
            return GateDecision.allow(GateReason.LIST_ALLOWED);
        }
        return ListRules.decide(entityId, list,
                SafetyListExpansion.whitelist(category),
                SafetyListExpansion.blacklist(category));
    }

    /** The ambient magicule at the spawn position, fallback when unanswered. */
    private static double ambient(ServerLevel level,
                                  BlockPos position,
                                  SafetyConfig config) {
        // Structure floors are not simulated here: they are written into the
        // chunks' Tensura storage on load (StructureElevationSource), so the
        // provider answers with the real, elevated concentration.
        OptionalDouble base = AmbientMagicule.baseAt(level, position);
        return AmbientRead.resolve(base, AmbientMagicule.modifiers(), config.ambientFallback());
    }

    /** The mob's own gate stat value; zero when no provider speaks for it. */
    private static double statValue(Mob mob, SafetyConfig config) {
        StatKind kind = StatKind.valueOf(
                config.gateStatName().orElse("ep").toUpperCase(Locale.ROOT));
        return EntityPowerStats.providerValue(mob, kind).orElse(0.0);
    }

    private static String entityId(Mob mob) {
        return String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
    }

    /**
     * Rejects the spawn, unless a pack override vetoes the block.
     *
     * <p>The API's cancellable {@link SpawnGateDecisionEvent} fires first,
     * carrying the facts the decision used (the ambient and required values are
     * zero when the block came from an earlier rule, which never computed
     * them). A listener that cancels it re-allows the spawn, so the result is
     * left untouched. Otherwise {@code Result.FAIL} lands and the reason is
     * recorded at debug level: a passing gate and an untouched spawn log
     * nothing, so the default experience stays quiet.</p>
     */
    private static void block(MobSpawnEvent.PositionCheck event,
                              ServerLevel level,
                              Mob mob,
                              SpawnCategory category,
                              BlockPos position,
                              double ambient,
                              double required,
                              String message) {
        SpawnGateDecisionEvent decision = new SpawnGateDecisionEvent(
                level.dimension(), mob, category, position, ambient, required);
        NeoForge.EVENT_BUS.post(decision);
        if (decision.isCanceled()) {
            LOGGER.debug("Pack override re-allowed a spawn: {}", message);
            return;
        }
        event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        LOGGER.debug("{}", message);
    }
}
