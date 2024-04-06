package dev.dyzjct.kura.mixin.player;

import base.utils.chat.ChatUtil;
import dev.dyzjct.kura.manager.EventAccessManager;
import dev.dyzjct.kura.module.modules.render.AntiPlayerSwing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class MixinBipedEntityModel<T extends LivingEntity> {
    @Mutable
    @Final
    @Shadow
    public final ModelPart head;
    @Mutable
    @Final
    @Shadow
    public final ModelPart leftArm;
    @Mutable
    @Final
    @Shadow
    public final ModelPart rightArm;
    @Mutable
    @Final
    @Shadow
    public final ModelPart leftLeg;
    @Mutable
    @Final
    @Shadow
    public final ModelPart rightLeg;

    @Shadow
    @Final
    public ModelPart body;

    @Shadow
    @Final
    public ModelPart hat;

    public MixinBipedEntityModel(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
        this.head = head;
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
    }

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;leaningPitch:F", ordinal = 3))
    private void revertSwordAnimation(T livingEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if (livingEntity instanceof PlayerEntity && livingEntity.equals(MinecraftClient.getInstance().player) && EventAccessManager.INSTANCE.getData() != null) {
            if (head != null) {
                head.pitch = EventAccessManager.INSTANCE.getData().getPitch() / (180F / (float) Math.PI);
            }
        }
        if (livingEntity != null && AntiPlayerSwing.INSTANCE.isEnabled() && EventAccessManager.INSTANCE.getData() != null && livingEntity instanceof PlayerEntity) {
            if (AntiPlayerSwing.INSTANCE.getFakeSneak()) {
                if (head != null) {
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("head.pivotX " + head.pivotX);
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("head.pivotY " + head.pivotY);
                    head.pivotY = 4.2f;
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("head.pivotZ " + head.pivotZ);
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("head.yaw " + head.yaw);
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("head.pitch " + head.pitch);
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("head.roll " + head.roll);
                }
                if (body != null) {
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("body.pivotX " + body.pivotX);
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("body.pivotY " + body.pivotY);
                    body.pivotY = 3.2f;
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("body.pivotZ " + body.pivotZ);
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("body.yaw " + body.yaw);
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("body.pitch " + body.pitch);
                    body.pitch = 0.5f;
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("body.roll " + body.roll);
                }
                if (leftLeg != null) {
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftLeg.pivotX " + leftLeg.pivotX);
                    leftLeg.pivotX = 1.9f;
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftLeg.pivotY " + leftLeg.pivotY);
                    leftLeg.pivotY = 12.2f;
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftLeg.pivotZ " + leftLeg.pivotZ);
                    leftLeg.pivotZ = 4.0f;
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("leftLeg.yaw" + leftLeg.yaw);
                    leftLeg.yaw = -0.005f;
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftLeg.pitch " + leftLeg.pitch);
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftLeg.roll " + leftLeg.roll);
                }
                if (leftArm != null) {
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftArm.pivotX " + leftArm.pivotX);
                    leftArm.pivotX = 5.0f;
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftArm.pivotY " + leftArm.pivotY);
                    leftArm.pivotY = 5.2f;
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftArm.pivotZ " + leftArm.pivotZ);
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftArm.yaw " + leftArm.yaw);
                    if (AntiPlayerSwing.INSTANCE.getDebug())
                        ChatUtil.INSTANCE.sendMessage("leftArm.roll " + leftArm.roll);
                }
                if (hat != null) {
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("hat.pivotX " + hat.pivotX);
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("hat.pivotY " + hat.pivotY);
                    hat.pivotY = 4.2f;
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("hat.pivotZ " + hat.pivotZ);
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("hat.yaw " + hat.yaw);
                    if (AntiPlayerSwing.INSTANCE.getDebug()) ChatUtil.INSTANCE.sendMessage("hat.roll " + hat.roll);
                }
                if (rightLeg != null) {
                    rightLeg.pivotY = 12.2f;
                    rightLeg.pivotZ = 4.0f;
                    rightLeg.yaw = -0.005f;
                }
                if (rightArm != null) {
                    rightArm.pivotY = 5.2f;
                }
            }
            if (AntiPlayerSwing.INSTANCE.getArm()) {
                if (leftArm != null) {
                    leftArm.pitch = 0f;
                }
                if (rightArm != null) {
                    rightArm.pitch = 0f;
                }
            }
            if (AntiPlayerSwing.INSTANCE.getLeg()) {
                if (leftLeg != null) {
                    leftLeg.pitch = 0f;
                }
                if (rightLeg != null) {
                    rightLeg.pitch = 0f;
                }
            }
        }
    }
}
