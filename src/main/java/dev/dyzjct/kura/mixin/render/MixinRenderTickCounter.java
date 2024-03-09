package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.Kura;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.class)
public class MixinRenderTickCounter {
    @Shadow
    public float lastFrameDuration;
    @Shadow
    public float tickDelta;
    @Shadow
    public float tickTime;
    @Shadow
    private long prevTimeMillis;

    @Inject(method = "beginRenderTick", at = @At("HEAD"), cancellable = true)
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> ci) {
        this.lastFrameDuration = ((timeMillis - this.prevTimeMillis) / this.tickTime) * Kura.Companion.getTICK_TIMER();
        this.prevTimeMillis = timeMillis;
        this.tickDelta += this.lastFrameDuration;
        int i = (int) this.tickDelta;
        this.tickDelta -= i;
        ci.setReturnValue(i);
    }
}
