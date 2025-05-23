package dev.dyzjct.kura.mixin.entity;

import dev.dyzjct.kura.event.events.player.JumpEvent;
import dev.dyzjct.kura.module.modules.render.HandView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.dyzjct.kura.module.AbstractModule.mc;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {
    @Shadow
    protected void jump() {
    }

    @Shadow
    public abstract boolean isFallFlying();

    @ModifyConstant(method = "getHandSwingDuration", constant = @Constant(intValue = 6))
    private int getHandSwingDuration(int constant) {
        if ((Object) this != MinecraftClient.getInstance().player) return constant;
        return HandView.INSTANCE.isEnabled() && MinecraftClient.getInstance().options.getPerspective().isFirstPerson() ? HandView.INSTANCE.getSwingSpeed() : constant;
    }


    @Inject(method = "jump", at = @At("HEAD"))
    private void jump$HEAD(CallbackInfo info) {
        if ((Object) this != mc.player) return;
        JumpEvent.Pre event = new JumpEvent.Pre();
        event.post();
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void jump$RETURN(CallbackInfo info) {
        if ((Object) this != mc.player) return;
        JumpEvent.Post event = new JumpEvent.Post();
        event.post();
    }
}
