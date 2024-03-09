package dev.dyzjct.kura.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dyzjct.kura.module.modules.render.NoRender;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {
    @Redirect(method = "applyFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getSubmersionType()Lnet/minecraft/client/render/CameraSubmersionType;"))
    private static CameraSubmersionType onApplyFog(Camera camera) {
        CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getBlockLayer().getValue()) {
            if (cameraSubmersionType == CameraSubmersionType.LAVA || cameraSubmersionType == CameraSubmersionType.WATER) {
                return CameraSubmersionType.NONE;
            }
        }
        return cameraSubmersionType;
    }

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void onRenderFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo info) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getFog().getValue()) {
            if (fogType == BackgroundRenderer.FogType.FOG_TERRAIN) {
                RenderSystem.setShaderFogStart(viewDistance * 4);
                RenderSystem.setShaderFogEnd(viewDistance * 4.25f);
            }
        }
    }

}
