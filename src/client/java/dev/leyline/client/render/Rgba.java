package dev.leyline.client.render;

/**
 * A colour for Leyline's shapes and lights, each channel 0 to 1. Shapes read it premultiplied: red, green and blue are
 * the light a shape adds, and alpha how much of what's behind it covers, so scaling every channel together fades a
 * shape out, and an alpha of 0 adds light without covering anything.
 */
public record Rgba(float red, float green, float blue, float alpha) {
    public static final Rgba WHITE = new Rgba(1, 1, 1, 1);

    /** From {@code 0xRRGGBB}, opaque. */
    public static Rgba rgb(int rgb) {
        return new Rgba(channel(rgb, 16), channel(rgb, 8), channel(rgb, 0), 1);
    }

    /** From {@code 0xRRGGBBAA}. */
    public static Rgba rgba(int rgba) {
        return new Rgba(channel(rgba, 24), channel(rgba, 16), channel(rgba, 8), channel(rgba, 0));
    }

    private static float channel(int value, int shift) {
        return ((value >>> shift) & 0xFF) / 255f;
    }
}
