package dev.leyline.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.leyline.client.render.AfterWorld;
import dev.leyline.client.render.RenderCompat;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A full-screen shader pass drawn straight into the finished frame, after the world is drawn and before the hand and
 * HUD ({@link AfterWorld}): for what flat shapes and particles can't draw, such as light bent round a black hole or the
 * colour drained from part of the world. It only runs while something wants it ({@link #run}, every tick), so it costs
 * nothing the rest of the time. With a shader pack or VulkanMod, or once it has failed, it isn't
 * {@linkplain #available available}: effects check that, and draw themselves another way.
 *
 * <p>{@link ScreenPasses} has the pieces to draw one with.
 */
public final class ScreenPass {
    private static final Logger LOGGER = LoggerFactory.getLogger("Leyline");

    private final Identifier id;
    private final int priority;
    private final Consumer<AfterWorld.Frame> render;
    private final Runnable release;
    private final String description;
    private AfterWorld.Registration registration;
    /** Set by the first failure; the pass then stays off until the game restarts. */
    private boolean failed;

    /**
     * A pass named {@code id}, drawn each frame it runs by {@code render}, in {@code priority} order among everything
     * drawn after the world (lowest first; Leyline's lights and shapes come before any pass at 0 or above).
     * {@code release} lets go of its textures when it stops, and {@code description} names it in the log if it fails
     * ("Planetfall's screen pass").
     */
    public ScreenPass(Identifier id, int priority, Consumer<AfterWorld.Frame> render, Runnable release, String description) {
        this.id = id;
        this.priority = priority;
        this.render = render;
        this.release = release;
        this.description = description;
    }

    /** Whether a pass named {@code id} is running: drawn every frame until it stops. */
    public static boolean running(Identifier id) {
        return AfterWorld.isRegistered(id);
    }

    /** Whether the pass can run here: not with VulkanMod or a shader pack ({@link RenderCompat}), nor once it has failed. */
    public boolean available() {
        return !failed && RenderCompat.drawable();
    }

    /** Called every tick, once everything that might want the pass has asked: starts it if {@code wanted}, or stops it. */
    public void run(boolean wanted) {
        if (wanted && available()) {
            if (registration == null) registration = AfterWorld.register(id, priority, this::draw);
        } else if (registration != null) {
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
        AfterWorld.Registration closing = registration;
        registration = null;
        if (closing != null) closing.remove();
        try {
            release.run();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("{} failed to let go of its textures", description, e);
        }
    }

    /** Runs inside world rendering. Any failure is logged once and switches the pass off. */
    private void draw(AfterWorld.Frame frame) {
        if (failed) return;
        try {
            render.accept(frame);
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }
}
