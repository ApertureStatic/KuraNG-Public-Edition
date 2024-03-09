package dev.dyzjct.kura.mixin.player;

import dev.dyzjct.kura.mixin.entity.MixinLivingEntity;
import base.events.player.PlayerTravelEvent;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void onTravel(Vec3d movementInput, CallbackInfo ci) {
        PlayerTravelEvent event = new PlayerTravelEvent(movementInput);
        event.post();
        if (event.getCancelled()) {
            move(MovementType.PLAYER, getVelocity());
            ci.cancel();
        }
    }
}