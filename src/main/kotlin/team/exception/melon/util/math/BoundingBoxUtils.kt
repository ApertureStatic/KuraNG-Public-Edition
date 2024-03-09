package team.exception.melon.util.math

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import team.exception.melon.util.math.VectorUtils.plus
import team.exception.melon.util.math.VectorUtils.times
import team.exception.melon.util.math.VectorUtils.toViewVec
import team.exception.melon.util.math.vector.Vec2f
import kotlin.math.min


val Box.xCenter get() = minX + xLength * 0.5

val Box.yCenter get() = minY + yLength * 0.5

val Box.zCenter get() = minZ + zLength * 0.5
val Box.lengths get() = Vec3d(xLength, yLength, zLength)

fun Box.corners(scale: Double): Array<Vec3d> {
    val growSizes = lengths * (scale - 1.0)
    return expand(growSizes.x, growSizes.y, growSizes.z).corners()
}

fun Box.corners() = arrayOf(
    Vec3d(minX, minY, minZ),
    Vec3d(minX, minY, maxZ),
    Vec3d(minX, maxY, minZ),
    Vec3d(minX, maxY, maxZ),
    Vec3d(maxX, minY, minZ),
    Vec3d(maxX, minY, maxZ),
    Vec3d(maxX, maxY, minZ),
    Vec3d(maxX, maxY, maxZ),
)

fun Box.side(side: Direction, scale: Double = 0.5): Vec3d {
    val lengths = lengths
    val sideDirectionVec = side.vector.toVec3d()
    return lengths * sideDirectionVec * scale + center
}

fun Box.scale(multiplier: Double): Box {
    return this.scale(multiplier, multiplier, multiplier)
}

fun Box.scale(x: Double, y: Double, z: Double): Box {
    val halfXLength = this.xLength * 0.5
    val halfYLength = this.yLength * 0.5
    val halfZLength = this.zLength * 0.5

    return this.expand(halfXLength * (x - 1.0), halfYLength * (y - 1.0), halfZLength * (z - 1.0))
}

fun Box.scale(multiplier: Float): Box {
    return this.scale(multiplier, multiplier, multiplier)
}

fun Box.scale(x: Float, y: Float, z: Float): Box {
    val halfXLength = this.xLength * 0.5f
    val halfYLength = this.yLength * 0.5f
    val halfZLength = this.zLength * 0.5f

    return this.expand(halfXLength * (x - 1f), halfYLength * (y - 1f), halfZLength * (z - 1f))
}

fun Box.limitSize(x: Double, y: Double, z: Double): Box {
    val halfX = min(xLength, x) / 2.0
    val halfY = min(yLength, y) / 2.0
    val halfZ = min(zLength, z) / 2.0
    val center = center

    return Box(
        center.x - halfX, center.y - halfY, center.z - halfZ,
        center.x + halfX, center.y + halfY, center.z + halfZ,
    )
}

fun Box.intersectsBlock(x: Int, y: Int, z: Int): Boolean {
    return intersects(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
}

fun Box.intersectsBlock(pos: BlockPos): Boolean {
    return intersectsBlock(pos.x, pos.y, pos.z)
}

fun Box.isInSight(
    posFrom: Vec3d,
    rotation: Vec2f,
    range: Double = 8.0,
    tolerance: Double = 0.0
): Boolean {
    return isInSight(posFrom, rotation.toViewVec(), range)
}

fun Box.isInSight(
    posFrom: Vec3d,
    viewVec: Vec3d,
    range: Double = 4.25,
    tolerance: Double = 0.0
): Boolean {
    val sightEnd = posFrom.add(viewVec.times(range))
    return expand(tolerance).intersects(posFrom, sightEnd)
}