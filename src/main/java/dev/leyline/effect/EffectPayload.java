package dev.leyline.effect;

import dev.leyline.Leyline;
import dev.leyline.LeylineRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * One compact message per effect; particles are generated locally, never individually networked.
 *
 * <p>Effect types are sent by registry id, which registry sync keeps in step between client and server, so the
 * message's id only needs bumping when its layout changes.
 */
public record EffectPayload(Effect effect) implements CustomPacketPayload {
    public static final Type<EffectPayload> TYPE = new Type<>(Leyline.id("effect"));
    private static final StreamCodec<RegistryFriendlyByteBuf, EffectType> TYPE_CODEC = ByteBufCodecs.idMapper(
            LeylineRegistries.EFFECT_TYPES::byIdOrThrow, LeylineRegistries.EFFECT_TYPES::getIdOrThrow).cast();
    public static final StreamCodec<RegistryFriendlyByteBuf, EffectPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                Effect effect = value.effect;
                TYPE_CODEC.encode(buf, effect.type());
                buf.writeDouble(effect.position().x);
                buf.writeDouble(effect.position().y);
                buf.writeDouble(effect.position().z);
                buf.writeFloat((float) effect.direction().x);
                buf.writeFloat((float) effect.direction().y);
                buf.writeFloat((float) effect.direction().z);
                buf.writeFloat(effect.power());
                buf.writeVarInt(effect.age());
                buf.writeVarInt(effect.sourceId());
            },
            buf -> new EffectPayload(new Effect(TYPE_CODEC.decode(buf),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    buf.readFloat(), buf.readVarInt(), buf.readVarInt()))
    );

    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
