package caeruleusTait.world.preview.backend.color;

import caeruleusTait.world.preview.WorldPreview;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.Map;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;

public class HeightmapPresetReloadListener extends BaseMultiJsonResourceReloadListener {

    public HeightmapPresetReloadListener() {
        super(null);
    }

    @Override
    protected Map<Identifier, List<JsonElement>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return scanDirectory(resourceManager, "heightmap_preview_presets");
    }

    @Override
    protected void apply(Map<Identifier, List<JsonElement>> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        final WorldPreview worldPreview = WorldPreview.get();
        final PreviewMappingData previewMappingData = worldPreview.biomeColorMap();
        previewMappingData.clearHeightmapPresets();

        LOGGER.debug("Loading heightmap presets:");
        for (Map.Entry<Identifier, List<JsonElement>> entry : object.entrySet()) {
            for (JsonElement jsonElement : entry.getValue()) {
                final PreviewData.HeightmapPresetData value = GSON.fromJson(jsonElement, PreviewData.HeightmapPresetData.class);
                LOGGER.debug(" - {}: {} | {} to {}", entry.getKey(), value.name(), value.minY(), value.maxY());
                previewMappingData.addHeightmapPreset(value);
            }
        }
    }
}
