package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.module.modules.player.FreeCam;
import dev.dyzjct.kura.module.modules.render.CameraClip;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public class MixinCamera {

    @Shadow
    private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void updateHook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled()) {
            this.thirdPerson = true;
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void setRotationHook(Args args) {
        if (FreeCam.INSTANCE.isEnabled())
            args.setAll(FreeCam.INSTANCE.getFakeYaw(), FreeCam.INSTANCE.getFakePitch());
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void setPosHook(Args args) {
        if (FreeCam.INSTANCE.isEnabled())
            args.setAll(FreeCam.INSTANCE.getFakeX(), FreeCam.INSTANCE.getFakeY(), FreeCam.INSTANCE.getFakeZ());
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(double desiredCameraDistance, CallbackInfoReturnable<Double> info) {
        if (CameraClip.INSTANCE.isEnabled() && CameraClip.INSTANCE.getClip()) {
            info.setReturnValue(desiredCameraDistance);
        }
    }
}
