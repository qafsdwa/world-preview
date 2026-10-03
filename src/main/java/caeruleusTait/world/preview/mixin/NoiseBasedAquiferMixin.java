package caeruleusTait.world.preview.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Aquifer.NoiseBasedAquifer.class)
public abstract class NoiseBasedAquiferMixin {

    @Final
    @Shadow
    private NoiseChunk noiseChunk;

    @Unique
    private int world_preview$sampleRegionWidth() {
        return ((NoiseChunkAccessor) this.noiseChunk).getCellCountXZ() * ((NoiseChunkAccessor) this.noiseChunk).getCellWidth();
    }

    /**
     * The preview samples noise over multiple chunks inside a single NoiseChunk,
     * but vanilla sizes the aquifer grid for the single ChunkPos it is given.
     * Extend the grid so it covers the full sampled region.
     */
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;getMaxBlockX()I")
    )
    private int fixMaxPosX(ChunkPos pos) {
        return pos.getMinBlockX() + this.world_preview$sampleRegionWidth() - 1;
    }

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;getMaxBlockZ()I")
    )
    private int fixMaxPosZ(ChunkPos pos) {
        return pos.getMinBlockZ() + this.world_preview$sampleRegionWidth() - 1;
    }
}
