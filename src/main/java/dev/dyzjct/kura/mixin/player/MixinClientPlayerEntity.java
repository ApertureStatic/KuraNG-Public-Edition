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
    private PlayerMotionEvent motionEvent;

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

    /**
     * 合并并重写原版系统的发包核心
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void sendMovementPacketsHook(CallbackInfo ci) {
        ci.cancel(); // 挂起原版发包
        if (Wrapper.getPlayer() == null) return;

        try {
            // 1. 触发第一阶段的物理移动前置事件
            motionEvent = new PlayerMotionEvent(StageType.START, this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), this.isOnGround());
            motionEvent.post();
            EventAccessManager.INSTANCE.setData(motionEvent);
            if (motionEvent.getCancelled()) return;

            UpdateWalkingPlayerEvent.Pre.INSTANCE.post();
            this.sendSprintingPacket();

            boolean sneaking = this.isSneaking();
            if (sneaking != this.lastSneaking) {
                ClientCommandC2SPacket.Mode mode = sneaking ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
                this.networkHandler.sendPacket(new ClientCommandC2SPacket(this, mode));
                this.lastSneaking = sneaking;
            }

            if (this.isCamera()) {
                // 使用 motionEvent 中安全代理后的位置计算位移增量
                double deltaX = motionEvent.getX() - this.lastX;
                double deltaY = motionEvent.getY() - this.lastBaseY;
                double deltaZ = motionEvent.getZ() - this.lastZ;

                // 2. 提取玩家实体的原生朝向（绕过 Redirect 污染）
                float currentYaw = super.getYaw();
                float currentPitch = super.getPitch();

                // 3. 触发角度修改管道，派发给 RotationManager 计算平滑旋转
                MovementPacketsEvent movementPacketsEvent = new MovementPacketsEvent(currentYaw, currentPitch);
                movementPacketsEvent.post();

                // 得到最终要发往服务器的角度（如果是模块在转，这里就是平滑算出来的假角度）
                float finalYaw = movementPacketsEvent.getYaw();
                float finalPitch = movementPacketsEvent.getPitch();

                // 4. 【核心修复】使用 目标发送角 减去 上一次发送给服务器的角，来计算精确的视线裁决剪刀差！
                double deltaYaw = finalYaw - this.lastYaw;
                double deltaPitch = finalPitch - this.lastPitch;

                this.ticksSinceLastPositionPacketSent++;

                // 判定位置是否改变
                boolean positionChanged = MathHelper.squaredMagnitude(deltaX, deltaY, deltaZ) > MathHelper.square(2.0E-4)
                        || this.ticksSinceLastPositionPacketSent >= 20
                        || (CombatSystem.INSTANCE.getPacketControl() && CombatSystem.INSTANCE.getPositionControl() && CombatSystem.INSTANCE.getPosition_timer().passed(CombatSystem.INSTANCE.getPositionDelay()));

                // 判定角度是否改变
                boolean rotationChanged = (deltaYaw != 0.0 || deltaPitch != 0.0
                        || (CombatSystem.INSTANCE.getPacketControl() && CombatSystem.INSTANCE.getRotateControl() && CombatSystem.INSTANCE.getRotation_timer().passed(CombatSystem.INSTANCE.getRotationDelay())));

                // 5. 根据裁决结果，组装正确的 C03 数据包发往服务器
                if (this.hasVehicle()) {
                    Vec3d vec3d = this.getVelocity();
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(vec3d.x, -999.0, vec3d.z, finalYaw, finalPitch, motionEvent.isOnGround()));
                    positionChanged = false;
                } else if (positionChanged && rotationChanged) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(motionEvent.getX(), motionEvent.getY(), motionEvent.getZ(), finalYaw, finalPitch, motionEvent.isOnGround()));
                } else if (positionChanged) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(motionEvent.getX(), motionEvent.getY(), motionEvent.getZ(), motionEvent.isOnGround()));
                } else if (rotationChanged) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(finalYaw, finalPitch, motionEvent.isOnGround()));
                } else if (this.lastOnGround != motionEvent.isOnGround() || (CombatSystem.INSTANCE.getPacketControl() && CombatSystem.INSTANCE.getGroundControl() && CombatSystem.INSTANCE.getGround_timer().passed(CombatSystem.INSTANCE.getGroundDelay()))) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(motionEvent.isOnGround()));
                }

                // 更新历史状态缓存
                if (positionChanged) {
                    this.lastX = motionEvent.getX();
                    this.lastBaseY = motionEvent.getY();
                    this.lastZ = motionEvent.getZ();
                    this.ticksSinceLastPositionPacketSent = 0;
                }

                if (rotationChanged) {
                    this.lastYaw = finalYaw;
                    this.lastPitch = finalPitch;
                }

                this.lastOnGround = motionEvent.isOnGround();
                this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
            }

            UpdateWalkingPlayerEvent.Post.INSTANCE.post();

            // 6. 触发末尾发包返回事件
            PlayerMotionEvent oldEvent = new PlayerMotionEvent(StageType.END, motionEvent);
            oldEvent.post();
            EventAccessManager.INSTANCE.setData(oldEvent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 影子注入与影子映射字段 ====================
    @Shadow private void sendSprintingPacket() {}
    @Shadow public abstract boolean isSneaking();
    @Shadow public boolean lastSneaking;
    @Shadow @Final public ClientPlayNetworkHandler networkHandler;
    @Shadow protected abstract boolean isCamera();
    @Shadow public double lastX;
    @Shadow public double lastBaseY;
    @Shadow public double lastZ;
    @Shadow public int ticksSinceLastPositionPacketSent;
    @Shadow public float lastYaw;
    @Shadow public float lastPitch;
    @Shadow public boolean lastOnGround;
    @Shadow private boolean autoJumpEnabled;
    @Shadow @Final protected MinecraftClient client;
}