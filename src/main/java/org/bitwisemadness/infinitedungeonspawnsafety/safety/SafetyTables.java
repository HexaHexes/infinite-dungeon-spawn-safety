package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;

/**
 * The loaded per-category lists, replaced wholesale on every datapack reload.
 *
 * <p>Mirrors the API's {@code ClassificationOverrides} holder: a volatile
 * immutable table so the decision path always reads one consistent snapshot.
 * Empty means "no lists shipped", which allows everything and leaves the config
 * toggles and the magicule gate in charge.</p>
 */
public final class SafetyTables {

    private static volatile Map<SpawnCategory, CategoryList> lists = Map.of();

    private SafetyTables() {
    }

    /** Replaces the table, for example on datapack reload. */
    public static void set(Map<SpawnCategory, CategoryList> entries) {
        Map<SpawnCategory, CategoryList> copy = new EnumMap<>(SpawnCategory.class);
        if (entries != null) {
            copy.putAll(entries);
        }
        lists = Collections.unmodifiableMap(copy);
    }

    /** The lists for a category, when the pack supplied any. */
    public static Optional<CategoryList> get(SpawnCategory category) {
        return category == null ? Optional.empty() : Optional.ofNullable(lists.get(category));
    }

    /** The current table. */
    public static Map<SpawnCategory, CategoryList> all() {
        return lists;
    }

    /** Clears the table; used on reload teardown and in tests. */
    public static void clear() {
        lists = Map.of();
    }
}
