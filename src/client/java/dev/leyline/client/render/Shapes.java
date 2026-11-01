package dev.leyline.client.render;

import dev.leyline.Leyline;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * The shapes Leyline draws in the world: {@link #add} one to draw until it's removed, or {@link #spawn} one for a few
 * ticks. They are drawn after the world, hidden by terrain in front of them, in one batch on the GPU. Everything
 * belongs to the world it was made in, and leaving that world clears it. Render thread only.
 */
public final class Shapes {
    /** Shapes further than this from the camera aren't drawn. */
    public static final double RENDER_DISTANCE = 448;

    private static final List<ShapeHandle<?>> HANDLES = new ArrayList<>();
    private static final List<Timed> TIMED = new ArrayList<>();
    private static final Set<Planes> PLANES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static ClientLevel level;
    private static AfterWorld.Registration registration;

    private Shapes() {
    }

    /** A shape drawn for a while: {@code ticks} long, fading out over the second half. */
    record Timed(Shape shape, float start, int ticks) {
    }

    /** Draws {@code shape} until its handle is removed. */
    public static <S extends Shape> ShapeHandle<S> add(S shape) {
        ensure();
        ShapeHandle<S> handle = new ShapeHandle<>(shape);
        HANDLES.add(handle);
        return handle;
    }

    /** Draws {@code shape} for {@code ticks} ticks, fading it out over the second half. */
    public static void spawn(Shape shape, int ticks) {
        ensure();
        if (level == null || ticks <= 0) return;
        TIMED.add(new Timed(shape, level.getGameTime(), ticks));
    }

    /** How many shapes, and sets of planes, are live: for tests. */
    public static int count() {
        return HANDLES.size() + TIMED.size() + PLANES.size();
    }

    /** How many shapes the last frame drew: for tests. */
    public static int drawn() {
        return ShapeRenderer.drawn();
    }

    /** Stops drawing everything. */
    public static void clear() {
        HANDLES.forEach(ShapeHandle::kill);
        HANDLES.clear();
        TIMED.clear();
        PLANES.clear();
    }

    static void forget(ShapeHandle<?> handle) {
        HANDLES.remove(handle);
    }

    static void showPlanes(Planes planes, boolean show) {
        if (show) {
            ensure();
            PLANES.add(planes);
        } else {
            PLANES.remove(planes);
        }
    }

    static List<ShapeHandle<?>> handles() {
        return HANDLES;
    }

    static List<Timed> timed() {
        return TIMED;
    }

    static Set<Planes> planes() {
        return PLANES;
    }

    /** Clears everything once the player is in another world, or none. */
    static void syncLevel() {
        ClientLevel now = Minecraft.getInstance().level;
        if (now != level) {
            clear();
            level = now;
        }
    }

    private static void ensure() {
        syncLevel();
        if (registration == null) registration = AfterWorld.register(Leyline.id("shapes"), ShapeRenderer.PRIORITY, ShapeRenderer::draw);
    }
}
