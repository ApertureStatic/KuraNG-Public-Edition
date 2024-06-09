package dev.dyzjct.kura.mixin.player;

import base.events.player.JumpEvent;
import base.events.player.PlayerMotionEvent;
import base.events.player.PlayerMoveEvent;
import base.system.event.StageType;
import dev.dyzjct.kura.manager.EventAccessManager;
import dev.dyzjct.kura.module.modules.movement.NoSlowDown;
import dev.dyzjct.kura.module.modules.movement.Velocity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends MixinPlayerEntity {
    @Unique
    public PlayerMotionEvent motionEvent;

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
    private boolean tickMovementHook(ClientPlayerEntity player) {
        if (NoSlowDown.INSTANCE.isEnabled()) {
            return false;
        }
        return player.isUsingItem();
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        PlayerMoveEvent event = new PlayerMoveEvent(movementType, movement);
        event.post();
        if (event.getCancelled()) {
            super.move(movementType, event.getVec());
            ci.cancel();
        }
    }

    @Inject(method = {"pushOutOfBlocks"}, at = @At("HEAD"), cancellable = true)
    public void pushOutOfBlocksHook(double x, double z, CallbackInfo ci) {
        if (Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.getNoPush().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void onTickMovementHead(CallbackInfo callbackInfo) {
        motionEvent = new PlayerMotionEvent(StageType.START, this.getX(), getY(), this.getZ(), this.yaw, this.pitch, this.onGround);
        motionEvent.post();
        EventAccessManager.INSTANCE.setData(motionEvent);
        if (motionEvent.getCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getX()D"))
    private double posXHook(ClientPlayerEntity instance) {
        return motionEvent.getX();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getY()D"))
    private double posYHook(ClientPlayerEntity instance) {
        return motionEvent.getY();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getZ()D"))
    private double posZHook(ClientPlayerEntity instance) {
        return motionEvent.getZ();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float yawHook(ClientPlayerEntity instance) {
        return motionEvent.getYaw();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float pitchHook(ClientPlayerEntity instance) {
        return motionEvent.getPitch();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
    private boolean groundHook(ClientPlayerEntity instance) {
        return motionEvent.isOnGround();
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "RETURN"))
    private void sendMovementPackets_Return(CallbackInfo callbackInfo) {
        PlayerMotionEvent oldEvent = new PlayerMotionEvent(StageType.END, motionEvent);
        oldEvent.post();
        EventAccessManager.INSTANCE.setData(oldEvent);
    }

    @Override
    public void jump() {
        JumpEvent event = JumpEvent.INSTANCE;
        event.post();
        if (!event.getCancelled()) super.jump();
    }
}
