package dev.dyzjct.kura.mixin.player;

import base.utils.Wrapper;
import com.mojang.authlib.GameProfile;
import dev.dyzjct.kura.event.eventbus.StageType;
import dev.dyzjct.kura.event.events.MovementPacketsEvent;
import dev.dyzjct.kura.event.events.player.*;
import dev.dyzjct.kura.manager.EventAccessManager;
import dev.dyzjct.kura.manager.RotationManager;
import dev.dyzjct.kura.module.modules.client.CombatSystem;
import dev.dyzjct.kura.module.modules.movement.NoSlowDown;
import dev.dyzjct.kura.module.modules.movement.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    @Unique
    public PlayerMotionEvent motionEvent;

    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

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

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.AFTER))
    private void tick$AFTER(CallbackInfo info) {
        UpdateMovementEvent.Pre event = new UpdateMovementEvent.Pre();
        event.post();
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;tickables:Ljava/util/List;", shift = At.Shift.BEFORE))
    private void tick$tickables(CallbackInfo ci) {
        UpdateMovementEvent.Post event = new UpdateMovementEvent.Post();
        event.post();
    }


    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.BEFORE))
    private void tick$BEFORE(CallbackInfo info) {
        PlayerUpdateEvent event = new PlayerUpdateEvent();
        event.post();
    }

    @Inject(method = "sendMovementPackets", at = {@At("HEAD")}, cancellable = true)
    private void sendMovementPacketsHook(CallbackInfo ci) {
        ci.cancel();
        if (Wrapper.getPlayer() == null) return;
        try {
            UpdateWalkingPlayerEvent.Pre.INSTANCE.post();
            this.sendSprintingPacket();
            boolean bl = this.isSneaking();
            if (bl != this.lastSneaking) {
                ClientCommandC2SPacket.Mode mode = bl ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
                this.networkHandler.sendPacket(new ClientCommandC2SPacket(this, mode));
                this.lastSneaking = bl;
            }

            if (this.isCamera()) {
                double d = this.getX() - this.lastX;
                double e = this.getY() - this.lastBaseY;
                double f = this.getZ() - this.lastZ;

                float yaw = this.getYaw();
                float pitch = this.getPitch();
                MovementPacketsEvent movementPacketsEvent = new MovementPacketsEvent(yaw, pitch);
                movementPacketsEvent.post();
                yaw = movementPacketsEvent.getYaw();
                pitch = movementPacketsEvent.getPitch();

                double g = yaw - RotationManager.INSTANCE.getYaw_value();//this.lastYaw;
                double h = pitch - RotationManager.INSTANCE.getPitch_value();//this.lastPitch;

                ++this.ticksSinceLastPositionPacketSent;
                boolean bl2 = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20 || (CombatSystem.INSTANCE.getPacketControl() && CombatSystem.INSTANCE.getPositionControl() && CombatSystem.INSTANCE.getPosition_timer().passed(CombatSystem.INSTANCE.getPositionDelay()));
                boolean bl3 = (g != 0.0 || h != 0.0 || (CombatSystem.INSTANCE.getPacketControl() && CombatSystem.INSTANCE.getRotateControl() && CombatSystem.INSTANCE.getRotation_timer().passed(CombatSystem.INSTANCE.getRotationDelay())));
                if (this.hasVehicle()) {
                    Vec3d vec3d = this.getVelocity();
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(vec3d.x, -999.0, vec3d.z, yaw, pitch, this.isOnGround()));
                    bl2 = false;
                } else if (bl2 && bl3) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(this.getX(), this.getY(), this.getZ(), yaw, pitch, this.isOnGround()));
                } else if (bl2) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(this.getX(), this.getY(), this.getZ(), this.isOnGround()));
                } else if (bl3) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, this.isOnGround()));
                } else if (this.lastOnGround != this.isOnGround() || CombatSystem.INSTANCE.getPacketControl() && CombatSystem.INSTANCE.getGroundControl() && CombatSystem.INSTANCE.getGround_timer().passed(CombatSystem.INSTANCE.getGroundDelay())) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(this.isOnGround()));
                }

                if (bl2) {
                    this.lastX = this.getX();
                    this.lastBaseY = this.getY();
                    this.lastZ = this.getZ();
                    this.ticksSinceLastPositionPacketSent = 0;
                }

                if (bl3) {
                    this.lastYaw = yaw;
                    this.lastPitch = pitch;
                }

                this.lastOnGround = this.isOnGround();
                this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
            }
            UpdateWalkingPlayerEvent.Post.INSTANCE.post();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Shadow
    private void sendSprintingPacket() {
    }

    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    public boolean lastSneaking;

    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    @Shadow
    protected abstract boolean isCamera();

    @Shadow
    public double lastX;

    @Shadow
    public double lastBaseY;

    @Shadow
    public double lastZ;

    @Shadow
    public int ticksSinceLastPositionPacketSent;

    @Shadow
    public float lastYaw;

    @Shadow
    public float lastPitch;

    @Shadow
    public boolean lastOnGround;

    @Shadow
    private boolean autoJumpEnabled;

    @Shadow
    @Final
    protected MinecraftClient client;

    @Shadow
    public abstract float getYaw(float tickDelta);

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
}
