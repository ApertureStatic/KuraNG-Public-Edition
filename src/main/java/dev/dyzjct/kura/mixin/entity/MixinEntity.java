package dev.dyzjct.kura.mixin.entity;

import dev.dyzjct.kura.module.modules.movement.NoSlowDown;
import dev.dyzjct.kura.module.modules.movement.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    static TrackedData<Byte> FLAGS;
    @Shadow
    public Vec3d pos;
    @Shadow
    public float yaw;
    @Shadow
    public float pitch;
    @Shadow
    public boolean onGround;

    @Shadow
    public abstract Vec3d getPos();

    @Shadow
    public abstract boolean isOnGround();

    @Shadow
    public abstract float getPitch();

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract Box getBoundingBox();

    @Shadow
    public void move(MovementType movementType, Vec3d movement) {
    }

    @Shadow
    public abstract Vec3d getVelocity();

    @Shadow public abstract boolean hasVehicle();

    @ModifyArgs(method = "pushAwayFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
    public void pushAwayFromHook(Args args) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            args.set(0, (double) args.get(0) * 0);
            args.set(2, (double) args.get(2) * 0);
        }
    }

    @Redirect(method = "updateMovementInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isPushedByFluids()Z"))
    public boolean updateMovementInFluid(Entity entity) {
        return entity.isPushedByFluids() && !(Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.getNoPush().getValue());
    }

    @Inject(method = "adjustMovementForPiston", at = @At("HEAD"), cancellable = true)
    public void onAdjustPistonMovement(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
        if (NoSlowDown.INSTANCE.isEnabled() && NoSlowDown.INSTANCE.getPiston()) {
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                MinecraftClient.getInstance().getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(getX(), getY() - 1f, getZ(), getYaw(), getPitch(), true));
            }
            cir.setReturnValue(Vec3d.ZERO);
        }
    }
}
