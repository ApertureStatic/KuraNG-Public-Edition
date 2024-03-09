package dev.dyzjct.kura.mixins

import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext.FluidHandling
import net.minecraft.world.RaycastContext.ShapeType

interface IRaycastContext {
    operator fun set(start: Vec3d?, end: Vec3d?, shapeType: ShapeType?, fluidHandling: FluidHandling?, entity: Entity?)
}
