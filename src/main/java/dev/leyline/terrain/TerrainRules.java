package dev.leyline.terrain;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What an effect may do to terrain, shared so every effect follows the same rules: blocks only break where the player
 * behind it could break them by hand, so never under spawn protection, in claimed land that refuses them, or beyond
 * the world border, and never anything unbreakable or as blast resistant as obsidian.
 *
 * <p>Whether an effect may break blocks at all is the caller's to decide (Adventure mode, a game rule of its own): read
 * it once, when the effect starts, and pass it in as {@code mayBuild}, so something still in flight follows the rules of
 * the moment it began, whatever the player or the server does meanwhile.
 */
public final class TerrainRules {
    /** Blocks at least this blast resistant (obsidian is 1200) are never vaporised. */
    public static final float VAPORISE_RESISTANCE_LIMIT = 1200;

    private TerrainRules() {
    }

    /**
     * The terrain half of a vanilla explosion: vanilla resistance, drops and TNT chain reactions, where {@code player} may
     * break blocks. It hurts and pushes no entities, so an effect that deals its own damage hits nothing twice. Skipped
     * blocks aren't hit at all, so protected TNT isn't set off either.
     */
    public static ExplosionDamageCalculator explosion(ServerLevel level, ServerPlayer player, boolean mayBuild) {
        return new ExplosionDamageCalculator() {
            @Override public boolean shouldBlockExplode(Explosion explosion, BlockGetter blocks, BlockPos pos, BlockState state, float strength) {
                return mayBuild && level.mayInteract(player, pos) && super.shouldBlockExplode(explosion, blocks, pos, state, strength);
            }

            @Override public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return false;
            }

            @Override public float getKnockbackMultiplier(Entity entity) {
                return 0;
            }
        };
    }

    /**
     * Whether an effect may vaporise this block: where {@code player} may break blocks, and unless it's unbreakable or at
     * least {@link #VAPORISE_RESISTANCE_LIMIT} blast resistant.
     */
    public static boolean mayVaporise(ServerLevel level, ServerPlayer player, boolean mayBuild, BlockPos pos, BlockState state) {
        return mayBuild && !state.isAir() && state.getDestroySpeed(level, pos) >= 0
                && state.getBlock().getExplosionResistance() < VAPORISE_RESISTANCE_LIMIT && level.mayInteract(player, pos);
    }

    /** Natural ground, which effects may scorch or reshape: stone, dirt, sand, gravel and terracotta. */
    public static boolean isGround(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(BlockTags.TERRACOTTA);
    }

    /** Whether the block at {@code pos} has air on any side. */
    public static boolean exposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir()) return true;
        }
        return false;
    }

    /** Removes the block without drops; a container still spills what it held. */
    public static void vaporise(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Sears a crater's exposed floor and walls ({@code shell}): natural ground turns partly to blackstone, basalt and magma. */
    public static void scorch(ServerLevel level, ServerPlayer player, boolean mayBuild, List<BlockPos> shell, RandomSource random) {
        for (BlockPos pos : shell) {
            BlockState state = level.getBlockState(pos);
            if (!isGround(state) || !mayVaporise(level, player, mayBuild, pos, state) || !exposed(level, pos)) continue;
            float roll = random.nextFloat();
            BlockState seared = roll < 0.15f ? Blocks.MAGMA_BLOCK.defaultBlockState()
                    : roll < 0.5f ? Blocks.BLACKSTONE.defaultBlockState()
                    : roll < 0.65f ? Blocks.BASALT.defaultBlockState()
                    : null;
            if (seared != null) level.setBlock(pos, seared, Block.UPDATE_ALL);
        }
    }
}
