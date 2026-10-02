package caeruleusTait.world.preview.backend.stubs;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.datafixers.DataFixer;
import net.minecraft.SystemReport;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.NotNull;

import java.net.Proxy;
import java.util.Optional;
import java.util.UUID;

public class DummyMinecraftServer extends MinecraftServer {
    public DummyMinecraftServer(
            Thread thread,
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            PackRepository packRepository,
            WorldStem worldStem,
            Proxy proxy,
            DataFixer dataFixer
    ) {
        super(
                thread,
                levelStorageAccess,
                packRepository,
                worldStem,
                Optional.empty(),
                proxy,
                dataFixer,
                new Services(null, ServicesKeySet.EMPTY, null, null, null),
                new LevelLoadListener() {
                    @Override
                    public void start(LevelLoadListener.Stage stage, int totalChunks) {
                    }

                    @Override
                    public void update(LevelLoadListener.Stage stage, int currentChunks, int totalChunks) {
                    }

                    @Override
                    public void finish(LevelLoadListener.Stage stage) {
                    }

                    @Override
                    public void updateFocus(ResourceKey<Level> dimension, ChunkPos chunkPos) {
                    }
                },
                false
        );
        this.setSingleplayerProfile(new GameProfile(UUID.randomUUID(), "world-preview"));
        this.setDemo(false);
        this.setPlayerList(new DummyPlayerList(this, this.registries(), this.playerDataStorage));
    }

    @Override
    protected boolean initServer() {
        return false;
    }

    @Override
    public LevelBasedPermissionSet operatorUserPermissions() {
        return LevelBasedPermissionSet.ALL;
    }

    @Override
    public PermissionSet getFunctionCompilationPermissions() {
        return PermissionSet.NO_PERMISSIONS;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    protected SampleLogger getTickTimeLogger() {
        return new SampleLogger() {
            @Override
            public void logFullSample(long[] ls) {
            }

            @Override
            public void logSample(long l) {
            }

            @Override
            public void logPartialSample(long l, int i) {
            }
        };
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return false;
    }

    @Override
    public @NotNull SystemReport fillServerSystemReport(@NotNull SystemReport report) {
        return report;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getMaxPlayers() {
        return 1;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean useNativeTransport() {
        return false;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(@NotNull NameAndId nameAndId) {
        return false;
    }
}
