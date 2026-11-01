package dev.leyline.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drawing into the finished frame: run once the whole world is drawn (terrain, entities, particles, weather and clouds),
 * before the hand and HUD, while the world's depth is still in the main target. Leyline's lights, shapes and screen
 * passes all draw from here, each at its priority, lowest first. Render thread only.
 */
public final class AfterWorld {
    private static final Logger LOGGER = LoggerFactory.getLogger("Leyline");
    private static final List<Registration> DRAWERS = new ArrayList<>();
    private static Registration[] snapshot = new Registration[0];

    private AfterWorld() {
    }

    /** Draws into the frame. */
    @FunctionalInterface
    public interface Drawer {
        void draw(Frame frame);
    }

    /**
     * The frame being drawn: the main target, holding the finished world and its depth; the camera it was drawn from;
     * and how far the frame is between ticks.
     */
    public record Frame(RenderTarget main, Camera camera, float partialTick) {
    }

    /** One drawer's place in the order. {@link #remove} takes it out. */
    public static final class Registration {
        private final Identifier id;
        private final int priority;
        private final Drawer drawer;
        private boolean removed;

        private Registration(Identifier id, int priority, Drawer drawer) {
            this.id = id;
            this.priority = priority;
            this.drawer = drawer;
        }

        public Identifier id() {
            return id;
        }

        public void remove() {
            if (removed) return;
            removed = true;
            DRAWERS.remove(this);
            snapshot = DRAWERS.toArray(Registration[]::new);
        }
    }

    /** Adds {@code drawer}, named {@code id}, to run every frame at {@code priority} until it's removed. */
    public static Registration register(Identifier id, int priority, Drawer drawer) {
        Registration registration = new Registration(id, priority, drawer);
        DRAWERS.add(registration);
        DRAWERS.sort(Comparator.comparingInt(r -> r.priority));
        snapshot = DRAWERS.toArray(Registration[]::new);
        return registration;
    }

    /** Whether a drawer named {@code id} is running. */
    public static boolean isRegistered(Identifier id) {
        for (Registration registration : snapshot) {
            if (registration.id.equals(id)) return true;
        }
        return false;
    }

    /**
     * Called by Leyline's mixin into world rendering. A drawer that throws is logged and removed, so one broken effect
     * can't take the frame, or the game, down with it.
     */
    public static void draw(DeltaTracker deltaTracker) {
        Registration[] drawers = snapshot;
        if (drawers.length == 0) return;
        Minecraft client = Minecraft.getInstance();
        Frame frame = new Frame(client.getMainRenderTarget(), client.gameRenderer.getMainCamera(),
                deltaTracker.getGameTimeDeltaPartialTick(true));
        for (Registration registration : drawers) {
            if (registration.removed) continue;
            try {
                registration.drawer.draw(frame);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.error("Drawing {} failed; it's off until it starts again", registration.id, e);
                registration.remove();
            }
        }
    }
}
