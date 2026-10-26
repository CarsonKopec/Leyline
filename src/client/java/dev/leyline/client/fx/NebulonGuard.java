package dev.leyline.client.fx;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Every call into Nebulon goes through here. Nebulon is a beta library driving the GPU directly, so the first failure,
 * anywhere, is logged once and switches Nebulon off for the rest of the session for every mod using Leyline: an
 * exception escaping a packet handler would otherwise disconnect the player. Effects then carry on without it, with
 * whatever particles they draw; owners of Nebulon handles let theirs go ({@link #onFailure}, {@link #cleanup}).
 */
public final class NebulonGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger("Leyline");
    private static final List<Runnable> ON_FAILURE = new ArrayList<>();
    /** Set by the first Nebulon failure; Nebulon then stays off until the game restarts. */
    private static boolean disabled;

    private NebulonGuard() {
    }

    /** Runs Nebulon work, unless Nebulon has already failed; the first failure switches it off. */
    public static void guarded(Runnable action) {
        if (disabled) return;
        try {
            action.run();
        } catch (RuntimeException | LinkageError e) { // LinkageError: an installed Nebulon with a different API
            disabled = true;
            LOGGER.error("Nebulon effects failed; effects will carry on without them until the game restarts", e);
            for (Runnable release : ON_FAILURE) {
                try {
                    release.run();
                } catch (RuntimeException | LinkageError ignored) {
                    // Nebulon is already failing: release what still can be released.
                }
            }
        }
    }

    /** Whether Nebulon has failed and is off: owners of Nebulon handles check it and let theirs go. */
    public static boolean disabled() {
        return disabled;
    }

    /**
     * Runs cleanup, such as removing a handle, even after Nebulon has failed; one broken handle must not strand the
     * others. A failure here, while Nebulon is still on, switches it off like any other.
     */
    public static void cleanup(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | LinkageError e) {
            if (!disabled) guarded(() -> { throw e; });
        }
    }

    /** Runs {@code release} once, if Nebulon fails: to let go of handles kept outside {@link #guarded} work. */
    public static void onFailure(Runnable release) {
        ON_FAILURE.add(release);
    }
}
