package dev.leyline.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.nexusrealms.nebulon.api.render.client.NebulonRenderSystem;
import de.nexusrealms.nebulon.api.render.client.RenderStage;
import de.nexusrealms.nebulon.api.render.client.post.NebulonPostProcessing;
import de.nexusrealms.nebulon.api.render.client.post.PostPassContext;
import de.nexusrealms.nebulon.api.render.client.post.PostPassHandle;
import dev.leyline.client.fx.NebulonGuard;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A full-screen shader pass drawn straight into the finished frame, from Nebulon's post-processing hook after the world
 * is drawn and before the hand and HUD: for what flat shapes and particles can't draw, such as light bent round a black
 * hole or the colour drained from part of the world. It only runs while something wants it ({@link #run}, every tick),
 * so it costs nothing the rest of the time. With a shader pack or VulkanMod, or once it or Nebulon has failed, it isn't
 * {@linkplain #available available}: effects check that, and draw themselves another way.
 *
 * <p>{@link ScreenPasses} has the pieces to draw one with.
 */
public final class ScreenPass {
    private static final Logger LOGGER = LoggerFactory.getLogger("Leyline");
    private static final boolean VULKAN_MOD = FabricLoader.getInstance().isModLoaded("vulkanmod");

    private final Identifier id;
    private final int priority;
    private final Consumer<PostPassContext> render;
    private final Runnable release;
    private final String description;
    private PostPassHandle handle;
    /** Set by the first failure; the pass then stays off until the game restarts. */
    private boolean failed;

    /**
     * A pass named {@code id}, drawn each frame it runs by {@code render}, in {@code priority} order among Nebulon's
     * passes at the end of the world's drawing. {@code release} lets go of its textures when it stops, and
     * {@code description} names it in the log if it fails ("Planetfall's screen pass").
     */
    public ScreenPass(Identifier id, int priority, Consumer<PostPassContext> render, Runnable release, String description) {
        this.id = id;
        this.priority = priority;
        this.render = render;
        this.release = release;
        this.description = description;
    }

    /**
     * Whether the pass can run here: not with VulkanMod, which replaces the OpenGL renderer it draws through, nor with a
     * shader pack, nor once it or Nebulon has failed.
     */
    public boolean available() {
        if (failed || NebulonGuard.disabled() || VULKAN_MOD) return false;
        try {
            return !NebulonRenderSystem.environment().irisShaderPackActive();
        } catch (RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    /** Called every tick, once everything that might want the pass has asked: starts it if {@code wanted}, or stops it. */
    public void run(boolean wanted) {
        if (wanted && available()) {
            if (handle == null) NebulonGuard.cleanup(() -> handle = NebulonPostProcessing.register(id, RenderStage.END_MAIN, priority, this::draw));
        } else if (handle != null) {
            close();
        }
    }

    /** Stops the pass and lets go of its textures. Safe from any thread: a disconnect clears effects from the network thread. */
    public void stop() {
        if (RenderSystem.isOnRenderThread()) close();
        else Minecraft.getInstance().execute(this::close);
    }

    /** Logs the first failure, and switches the pass off until the game restarts. */
    public void fail(Throwable e) {
        if (failed) return;
        failed = true;
        LOGGER.error("{} failed; it's off until the game restarts, and what it drew is drawn without it", description, e);
    }

    private void close() {
        PostPassHandle closing = handle;
        handle = null;
        if (closing != null) NebulonGuard.cleanup(closing::close);
        NebulonGuard.cleanup(release);
    }

    /** Runs inside world rendering. Any failure is logged once and switches the pass off. */
    private void draw(PostPassContext context) {
        try {
            render.accept(context);
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }
}
