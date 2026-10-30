package org.bitwisemadness.infinitedungeonspawnsafety.safety;

/**
 * One structure-elevation row: a structure raises the ambient magicule of the
 * chunks around its start by an amount, within a radius.
 *
 * <p>Pure by construction (strings and primitives) so the row list is
 * unit-testable without a game runtime. The elevation is <em>additive</em> -
 * the API's phase model sums additive modifiers before multiplicative ones, so
 * a row of elevation 800000 guarantees the ambient around that structure is at
 * least 800000, which is the pack rule: a structure's neighbourhood sustains
 * the EP baseline of the mobs related to it.</p>
 *
 * @param structure the structure's registry id, e.g. {@code iceandfire:fire_dragon_cave}
 * @param radius    the sphere radius in blocks around the structure's start
 * @param elevation the additive magicule floor inside that sphere
 */
public record StructureElevationRow(String structure, double radius, double elevation) {

    public StructureElevationRow {
        if (structure == null || structure.isBlank()) {
            throw new IllegalArgumentException("structure must not be blank");
        }
        if (!(radius >= 0.0) || Double.isInfinite(radius)) {
            throw new IllegalArgumentException("radius must be finite and non-negative, got " + radius);
        }
        if (Double.isNaN(elevation) || Double.isInfinite(elevation)) {
            throw new IllegalArgumentException("elevation must be finite, got " + elevation);
        }
    }
}
