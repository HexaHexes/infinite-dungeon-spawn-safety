package org.bitwisemadness.infinitedungeonspawnsafety.safety;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.bitwisemadness.infinitedungeonapi.data.InfiniteDungeonCodecs;
import org.bitwisemadness.infinitedungeonapi.data.InfiniteDungeonJsonReloadListener;
import org.slf4j.Logger;

/**
 * Loads {@code data/<namespace>/infinite_dungeon/structure_magicule/structures.json}
 * on server start and on every {@code /reload}.
 *
 * <p>Each file is one pack's list of elevation rows: a structure raises the
 * ambient magicule of the chunks around its start so the neighbourhood sustains
 * the EP baseline of the mobs related to that structure (pack rule; the values
 * themselves are datapack content shipped by the mods that own the structures).
 * Rows accumulate across namespaces - every pack contributes its own
 * structures, and none replaces another's. Malformed files are logged and
 * skipped by the API base listener, never fatal.</p>
 */
public final class StructureMagiculeReloadListener
        extends InfiniteDungeonJsonReloadListener<List<StructureElevationRow>> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String STRUCTURES_FILE = "structures";

    /** The parsed file: the rows, plus the shared required-mod gate. */
    public record StructureFile(List<StructureElevationRow> rows, Optional<String> requiredMod) {
    }

    public StructureMagiculeReloadListener() {
        super(new Gson(), "structure_magicule");
    }

    /** Registers the listener; called from the mod constructor. */
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new StructureMagiculeReloadListener());
    }

    @Override
    protected List<StructureElevationRow> parse(ResourceLocation location, JsonElement json) {
        if (!STRUCTURES_FILE.equals(location.getPath())) {
            // Anything else in the directory is ignored rather than fatal.
            return null;
        }
        StructureFile file = SafetyCodecs.STRUCTURE_FILE.parse(JsonOps.INSTANCE, json).getOrThrow();
        if (!org.bitwisemadness.infinitedungeonapi.data.RequiredModFilter.allowed(
                file.requiredMod(), modPresence())) {
            LOGGER.debug("Skipping {} - its required mod is absent", location);
            return null;
        }
        return file.rows();
    }

    @Override
    protected void applyEntries(Map<ResourceLocation, List<StructureElevationRow>> entries) {
        Map<String, List<StructureElevationRow>> byStructure = new HashMap<>();
        // Sorted location order: rows append per structure, so packs
        // accumulate rather than shadow - two rows for one structure both
        // apply (both floors lift).
        for (List<StructureElevationRow> rows : entries.values()) {
            for (StructureElevationRow row : rows) {
                byStructure.computeIfAbsent(row.structure(), key -> new ArrayList<>()).add(row);
            }
        }
        StructureElevation.set(byStructure);
        int total = byStructure.values().stream().mapToInt(List::size).sum();
        LOGGER.debug("Loaded {} structure elevation row(s) across {} structure(s)", total, byStructure.size());
    }

    private static java.util.function.Predicate<String> modPresence() {
        ModList modList = ModList.get();
        return modList == null ? modId -> false : modList::isLoaded;
    }
}
