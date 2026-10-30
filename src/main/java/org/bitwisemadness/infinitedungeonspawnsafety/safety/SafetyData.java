package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bitwisemadness.infinitedungeonapi.data.ResourceRef;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;

/** One parsed safety datapack file. */
public sealed interface SafetyData {

    /** {@code classification.json}: entity type id to category overrides. */
    record Classification(Map<String, SpawnCategory> entities) implements SafetyData {

        public Classification {
            entities = entities == null ? Map.of() : Map.copyOf(entities);
        }
    }

    /** One category file, with its target category resolved from the filename. */
    record Category(SpawnCategory category, CategoryList list) implements SafetyData {

        public Category {
            list = list == null ? CategoryList.empty() : list;
        }
    }

    /**
     * {@code exempt.json}: entities this module never blocks, wherever they
     * spawn. Exempt files union across packs (each contributes its own showcase
     * mobs) rather than replacing one another.
     */
    record Exempt(List<ResourceRef> entities, Optional<String> requiredMod) implements SafetyData {

        public Exempt {
            entities = entities == null ? List.of() : List.copyOf(entities);
            requiredMod = requiredMod == null ? Optional.empty() : requiredMod;
        }
    }
}
