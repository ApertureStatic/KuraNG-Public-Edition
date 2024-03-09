package dev.dyzjct.kura.mixin.entity;

import dev.dyzjct.kura.module.modules.render.HandView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {
    @Shadow
    protected void jump() {
    }

    @Shadow public abstract boolean isFallFlying();

    @ModifyConstant(method = "getHandSwingDuration", constant = @Constant(intValue = 6))
    private int getHandSwingDuration(int constant) {
        if ((Object) this != MinecraftClient.getInstance().player) return constant;
        return HandView.INSTANCE.isEnabled() && MinecraftClient.getInstance().options.getPerspective().isFirstPerson() ? HandView.INSTANCE.getSwingSpeed() : constant;
    }
}
