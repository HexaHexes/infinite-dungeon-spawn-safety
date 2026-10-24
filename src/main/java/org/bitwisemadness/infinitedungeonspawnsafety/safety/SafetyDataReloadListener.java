package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.bitwisemadness.infinitedungeonapi.data.InfiniteDungeonJsonReloadListener;
import org.bitwisemadness.infinitedungeonapi.spawn.ClassificationOverrides;
import org.bitwisemadness.infinitedungeonapi.spawn.SpawnCategory;
import org.slf4j.Logger;

/**
 * Loads {@code data/<namespace>/infinite_dungeon/safety/} on server start and on
 * every {@code /reload}.
 *
 * <p>Recognised files are {@code classification.json} (entity overrides) and
 * {@code hostile.json} / {@code neutral.json} / {@code passive.json} (the id-or-
 * tag lists for each category). Tables are rebuilt from scratch each load, so a
 * reload that drops a file also drops what it declared.</p>
 *
 * <p>Malformed files are logged and skipped by the API base listener, never
 * fatal. A file gated by {@code required_mod} whose mod is absent is skipped
 * silently, so a pack can ship data for optional content.</p>
 */
public final class SafetyDataReloadListener extends InfiniteDungeonJsonReloadListener<SafetyData> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CLASSIFICATION_FILE = "classification";

    private static final Map<String, SpawnCategory> CATEGORY_FILES = Map.of(
            "hostile", SpawnCategory.HOSTILE,
            "neutral", SpawnCategory.NEUTRAL,
            "passive", SpawnCategory.PASSIVE);

    public SafetyDataReloadListener() {
        super(new Gson(), "safety");
    }

    /** Registers the listener; called from the mod constructor. */
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SafetyDataReloadListener());
    }

    @Override
    protected SafetyData parse(ResourceLocation location, JsonElement json) {
        String file = location.getPath();
        if (CLASSIFICATION_FILE.equals(file)) {
            return new SafetyData.Classification(
                    SafetyCodecs.CLASSIFICATION.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
        SpawnCategory category = CATEGORY_FILES.get(file);
        if (category == null) {
            // Anything else in the directory is ignored rather than fatal, so the
            // folder can hold pack-specific extras.
            return null;
        }
        return new SafetyData.Category(category,
                SafetyCodecs.CATEGORY_LIST.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Override
    protected void applyEntries(Map<ResourceLocation, SafetyData> entries) {
        // The base listener hands entries in sorted location order, which is the
        // precedence the reducer expects.
        SafetyDataReducer.Applied applied = SafetyDataReducer.reduce(entries.values(), modPresence());
        ClassificationOverrides.set(applied.overrides());
        SafetyTables.set(applied.lists());
        // Expansion happens here, where tags are already bound for this
        // reload, so the spawn path only ever reads plain id sets.
        SafetyListExpansion.rebuild(applied.lists());
        LOGGER.debug("Loaded {} classification override(s) and {} category list(s) from safety data",
                applied.overrides().size(), applied.lists().size());
    }

    private static Predicate<String> modPresence() {
        ModList modList = ModList.get();
        return modList == null ? modId -> false : modList::isLoaded;
    }
}
