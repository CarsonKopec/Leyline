package dev.leyline.client.render;

import dev.leyline.Leyline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * The lights Leyline casts on the world: {@link #add} one, and close its handle to put it out. They light what the
 * world drew, after the world is drawn and before Leyline's shapes, smoothly and without shadows: a light reaches
 * through walls within its radius. Everything belongs to the world it was made in, and leaving that world puts it out.
 * Render thread only.
 */
public final class Lights {
    /** Lights further than this from the camera aren't drawn. */
    public static final double RENDER_DISTANCE = Shapes.RENDER_DISTANCE;

    private static final List<LightHandle<?>> HANDLES = new ArrayList<>();
    private static ClientLevel level;
    private static AfterWorld.Registration registration;

    private Lights() {
    }

    public static <L extends Light> LightHandle<L> add(L light) {
        ensure();
        LightHandle<L> handle = new LightHandle<>(light);
        HANDLES.add(handle);
        return handle;
    }

    /** How many lights are lit: for tests. */
    public static int count() {
        return HANDLES.size();
    }

    /** How many lights the last frame drew: for tests. */
    public static int drawn() {
        return LightRenderer.drawn();
    }

    /** Puts every light out. */
    public static void clear() {
        HANDLES.forEach(LightHandle::kill);
        HANDLES.clear();
    }

    static void forget(LightHandle<?> handle) {
        HANDLES.remove(handle);
    }

    static List<LightHandle<?>> handles() {
        return HANDLES;
    }

    static void syncLevel() {
        ClientLevel now = Minecraft.getInstance().level;
        if (now != level) {
            clear();
            level = now;
        }
    }

    private static void ensure() {
        syncLevel();
        if (registration == null) registration = AfterWorld.register(Leyline.id("lights"), LightRenderer.PRIORITY, LightRenderer::draw);
    }
}
