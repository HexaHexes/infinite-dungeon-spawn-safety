package org.bitwisemadness.infinitedungeonspawnsafety.safety;

/**
 * One structure-elevation row: a structure raises the magicule concentration of
 * its surroundings to at least {@code elevation}, the EP baseline of the mobs
 * related to it.
 *
 * <p>The radius is not carried here: it scales with the elevation (see
 * {@link StructureElevationLogic#radiusFor(double)}), so a structure with a
 * 350,000 baseline reaches wider than one with 20,000. Unknown keys in the
 * datapack file - an old {@code radius}, for example - are ignored by the
 * codec.</p>
 *
 * <p>Pure by construction (strings and primitives) so the row list is
 * unit-testable without a game runtime.</p>
 *
 * @param structure the structure's registry id, e.g. {@code iceandfire:fire_dragon_cave}
 * @param elevation the baseline the surroundings must reach
 */
public record StructureElevationRow(String structure, double elevation) {

    public StructureElevationRow {
        if (structure == null || structure.isBlank()) {
            throw new IllegalArgumentException("structure must not be blank");
        }
        if (Double.isNaN(elevation) || Double.isInfinite(elevation) || elevation < 0.0) {
            throw new IllegalArgumentException("elevation must be finite and non-negative, got " + elevation);
        }
    }
}
