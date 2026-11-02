package dev.leyline.client.fx;

import dev.leyline.client.render.Light;
import dev.leyline.client.render.LightHandle;
import dev.leyline.client.render.Lights;
import dev.leyline.client.render.Planes;
import dev.leyline.client.render.PointLight;
import dev.leyline.client.render.Rgba;
import dev.leyline.client.render.RuneCircle;
import dev.leyline.client.render.Shape;
import dev.leyline.client.render.ShapeHandle;
import dev.leyline.client.render.Shapes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Tools for drawing effects: Leyline's {@linkplain Shapes shapes} and {@linkplain Lights lights} that update in place,
 * particles that follow the Particles setting, and {@link Tracker}s that run each effect's animations. Flat shapes
 * redrawn every tick go through {@link Planes}.
 */
public final class Fx {
    public static final Vec3 UP = new Vec3(0, 1, 0), DOWN = new Vec3(0, -1, 0);
    /** How far away lights and shapes are drawn. */
    public static final double RENDER_DISTANCE = Shapes.RENDER_DISTANCE;
    /** Effects this far from the camera spawn no particles; their lights and shapes still show. */
    public static final double PARTICLE_RANGE = 192;

    private Fx() {
    }

    public static ClientLevel world() {
        return Minecraft.getInstance().level;
    }

    public static Vec3 camera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().position();
    }

    /** 1 within 48 blocks of the camera, fading to 0 at 192: how hard screen effects hit. */
    public static float proximity(Vec3 at) {
        return (float) Mth.clamp(1 - (camera().distanceTo(at) - 48) / 144, 0, 1);
    }

    /** Eases out: fast at first, settling at 1. */
    public static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    /** The colour at {@code f} of its strength: every channel scaled, as colours are premultiplied ({@link Rgba}). */
    public static Rgba scaled(Rgba color, float f) {
        f = Mth.clamp(f, 0, 1);
        return new Rgba(color.red() * f, color.green() * f, color.blue() * f, color.alpha() * f);
    }

    /** The colour, {@code amount} of the way to white. */
    public static Rgba whiter(Rgba color, float amount) {
        return new Rgba(Mth.lerp(amount, color.red(), 1), Mth.lerp(amount, color.green(), 1), Mth.lerp(amount, color.blue(), 1), color.alpha());
    }

    // Shapes and lights: each creates its handle on first use, then updates it in place.

    public static <S extends Shape> ShapeHandle<S> show(ShapeHandle<S> handle, S shape) {
        if (handle != null && handle.isAlive()) {
            handle.set(shape);
            return handle;
        }
        return Shapes.add(shape);
    }

    public static <S extends Shape> ShapeHandle<S> hide(ShapeHandle<S> handle) {
        if (handle != null) handle.remove();
        return null;
    }

    public static RuneCircle circle(Vec3 center, Vec3 normal, double radius, float lineWidth, Rgba color, int layers, long seed, float spin) {
        return new RuneCircle(center, normal, Math.max(0.05, radius), lineWidth, color, layers, seed, spin);
    }

    public static LightHandle<PointLight> light(LightHandle<PointLight> handle, Vec3 at, float radius, float brightness, Rgba color) {
        PointLight light = new PointLight(at, color, Mth.clamp(radius, 0.5f, 128), Mth.clamp(brightness, 0, 64));
        if (handle != null && handle.isAlive()) {
            handle.set(light);
            return handle;
        }
        return Lights.add(light);
    }

    public static <L extends Light> LightHandle<L> unlight(LightHandle<L> handle) {
        if (handle != null) handle.close();
        return null;
    }

    // Particles, following the Particles setting.

    public static boolean particlesVisible(Vec3 at) {
        return Minecraft.getInstance().options.particles().get() != ParticleStatus.MINIMAL
                && camera().distanceToSqr(at) < PARTICLE_RANGE * PARTICLE_RANGE;
    }

    /** {@code count} halved under Decreased particles. */
    public static int scaled(int count) {
        return Minecraft.getInstance().options.particles().get() == ParticleStatus.DECREASED ? (count + 1) / 2 : count;
    }

    public static void particle(ParticleOptions type, Vec3 at, Vec3 velocity) {
        world().addParticle(type, true, false, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
    }

    public static Vec3 onCircle(Vec3 center, double radius, double angle, double up) {
        return center.add(Math.cos(angle) * radius, up, Math.sin(angle) * radius);
    }

    /** A random direction in the upper half, tilted further up by {@code lift}: for things flung from the ground. */
    public static Vec3 upwards(RandomSource random, double lift) {
        Vec3 direction = new Vec3(random.nextGaussian(), Math.abs(random.nextGaussian()) + lift, random.nextGaussian());
        return direction.lengthSqr() < 1.0e-6 ? UP : direction.normalize();
    }

    /** A random direction, any way at all. */
    public static Vec3 anyDirection(RandomSource random) {
        Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        return direction.lengthSqr() < 1.0e-6 ? UP : direction.normalize();
    }

    /** A random offset between -spread/2 and spread/2. */
    public static double jitter(RandomSource random, double spread) {
        return (random.nextDouble() - 0.5) * spread;
    }

    /** One running animation: a strike, a meteor, a gate. */
    public interface Animation {
        /** Advances a tick; returns true once it's over. */
        boolean tick();

        /** Lets go of its shapes, lights and anything else it holds. */
        void release();
    }

    /**
     * An effect's running animations, keyed by the effects' source id. Everything belongs to the world it started in,
     * and a dimension change drops it. A mod ticks and clears its trackers together, in its own order, through
     * {@link Trackers}. An animation that throws is logged ({@link EffectGuard}), released and dropped; the rest
     * carry on.
     */
    public static final class Tracker<T extends Animation> {
        private final Map<Integer, T> live = new HashMap<>();
        private ClientLevel world;

        private Tracker() {
        }

        /** The animation running for {@code id}, made by {@code create} if there's none yet. */
        public T get(int id, ClientLevel level, IntFunction<T> create) {
            ensureWorld(level);
            return live.computeIfAbsent(id, create::apply);
        }

        void tick(ClientLevel level) {
            ensureWorld(level);
            for (Iterator<T> it = live.values().iterator(); it.hasNext(); ) {
                T animation = it.next();
                boolean over;
                try {
                    over = animation.tick();
                } catch (RuntimeException | LinkageError e) {
                    EffectGuard.failed(e);
                    over = true;
                }
                if (over) {
                    EffectGuard.guarded(animation::release);
                    it.remove();
                }
            }
        }

        void clear() {
            live.values().forEach(animation -> EffectGuard.guarded(animation::release));
            live.clear();
            world = null;
        }

        private void ensureWorld(ClientLevel level) {
            if (world != level) {
                clear();
                world = level;
            }
        }
    }

    /**
     * A mod's {@link Tracker}s, ticked and cleared together: each tick, from the mod's own client tick, in whatever order
     * it needs against its other work; cleared when the player leaves the world.
     */
    public static final class Trackers {
        private final List<Tracker<?>> trackers = new ArrayList<>();

        /** A new tracker, ticked and cleared with the rest. */
        public <T extends Animation> Tracker<T> create() {
            Tracker<T> tracker = new Tracker<>();
            trackers.add(tracker);
            return tracker;
        }

        public void tick(ClientLevel level) {
            for (Tracker<?> tracker : trackers) tracker.tick(level);
        }

        public void clear() {
            for (Tracker<?> tracker : trackers) tracker.clear();
        }
    }
}
