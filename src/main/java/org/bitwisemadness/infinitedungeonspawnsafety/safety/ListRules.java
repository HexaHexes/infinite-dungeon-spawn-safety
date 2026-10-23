package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Set;

/**
 * The list precedence rules, as pure logic.
 *
 * <ol>
 *   <li>a blacklist match blocks outright;</li>
 *   <li>a non-empty whitelist restricts spawning to its members;</li>
 *   <li>otherwise the lists allow the spawn.</li>
 * </ol>
 *
 * <p>Category toggles are a separate, earlier gate: the config blocks a disabled
 * category before these rules are consulted, so this class only decides between
 * the lists themselves. Membership arrives already expanded from id-or-tag
 * references, which keeps the class free of registry access.</p>
 */
public final class ListRules {

    private ListRules() {
    }

    /**
     * Applies the lists for one mob.
     *
     * @param entityId         the mob's entity type id
     * @param list             the category's lists, or null when none shipped
     * @param whitelistMembers the expanded whitelist ids
     * @param blacklistMembers the expanded blacklist ids
     */
    public static GateDecision decide(String entityId,
                                      CategoryList list,
                                      Set<String> whitelistMembers,
                                      Set<String> blacklistMembers) {
        if (list == null) {
            return GateDecision.allow(GateReason.LIST_ALLOWED);
        }
        if (entityId != null && blacklistMembers != null && blacklistMembers.contains(entityId)) {
            return GateDecision.block(GateReason.BLACKLISTED);
        }
        if (list.restrictsToWhitelist()
                && (entityId == null || whitelistMembers == null || !whitelistMembers.contains(entityId))) {
            return GateDecision.block(GateReason.OUTSIDE_WHITELIST);
        }
        return GateDecision.allow(GateReason.LIST_ALLOWED);
    }
}
