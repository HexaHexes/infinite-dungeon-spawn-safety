package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bitwisemadness.infinitedungeonapi.data.RequiredModFilter;
import org.bitwisemadness.infinitedungeonapi.data.ResourceRef;
import org.bitwisemadness.infinitedungeonapi.data.TagExpansion;
import org.junit.jupiter.api.Test;

/**
 * Drives the loader boundary the way the reload listener does, but with injected
 * registry and tag contents: references are expanded through the API's
 * {@link TagExpansion} and mod presence through {@link RequiredModFilter}, then
 * judged by this module's {@link ListRules}.
 */
class SafetyListIntegrationTest {

    private static final Set<String> PRESENT_IDS =
            Set.of("minecraft:pillager", "minecraft:vindicator", "minecraft:zombie");

    private static final Map<String, List<String>> TAGS = Map.of(
            "minecraft:raiders", List.of("minecraft:pillager", "minecraft:vindicator", "minecraft:absent"));

    private static final Set<String> MODS = Set.of("tensura", "infinite_dungeon_api");

    private static Set<String> expand(List<ResourceRef> refs) {
        Predicate<String> idExists = PRESENT_IDS::contains;
        Function<String, Collection<String>> tagContents = tag -> TAGS.getOrDefault(tag, List.of());
        return Set.copyOf(TagExpansion.expandAll(refs, idExists, tagContents));
    }

    @Test
    void tagMembersDecideThroughTheLists() {
        CategoryList list = new CategoryList(
                List.of(ResourceRef.parse("minecraft:zombie")),
                List.of(ResourceRef.parse("#minecraft:raiders")),
                Optional.empty());
        Set<String> whitelist = expand(list.whitelist());
        Set<String> blacklist = expand(list.blacklist());

        assertEquals(GateReason.BLACKLISTED,
                ListRules.decide("minecraft:pillager", list, whitelist, blacklist).reason());
        assertEquals(GateReason.LIST_ALLOWED,
                ListRules.decide("minecraft:zombie", list, whitelist, blacklist).reason());
        assertEquals(GateReason.OUTSIDE_WHITELIST,
                ListRules.decide("minecraft:skeleton", list, whitelist, blacklist).reason());
    }

    @Test
    void absentTagMembersAreDroppedNotFatal() {
        Set<String> expanded = expand(List.of(ResourceRef.parse("#minecraft:raiders")));

        assertFalse(expanded.contains("minecraft:absent"));
        assertEquals(2, expanded.size());
    }

    @Test
    void requiredModGateSkipsAbsentContent() {
        assertTrue(RequiredModFilter.allowed(Optional.of("tensura"), MODS::contains));
        assertFalse(RequiredModFilter.allowed(Optional.of("absent_mod"), MODS::contains));
        assertTrue(RequiredModFilter.allowed(Optional.empty(), id -> false));
    }
}
