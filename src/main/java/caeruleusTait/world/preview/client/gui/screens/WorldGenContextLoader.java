package caeruleusTait.world.preview.client.gui.screens;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldGenSettings;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class WorldGenContextLoader {

    record Cookie(WorldGenSettings worldGenSettings) {}

    private WorldGenContextLoader() {}

    public static PackRepository createPackRepository(Path dataPackDir) {
        return ServerPacksSource.createPackRepository(dataPackDir);
    }

    /**
     * Load a {@link WorldCreationContext} using the provided function to build the
     * {@link WorldGenSettings}. This is shared between the create-world screen tab and
     * the in-multiplayer preview screen so both rebuild a "fresh world" locally.
     */
    public static CompletableFuture<WorldCreationContext> load(
            WorldDataConfiguration worldDataConfiguration,
            PackRepository packRepository,
            Function<WorldLoader.DataLoadContext, WorldGenSettings> settingsFn,
            Executor ioExecutor,
            Executor backgroundExecutor
    ) {
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, worldDataConfiguration, false, true);
        WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, 2);

        return WorldLoader.<Cookie, WorldCreationContext>load(
                initConfig,
                dataLoadContext -> {
                    WorldGenSettings worldGenSettings = settingsFn.apply(dataLoadContext);
                    RegistryAccess.Frozen finalDimensions = dataLoadContext.datapackDimensions();
                    return new WorldLoader.DataLoadOutput<>(new Cookie(worldGenSettings), finalDimensions);
                },
                (closeableResourceManager, reloadableServerResources, layeredRegistryAccess, cookie) -> {
                    closeableResourceManager.close();
                    return new WorldCreationContext(
                            cookie.worldGenSettings,
                            layeredRegistryAccess,
                            reloadableServerResources,
                            worldDataConfiguration
                    );
                },
                ioExecutor,
                backgroundExecutor
        );
    }
}