package dev.leyline.client.render;

import net.minecraft.world.phys.Vec3;

/** Light from along a line, from {@code start} to {@code end}, reaching {@code radius} blocks from it: a beam's, or a laser's. */
public record BeamLight(Vec3 start, Vec3 end, Rgba color, float radius, float brightness) implements Light {
    public BeamLight withBrightness(float brightness) {
        return new BeamLight(start, end, color, radius, brightness);
    }
}
