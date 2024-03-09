package dev.dyzjct.kura.mixin.player;

import dev.dyzjct.kura.module.modules.client.Cape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity extends MixinPlayerEntity {
    @Inject(method = "getCapeTexture", at = @At("HEAD"), cancellable = true)
    public void getCapeTexture(CallbackInfoReturnable<Identifier> cir) {
        if (Cape.INSTANCE.isEnabled()) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && player.equals(this)) {
                cir.setReturnValue(Cape.INSTANCE.getCape());
            }
        }
    }

    @Inject(method = "getElytraTexture", at = @At("HEAD"), cancellable = true)
    public void getElytraTexture(CallbackInfoReturnable<Identifier> cir) {
        if (Cape.INSTANCE.isEnabled()) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && player.equals(this)) {
                cir.setReturnValue(Cape.INSTANCE.getCape());
            }
        }
    }
}
