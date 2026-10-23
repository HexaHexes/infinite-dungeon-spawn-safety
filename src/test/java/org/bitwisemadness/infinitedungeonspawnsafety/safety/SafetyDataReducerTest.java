package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bitwisemadness.infinitedungeonapi.data.ResourceRef;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;
import org.junit.jupiter.api.Test;

class SafetyDataReducerTest {

    private static CategoryList list(String whitelistRef, Optional<String> requiredMod) {
        List<ResourceRef> whitelist = whitelistRef == null
                ? List.of()
                : List.of(ResourceRef.parse(whitelistRef));
        return new CategoryList(whitelist, List.of(), requiredMod);
    }

    @Test
    void emptyInputYieldsEmptyTables() {
        SafetyDataReducer.Applied applied = SafetyDataReducer.reduce(List.of(), mod -> true);

        assertTrue(applied.overrides().isEmpty());
        assertTrue(applied.lists().isEmpty());
    }

    @Test
    void nullInputIsTolerated() {
        SafetyDataReducer.Applied applied = SafetyDataReducer.reduce(null, mod -> true);

        assertTrue(applied.overrides().isEmpty());
        assertTrue(applied.lists().isEmpty());
    }

    @Test
    void requiredModAbsentSkipsTheCategoryFile() {
        SafetyData data = new SafetyData.Category(SpawnCategory.HOSTILE,
                list("minecraft:zombie", Optional.of("tensura")));

        assertTrue(SafetyDataReducer.reduce(List.of(data), mod -> false).lists().isEmpty());
        assertFalse(SafetyDataReducer.reduce(List.of(data), mod -> true).lists().isEmpty());
    }

    @Test
    void laterCategoryFileReplacesTheEarlierOneWholesale() {
        SafetyData first = new SafetyData.Category(SpawnCategory.HOSTILE,
                list("minecraft:zombie", Optional.empty()));
        SafetyData second = new SafetyData.Category(SpawnCategory.HOSTILE,
                list("minecraft:skeleton", Optional.empty()));

        CategoryList winner = SafetyDataReducer.reduce(List.of(first, second), mod -> true)
                .lists().get(SpawnCategory.HOSTILE);

        assertEquals(List.of(ResourceRef.parse("minecraft:skeleton")), winner.whitelist());
    }

    @Test
    void classificationMergesByKeyWithLaterWinning() {
        SafetyData first = new SafetyData.Classification(Map.of(
                "minecraft:zombie", SpawnCategory.PASSIVE,
                "minecraft:creeper", SpawnCategory.PASSIVE));
        SafetyData second = new SafetyData.Classification(Map.of(
                "minecraft:creeper", SpawnCategory.NEUTRAL));

        Map<String, SpawnCategory> overrides = SafetyDataReducer.reduce(List.of(first, second), mod -> true)
                .overrides();

        assertEquals(SpawnCategory.PASSIVE, overrides.get("minecraft:zombie"));
        assertEquals(SpawnCategory.NEUTRAL, overrides.get("minecraft:creeper"));
        assertEquals(2, overrides.size());
    }

    @Test
    void categoryFilesAndClassificationShareOneLoad() {
        SafetyDataReducer.Applied applied = SafetyDataReducer.reduce(List.of(
                new SafetyData.Classification(Map.of("minecraft:creeper", SpawnCategory.NEUTRAL)),
                new SafetyData.Category(SpawnCategory.PASSIVE, list(null, Optional.empty()))),
                mod -> true);

        assertEquals(SpawnCategory.NEUTRAL, applied.overrides().get("minecraft:creeper"));
        assertTrue(applied.lists().containsKey(SpawnCategory.PASSIVE));
        assertFalse(applied.lists().get(SpawnCategory.PASSIVE).restrictsToWhitelist());
    }
}
