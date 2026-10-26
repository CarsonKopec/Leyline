package dev.leyline;

import dev.leyline.terrain.Carving;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/** Leyline's common entrypoint: starts what effects lean on from the server, such as the crater {@link Carving} queue. */
public final class Leyline implements ModInitializer {
    public static final String ID = "leyline";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    @Override public void onInitialize() {
        Carving.register();
    }
}
