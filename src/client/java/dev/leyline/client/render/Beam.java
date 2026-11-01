package dev.leyline.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * A beam of light: a strip {@code width} by {@code height} blocks, centred on {@code center} along {@code axis}, turned
 * about its axis to face the camera. It adds light, covering nothing. With a {@code sprite}, it shows that texture
 * tinted by {@code color}: {@code frames} frames side by side, played at {@code framesPerSecond}. Without one, it is
 * bright down its middle and soft at its edges.
 */
public record Beam(Vec3 center, Vec3 axis, float width, float height, Rgba color, Identifier sprite, int frames, float framesPerSecond)
        implements Shape {
    /** An upright beam drawn with {@code sprite}'s {@code frames}. */
    public static Beam upright(Vec3 center, float width, float height, Rgba color, Identifier sprite, int frames, float framesPerSecond) {
        return new Beam(center, new Vec3(0, 1, 0), width, height, color, sprite, frames, framesPerSecond);
    }
}
