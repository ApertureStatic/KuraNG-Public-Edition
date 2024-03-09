package dev.dyzjct.kura.mixin.render;

import base.events.client.VerificationEvent;
import net.minecraft.client.render.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Tessellator.class)
public class MixinTessellator {
    @Inject(method = "draw", at = @At("HEAD"))
    public void draw(CallbackInfo ci) {
        new VerificationEvent.DrawTessellator().post();
    }
}
