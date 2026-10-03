package caeruleusTait.world.preview.client.gui.screens;

import caeruleusTait.world.preview.WorldPreview;
import caeruleusTait.world.preview.backend.storage.PreviewStorage;
import caeruleusTait.world.preview.client.WorldPreviewComponents;
import caeruleusTait.world.preview.client.gui.PreviewContainerDataProvider;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static caeruleusTait.world.preview.client.WorldPreviewComponents.WORLD_TYPE_LABEL;
import static caeruleusTait.world.preview.client.WorldPreviewComponents.LOADING_PREVIEW;
import static caeruleusTait.world.preview.client.WorldPreviewComponents.SAVING_PREVIEW;
import static caeruleusTait.world.preview.client.WorldPreviewComponents.TITLE_FULL;

/**
 * Preview for worlds the client is merely connected to (multiplayer).
 * The server's datapack worldgen configuration is not known to the client,
 * so the preview is generated from vanilla (plus client-local datapacks)
 * using a seed entered by the user.
 */
public class MultiplayerInGamePreviewScreen extends Screen implements PreviewContainerDataProvider {

    private static final List<ResourceKey<WorldPreset>> WORLD_PRESETS = List.of(
            WorldPresets.NORMAL,
            WorldPresets.LARGE_BIOMES,
            WorldPresets.AMPLIFIED,
            WorldPresets.FLAT,
            WorldPresets.SINGLE_BIOME_SURFACE
    );

    private PreviewContainer previewContainer;
    private final WorldPreview worldPreview = WorldPreview.get();
    private final Executor loadingExecutor = Executors.newFixedThreadPool(2);
    private final DirectoryValidator packValidator = Minecraft.getInstance().directoryValidator();

    private Path tempDataPackDir;
    private PackRepository tempPackRepository;

    private String currentSeed = "";
    private ResourceKey<WorldPreset> selectedWorldPreset = WorldPresets.NORMAL;

    private CycleButton<ResourceKey<WorldPreset>> worldTypeButton;

    public MultiplayerInGamePreviewScreen() {
        super(TITLE_FULL);
    }

    @Override
    protected void init() {
        if (previewContainer == null) {
            previewContainer = new PreviewContainer(this, this);
            previewContainer.start();
        }

        previewContainer.widgets().forEach(this::addRenderableWidget);
        previewContainer.doLayout(new ScreenRectangle(0, 38, width, height - 58));

        worldTypeButton = CycleButton
                .builder(MultiplayerInGamePreviewScreen::presetLabel, selectedWorldPreset)
                .withValues(WORLD_PRESETS)
                .create(0, 20, 150, 20, WORLD_TYPE_LABEL, (btn, val) -> {
                    if (selectedWorldPreset != val) {
                        selectedWorldPreset = val;
                        previewContainer.refreshSettings();
                    }
                });
        worldTypeButton.setPosition(4, 20);
        addRenderableWidget(worldTypeButton);

        Button btn = Button
                .builder(CommonComponents.GUI_BACK, x -> onClose())
                .width(100)
                .pos(width / 2 - 50, height - 24)
                .build();
        addRenderableWidget(btn);
    }

    private static Component presetLabel(ResourceKey<WorldPreset> key) {
        return Component.translatable(key.identifier().toLanguageKey("generator"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(minecraft.font, TITLE_FULL, width / 2, 6, 0xFFFFFF);
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                FOOTER_SEPARATOR,
                0,
                Mth.roundToward(this.height - 30, 2),
                0.0F,
                0.0F,
                this.width,
                2,
                32,
                2
        );
    }

    @Override
    public void onClose() {
        worldPreview.saveConfig();
        previewContainer.close();
        super.onClose();
    }

    @Nullable
    private PackRepository getOrCreatePackRepository() {
        if (tempDataPackDir == null) {
            try {
                tempDataPackDir = Files.createTempDirectory("mcworld-");
            } catch (IOException e) {
                return null;
            }
        }
        if (tempPackRepository == null) {
            tempPackRepository = ServerPacksSource.createPackRepository(tempDataPackDir, packValidator);
            tempPackRepository.reload();
        }
        return tempPackRepository;
    }

    private record PreviewCookie(WorldGenSettings worldGenSettings, WorldDataConfiguration worldDataConfiguration) {}

    @Override
    public @Nullable WorldCreationContext previewWorldCreationContext() {
        PackRepository packRepository = getOrCreatePackRepository();
        if (packRepository == null) {
            return null;
        }

        final WorldDataConfiguration worldDataConfiguration = WorldDataConfiguration.DEFAULT;

        final OptionalLong parsedSeed = WorldOptions.parseSeed(currentSeed);
        final WorldOptions worldOptions = parsedSeed.isPresent()
                ? new WorldOptions(parsedSeed.getAsLong(), true, false)
                : WorldOptions.defaultWithRandomSeed();

        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, worldDataConfiguration, false, true);
        WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, LevelBasedPermissionSet.GAMEMASTER);
        CompletableFuture<WorldCreationContext> completableFuture = WorldLoader.load(
                initConfig,
                dataLoadContext -> {
                    WorldDimensions worldDimensions;
                    try {
                        WorldPreset worldPreset = dataLoadContext.datapackWorldgen()
                                .lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(selectedWorldPreset)
                                .value();
                        worldDimensions = worldPreset.createWorldDimensions();
                    } catch (Exception ex) {
                        worldDimensions = WorldPresets.createNormalWorldDimensions(dataLoadContext.datapackWorldgen());
                    }
                    WorldGenSettings worldGenSettings = new WorldGenSettings(worldOptions, worldDimensions);
                    return new WorldLoader.DataLoadOutput<>(
                            new PreviewCookie(worldGenSettings, worldDataConfiguration),
                            dataLoadContext.datapackDimensions()
                    );
                },
                (closeableResourceManager, reloadableServerResources, layeredRegistryAccess, cookie) -> {
                    closeableResourceManager.close();
                    return new WorldCreationContext(
                            cookie.worldGenSettings(),
                            layeredRegistryAccess,
                            reloadableServerResources,
                            cookie.worldDataConfiguration()
                    );
                },
                loadingExecutor,
                loadingExecutor
        );

        try {
            return completableFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path cacheDir() {
        final Path previewDir = worldPreview.configDir().resolve("world-preview");
        previewDir.toFile().mkdirs();
        return previewDir;
    }

    private String filename() {
        return String.format("%s-%s.zip", currentSeed, cacheFileCompatPart());
    }

    @Override
    public void storePreviewStorage(long seed, PreviewStorage storage) {
        if (!worldPreview.cfg().cacheInGame) {
            return;
        }
        minecraft.setScreen(new PreviewCacheLoadingScreen(SAVING_PREVIEW));
        writeCacheFile(previewContainer.workManager().previewStorage(), cacheDir().resolve(filename()));
        minecraft.setScreen(this);
    }

    @Override
    public PreviewStorage loadPreviewStorage(long seed, int yMin, int yMax) {
        if (!worldPreview.cfg().cacheInGame) {
            return new PreviewStorage(yMin, yMax);
        }

        minecraft.setScreen(new PreviewCacheLoadingScreen(LOADING_PREVIEW));
        final PreviewStorage res = readCacheFile(yMin, yMax, cacheDir().resolve(filename()));
        minecraft.setScreen(this);
        return res;
    }

    @Override
    public void registerSettingsChangeListener(Runnable listener) {
        // Nothing to do
    }

    @Override
    public String seed() {
        return currentSeed;
    }

    @Override
    public void updateSeed(String newSeed) {
        currentSeed = newSeed;
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
        WorldDimensions.Complete worldDimensions = wcContext.selectedDimensions().bake(wcContext.datapackDimensions());
        return worldDimensions.dimensions();
    }

    @Override
    public LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess(@Nullable WorldCreationContext wcContext) {
        if (wcContext == null) throw new AssertionError();
        WorldDimensions.Complete worldDimensions = wcContext.selectedDimensions().bake(wcContext.datapackDimensions());
        return wcContext
                .worldgenRegistries()
                .replaceFrom(RegistryLayer.DIMENSIONS, worldDimensions.dimensionsRegistryAccess());
    }
}
