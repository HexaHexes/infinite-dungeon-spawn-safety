package org.bitwisemadness.infinitedungeonspawnsafety.gametest;

import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.infinitedungeonapi.magicule.AmbientMagicule;
import org.bitwisemadness.infinitedungeonapi.magicule.MagiculeProvider;
import org.bitwisemadness.infinitedungeonapi.stats.EntityPowerStatProvider;
import org.bitwisemadness.infinitedungeonapi.stats.EntityPowerStats;
import org.bitwisemadness.infinitedungeonapi.stats.StatKind;
import org.bitwisemadness.infinitedungeonspawnsafety.InfiniteDungeonSpawnSafety;

/**
 * Live-server proof of the natural-spawn gate.
 *
 * <p>The gate hangs off {@code MobSpawnEvent.PositionCheck}, which vanilla's
 * {@code NaturalSpawner} posts for a mob that is built and positioned but not
 * yet added to the world. A real natural spawn cannot be forced reliably, so
 * the tests post the same event on the same bus the game uses, then read the
 * result back - that exercises the whole handler, the config, the loaded
 * tables, and the API services, with only the trigger synthed.</p>
 *
 * <p>Keep the {@code expectedTests} count in {@code build.gradle} in sync with
 * the number of {@code @GameTest} methods here, or the harness fails the
 * run.</p>
 */
@GameTestHolder(InfiniteDungeonSpawnSafety.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpawnSafetyGameTests {

    private SpawnSafetyGameTests() {
    }

    /** The mod is loaded and the empty structure template resolves. */
    @GameTest(template = "empty")
    public static void mod_loads(GameTestHelper helper) {
        helper.succeed();
    }

    /**
     * A natural spawn is gated: blocked when the ambient magicule cannot
     * sustain the mob, and left alone once it can. With no ambient provider
     * the fallback (zero) applies, so a stub stat provider of 100 EP makes the
     * requirement positive and the spawn fails; an ambient provider at 1000
     * then clears it.
     */
    @GameTest(template = "empty")
    public static void natural_spawn_is_gated(GameTestHelper helper) {
        // A sheep is passive, which the shipped config allows: the category
        // toggle cannot be what blocks it.
        Mob sheep = EntityType.SHEEP.create(helper.getLevel());
        helper.assertTrue(sheep != null, "the sheep must be creatable");
        positionAtOrigin(helper, sheep);

        EntityPowerStatProvider stats = (entity, kind) ->
                kind == StatKind.EP ? OptionalDouble.of(100.0) : OptionalDouble.empty();
        EntityPowerStats.registerProvider(stats);
        try {
            helper.assertTrue(
                    spawnResult(helper, sheep, MobSpawnType.NATURAL)
                            == MobSpawnEvent.PositionCheck.Result.FAIL,
                    "an unsustained natural spawn must be blocked");

            MagiculeProvider ambient = (level, pos) -> OptionalDouble.of(1000.0);
            AmbientMagicule.registerProvider(ambient);
            try {
                helper.assertTrue(
                        spawnResult(helper, sheep, MobSpawnType.NATURAL)
                                == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                        "a sustained natural spawn must be left alone");
            } finally {
                AmbientMagicule.removeProvider(ambient);
            }
        } finally {
            EntityPowerStats.removeProvider(stats);
        }
        helper.succeed();
    }

    /**
     * A spawner spawn is outside the API's allowlist and is untouched, even
     * though a zombie is hostile and the shipped config has hostiles off: the
     * spawn type is consulted before any other rule.
     */
    @GameTest(template = "empty")
    public static void spawner_spawn_is_untouched(GameTestHelper helper) {
        Mob zombie = EntityType.ZOMBIE.create(helper.getLevel());
        helper.assertTrue(zombie != null, "the zombie must be creatable");
        positionAtOrigin(helper, zombie);

        helper.assertTrue(
                spawnResult(helper, zombie, MobSpawnType.SPAWNER)
                        == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                "spawner spawns must never be touched");
        helper.succeed();
    }

    /** Posts the event exactly as the natural spawner would, and reads it back. */
    private static MobSpawnEvent.PositionCheck.Result spawnResult(GameTestHelper helper,
                                                                 Mob mob,
                                                                 MobSpawnType type) {
        MobSpawnEvent.PositionCheck event =
                new MobSpawnEvent.PositionCheck(mob, helper.getLevel(), type, null);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    private static void positionAtOrigin(GameTestHelper helper, Mob mob) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
    }
}
