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

    /** One exempt file: the entities this module never blocks. */
    public static final Codec<SafetyData.Exempt> EXEMPT = RecordCodecBuilder.create(instance -> instance.group(
            InfiniteDungeonCodecs.RESOURCE_REF.listOf()
                    .fieldOf("entities").forGetter(SafetyData.Exempt::entities),
            InfiniteDungeonCodecs.REQUIRED_MOD.forGetter(SafetyData.Exempt::requiredMod)
    ).apply(instance, SafetyData.Exempt::new));

    /**
     * One structure-elevation row: a structure and its baseline. The reach is
     * derived from the baseline (see {@code StructureElevationLogic}), so an old
     * {@code radius} key in a pack's file is simply ignored.
     */
    public static final Codec<StructureElevationRow> STRUCTURE_ROW = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("structure").forGetter(StructureElevationRow::structure),
            Codec.DOUBLE.fieldOf("elevation").forGetter(StructureElevationRow::elevation)
    ).apply(instance, StructureElevationRow::new));

    /** One structures.json file: the rows, plus the required-mod gate. */
    public static final Codec<StructureMagiculeReloadListener.StructureFile> STRUCTURE_FILE =
            RecordCodecBuilder.create(instance -> instance.group(
                    STRUCTURE_ROW.listOf().fieldOf("structures")
                            .forGetter(StructureMagiculeReloadListener.StructureFile::rows),
                    InfiniteDungeonCodecs.REQUIRED_MOD
                            .forGetter(StructureMagiculeReloadListener.StructureFile::requiredMod)
            ).apply(instance, StructureMagiculeReloadListener.StructureFile::new));

    /** Reads a category name, rejecting anything that is not a category. */
    public static SpawnCategory category(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return SpawnCategory.valueOf(value);
    }
}
