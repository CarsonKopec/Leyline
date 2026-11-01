package dev.leyline.client.render;

/**
 * A shape being drawn, from {@link Shapes#add}: {@link #set} changes it in place, and {@link #remove} stops drawing it.
 * Once the player leaves the world it's dead too.
 */
public final class ShapeHandle<S extends Shape> {
    private S shape;
    private boolean alive = true;

    ShapeHandle(S shape) {
        this.shape = shape;
    }

    public S shape() {
        return shape;
    }

    public boolean isAlive() {
        return alive;
    }

    /** Draws {@code shape} instead, from the next frame. */
    public void set(S shape) {
        this.shape = shape;
    }

    public void remove() {
        if (!alive) return;
        alive = false;
        Shapes.forget(this);
    }

    void kill() {
        alive = false;
    }
}
