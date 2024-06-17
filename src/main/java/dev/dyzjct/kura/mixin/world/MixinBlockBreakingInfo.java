package dev.dyzjct.kura.mixin.world;

import dev.dyzjct.kura.event.events.block.BlockBreakEvent;
import net.minecraft.client.render.BlockBreakingInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBreakingInfo.class)
public class MixinBlockBreakingInfo {
    @Inject(method = "compareTo(Lnet/minecraft/client/render/BlockBreakingInfo;)I", at = @At("HEAD"))
    public void onSendingBlockBreakProgressPre(BlockBreakingInfo blockBreakingInfo, CallbackInfoReturnable<Integer> cir){
        new BlockBreakEvent(blockBreakingInfo.getActorId(), blockBreakingInfo.getPos(), blockBreakingInfo.getStage()).post();
    }
}
