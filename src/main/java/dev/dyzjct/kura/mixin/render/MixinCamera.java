package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.module.modules.player.Freecam;
import dev.dyzjct.kura.module.modules.render.CameraClip;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class MixinCamera {

    @ModifyVariable(method = "clipToSpace", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double modifyClipToSpace(double d) {
        if (Freecam.INSTANCE.isEnabled()) {
            return 0;
        } else if (CameraClip.INSTANCE.isEnabled()) {
            return CameraClip.INSTANCE.getDistance();
        } else {
            return d;
        }
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(double desiredCameraDistance, CallbackInfoReturnable<Double> info) {
        if (CameraClip.INSTANCE.isEnabled() && CameraClip.INSTANCE.getClip()) {
            info.setReturnValue(desiredCameraDistance);
        }
    }
}
