package dev.leyline.client.render;

import java.util.function.UnaryOperator;

/**
 * A light being cast, from {@link Lights#add}: {@link #set} or {@link #update} change it in place, and {@link #close}
 * puts it out. Once the player leaves the world it's out too.
 */
public final class LightHandle<L extends Light> implements AutoCloseable {
    private L light;
    private boolean alive = true;

    LightHandle(L light) {
        this.light = light;
    }

    public L light() {
        return light;
    }

    public boolean isAlive() {
        return alive;
    }

    public void set(L light) {
        this.light = light;
    }

    public void update(UnaryOperator<L> change) {
        this.light = change.apply(light);
    }

    @Override
    public void close() {
        if (!alive) return;
        alive = false;
        Lights.forget(this);
    }

    void kill() {
        alive = false;
    }
}
