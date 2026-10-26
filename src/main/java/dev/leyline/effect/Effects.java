package dev.leyline.effect;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** The server side of the effect channel: sends each {@link Effect} once to every nearby player who has Leyline. */
public final class Effects {
    /** Players this far from an effect, or further, aren't sent it. */
    public static final double RANGE = 448;

    private Effects() {
    }

    public static void send(ServerLevel level, EffectType type, Vec3 position, Vec3 direction, float power, int age, int sourceId) {
        send(level, new Effect(type, position, direction, power, age, sourceId));
    }

    /** Sends {@code effect} to every player within {@link #RANGE} of its position who has that chunk loaded. */
    public static void send(ServerLevel level, Effect effect) {
        EffectPayload payload = new EffectPayload(effect);
        Vec3 position = effect.position();
        // Only players with this chunk loaded: anyone else would light up empty, unloaded terrain.
        for (ServerPlayer player : PlayerLookup.tracking(level, BlockPos.containing(position))) {
            if (player.distanceToSqr(position) < RANGE * RANGE && ServerPlayNetworking.canSend(player, EffectPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
