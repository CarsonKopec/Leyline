package dev.leyline.client.render;

import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Whether Leyline can draw into the frame itself. Not with VulkanMod, which replaces the OpenGL renderer it draws
 * through, nor while Iris runs a shader pack, which draws the world its own way. Effects check this and draw
 * themselves another way, with particles.
 */
public final class RenderCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("Leyline");
    private static final boolean VULKAN_MOD = FabricLoader.getInstance().isModLoaded("vulkanmod");
    private static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");
    private static Object irisApi;
    private static Method shaderPackInUse;
    private static boolean irisBroken;

    private RenderCompat() {
    }

    /** Whether Leyline's own drawing can run this frame. */
    public static boolean drawable() {
        return !VULKAN_MOD && !shaderPackInUse();
    }

    /** Whether Iris is running a shader pack. If Iris is installed but can't be asked, assumes it is, to be safe. */
    public static boolean shaderPackInUse() {
        if (!IRIS || irisBroken) return IRIS;
        try {
            if (shaderPackInUse == null) {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                shaderPackInUse = api.getMethod("isShaderPackInUse");
            }
            return (boolean) shaderPackInUse.invoke(irisApi);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            irisBroken = true;
            LOGGER.warn("Couldn't ask Iris whether a shader pack is on; Leyline's drawing stays off while Iris is installed", e);
            return true;
        }
    }
}
