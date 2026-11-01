package dev.leyline.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.DynamicUniformStorage;

/**
 * A uniform block a shader reads, written afresh each frame: {@link #beginFrame}, then {@link #upload} each value a
 * draw needs, passing the slice it returns to {@code RenderPass.setUniform}. Values are written std140 by the encoder
 * given, in the order the shader declares them. Built on Minecraft's own ring of buffers, so a frame's writes never
 * touch a buffer the GPU may still be reading. Render thread only.
 */
public final class UniformBlock<T> implements AutoCloseable {
    private final DynamicUniformStorage<Upload> storage;
    private final BiConsumer<Std140Builder, T> encode;

    /**
     * A block of {@code bytes} bytes, named {@code label} for graphics debuggers, with room for {@code capacity} uploads
     * a frame before it grows; {@code encode} writes one value.
     */
    public UniformBlock(String label, int bytes, int capacity, BiConsumer<Std140Builder, T> encode) {
        this.storage = new DynamicUniformStorage<>(label, bytes, capacity);
        this.encode = encode;
    }

    /** Once a frame, before the first upload: last frame's blocks are let go. */
    public void beginFrame() {
        storage.endFrame();
    }

    /** Writes {@code value} into a fresh block, for one draw. */
    public GpuBufferSlice upload(T value) {
        return storage.writeUniform(new Upload(value));
    }

    @Override
    public void close() {
        storage.close();
    }

    /** One upload. Never equal to another, so the storage doesn't hand back an older block for a value changed since. */
    private final class Upload implements DynamicUniformStorage.DynamicUniform {
        private final T value;

        private Upload(T value) {
            this.value = value;
        }

        @Override
        public void write(ByteBuffer buffer) {
            encode.accept(Std140Builder.intoBuffer(buffer), value);
        }
    }
}
