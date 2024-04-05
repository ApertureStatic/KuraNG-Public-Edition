package dev.dyzjct.kura.mixin.player;

import dev.dyzjct.kura.mixins.IClientPlayerInteractionManager;
import dev.dyzjct.kura.module.modules.misc.BetterEat;
import dev.dyzjct.kura.module.modules.player.Reach;
import base.events.block.BlockEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PotionItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager implements IClientPlayerInteractionManager {
    @Shadow
    protected abstract void syncSelectedSlot();

    @Inject(method = {"attackBlock"}, at = @At("HEAD"))
    private void onPlayerClickBlock(final BlockPos pos, final Direction face, final CallbackInfoReturnable<Boolean> info) {
        new BlockEvent(pos, face).post();
    }

    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true)
    private void onGetReachDistance(CallbackInfoReturnable<Float> info) {
        if (Reach.INSTANCE.isEnabled()) info.setReturnValue(Reach.INSTANCE.getRange().getValue());
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
    public void onStoppedUsingItem(PlayerEntity playerIn, CallbackInfo ci) {
        if (playerIn.getActiveItem().getItem().isFood() || playerIn.getActiveItem().getItem() instanceof PotionItem) {
            if (playerIn.isUsingItem() && BetterEat.INSTANCE.getPacketEat() && BetterEat.INSTANCE.isEnabled()) {
                this.syncSelectedSlot();
                playerIn.stopUsingItem();
                ci.cancel();
            }
        }
    }

    @Override
    public void syncSelected() {
        syncSelectedSlot();
    }
}
