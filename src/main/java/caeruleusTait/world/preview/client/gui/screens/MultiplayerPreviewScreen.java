package caeruleusTait.world.preview.client.gui.screens;

import caeruleusTait.world.preview.WorldPreview;
import caeruleusTait.world.preview.backend.storage.PreviewStorage;
import caeruleusTait.world.preview.client.WorldPreviewComponents;
import caeruleusTait.world.preview.client.gui.PreviewContainerDataProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static caeruleusTait.world.preview.client.WorldPreviewComponents.LOADING_PREVIEW;
import static caeruleusTait.world.preview.client.WorldPreviewComponents.SAVING_PREVIEW;
import static net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.FOOTER_SEPERATOR;

/**
 * Preview screen for Multiplayer mode. Since no integrated server exists, the preview
 * rebuilds a fresh world locally from a user-provided seed (like the create-world tab).
 */
public class MultiplayerPreviewScreen extends Screen implements PreviewContainerDataProvider {

    private final WorldPreview worldPreview = WorldPreview.get();
    private PreviewContainer previewContainer;
    private WorldCreationContext previewContext;
    private PackRepository packRepository;
    private Path tempDataPackDir;
    private String seed = previewInitialSeed();

    private final Executor loadingExecutor = Executors.newFixedThreadPool(2);

    public MultiplayerPreviewScreen() {
        super(WorldPreviewComponents.TITLE_FULL);
    }

    private String previewInitialSeed() {
        final List<String> saved = worldPreview.cfg().savedSeeds;
        return saved.isEmpty() ? "" : saved.get(saved.size() - 1);
    }

    @Override
    protected void init() {
        if (tempDataPackDir == null) {
            try {
                tempDataPackDir = Files.createTempDirectory("world_preview_mp");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (packRepository == null) {
            packRepository = WorldGenContextLoader.createPackRepository(tempDataPackDir);
        }

        if (previewContainer == null) {
            previewContainer = new PreviewContainer(this, this);
            previewContainer.start();
        }

        previewContainer.widgets().forEach(this::addRenderableWidget);
        previewContainer.doLayout(new ScreenRectangle(0, 18, width, height - 38));

        Button btn = Button
                .builder(CommonComponents.GUI_BACK, x -> onClose())
                .width(100)
                .pos(width / 2 - 50, height - 24)
                .build();
        addRenderableWidget(btn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderDirtBackground(guiGraphics);
        guiGraphics.drawCenteredString(minecraft.font, WorldPreviewComponents.TITLE_FULL, width / 2, 6, 0xFFFFFF);
        guiGraphics.blit(FOOTER_SEPERATOR, 0, Mth.roundToward(this.height - 30, 2), 0.0F, 0.0F, this.width, 2, 32, 2);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        worldPreview.saveConfig();
        previewContainer.close();
        super.onClose();
    }

    @Override
    public @Nullable WorldCreationContext previewWorldCreationContext() {
        if (previewContext == null) {
            reloadPreviewContext();
        }
        return previewContext;
    }

    private void reloadPreviewContext() {
        WorldDataConfiguration worldDataConfiguration = WorldDataConfiguration.DEFAULT;
        WorldOptions options = seed.isEmpty() ? WorldOptions.defaultWithRandomSeed() : new WorldOptions(WorldOptions.parseSeed(seed).orElseThrow(), true, false);

        CompletableFuture<WorldCreationContext> future = WorldGenContextLoader.load(
                worldDataConfiguration,
                packRepository,
                dataLoadContext -> {
                    var worldDimensions = WorldPresets.createNormalWorldDimensions(dataLoadContext.datapackWorldgen());
                    return new WorldGenSettings(options, worldDimensions);
                },
                loadingExecutor,
                loadingExecutor
        );

        try {
            previewContext = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerSettingsChangeListener(Runnable listener) {
        // No WorldCreationUiState available; nothing to register
    }

    @Override
    public String seed() {
        return seed;
    }

    @Override
    public void updateSeed(String newSeed) {
        seed = newSeed;
        previewContext = null;
    }

    @Override
    public boolean seedIsEditable() {
        return true;
    }

    @Override
    public @Nullable Path tempDataPackDir() {
        return tempDataPackDir;
    }

    @Override
    public @Nullable MinecraftServer minecraftServer() {
        return null;
    }

    @Override
    public WorldOptions worldOptions(@Nullable WorldCreationContext wcContext) {
        if (wcContext == null) throw new AssertionError();
        return wcContext.options();
    }

    @Override
    public WorldDataConfiguration worldDataConfiguration(@Nullable WorldCreationContext wcContext) {
        if (wcContext == null) throw new AssertionError();
        return wcContext.dataConfiguration();
    }

    @Override
    public RegistryAccess.Frozen registryAccess(@Nullable WorldCreationContext wcContext) {
        if (wcContext == null) throw new AssertionError();
        return wcContext.worldgenLoadContext();
    }

    @Override
    public Registry<LevelStem> levelStemRegistry(@Nullable WorldCreationContext wcContext) {
        if (wcContext == null) throw new AssertionError();
        WorldDimensions.Complete complete = wcContext.selectedDimensions().bake(wcContext.datapackDimensions());
        return complete.dimensions();
    }

    @Override
    public LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess(@Nullable WorldCreationContext wcContext) {
        if (wcContext == null) throw new AssertionError();
        WorldDimensions.Complete complete = wcContext.selectedDimensions().bake(wcContext.datapackDimensions());
        return wcContext
                .worldgenRegistries()
                .replaceFrom(RegistryLayer.DIMENSIONS, complete.dimensionsRegistryAccess());
    }

    @Override
    public Path cacheDir() {
        final Path previewDir = worldPreview.configDir().resolve("world-preview");
        previewDir.toFile().mkdirs();
        return previewDir;
    }

    private String filename() {
        return String.format("%s.zip", cacheFileCompatPart());
    }

    @Override
    public void storePreviewStorage(long seed, PreviewStorage storage) {
        if (!worldPreview.cfg().cacheInGame) {
            return;
        }
        minecraft.forceSetScreen(new PreviewCacheLoadingScreen(SAVING_PREVIEW));
        writeCacheFile(previewContainer.workManager().previewStorage(), cacheDir().resolve(filename()));
    }

    @Override
    public PreviewStorage loadPreviewStorage(long seed, int yMin, int yMax) {
        if (!worldPreview.cfg().cacheInGame) {
            return new PreviewStorage(yMin, yMax);
        }
        minecraft.forceSetScreen(new PreviewCacheLoadingScreen(LOADING_PREVIEW));
        final PreviewStorage res = readCacheFile(yMin, yMax, cacheDir().resolve(filename()));
        minecraft.forceSetScreen(this);
        return res;
    }
}