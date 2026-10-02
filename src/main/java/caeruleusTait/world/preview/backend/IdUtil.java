package caeruleusTait.world.preview.backend;

import net.minecraft.resources.Identifier;

public final class IdUtil {
    private IdUtil() {
    }

    /**
     * Same semantics as the old {@code new ResourceLocation(String)}: a missing
     * namespace is treated as the default ("minecraft") namespace.
     */
    public static Identifier parse(String s) {
        return s.indexOf(':') < 0 ? Identifier.withDefaultNamespace(s) : Identifier.parse(s);
    }
}
