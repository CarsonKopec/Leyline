package dev.leyline.client.render;

import net.minecraft.world.phys.Vec3;

/** Light from a point, reaching {@code radius} blocks. */
public record PointLight(Vec3 position, Rgba color, float radius, float brightness) implements Light {
    public PointLight withBrightness(float brightness) {
        return new PointLight(position, color, radius, brightness);
    }
}
