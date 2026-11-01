package dev.leyline.client.render;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning through {@code points}: a jagged, flickering line, re-forked every tick, with a white-hot core and a glow of
 * {@code color}. It is {@code width} blocks across at its core; each kink is about {@code linkLength} blocks from the
 * last. {@code flicker} 0 holds one shape; anything else re-forks it every tick.
 */
public record Bolt(List<Vec3> points, Rgba color, float width, float linkLength, float flicker) implements Shape {
    public Bolt {
        if (points.size() < 2) throw new IllegalArgumentException("A bolt needs at least two points");
        points = List.copyOf(points);
    }

    /** A bolt straight from {@code start} to {@code end}, of the usual size. */
    public static Bolt between(Vec3 start, Vec3 end, Rgba color) {
        return new Bolt(List.of(start, end), color, 0.08f, 0.28f, 1);
    }

    @Override
    public Vec3 center() {
        return points.getFirst().add(points.getLast()).scale(0.5);
    }
}
