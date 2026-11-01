package dev.leyline.client.render;

import net.minecraft.world.phys.Vec3;

/**
 * A circle of runes, laid flat facing {@code normal}: rings, a band of runes and, with more {@code layers} (1 to 6),
 * stars, a second band and inner rings. The runes are drawn from {@code seed}, so the same seed draws the same circle.
 * {@code lineWidth} is in blocks; the circle turns at {@code spin} radians a second, its inner layers the other way.
 * Its lines are crisp and cover what's behind them, so it reads in daylight as well as at night.
 */
public record RuneCircle(Vec3 center, Vec3 normal, double radius, float lineWidth, Rgba color, int layers, long seed, float spin)
        implements Shape {
}
