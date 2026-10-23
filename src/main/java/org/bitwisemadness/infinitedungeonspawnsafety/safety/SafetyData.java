package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Map;
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
}
