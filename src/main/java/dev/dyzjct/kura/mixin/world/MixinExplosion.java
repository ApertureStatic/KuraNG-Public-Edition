package dev.dyzjct.kura.mixin.world;

import dev.dyzjct.kura.mixins.IExplosion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Explosion.class)
public class MixinExplosion implements IExplosion {
    @Shadow
    @Final
    @Mutable
    private World world;
    @Shadow
    @Final
    @Mutable
    @Nullable
    private Entity entity;

    @Shadow
    @Final
    @Mutable
    private double x;
    @Shadow
    @Final
    @Mutable
    private double y;
    @Shadow
    @Final
    @Mutable
    private double z;

    @Shadow
    @Final
    @Mutable
    private float power;
    @Shadow
    @Final
    @Mutable
    private boolean createFire;
    @Shadow
    @Final
    @Mutable
    private Explosion.DestructionType destructionType;
    @Shadow
    @Final
    private DamageSource damageSource;

    @Override
    public @NotNull DamageSource getDamageSource() {
        return damageSource;
    }

    @Override
    public void set(Vec3d pos, float power, boolean createFire) {
        this.world = MinecraftClient.getInstance().world;
        this.entity = null;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.power = power;
        this.createFire = createFire;
        this.destructionType = Explosion.DestructionType.DESTROY;
    }
}
