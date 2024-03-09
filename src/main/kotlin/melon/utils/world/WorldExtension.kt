package melon.utils.world

import com.google.common.collect.Lists
import net.minecraft.util.math.BlockPos.Mutable
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World

class PooledMutableBlockPos private constructor(p_i46586_1_: Int, p_i46586_2_: Int, p_i46586_3_: Int) :
    Mutable(p_i46586_1_, p_i46586_2_, p_i46586_3_) {
    private var released: Boolean = false

    companion object {
        private val POOL: MutableList<PooledMutableBlockPos> = Lists.newArrayList()

        @JvmStatic
        fun retain(): PooledMutableBlockPos {
            return retain(0.0, 0.0, 0.0)
        }

        @JvmStatic
        fun retain(p_185345_0_: Double, p_185345_2_: Double, p_185345_4_: Double): PooledMutableBlockPos {
            return retain(MathHelper.floor(p_185345_0_), MathHelper.floor(p_185345_2_), MathHelper.floor(p_185345_4_))
        }

        @JvmStatic
        fun retain(p_185342_0_: Vec3i): PooledMutableBlockPos {
            return retain(p_185342_0_.x, p_185342_0_.y, p_185342_0_.z)
        }

        @JvmStatic
        fun retain(p_185339_0_: Int, p_185339_1_: Int, p_185339_2_: Int): PooledMutableBlockPos {
            synchronized(POOL) {
                if (POOL.isNotEmpty()) {
                    val lvt_4_1_ = POOL.removeAt(POOL.size - 1)
                    if (lvt_4_1_ != null && lvt_4_1_.released) {
                        lvt_4_1_.released = false
                        lvt_4_1_.set(p_185339_0_, p_185339_1_, p_185339_2_)
                        return lvt_4_1_
                    }
                }
            }

            return PooledMutableBlockPos(p_185339_0_, p_185339_1_, p_185339_2_)
        }
    }

    fun release() {
        synchronized(POOL) {
            if (POOL.size < 100) {
                POOL.add(this)
            }
            this.released = true
        }
    }

    override fun set(p_181079_1_: Int, p_181079_2_: Int, p_181079_3_: Int): PooledMutableBlockPos {
        if (this.released) {
            // Replace with your desired error handling
            println("PooledMutableBlockPosition modified after it was released.")
            this.released = false
        }

        return super.set(p_181079_1_, p_181079_2_, p_181079_3_) as PooledMutableBlockPos
    }

    override fun set(p_189532_1_: Double, p_189532_3_: Double, p_189532_5_: Double): PooledMutableBlockPos {
        return super.set(p_189532_1_, p_189532_3_, p_189532_5_) as PooledMutableBlockPos
    }

    override fun set(p_189533_1_: Vec3i): PooledMutableBlockPos {
        return super.set(p_189533_1_) as PooledMutableBlockPos
    }

    override fun move(p_189536_1_: Direction): PooledMutableBlockPos {
        return super.move(p_189536_1_) as PooledMutableBlockPos
    }

    override fun move(p_189534_1_: Direction, p_189534_2_: Int): PooledMutableBlockPos {
        return super.move(p_189534_1_, p_189534_2_) as PooledMutableBlockPos
    }
}

private fun World.getCollisionBoxes(
    aabb: Box,
    checkInvisibleEntities: Boolean,
    collisionBoxList: MutableList<Box>?
): Boolean {
    val minX = aabb.minX.toInt() - 1
    val maxX = aabb.maxX.toInt() + 1
    val minY = aabb.minY.toInt() - 1
    val maxY = aabb.maxY.toInt() + 1
    val minZ = aabb.minZ.toInt() - 1
    val maxZ = aabb.maxZ.toInt() + 1
    val mutableBlockPos = PooledMutableBlockPos.retain()

    if (checkInvisibleEntities) {
        return true
    } else {
        try {
            for (x in minX until maxX) {
                for (z in minZ until maxZ) {
                    val isOnBorderX = x == minX || x == maxX - 1
                    val isOnBorderZ = z == minZ || z == maxZ - 1

                    if ((!isOnBorderX || !isOnBorderZ) && this.isChunkLoaded(mutableBlockPos.set(x, 64, z))) {
                        for (y in minY until maxY) {
                            if (!isOnBorderX && !isOnBorderZ || y != maxY - 1) {
                                mutableBlockPos.set(x, y, z)
                            }
                        }
                    }
                }
            }
            return !collisionBoxList.isNullOrEmpty()
        } finally {
            mutableBlockPos.release()
        }
    }
}

fun World.collidesWithAnyBlockOld(boundingBox: Box): Boolean {
    return getCollisionBoxes(boundingBox, true, Lists.newArrayList<Box>())
}

fun World.collidesWithAnyBlock(boundingBox: Box): Boolean {
    val pos = Mutable()
    for (x in MathHelper.floor(boundingBox.minX)..MathHelper.floor(boundingBox.maxX)) {
        for (y in MathHelper.floor(boundingBox.minY)..MathHelper.floor(boundingBox.maxY)) {
            for (z in MathHelper.floor(boundingBox.minZ)..MathHelper.floor(boundingBox.maxZ)) {
                pos[x, y] = z
                if (this.isSpaceEmpty(
                        boundingBox.offset(
                            -pos.x.toDouble(),
                            -pos.y.toDouble(),
                            -pos.z.toDouble()
                        )
                    )
                ) {
                    return true
                }
            }
        }
    }
    return false
}
