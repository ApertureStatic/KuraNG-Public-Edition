package dev.dyzjct.kura.mixin.player;

import dev.dyzjct.kura.manager.EventAccessManager;
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

    public MixinBipedEntityModel(ModelPart head) {
        this.head = head;
    }

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;leaningPitch:F", ordinal = 3))
    private void revertSwordAnimation(T livingEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if (livingEntity instanceof PlayerEntity && livingEntity.equals(MinecraftClient.getInstance().player) && EventAccessManager.INSTANCE.getData() != null) {
            head.pitch = EventAccessManager.INSTANCE.getData().getPitch() / (180F / (float) Math.PI);
        }
    }
}
