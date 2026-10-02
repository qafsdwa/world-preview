package caeruleusTait.world.preview.backend.color;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseMultiJsonResourceReloadListener extends SimplePreparableReloadListener<Map<Identifier, List<JsonElement>>> {
    protected static final Gson GSON = (new GsonBuilder()).create();

    private final String filename;

    protected BaseMultiJsonResourceReloadListener(String filename) {
        this.filename = filename;
    }

    @Override
    protected Map<Identifier, List<JsonElement>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, List<JsonElement>> res = new HashMap<>();

        for (String namespace : resourceManager.getNamespaces()) {
            loadAllForLocation(resourceManager, res, Identifier.fromNamespaceAndPath(namespace, filename));
        }

        loadAllForLocation(resourceManager, res, Identifier.fromNamespaceAndPath("c", "worldgen/" + filename));

        return res;
    }

    private void loadAllForLocation(ResourceManager resourceManager, Map<Identifier, List<JsonElement>> res, Identifier rl) {
        for (Resource x : resourceManager.getResourceStack(rl)) {
            try (Reader reader = x.openAsReader()) {
                final List<JsonElement> jsonElements = res.computeIfAbsent(rl, z -> new ArrayList<>());
                jsonElements.add(GsonHelper.fromJson(GSON, reader, JsonElement.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Scans every {@code <dir>/**.json} file in every namespace, keyed by
     * {@code <ns>:<relative path without dir and extension>} -- the old
     * {@code SimpleJsonResourceReloadListener(gson, dir)} behaviour.
     */
    protected Map<Identifier, List<JsonElement>> scanDirectory(ResourceManager resourceManager, String directory) {
        Map<Identifier, List<JsonElement>> res = new HashMap<>();
        final String prefix = directory + "/";
        resourceManager.listResources(directory, p -> p.getPath().endsWith(".json")).forEach((location, resource) -> {
            final String path = location.getPath();
            if (!path.startsWith(prefix) || path.length() <= prefix.length() + ".json".length()) {
                return;
            }
            final Identifier id = Identifier.fromNamespaceAndPath(
                    location.getNamespace(),
                    path.substring(prefix.length(), path.length() - ".json".length())
            );
            try (Reader reader = resource.openAsReader()) {
                final List<JsonElement> jsonElements = res.computeIfAbsent(id, z -> new ArrayList<>());
                jsonElements.add(GsonHelper.fromJson(GSON, reader, JsonElement.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return res;
    }
}
