package dev.leyline.effect;

import dev.leyline.LeylineRegistries;
import net.minecraft.resources.Identifier;

/**
 * A kind of visual event, such as a bolt's impact. Register one in {@link LeylineRegistries#EFFECT_TYPES} for each kind
 * your mod sends, document what its {@link Effect} fields mean, and draw it on the client with
 * {@code dev.leyline.client.effect.EffectRenderers}.
 */
public final class EffectType {
    public Identifier id() {
        return LeylineRegistries.EFFECT_TYPES.getKey(this);
    }

    @Override public String toString() {
        return String.valueOf(id());
    }
}
