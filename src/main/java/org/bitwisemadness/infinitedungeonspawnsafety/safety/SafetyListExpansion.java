package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.bitwisemadness.infinitedungeonapi.data.ResourceRef;
import org.bitwisemadness.infinitedungeonapi.data.TagExpansion;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;

/**
 * The expanded id sets behind the datapack lists, rebuilt on every reload.
 *
 * <p>{@link SafetyTables} keeps the raw id-or-tag references as pure data;
 * this class is the boundary that turns them into plain id sets once, when the
 * reload listener runs. Tags are bound by then - tag binding precedes reload
 * listeners - and every {@code /reload} reruns the listener, so the sets track
 * tag edits without any per-spawn registry walk. Membership is judged against
 * the entity type registry: missing ids drop out and empty tags degrade to
 * nothing, exactly the API's {@link TagExpansion} contract.</p>
 */
public final class SafetyListExpansion {

    /** One category's expanded membership. */
    private record Expanded(Set<String> whitelist, Set<String> blacklist) {

        private static final Expanded EMPTY = new Expanded(Set.of(), Set.of());
    }

    private static volatile Map<SpawnCategory, Expanded> expansions = Map.of();

    private SafetyListExpansion() {
    }

    /** Expands the given lists into id sets; replaces the whole cache. */
    public static void rebuild(Map<SpawnCategory, CategoryList> lists) {
        Map<SpawnCategory, Expanded> next = new EnumMap<>(SpawnCategory.class);
        if (lists != null) {
            for (Map.Entry<SpawnCategory, CategoryList> entry : lists.entrySet()) {
                CategoryList list = entry.getValue();
                next.put(entry.getKey(), new Expanded(
                        expand(list.whitelist()),
                        expand(list.blacklist())));
            }
        }
        expansions = Collections.unmodifiableMap(next);
    }

    /** The expanded whitelist ids for a category; empty when none shipped. */
    public static Set<String> whitelist(SpawnCategory category) {
        return expansions.getOrDefault(category, Expanded.EMPTY).whitelist();
    }

    /** The expanded blacklist ids for a category; empty when none shipped. */
    public static Set<String> blacklist(SpawnCategory category) {
        return expansions.getOrDefault(category, Expanded.EMPTY).blacklist();
    }

    /** Clears the cache; used on reload teardown and in tests. */
    public static void clear() {
        expansions = Map.of();
    }

    private static Set<String> expand(List<ResourceRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return Set.of();
        }
        Collection<String> found = TagExpansion.expandAll(refs,
                SafetyListExpansion::idExists,
                SafetyListExpansion::tagContents);
        Set<String> ids = new HashSet<>(found);
        ids.remove(null);
        return Collections.unmodifiableSet(ids);
    }

    private static boolean idExists(String id) {
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(parseQuietly(id));
    }

    private static Collection<String> tagContents(String location) {
        TagKey<EntityType<?>> key = TagKey.create(Registries.ENTITY_TYPE, parseQuietly(location));
        return BuiltInRegistries.ENTITY_TYPE.getTag(key)
                .map(holders -> holders.stream()
                        .map(holder -> BuiltInRegistries.ENTITY_TYPE.getKey(holder.value()))
                        .filter(Objects::nonNull)
                        .map(ResourceLocation::toString)
                        .<String>toList())
                .orElse(List.of());
    }

    private static ResourceLocation parseQuietly(String id) {
        // Ids arrive from a validated datapack reference or a registry key, so
        // this parse holds; guard anyway so a malformed input degrades to
        // "absent" rather than failing a spawn.
        try {
            return ResourceLocation.parse(id);
        } catch (RuntimeException ex) {
            return ResourceLocation.withDefaultNamespace("air");
        }
    }
}
