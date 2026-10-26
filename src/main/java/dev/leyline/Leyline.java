package dev.leyline;

import dev.leyline.effect.EffectPayload;
import dev.leyline.terrain.Carving;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

/**
 * Leyline's common entrypoint: sets up its registries and the effect message, and starts what effects lean on from the
 * server, such as the crater {@link Carving} queue.
 */
public final class Leyline implements ModInitializer {
    public static final String ID = "leyline";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    @Override public void onInitialize() {
        LeylineRegistries.register();
        PayloadTypeRegistry.playS2C().register(EffectPayload.TYPE, EffectPayload.CODEC);
        Carving.register();
    }
}
