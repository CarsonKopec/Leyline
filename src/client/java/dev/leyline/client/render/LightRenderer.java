package dev.leyline.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
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
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Casts {@link Lights} on the finished frame, after the world is drawn: a copy of the frame is taken
 * ({@link ScreenPasses#copy}), then each light is drawn over the part of the screen it can reach by
 * {@code leyline:core/lights}, up to {@link #PER_BATCH} a draw, adding its light. One copy and one pass a frame,
 * however many lights, and none at all while there are none.
 */
final class LightRenderer {
    /** Before shapes, which glow by themselves and aren't lit. */
    static final int PRIORITY = -200;
    /** Lights a draw; as {@code LIGHTS_PER_BATCH} in {@code lights.glsl}. */
    private static final int PER_BATCH = 128;
    private static final int FLOATS = 12;
    /** How strongly lights light: what a light of brightness 1 adds, at its heart, times the surface's colour. */
    private static final float STRENGTH = 0.5f;

    private static float[] data = new float[FLOATS * 32];
    private static int count;
    private static RenderPipeline pipeline;
    private static UniformBlock<View> views;
    private static UniformBlock<Batch> batches;
    /** The frame as the world drew it, for each light to read the surface's colour from. */
    private static TextureTarget frame;
    private static int drawn;

    private LightRenderer() {
    }

    static int drawn() {
        return drawn;
    }

    static void draw(AfterWorld.Frame context) {
        drawn = 0;
        Lights.syncLevel();
        if (Minecraft.getInstance().level == null || !RenderCompat.drawable()) return;
        Vec3 camera = context.camera().position();
        count = 0;
        for (LightHandle<?> handle : Lights.handles()) add(handle.light(), camera);
        if (count == 0) return;

        if (pipeline == null) build();
        RenderTarget main = context.main();
        frame = ScreenPasses.sized(frame, "Leyline lit frame", main.width, main.height);
        views.beginFrame();
        batches.beginFrame();
        Matrix4f viewProj = new Matrix4f(context.projection()).mul(context.view());
        Vector3f forward = new Matrix4f(context.view()).invert().transformDirection(new Vector3f(0, 0, -1)).normalize();
        GpuBufferSlice view = views.upload(new View(viewProj, viewProj.invert(new Matrix4f()), new Vector4f(forward, 0),
                new Vector4f(main.width, main.height, STRENGTH, 0)));
        // Writing a uniform block maps a buffer, which can't be done while a render pass is open: write every batch first.
        List<GpuBufferSlice> slices = new ArrayList<>();
        for (int first = 0; first < count; first += PER_BATCH) slices.add(batches.upload(new Batch(first, Math.min(PER_BATCH, count - first))));
        ScreenPasses.copy(main, frame, ScreenPasses.transforms());
        var nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Leyline lights",
                main.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("LightView", view);
            pass.bindTexture("SceneDepth", main.getDepthTextureView(), nearest);
            pass.bindTexture("SceneColor", frame.getColorTextureView(), nearest);
            for (int i = 0; i < slices.size(); i++) {
                pass.setUniform("Lights", slices.get(i));
                pass.draw(0, Math.min(PER_BATCH, count - i * PER_BATCH) * 6);
            }
        }
        drawn = count;
    }

    private static void add(Light light, Vec3 camera) {
        if (light.radius() <= 0 || light.brightness() <= 0) return;
        Vec3 start, end;
        switch (light) {
            case PointLight point -> start = end = point.position();
            case BeamLight beam -> {
                start = beam.start();
                end = beam.end();
            }
        }
        Vec3 middle = start.add(end).scale(0.5);
        double range = Lights.RENDER_DISTANCE + light.radius() + start.distanceTo(end) * 0.5;
        if (camera.distanceToSqr(middle) > range * range) return;
        if ((count + 1) * FLOATS > data.length) data = Arrays.copyOf(data, data.length * 2);
        int i = count++ * FLOATS;
        float brightness = light.brightness();
        Rgba color = light.color();
        put(i, start.x - camera.x, start.y - camera.y, start.z - camera.z, light.radius());
        put(i + 4, color.red() * brightness, color.green() * brightness, color.blue() * brightness, light instanceof BeamLight ? 1 : 0);
        put(i + 8, end.x - camera.x, end.y - camera.y, end.z - camera.z, 0);
    }

    private static void put(int i, double x, double y, double z, double w) {
        data[i] = (float) x;
        data[i + 1] = (float) y;
        data[i + 2] = (float) z;
        data[i + 3] = (float) w;
    }

    private static void build() {
        RenderPipeline built = RenderPipeline.builder()
                .withLocation(Leyline.id("lights"))
                .withVertexShader(Leyline.id("core/lights"))
                .withFragmentShader(Leyline.id("core/lights"))
                .withUniform("LightView", UniformType.UNIFORM_BUFFER)
                .withUniform("Lights", UniformType.UNIFORM_BUFFER)
                .withSampler("SceneDepth")
                .withSampler("SceneColor")
                .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                .withBlend(BlendFunction.ADDITIVE)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();
        ScreenPasses.precompile("Leyline's light shader", built);
        views = new UniformBlock<>("Leyline light view", 64 * 2 + 16 * 2, 2, (out, view) ->
                out.putMat4f(view.viewProj()).putMat4f(view.inverse()).putVec4(view.forward()).putVec4(view.screen()));
        batches = new UniformBlock<>("Leyline lights", PER_BATCH * FLOATS * 4, 4, (out, batch) -> {
            for (int i = batch.first() * FLOATS, end = (batch.first() + batch.count()) * FLOATS; i < end; i += 4) {
                out.putVec4(data[i], data[i + 1], data[i + 2], data[i + 3]);
            }
        });
        pipeline = built;
    }

    private record View(Matrix4f viewProj, Matrix4f inverse, Vector4f forward, Vector4f screen) {
    }

    private record Batch(int first, int count) {
    }
}
