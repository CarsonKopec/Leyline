package dev.leyline.client.render;

import net.minecraft.world.phys.Vec3;

/**
 * Something Leyline draws in the world, until it's removed or its time is up: see {@link Shapes}. Flat shapes redrawn
 * every tick (rings, arcs, polygons, glows) go through {@link Planes} instead.
 */
public sealed interface Shape permits RuneCircle, Bolt, Beam {
    /** Where it is: shapes further than {@link Shapes#RENDER_DISTANCE} from the camera aren't drawn. */
    Vec3 center();
}
