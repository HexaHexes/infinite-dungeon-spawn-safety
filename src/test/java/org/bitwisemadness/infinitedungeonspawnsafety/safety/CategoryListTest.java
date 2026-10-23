package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bitwisemadness.infinitedungeonapi.data.ResourceRef;
import org.junit.jupiter.api.Test;

class CategoryListTest {

    private static ResourceRef ref(String value) {
        return ResourceRef.parse(value);
    }

    @Test
    void emptyAllowsEverythingAndGatesOnNothing() {
        CategoryList empty = CategoryList.empty();

        assertTrue(empty.whitelist().isEmpty());
        assertTrue(empty.blacklist().isEmpty());
        assertTrue(empty.requiredMod().isEmpty());
        assertFalse(empty.restrictsToWhitelist());
    }

    @Test
    void nullsNormaliseToEmpty() {
        CategoryList list = new CategoryList(null, null, null);

        assertTrue(list.whitelist().isEmpty());
        assertTrue(list.blacklist().isEmpty());
        assertTrue(list.requiredMod().isEmpty());
        assertFalse(list.restrictsToWhitelist());
    }

    @Test
    void restrictsOnlyWhenTheWhitelistHasEntries() {
        assertFalse(new CategoryList(List.of(), List.of(ref("minecraft:zombie")), Optional.empty())
                .restrictsToWhitelist());
        assertTrue(new CategoryList(List.of(ref("minecraft:zombie")), List.of(), Optional.empty())
                .restrictsToWhitelist());
    }

    @Test
    void copiesAreImmutable() {
        List<ResourceRef> source = new ArrayList<>();
        source.add(ref("minecraft:zombie"));

        CategoryList list = new CategoryList(source, List.of(), Optional.of("tensura"));
        source.clear();

        assertEquals(1, list.whitelist().size());
        assertEquals("tensura", list.requiredMod().orElseThrow());
        assertThrows(UnsupportedOperationException.class,
                () -> list.whitelist().add(ref("minecraft:pig")));
    }
}
