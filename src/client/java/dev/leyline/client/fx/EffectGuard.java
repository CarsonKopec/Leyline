package dev.leyline.client.fx;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs effect work where an exception would do real harm: escaping a client tick it crashes the game, escaping a packet
 * handler it disconnects the player. {@link #guarded} catches it instead and logs it, each different failure once in
 * full, so a broken effect costs that effect, not the session. {@link Fx.Tracker}s run their animations this way, and
 * drop one that fails.
 */
public final class EffectGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger("Leyline");
    /** Failures already logged, by what failed and where. */
    private static final Set<String> LOGGED = new HashSet<>();
    private static final int MAX_LOGGED = 64;

    private EffectGuard() {
    }

    /** Runs {@code action}; returns false if it failed, which is logged. */
    public static boolean guarded(Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuntimeException | LinkageError e) {
            failed(e);
            return false;
        }
    }

    /** Logs {@code failure}, unless the same failure, from the same place, has been logged already. */
    public static void failed(Throwable failure) {
        StackTraceElement[] stack = failure.getStackTrace();
        String key = failure.getClass().getName() + (stack.length > 0 ? " at " + stack[0] : "");
        if (LOGGED.size() < MAX_LOGGED && LOGGED.add(key)) {
            LOGGER.error("An effect failed and carries on without that step; the same failure won't be logged again", failure);
        }
    }
}
