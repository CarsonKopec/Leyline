package dev.leyline.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Flat shapes redrawn every tick: {@link #begin}, draw this tick's shapes, then {@link #end}, which shows them in place
 * of last tick's. Until then, last tick's stay on screen. {@link #clear} takes them all away.
 *
 * <p>Lines are crisp, antialiased and never thinner than about a pixel, so a ring a hundred blocks off still shows.
 * Colours are premultiplied ({@link Rgba}): an opaque colour covers what's behind its lines, so they read in daylight;
 * scale every channel to fade one. Glows mostly add light. Everything is hidden by terrain in front of it.
 */
public final class Planes {
    private List<Flat> building = new ArrayList<>();
    private List<Flat> shown = List.of();

    /** One shape: what {@link ShapeRenderer} draws. Sizes in blocks, angles in radians. */
    record Flat(int type, boolean facing, Vec3 center, Vec3 normal, double size, float extent,
                float a, float b, float c, Rgba color, float cover) {
    }

    public void begin() {
        building = new ArrayList<>();
    }

    /** A ring of {@code radius}, its line {@code line} wide, lying flat facing {@code normal}. */
    public void ring(Vec3 center, Vec3 normal, double radius, float line, Rgba color) {
        if (radius <= line) return;
        building.add(new Flat(ShapeRenderer.RING, false, center, normal, radius, reach(radius, line), line / (float) radius, 0, 0, color, 1));
    }

    /**
     * Part of a ring: from angle {@code from} round to {@code to}, in radians. Angles start on the shape's first axis
     * (for a shape facing up, +Z), and turn towards its second (+X).
     */
    public void arc(Vec3 center, Vec3 normal, double radius, float line, float from, float to, Rgba color) {
        if (radius <= line || to <= from) return;
        building.add(new Flat(ShapeRenderer.ARC, false, center, normal, radius, reach(radius, line), line / (float) radius, from, to, color, 1));
    }

    /** The outline of a regular polygon of {@code sides}, its corners {@code radius} from the centre, turned by {@code turn}. */
    public void polygon(Vec3 center, Vec3 normal, double radius, int sides, float turn, float line, Rgba color) {
        if (radius <= line || sides < 3) return;
        building.add(new Flat(ShapeRenderer.POLYGON, false, center, normal, radius, reach(radius, line), line / (float) radius, sides, turn, color, 1));
    }

    /** A glowing ball: a soft disc turned to face the camera, brightest at its centre. It mostly adds light. */
    public void glow(Vec3 center, float radius, Rgba color) {
        if (radius <= 0.05f) return;
        building.add(new Flat(ShapeRenderer.DISC, true, center, Vec3.ZERO, radius, 1, 0, 0, 0, color, 0.35f));
    }

    /** A ring turned to face the camera: a halo. */
    public void halo(Vec3 center, double radius, float line, Rgba color) {
        if (radius <= line) return;
        building.add(new Flat(ShapeRenderer.RING, true, center, Vec3.ZERO, radius, reach(radius, line), line / (float) radius, 0, 0, color, 1));
    }

    /**
     * A seal of radius {@code outer}: a bold outer ring with a fine one inside it, framing a star of {@code points}
     * (two polygons of half as many sides, the second turned between the first's corners) turned by {@code turn}.
     */
    public void seal(Vec3 center, Vec3 normal, double outer, float line, int points, float turn, Rgba color) {
        if (outer < 0.5) return;
        ring(center, normal, outer, line, color);
        ring(center, normal, outer * 0.9, line * 0.45f, scaled(color, 0.8f));
        int sides = points / 2;
        polygon(center, normal, outer * 0.86, sides, turn, line * 0.6f, scaled(color, 0.85f));
        polygon(center, normal, outer * 0.86, sides, turn + Mth.PI / sides, line * 0.6f, scaled(color, 0.85f));
    }

    /** Shows this tick's shapes in place of last tick's. */
    public void end() {
        shown = building;
        building = new ArrayList<>();
        Shapes.showPlanes(this, !shown.isEmpty());
    }

    /** Takes every shape away. */
    public void clear() {
        building = new ArrayList<>();
        shown = List.of();
        Shapes.showPlanes(this, false);
    }

    List<Flat> shown() {
        return shown;
    }

    /** How far a shape's quad reaches past its radius, in radii: the line's half width, and a little for its edge. */
    private static float reach(double radius, float line) {
        return 1 + line / (float) radius * 0.5f + 0.02f;
    }

    private static Rgba scaled(Rgba color, float f) {
        return new Rgba(color.red() * f, color.green() * f, color.blue() * f, color.alpha() * f);
    }
}
