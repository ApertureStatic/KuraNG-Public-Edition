package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.module.modules.render.Brightness;
import dev.dyzjct.kura.module.modules.render.NameTags;
import base.events.render.RenderEntityEvent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer<T extends Entity> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void renderLabelIfPresent(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        if (entity instanceof PlayerEntity && NameTags.INSTANCE.isEnabled()) {
            info.cancel();
        }
    }

    @Inject(method = "getSkyLight", at = @At("RETURN"), cancellable = true)
    private void onGetSkyLight(CallbackInfoReturnable<Integer> info) {
        if (Brightness.INSTANCE.isEnabled()) {
            info.setReturnValue(Brightness.INSTANCE.getBrightness().getValue());
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityEvent.setRenderingEntities(true);
        if (entity == null || !RenderEntityEvent.getRenderingEntities()) return;

        RenderEntityEvent eventAll = new RenderEntityEvent.All.Pre(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        eventAll.post();
    }

    @Inject(method = "render", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void renderEntityPost(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityEvent.setRenderingEntities(false);
        if (entity == null || !RenderEntityEvent.getRenderingEntities()) return;

        RenderEntityEvent event = new RenderEntityEvent.All.Post(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        event.post();
    }
}
