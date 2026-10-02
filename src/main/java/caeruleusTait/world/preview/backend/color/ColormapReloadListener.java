package caeruleusTait.world.preview.backend.color;

import caeruleusTait.world.preview.WorldPreview;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.Map;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;

public class ColormapReloadListener extends BaseMultiJsonResourceReloadListener {

    public ColormapReloadListener() {
        super(null);
    }

    @Override
    protected Map<Identifier, List<JsonElement>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return scanDirectory(resourceManager, "colormap_preview");
    }

    @Override
    protected void apply(Map<Identifier, List<JsonElement>> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        final WorldPreview worldPreview = WorldPreview.get();
        final PreviewMappingData previewMappingData = worldPreview.biomeColorMap();
        previewMappingData.clearColorMappings();

        LOGGER.debug("Loading colormaps:");
        for (Map.Entry<Identifier, List<JsonElement>> entry : object.entrySet()) {
            for (JsonElement jsonElement : entry.getValue()) {
                final ColorMap.RawColorMap value = GSON.fromJson(jsonElement, ColorMap.RawColorMap.class);
                LOGGER.debug(" - {}: {} | {} entries", entry.getKey(), value.name(), value.data().size());
                previewMappingData.addColormap(new ColorMap(entry.getKey(), value));
            }
        }
    }
}
