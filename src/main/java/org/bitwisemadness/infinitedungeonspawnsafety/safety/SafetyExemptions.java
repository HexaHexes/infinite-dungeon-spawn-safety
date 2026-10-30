package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Set;

/**
 * The entities the safety module never blocks, expanded from the datapack's
 * {@code exempt.json} files and replaced on every reload.
 *
 * <p>Exemption is total for this module: an exempt entity's natural spawns skip
 * the dimension filter, the category toggle, the datapack lists and the
 * magicule gate alike - the module simply does not touch it. Content mods use
 * this to keep their showcase mobs (dragons, structure bosses) spawning while
 * the pack's magicule ecology governs everything else.</p>
 */
public final class SafetyExemptions {

    private static volatile Set<String> exempt = Set.of();

    private SafetyExemptions() {
    }

    /** Replaces the set; called on datapack reload. */
    public static void set(Set<String> ids) {
        exempt = ids == null ? Set.of() : Set.copyOf(ids);
    }

    /** Whether the entity id is exempt from every rule this module applies. */
    public static boolean contains(String entityId) {
        return entityId != null && exempt.contains(entityId);
    }

    /** The current set (unmodifiable). */
    public static Set<String> all() {
        return exempt;
    }

    /** Clears the set; used on reload teardown and in tests. */
    public static void clear() {
        exempt = Set.of();
    }
}
