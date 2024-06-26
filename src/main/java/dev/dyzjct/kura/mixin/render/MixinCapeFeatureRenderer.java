package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.module.modules.render.AnimationRemover;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CapeFeatureRenderer.class)
public abstract class MixinCapeFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    public MixinCapeFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    /**
     * @author dyzjct
     * @reason fix AnimationRemover's Bug
     */
    @Overwrite
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, float h, float j, float k, float l) {
        if (!abstractClientPlayerEntity.isInvisible() && abstractClientPlayerEntity.isPartVisible(PlayerModelPart.CAPE)) {
            SkinTextures skinTextures = abstractClientPlayerEntity.getSkinTextures();
            if (skinTextures.capeTexture() != null) {
                ItemStack itemStack = abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST);
                if (!itemStack.isOf(Items.ELYTRA)) {
                    matrixStack.push();
                    matrixStack.translate(0.0F, 0.0F, 0.125F);
                    if (AnimationRemover.INSTANCE.getFakeSneak() && AnimationRemover.INSTANCE.isEnabled() && !abstractClientPlayerEntity.isInSneakingPose()) {
                        matrixStack.translate(0.0F, 0.250F, 0.125F);
                    }
                    double d = MathHelper.lerp((double)h, abstractClientPlayerEntity.prevCapeX, abstractClientPlayerEntity.capeX) - MathHelper.lerp((double)h, abstractClientPlayerEntity.prevX, abstractClientPlayerEntity.getX());
                    double e = MathHelper.lerp((double)h, abstractClientPlayerEntity.prevCapeY, abstractClientPlayerEntity.capeY) - MathHelper.lerp((double)h, abstractClientPlayerEntity.prevY, abstractClientPlayerEntity.getY());
                    double m = MathHelper.lerp((double)h, abstractClientPlayerEntity.prevCapeZ, abstractClientPlayerEntity.capeZ) - MathHelper.lerp((double)h, abstractClientPlayerEntity.prevZ, abstractClientPlayerEntity.getZ());
                    float n = MathHelper.lerpAngleDegrees(h, abstractClientPlayerEntity.prevBodyYaw, abstractClientPlayerEntity.bodyYaw);
                    double o = (double)MathHelper.sin(n * 0.017453292F);
                    double p = (double)(-MathHelper.cos(n * 0.017453292F));
                    float q = (float)e * 10.0F;
                    q = MathHelper.clamp(q, -6.0F, 32.0F);
                    float r = (float)(d * o + m * p) * 100.0F;
                    r = MathHelper.clamp(r, 0.0F, 150.0F);
                    float s = (float)(d * p - m * o) * 100.0F;
                    s = MathHelper.clamp(s, -20.0F, 20.0F);
                    if (r < 0.0F) {
                        r = 0.0F;
                    }

                    float t = MathHelper.lerp(h, abstractClientPlayerEntity.prevStrideDistance, abstractClientPlayerEntity.strideDistance);
                    q += MathHelper.sin(MathHelper.lerp(h, abstractClientPlayerEntity.prevHorizontalSpeed, abstractClientPlayerEntity.horizontalSpeed) * 6.0F) * 32.0F * t;

                if (abstractClientPlayerEntity.isInSneakingPose() || (AnimationRemover.INSTANCE.getFakeSneak() && AnimationRemover.INSTANCE.isEnabled())) {
                    q += 25.0F;
                }

                    matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F + r / 2.0F + q));
                    matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s / 2.0F));
                    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - s / 2.0F));
                    VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntitySolid(skinTextures.capeTexture()));
                    ((PlayerEntityModel)this.getContextModel()).renderCape(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV);
                    matrixStack.pop();
                }
            }
        }
    }
//    @Overwrite
//    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, float h, float j, float k, float l) {
//        if (abstractClientPlayerEntity.isInvisible() && !abstractClientPlayerEntity.isInvisible() && abstractClientPlayerEntity.isPartVisible(PlayerModelPart.CAPE) && abstractClientPlayerEntity.getSkinTextures().capeTexture() != null) {
//            ItemStack itemStack = abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST);
//            if (!itemStack.isOf(Items.ELYTRA)) {
//                matrixStack.push();
//                matrixStack.translate(0.0F, 0.0F, 0.125F);
//                if (AnimationRemover.INSTANCE.getFakeSneak() && AnimationRemover.INSTANCE.isEnabled() && !abstractClientPlayerEntity.isInSneakingPose()) {
//                    matrixStack.translate(0.0F, 0.250F, 0.125F);
//                }
//                double d = MathHelper.lerp(h, abstractClientPlayerEntity.prevCapeX, abstractClientPlayerEntity.capeX) - MathHelper.lerp(h, abstractClientPlayerEntity.prevX, abstractClientPlayerEntity.getX());
//                double e = MathHelper.lerp(h, abstractClientPlayerEntity.prevCapeY, abstractClientPlayerEntity.capeY) - MathHelper.lerp(h, abstractClientPlayerEntity.prevY, abstractClientPlayerEntity.getY());
//                double m = MathHelper.lerp(h, abstractClientPlayerEntity.prevCapeZ, abstractClientPlayerEntity.capeZ) - MathHelper.lerp(h, abstractClientPlayerEntity.prevZ, abstractClientPlayerEntity.getZ());
//                float n = MathHelper.lerpAngleDegrees(h, abstractClientPlayerEntity.prevBodyYaw, abstractClientPlayerEntity.bodyYaw);
//                double o = MathHelper.sin(n * 0.017453292F);
//                double p = -MathHelper.cos(n * 0.017453292F);
//                float q = (float) e * 10.0F;
//                q = MathHelper.clamp(q, -6.0F, 32.0F);
//                float r = (float) (d * o + m * p) * 100.0F;
//                r = MathHelper.clamp(r, 0.0F, 150.0F);
//                float s = (float) (d * p - m * o) * 100.0F;
//                s = MathHelper.clamp(s, -20.0F, 20.0F);
//                if (r < 0.0F) {
//                    r = 0.0F;
//                }
//
//                float t = MathHelper.lerp(h, abstractClientPlayerEntity.prevStrideDistance, abstractClientPlayerEntity.strideDistance);
//                q += MathHelper.sin(MathHelper.lerp(h, abstractClientPlayerEntity.prevHorizontalSpeed, abstractClientPlayerEntity.horizontalSpeed) * 6.0F) * 32.0F * t;
//                if (abstractClientPlayerEntity.isInSneakingPose() || (AnimationRemover.INSTANCE.getFakeSneak() && AnimationRemover.INSTANCE.isEnabled())) {
//                    q += 25.0F;
//                }
//
//                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F + r / 2.0F + q));
//                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s / 2.0F));
//                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - s / 2.0F));
//                VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntitySolid(abstractClientPlayerEntity.getSkinTextures().capeTexture()));
//                this.getContextModel().renderCape(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV);
//                matrixStack.pop();
//            }
//        }
//    }
}
