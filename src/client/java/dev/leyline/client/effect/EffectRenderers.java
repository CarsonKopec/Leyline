package dev.leyline.client.effect;

import dev.leyline.effect.Effect;
import dev.leyline.effect.EffectType;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draws effects on the client. Register renderers in your client initializer, for your own effect types or to add to
 * another mod's; every renderer for a type runs, in registration order, as each effect arrives.
 *
 * <p>A renderer that throws is logged and switched off until the game restarts, rather than disconnecting the player.
 */
public final class EffectRenderers {
    private static final Logger LOGGER = LoggerFactory.getLogger("Leyline");
    private static final Map<EffectType, List<EffectRenderer>> RENDERERS = new IdentityHashMap<>();

    private EffectRenderers() {
    }

    public static void register(EffectType type, EffectRenderer renderer) {
        RENDERERS.computeIfAbsent(type, t -> new ArrayList<>()).add(renderer);
    }

    /** Called by Leyline for each effect the server sends. */
    public static void render(Effect effect, ClientLevel level) {
        List<EffectRenderer> renderers = RENDERERS.get(effect.type());
        if (renderers == null) return;
        Iterator<EffectRenderer> iterator = renderers.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().render(effect, level);
            } catch (RuntimeException | LinkageError e) {
                iterator.remove();
                LOGGER.error("A renderer for effect {} failed and is switched off until the game restarts", effect.type(), e);
            }
        }
    }

    @FunctionalInterface
    public interface EffectRenderer {
        /** Called on the client thread, as the effect arrives. */
        void render(Effect effect, ClientLevel level);
    }
}
