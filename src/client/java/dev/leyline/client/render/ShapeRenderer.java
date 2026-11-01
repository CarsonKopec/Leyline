package dev.leyline.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.leyline.Leyline;
import dev.leyline.client.screen.ScreenPasses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Draws {@link Shapes} each frame, after the world: every shape becomes a quad of six vertices placed by
 * {@code leyline:core/shapes.vsh} from the shape's data in a uniform block, up to {@link #PER_BATCH} a draw, with no
 * vertex buffer at all. {@code shapes.fsh} draws each from its distance field. Beams with a sprite are drawn by
 * {@code shape_sprite.fsh}, a batch per texture. Blending is premultiplied: colours add light and cover what's behind
 * them by their alpha.
 */
final class ShapeRenderer {
    /** Before any screen pass, so passes see the shapes as part of the world. */
    static final int PRIORITY = -100;
    static final int DISC = 0, RING = 1, ARC = 2, POLYGON = 3, RUNES = 4, LINE = 5, BEAM = 6, FACING = 16;
    /** Shapes a draw; as {@code SHAPES_PER_BATCH} in {@code shapes.glsl}. */
    private static final int PER_BATCH = 160;
    /** Floats a shape: six vec4s. */
    private static final int FLOATS = 24;

    private static final Prims PLAIN = new Prims();
    private static final Map<Identifier, Prims> SPRITES = new HashMap<>();
    private static RenderPipeline plain, sprite;
    private static UniformBlock<View> views;
    private static UniformBlock<Batch> batches;
    private static int drawn;

    private ShapeRenderer() {
    }

    static int drawn() {
        return drawn;
    }

    static void draw(AfterWorld.Frame frame) {
        drawn = 0;
        Shapes.syncLevel();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !RenderCompat.drawable()) return;
        Vec3 camera = frame.camera().position();
        float now = level.getGameTime() + frame.partialTick();
        PLAIN.reset(camera);
        SPRITES.values().forEach(prims -> prims.reset(camera));
        emit(now);
        int total = PLAIN.count;
        for (Prims prims : SPRITES.values()) total += prims.count;
        if (total == 0) return;

        if (plain == null) build();
        views.beginFrame();
        batches.beginFrame();
        RenderTarget main = frame.main();
        float pixelScale = frame.projection().m11() * main.height * 0.5f;
        GpuBufferSlice view = views.upload(new View(new Matrix4f(frame.projection()).mul(frame.view()), new Vector4f(pixelScale, 0, 0, 0)));
        // Writing a uniform block maps a buffer, which can't be done while a render pass is open: write every batch first.
        List<Draw> draws = new ArrayList<>();
        batch(draws, plain, null, PLAIN);
        for (Map.Entry<Identifier, Prims> entry : SPRITES.entrySet()) {
            if (entry.getValue().count == 0) continue;
            batch(draws, sprite, Minecraft.getInstance().getTextureManager().getTexture(entry.getKey()), entry.getValue());
        }
        var linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Leyline shapes",
                main.getColorTextureView(), OptionalInt.empty(), main.getDepthTextureView(), OptionalDouble.empty())) {
            RenderPipeline current = null;
            AbstractTexture bound = null;
            for (Draw draw : draws) {
                if (draw.pipeline() != current) {
                    current = draw.pipeline();
                    pass.setPipeline(current);
                    pass.setUniform("ShapeView", view);
                    bound = null;
                }
                if (draw.texture() != null && draw.texture() != bound) {
                    bound = draw.texture();
                    pass.bindTexture("Sprite", bound.getTextureView(), linear);
                }
                pass.setUniform("Shapes", draw.shapes());
                pass.draw(0, draw.count() * 6);
            }
        }
        drawn = total;
    }

    /** Writes {@code prims} into uniform blocks, a batch each, to be drawn with {@code pipeline} (and {@code texture}). */
    private static void batch(List<Draw> draws, RenderPipeline pipeline, AbstractTexture texture, Prims prims) {
        for (int first = 0; first < prims.count; first += PER_BATCH) {
            int count = Math.min(PER_BATCH, prims.count - first);
            draws.add(new Draw(pipeline, texture, batches.upload(new Batch(prims.data, first, count)), count));
        }
    }

    /** Turns every live shape into this frame's data, {@code now} in ticks. */
    private static void emit(float now) {
        float seconds = now / 20;
        long tick = (long) Math.floor(now);
        for (ShapeHandle<?> handle : Shapes.handles()) emit(handle.shape(), 1, seconds, tick);
        for (Iterator<Shapes.Timed> it = Shapes.timed().iterator(); it.hasNext(); ) {
            Shapes.Timed timed = it.next();
            float age = (now - timed.start()) / timed.ticks();
            if (age >= 1) {
                it.remove();
                continue;
            }
            emit(timed.shape(), age < 0.5f ? 1 : (1 - age) * 2, seconds, tick);
        }
        for (Planes planes : Shapes.planes()) {
            for (Planes.Flat flat : planes.shown()) {
                PLAIN.planar(flat.type(), flat.facing(), flat.center(), flat.normal(), flat.size(), flat.extent(),
                        flat.a(), flat.b(), flat.c(), 0, flat.color(), 1, flat.cover());
            }
        }
    }

    private static void emit(Shape shape, float fade, float seconds, long tick) {
        switch (shape) {
            case RuneCircle circle -> {
                float line = circle.lineWidth() / (float) Math.max(circle.radius(), 0.05);
                PLAIN.planar(RUNES, false, circle.center(), circle.normal(), Math.max(circle.radius(), 0.05), 1 + line + 0.02f,
                        line, Mth.clamp(circle.layers(), 1, 6), (float) (circle.seed() & 0xFFFF), circle.spin() * seconds,
                        circle.color(), fade, 1);
            }
            case Bolt bolt -> bolt(bolt, fade, tick);
            case Beam beam -> {
                Vec3 axis = beam.axis().lengthSqr() < 1.0e-8 ? new Vec3(0, 1, 0) : beam.axis().normalize();
                int frames = Math.max(1, beam.frames());
                float frame = beam.sprite() == null ? 0 : (float) Math.floor(seconds * beam.framesPerSecond()) % frames;
                Prims into = beam.sprite() == null ? PLAIN : SPRITES.computeIfAbsent(beam.sprite(), id -> new Prims());
                if (into.camera == null) into.reset(PLAIN.camera);
                into.beam(beam.center(), axis.scale(beam.height() * 0.5), beam.width() * 0.5, frame, frames, beam.color(), fade);
            }
        }
    }

    /** A bolt as lines: its path kinked afresh each tick (unless it holds still), a soft glow under a white-hot core. */
    private static void bolt(Bolt bolt, float fade, long tick) {
        long seed = System.identityHashCode(bolt) * 0x9E3779B97F4A7C15L + (bolt.flicker() == 0 ? 0 : tick * 0x632BE59BD9B4E019L);
        float link = Math.max(0.05f, bolt.linkLength());
        List<Vec3> path = new ArrayList<>();
        List<Vec3> points = bolt.points();
        path.add(points.getFirst());
        for (int k = 0; k + 1 < points.size(); k++) {
            Vec3 a = points.get(k), b = points.get(k + 1);
            Vec3 span = b.subtract(a);
            double length = span.length();
            if (length < 1.0e-4) continue;
            int kinks = Math.max(1, (int) Math.ceil(length / link));
            Vec3 along = span.scale(1 / length);
            Vec3 side = perpendicular(along), other = along.cross(side);
            for (int j = 1; j < kinks; j++) {
                double swing = link * 0.55;
                path.add(a.add(span.scale(j / (double) kinks))
                        .add(side.scale(random(seed, k, j, 0) * swing)).add(other.scale(random(seed, k, j, 1) * swing)));
            }
            path.add(b);
        }
        Rgba color = bolt.color();
        Rgba core = new Rgba(Mth.lerp(0.7f, color.red(), 1), Mth.lerp(0.7f, color.green(), 1), Mth.lerp(0.7f, color.blue(), 1), color.alpha());
        for (int i = 0; i + 1 < path.size(); i++) PLAIN.line(path.get(i), path.get(i + 1), bolt.width() * 1.3, 2, true, color, fade * 0.6f, 0.15f);
        for (int i = 0; i + 1 < path.size(); i++) PLAIN.line(path.get(i), path.get(i + 1), bolt.width() * 0.5, 1.5f, false, core, fade, 1);
    }

    /** A random number from -1 to 1, the same for the same inputs. */
    private static double random(long seed, int a, int b, int c) {
        long h = seed ^ a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53 * 2 - 1;
    }

    private static Vec3 perpendicular(Vec3 unit) {
        Vec3 reference = Math.abs(unit.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return reference.cross(unit).normalize();
    }

    private static void build() {
        RenderPipeline.Builder plainBuilder = pipeline("shapes", Leyline.id("core/shapes"));
        RenderPipeline.Builder spriteBuilder = pipeline("shape_sprite", Leyline.id("core/shape_sprite")).withSampler("Sprite");
        RenderPipeline builtPlain = plainBuilder.build(), builtSprite = spriteBuilder.build();
        ScreenPasses.precompile("Leyline's shape shaders", builtPlain, builtSprite);
        views = new UniformBlock<>("Leyline shape view", 64 + 16, 2, (out, view) -> out.putMat4f(view.viewProj()).putVec4(view.pixelScale()));
        batches = new UniformBlock<>("Leyline shapes", PER_BATCH * FLOATS * 4, 8, (out, batch) -> {
            float[] data = batch.data();
            for (int i = batch.first() * FLOATS, end = (batch.first() + batch.count()) * FLOATS; i < end; i += 4) {
                out.putVec4(data[i], data[i + 1], data[i + 2], data[i + 3]);
            }
        });
        plain = builtPlain;
        sprite = builtSprite;
    }

    private static RenderPipeline.Builder pipeline(String name, Identifier fragment) {
        return RenderPipeline.builder()
                .withLocation(Leyline.id(name))
                .withVertexShader(Leyline.id("core/shapes"))
                .withFragmentShader(fragment)
                .withUniform("ShapeView", UniformType.UNIFORM_BUFFER)
                .withUniform("Shapes", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withDepthBias(-1, -10)
                .withCull(false);
    }

    private record View(Matrix4f viewProj, Vector4f pixelScale) {
    }

    private record Batch(float[] data, int first, int count) {
    }

    private record Draw(RenderPipeline pipeline, AbstractTexture texture, GpuBufferSlice shapes, int count) {
    }

    /** One frame's shapes for one pipeline (and sprite): six vec4s each, positions relative to the camera. */
    private static final class Prims {
        float[] data = new float[FLOATS * 64];
        int count;
        Vec3 camera;

        void reset(Vec3 camera) {
            this.camera = camera;
            count = 0;
        }

        private int next() {
            if ((count + 1) * FLOATS > data.length) data = Arrays.copyOf(data, data.length * 2);
            return count++ * FLOATS;
        }

        private boolean visible(Vec3 at, double reach) {
            double range = Shapes.RENDER_DISTANCE + reach;
            return camera.distanceToSqr(at) < range * range;
        }

        void planar(int type, boolean facing, Vec3 center, Vec3 normal, double size, float extent, float a, float b, float c, float d,
                    Rgba color, float fade, float cover) {
            if (!visible(center, size * extent)) return;
            double ux = 0, uy = 0, uz = 1, vx = 1, vy = 0, vz = 0;
            if (!facing) {
                Vec3 n = normal.lengthSqr() < 1.0e-8 ? new Vec3(0, 1, 0) : normal.normalize();
                Vec3 u = perpendicular(n), v = n.cross(u);
                ux = u.x; uy = u.y; uz = u.z;
                vx = v.x; vy = v.y; vz = v.z;
            }
            int i = next();
            put(i, center.x - camera.x, center.y - camera.y, center.z - camera.z, type + (facing ? FACING : 0));
            put(i + 4, ux, uy, uz, size);
            put(i + 8, vx, vy, vz, extent);
            put(i + 12, a, b, c, d);
            color(i + 16, color, fade);
            put(i + 20, cover, 0, 0, 0);
        }

        void line(Vec3 from, Vec3 to, double radius, float extent, boolean soft, Rgba color, float fade, float cover) {
            if (!visible(from.add(to).scale(0.5), from.distanceTo(to))) return;
            int i = next();
            put(i, from.x - camera.x, from.y - camera.y, from.z - camera.z, LINE);
            put(i + 4, to.x - from.x, to.y - from.y, to.z - from.z, radius);
            put(i + 8, 0, 0, 0, extent);
            put(i + 12, soft ? 1 : 0, 0, 0, 0);
            color(i + 16, color, fade);
            put(i + 20, cover, 0, 0, 0);
        }

        void beam(Vec3 center, Vec3 halfAxis, double halfWidth, float frame, int frames, Rgba color, float fade) {
            if (!visible(center, halfAxis.length())) return;
            int i = next();
            put(i, center.x - camera.x, center.y - camera.y, center.z - camera.z, BEAM);
            put(i + 4, halfAxis.x, halfAxis.y, halfAxis.z, halfWidth);
            put(i + 8, 0, 0, 0, 1);
            put(i + 12, 0, 0, 0, 0);
            color(i + 16, color, fade);
            put(i + 20, 0, frame, frames, 0);
        }

        private void put(int i, double x, double y, double z, double w) {
            data[i] = (float) x;
            data[i + 1] = (float) y;
            data[i + 2] = (float) z;
            data[i + 3] = (float) w;
        }

        private void color(int i, Rgba color, float fade) {
            put(i, color.red() * fade, color.green() * fade, color.blue() * fade, color.alpha() * fade);
        }
    }
}
