package dev.dyzjct.kura.mixin.player;

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
