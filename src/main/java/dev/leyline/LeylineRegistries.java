package dev.leyline;

import dev.leyline.effect.EffectType;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * The registries mods add to during their own initializer:
 *
 * <pre>{@code
 * public static final EffectType BURST = Registry.register(LeylineRegistries.EFFECT_TYPES,
 *         Identifier.fromNamespaceAndPath("mymod", "burst"), new EffectType());
 * }</pre>
 *
 * <p>Synced: a client missing an effect type the server has is refused at login, with a message naming it, rather than
 * joining and failing later.
 */
public final class LeylineRegistries {
    public static final ResourceKey<Registry<EffectType>> EFFECT_TYPE = ResourceKey.createRegistryKey(Leyline.id("effect_type"));

    /** Kinds of visual event the server can send ({@link dev.leyline.effect.Effects#send}). */
    public static final MappedRegistry<EffectType> EFFECT_TYPES = FabricRegistryBuilder.createSimple(EFFECT_TYPE)
            .attribute(RegistryAttribute.SYNCED).buildAndRegister();

    private LeylineRegistries() {
    }

    /** Called by Leyline as it starts, so the registries exist before any mod adds to them. */
    static void register() {
    }
}
