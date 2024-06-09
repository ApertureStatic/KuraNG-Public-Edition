package dev.dyzjct.kura.mixins

import net.minecraft.entity.damage.DamageSource
import net.minecraft.util.math.Vec3d

interface IExplosion {
    operator fun set(pos: Vec3d?, power: Float, createFire: Boolean)
    val damageSource: DamageSource
}
