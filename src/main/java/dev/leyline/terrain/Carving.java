package dev.leyline.terrain;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Craters carved over a few ticks rather than all in one: an effect asks for one ({@link #start}), it's planned
 * ({@link Crater}), and each tick the next share of its blocks vanishes, nearest the centre first, under the
 * {@link TerrainRules}. Blocks in chunks that have unloaded are left alone. Owns every crater still being carved.
 */
public final class Carving {
    private static final List<Carving> CARVINGS = new ArrayList<>();

    private final ServerLevel level;
    private final ServerPlayer player;
    private final boolean mayBuild;
    private final Crater crater;
    private final int perTick;
    private final boolean scorch;
    private int carved;

    private Carving(ServerLevel level, ServerPlayer player, boolean mayBuild, Crater crater, int ticks, boolean scorch) {
        this.level = level;
        this.player = player;
        this.mayBuild = mayBuild;
        this.crater = crater;
        this.perTick = Math.max(1, Mth.ceil(crater.blocks().size() / (float) Math.max(1, ticks)));
        this.scorch = scorch;
    }

    /** Called once by Leyline as it starts. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> CARVINGS.removeIf(Carving::tick));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CARVINGS.clear());
    }

    /**
     * Carves a crater {@code radius} wide and {@code depth} deep around {@code at} ({@link Crater#plan}) over
     * {@code ticks} ticks, starting now, so one given a single tick is carved at once; then, if {@code scorch}, sears its
     * floor and walls. Blocks break as {@code player} could break them; where they may not break at all
     * ({@code mayBuild} false), nothing is even planned.
     */
    public static void start(ServerLevel level, ServerPlayer player, boolean mayBuild, Vec3 at, double radius, double depth,
                             int ticks, boolean scorch) {
        if (!mayBuild) return;
        Crater crater = Crater.plan(level, at, radius, depth, level.getRandom());
        Carving carving = new Carving(level, player, mayBuild, crater, ticks, scorch);
        if (!carving.tick()) CARVINGS.add(carving);
    }

    /** Returns true once it's done. */
    private boolean tick() {
        List<BlockPos> blocks = crater.blocks();
        for (int i = 0; i < perTick && carved < blocks.size(); i++) {
            BlockPos pos = blocks.get(carved++);
            if (level.isLoaded(pos) && TerrainRules.mayVaporise(level, player, mayBuild, pos, level.getBlockState(pos))) {
                TerrainRules.vaporise(level, pos);
            }
        }
        if (carved < blocks.size()) return false;
        if (scorch) TerrainRules.scorch(level, player, mayBuild, crater.shell(), level.getRandom());
        return true;
    }
}
