package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.module.modules.render.Chams;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(EndCrystalEntityRenderer.class)
public class MixinEndCrystalEntityRenderer {
    @Unique
    private EndCrystalEntity lastEntity;

    @Inject(method = "render*", at = @At("HEAD"))
    public void onRender(EndCrystalEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        lastEntity = livingEntity;
    }

    @Redirect(method = "render*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"))
    public void onRenderPart(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        if (Chams.INSTANCE.isEnabled() && Chams.INSTANCE.getCrystals()) {
            Color clr = Chams.INSTANCE.getEntityColor(lastEntity);
            modelPart.render(matrices, vertices, light, overlay, clr.getRed() / 255F, clr.getGreen() / 255F, clr.getBlue() / 255F, clr.getAlpha() / 255F);
            return;
        }
        modelPart.render(matrices, vertices, light, overlay, 1f, 1f, 1f, 1f);
    }
}
