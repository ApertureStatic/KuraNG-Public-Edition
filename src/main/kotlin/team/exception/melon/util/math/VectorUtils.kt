package team.exception.melon.util.math

import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import team.exception.melon.util.math.vector.Vec2f
import kotlin.math.cos
import kotlin.math.sin

object VectorUtils {
    fun getBlockPosInSphere(entity: Entity, radius: Float): Sequence<BlockPos> {
        return getBlockPosInSphere(entity.pos, radius)
    }

    fun getBlockPosInSphere(pos: Vec3d, radius: Float): Sequence<BlockPos> {
        return getBlockPosInSphere(pos.x, pos.y, pos.z, radius)
    }

    fun getBlockPosInSphere(cx: Double, cy: Double, cz: Double, radius: Float): Sequence<BlockPos> {
        val squaredRadius = radius.sq
        val blockPos = BlockPos.Mutable()

        return sequence {
            for (x in getAxisRange(cx, radius)) {
                for (y in getAxisRange(cy, radius)) {
                    for (z in getAxisRange(cz, radius)) {
                        blockPos.set(x, y, z)
                        if (blockPos.getSquaredDistanceFromCenter(cx, cy, cz) > squaredRadius) continue
                        yield(blockPos.toImmutable())
                    }
                }
            }
        }
    }

    private fun getAxisRange(d1: Double, d2: Float): IntRange {
        return IntRange((d1 - d2).floorToInt(), (d1 + d2).ceilToInt())
    }

    fun Vec2f.toViewVec(): Vec3d {
        val yawRad = this.x.toDouble().toRadians()
        val pitchRag = this.y.toDouble().toRadians()
        val yaw = -yawRad - PI_FLOAT
        val pitch = -pitchRag

        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = -cos(pitch)
        val sinPitch = sin(pitch)

        return Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch)
    }

    fun Vec3i.multiply(multiplier: Int): Vec3i {
        return Vec3i(this.x * multiplier, this.y * multiplier, this.z * multiplier)
    }

    infix operator fun Vec3d.times(vec3d: Vec3d): Vec3d = Vec3d(x * vec3d.x, y * vec3d.y, z * vec3d.z)

    infix operator fun Vec3d.times(multiplier: Double): Vec3d =
        Vec3d(x * multiplier, y * multiplier, z * multiplier)

    infix operator fun Vec3d.plus(vec3d: Vec3d): Vec3d = add(vec3d)

    infix operator fun Vec3d.minus(vec3d: Vec3d): Vec3d = subtract(vec3d)

    fun BlockPos.Mutable.setAndAdd(set: Vec3i, add: Vec3i): BlockPos.Mutable {
        return this.set(set.x + add.x, set.y + add.y, set.z + add.z)
    }

    fun BlockPos.Mutable.setAndAdd(set: Vec3i, x: Int, y: Int, z: Int): BlockPos.Mutable {
        return this.set(set.x + x, set.y + y, set.z + z)
    }

    fun BlockPos.Mutable.setAndAdd(set: BlockPos, direction: Direction): BlockPos.Mutable {
        return this.setAndAdd(set, direction.vector)
    }

    fun BlockPos.Mutable.setAndAdd(set: BlockPos, direction: Direction, n: Int): BlockPos.Mutable {
        val dirVec = direction.vector
        return this.set(set.x + dirVec.x * n, set.y + dirVec.y * n, set.z + dirVec.z * n)
    }

    fun toLong(blockPos: BlockPos): Long {
        return BlockPosUtil.toLong(blockPos.x, blockPos.y, blockPos.z)
    }

    fun toLong(x: Int, y: Int, z: Int): Long {
        return BlockPosUtil.toLong(x, y, z)
    }

    fun toLong(x: Double, y: Double, z: Double): Long {
        return BlockPosUtil.toLong(x.floorToInt(), y.floorToInt(), z.floorToInt())
    }

    fun fromLong(long: Long): BlockPos {
        return BlockPos(BlockPosUtil.xFromLong(long), BlockPosUtil.yFromLong(long), BlockPosUtil.zFromLong(long))
    }

    fun xFromLong(long: Long): Int {
        return BlockPosUtil.xFromLong(long)
    }

    fun yFromLong(long: Long): Int {
        return BlockPosUtil.yFromLong(long)
    }

    fun zFromLong(long: Long): Int {
        return BlockPosUtil.zFromLong(long)
    }
}