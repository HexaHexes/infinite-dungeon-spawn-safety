package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SafetyTablesTest {

    @AfterEach
    void clearTables() {
        SafetyTables.clear();
    }

    @Test
    void emptyByDefault() {
        assertTrue(SafetyTables.all().isEmpty());
        assertTrue(SafetyTables.get(SpawnCategory.HOSTILE).isEmpty());
    }

    @Test
    void setReplacesWholesale() {
        SafetyTables.set(Map.of(SpawnCategory.HOSTILE, CategoryList.empty()));
        assertTrue(SafetyTables.get(SpawnCategory.HOSTILE).isPresent());

        // A reload that drops a file must drop what that file declared.
        SafetyTables.set(Map.of(SpawnCategory.PASSIVE, CategoryList.empty()));
        assertTrue(SafetyTables.get(SpawnCategory.HOSTILE).isEmpty());
        assertTrue(SafetyTables.get(SpawnCategory.PASSIVE).isPresent());
    }

    @Test
    void getWithNullCategoryIsEmpty() {
        assertTrue(SafetyTables.get(null).isEmpty());
    }

    @Test
    void setNullClears() {
        SafetyTables.set(Map.of(SpawnCategory.NEUTRAL, CategoryList.empty()));

        SafetyTables.set(null);

        assertTrue(SafetyTables.all().isEmpty());
    }

    @Test
    void tableIsUnmodifiable() {
        SafetyTables.set(Map.of(SpawnCategory.NEUTRAL, CategoryList.empty()));

        assertThrows(UnsupportedOperationException.class, () -> SafetyTables.all().clear());
    }
}
