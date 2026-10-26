package dev.leyline.client.screen;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.nexusrealms.nebulon.api.render.client.ShaderMaterial;
import dev.leyline.Leyline;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Pieces for drawing a {@link ScreenPass}: materials that cover the whole screen, textures sized to the frame, the draw
 * itself, and a copy of the frame to read while drawing over it. Shaders can import {@code leyline:screen_space.glsl}
 * for the ray through each pixel and the geometry to draw with. Render thread only.
 */
public final class ScreenPasses {
    private static final Identifier VERTEX = Identifier.fromNamespaceAndPath("nebulon", "core/fullscreen_triangle");
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static ShaderMaterial copy;
    private static Boolean intelIntegrated;

    private ScreenPasses() {
    }

    /**
     * A material named {@code name} that draws over the whole screen, with Nebulon's full-screen triangle and the fragment
     * shader {@code fragment} ({@code mymod:core/glow} for {@code assets/mymod/shaders/core/glow.fsh}): opaque, with no
     * depth test. Add its samplers and uniform blocks, then build it; its shader reads {@code texCoord}.
     */
    public static ShaderMaterial.Builder fullScreen(Identifier name, Identifier fragment) {
        return ShaderMaterial.builder(name)
                .shaders(VERTEX, fragment)
                .geometry(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                .transparency(ShaderMaterial.Transparency.OPAQUE)
                .depth(DepthTestFunction.NO_DEPTH_TEST, false)
                .cull(false);
    }

    /**
     * Compiles {@code materials} now. A shader that fails to compile draws nothing at all, rather than throwing, so this
     * throws instead, naming {@code what}; a {@link ScreenPass} then switches itself off.
     */
    public static void precompile(String what, ShaderMaterial... materials) {
        for (ShaderMaterial material : materials) {
            material.precompile();
            if (!RenderSystem.getDevice().precompilePipeline(material.pipeline()).isValid()) {
                throw new IllegalStateException(what + " did not compile; see the log above");
            }
        }
    }

    /** {@code target}, made (named {@code label}) or resized to {@code width} by {@code height}, at least 1 by 1. */
    public static TextureTarget sized(TextureTarget target, String label, int width, int height) {
        width = Math.max(1, width);
        height = Math.max(1, height);
        if (target == null) return new TextureTarget(label, width, height, false);
        if (target.width != width || target.height != height) target.resize(width, height);
        return target;
    }

    /** The transforms a full-screen draw uses: none. Write them once a frame and pass them to each {@link #draw}. */
    public static GpuBufferSlice transforms() {
        return RenderSystem.getDynamicUniforms().writeTransform(IDENTITY, new Vector4f(1), new Vector3f(), IDENTITY);
    }

    /** Draws {@code material} over the whole of {@code target}; {@code inputs} binds its textures and uniform blocks. */
    public static void draw(RenderTarget target, String label, ShaderMaterial material, GpuBufferSlice transforms, Consumer<RenderPass> inputs) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> label, target.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(material.pipeline());
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            inputs.accept(pass);
            pass.draw(0, 3);
        }
    }

    /**
     * Copies {@code from}'s colour into {@code into}, for a pass to read while it draws over {@code from}. It copies with a
     * draw: copying textures directly ({@code copyTextureToTexture}, as Nebulon's compositor does) has lost the colour on
     * some graphics drivers.
     */
    public static void copy(RenderTarget from, TextureTarget into, GpuBufferSlice transforms) {
        if (copy == null) {
            ShaderMaterial built = fullScreen(Leyline.id("copy"), Leyline.id("core/copy")).sampler("InSampler").build();
            precompile("Leyline's frame copy", built);
            copy = built;
        }
        var nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        draw(into, "Leyline frame copy", copy, transforms, pass -> pass.bindTexture("InSampler", from.getColorTextureView(), nearest));
    }

    /**
     * Whether the game is drawn by Intel's integrated graphics (not its discrete Arc cards). On one of them, a long chain
     * of passes (a bloom's) has hung the driver until Windows reset it; a pass may want to leave such work out there.
     */
    public static boolean intelIntegrated() {
        if (intelIntegrated == null) {
            var device = RenderSystem.getDevice();
            String renderer = device.getRenderer();
            boolean intel = (device.getVendor() + " " + renderer).toLowerCase(Locale.ROOT).contains("intel");
            intelIntegrated = intel && !renderer.contains("Arc");
        }
        return intelIntegrated;
    }
}
