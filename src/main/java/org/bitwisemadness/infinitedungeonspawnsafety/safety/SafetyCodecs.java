package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bitwisemadness.infinitedungeonapi.data.InfiniteDungeonCodecs;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;

/**
 * JSON codecs for the safety datapack files.
 *
 * <p>Codecs use Mojang's serialization library, which is deliberately absent
 * from the JUnit classpath, so this class stays a thin boundary: every decision
 * lives in the pure {@link CategoryList} and {@link ListRules}. The id-or-tag
 * reference codec and the shared {@code required_mod} field come from the API's
 * {@link InfiniteDungeonCodecs}.</p>
 */
public final class SafetyCodecs {

    private SafetyCodecs() {
    }

    /** A category name, tolerating case on read. */
    public static final Codec<SpawnCategory> SPAWN_CATEGORY =
            Codec.STRING.xmap(SafetyCodecs::category, SpawnCategory::name);

    /** {@code classification.json}: entity type id to category overrides. */
    public static final Codec<Map<String, SpawnCategory>> CLASSIFICATION =
            Codec.unboundedMap(Codec.STRING, SPAWN_CATEGORY);

    /** One category file: whitelist, blacklist, and the required-mod gate. */
    public static final Codec<CategoryList> CATEGORY_LIST = RecordCodecBuilder.create(instance -> instance.group(
            InfiniteDungeonCodecs.RESOURCE_REF.listOf()
                    .optionalFieldOf("whitelist", List.of()).forGetter(CategoryList::whitelist),
            InfiniteDungeonCodecs.RESOURCE_REF.listOf()
                    .optionalFieldOf("blacklist", List.of()).forGetter(CategoryList::blacklist),
            InfiniteDungeonCodecs.REQUIRED_MOD.forGetter(CategoryList::requiredMod)
    ).apply(instance, CategoryList::new));

    /** Reads a category name, rejecting anything that is not a category. */
    public static SpawnCategory category(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return SpawnCategory.valueOf(value);
    }
}
