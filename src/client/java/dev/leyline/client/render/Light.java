package dev.leyline.client.render;

/**
 * A light Leyline casts on the world: see {@link Lights}. Its colour is multiplied by its brightness; it fades smoothly
 * to nothing at its radius, and lights the surfaces turned towards it most.
 */
public sealed interface Light permits PointLight, BeamLight {
    Rgba color();

    float radius();

    float brightness();
}
