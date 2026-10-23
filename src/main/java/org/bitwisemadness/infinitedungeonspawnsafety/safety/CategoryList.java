package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.List;
import java.util.Optional;
import org.bitwisemadness.infinitedungeonapi.data.ResourceRef;

/**
 * One category's datapack lists: the id-or-tag references that restrict or ban
 * its members, plus the shared {@code required_mod} gate.
 *
 * <p>Kept free of Minecraft types so the precedence rules are unit-testable; the
 * game-facing loader expands the references at the boundary, where registries
 * and tags are reachable.</p>
 *
 * @param whitelist   when non-empty, only these references may spawn
 * @param blacklist   these references never spawn, whatever the whitelist says
 * @param requiredMod when set, the file applies only if that mod is loaded
 */
public record CategoryList(List<ResourceRef> whitelist,
                           List<ResourceRef> blacklist,
                           Optional<String> requiredMod) {

    /** An empty list set: allows everything and gates on nothing. */
    public static CategoryList empty() {
        return new CategoryList(List.of(), List.of(), Optional.empty());
    }

    public CategoryList {
        whitelist = whitelist == null ? List.of() : List.copyOf(whitelist);
        blacklist = blacklist == null ? List.of() : List.copyOf(blacklist);
        requiredMod = requiredMod == null ? Optional.empty() : requiredMod;
    }

    /** Whether the whitelist restricts spawning to its members. */
    public boolean restrictsToWhitelist() {
        return !whitelist.isEmpty();
    }
}
