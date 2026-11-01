package dev.leyline.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.leyline.client.render.AfterWorld;
import dev.leyline.client.screen.ScreenEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Widens or narrows the world's field of view, and rolls the view, for {@link ScreenEffects}. The roll goes in with the
 * hurt tilt, which tilts the world and the hand together; the hand keeps its own field of view. Also runs
 * {@link AfterWorld}'s drawing, once the whole world is drawn and before the hand.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void leyline$fov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (useFovSetting) cir.setReturnValue(cir.getReturnValueF() * ScreenEffects.fovScale(partialTick));
    }

    @Inject(method = "bobHurt", at = @At("HEAD"))
    private void leyline$roll(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        float roll = ScreenEffects.rollDegrees(partialTick);
        if (roll != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }

    /** The world is drawn, its depth still in the main target; the hand comes next. */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand"))
    private void leyline$afterWorld(DeltaTracker deltaTracker, CallbackInfo ci) {
        AfterWorld.draw(deltaTracker);
    }
}
