package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.bitwisemadness.infinitedungeonapi.data.RequiredModFilter;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;

/**
 * Folds parsed files into the two tables a load produces, as pure logic.
 *
 * <p>The reload listener parses files and hands them here in sorted location
 * order, which keeps the merge rules - required-mod gating, whole-file
 * replacement per category, and a keyed merge for classification overrides -
 * unit-testable without a game runtime.</p>
 */
public final class SafetyDataReducer {

    private SafetyDataReducer() {
    }

    /**
     * Reduces parsed entries to the tables they declare.
     *
     * @param entries    parsed files, in the order they should win
     * @param modPresent whether a named mod is loaded
     */
    public static Applied reduce(Collection<SafetyData> entries, Predicate<String> modPresent) {
        Map<String, SpawnCategory> overrides = new TreeMap<>();
        Map<SpawnCategory, CategoryList> lists = new EnumMap<>(SpawnCategory.class);
        if (entries != null) {
            for (SafetyData data : entries) {
                if (data instanceof SafetyData.Classification classification) {
                    // Keyed merge: a later pack refines named entities, leaving the
                    // rest of the derived classification intact.
                    overrides.putAll(classification.entities());
                } else if (data instanceof SafetyData.Category category) {
                    if (RequiredModFilter.allowed(category.list().requiredMod(), modPresent)) {
                        // Whole-file replace per category: a higher-priority pack's
                        // hostile.json supersedes another's rather than merging.
                        lists.put(category.category(), category.list());
                    }
                }
            }
        }
        return new Applied(overrides, lists);
    }

    /**
     * The tables a load produced.
     *
     * @param overrides entity type id to category, for the API's classification holder
     * @param lists     per-category spawn lists
     */
    public record Applied(Map<String, SpawnCategory> overrides,
                          Map<SpawnCategory, CategoryList> lists) {
    }
}
