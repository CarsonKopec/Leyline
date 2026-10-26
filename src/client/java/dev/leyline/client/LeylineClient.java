package dev.leyline.client;

import dev.leyline.client.effect.EffectRenderers;
import dev.leyline.client.screen.ScreenEffects;
import dev.leyline.effect.EffectPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Leyline's client entrypoint: hands each effect the server sends to its {@link EffectRenderers}, and runs
 * {@link ScreenEffects}, ticking it while a world is open and the game isn't paused, and clearing it when the player
 * leaves.
 */
public final class LeylineClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EffectPayload.TYPE, (payload, context) -> {
            if (context.client().level != null) EffectRenderers.render(payload.effect(), context.client().level);
        });
        ScreenEffects.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) ScreenEffects.clear();
            else if (!client.isPaused()) ScreenEffects.tick();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ScreenEffects.clear());
    }
}
