package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bitwisemadness.infinitedungeonapi.data.ResourceRef;
import org.junit.jupiter.api.Test;

class ListRulesTest {

    private static CategoryList list(List<ResourceRef> whitelist, List<ResourceRef> blacklist) {
        return new CategoryList(whitelist, blacklist, Optional.empty());
    }

    private static ResourceRef ref(String value) {
        return ResourceRef.parse(value);
    }

    @Test
    void noListsAllow() {
        GateDecision decision = ListRules.decide("minecraft:zombie", null, Set.of(), Set.of());

        assertTrue(decision.allowed());
        assertEquals(GateReason.LIST_ALLOWED, decision.reason());
    }

    @Test
    void emptyListsAllow() {
        assertTrue(ListRules.decide("minecraft:zombie", list(List.of(), List.of()), Set.of(), Set.of()).allowed());
    }

    @Test
    void blacklistBeatsWhitelist() {
        CategoryList both = list(List.of(ref("minecraft:zombie")), List.of(ref("minecraft:zombie")));

        GateDecision decision = ListRules.decide("minecraft:zombie", both,
                Set.of("minecraft:zombie"), Set.of("minecraft:zombie"));

        assertFalse(decision.allowed());
        assertEquals(GateReason.BLACKLISTED, decision.reason());
    }

    @Test
    void blacklistBlocksEvenWhenNoWhitelistShipped() {
        CategoryList blacklistOnly = list(List.of(), List.of(ref("minecraft:creeper")));

        GateDecision decision = ListRules.decide("minecraft:creeper", blacklistOnly,
                Set.of(), Set.of("minecraft:creeper"));

        assertFalse(decision.allowed());
        assertEquals(GateReason.BLACKLISTED, decision.reason());
    }

    @Test
    void nonEmptyWhitelistRestrictsToItsMembers() {
        CategoryList whitelist = list(List.of(ref("minecraft:zombie")), List.of());

        assertEquals(GateReason.LIST_ALLOWED,
                ListRules.decide("minecraft:zombie", whitelist, Set.of("minecraft:zombie"), Set.of()).reason());

        GateDecision outsider = ListRules.decide("minecraft:skeleton", whitelist,
                Set.of("minecraft:zombie"), Set.of());
        assertFalse(outsider.allowed());
        assertEquals(GateReason.OUTSIDE_WHITELIST, outsider.reason());
    }

    @Test
    void expandedTagMembersSatisfyTheWhitelist() {
        CategoryList whitelist = list(List.of(ref("#minecraft:raiders")), List.of());

        assertTrue(ListRules.decide("minecraft:pillager", whitelist,
                Set.of("minecraft:pillager", "minecraft:vindicator"), Set.of()).allowed());
    }

    @Test
    void unknownEntityFailsAClosedWhitelist() {
        CategoryList whitelist = list(List.of(ref("minecraft:zombie")), List.of());

        GateDecision decision = ListRules.decide(null, whitelist, Set.of("minecraft:zombie"), Set.of());

        assertFalse(decision.allowed());
        assertEquals(GateReason.OUTSIDE_WHITELIST, decision.reason());
    }

    @Test
    void nullMemberSetsAreSafe() {
        CategoryList whitelist = list(List.of(ref("minecraft:zombie")), List.of());

        assertFalse(ListRules.decide("minecraft:zombie", whitelist, null, null).allowed());
        assertTrue(ListRules.decide("minecraft:zombie", list(List.of(), List.of()), null, null).allowed());
    }
}
