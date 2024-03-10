package base.utils.combat

import base.utils.Wrapper
import base.utils.entity.EntityUtils
import base.utils.world.noCollision
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import base.utils.math.toBlockPos

class MotionTracker(val entity: Entity) {
    private val motionLog = ArrayDeque<Vec3d>()

    var prevMotion = Vec3d(0.0, 0.0, 0.0); private set
    var motion = Vec3d(0.0, 0.0, 0.0); private set

    fun tick() {
        synchronized(this) {
            motionLog.add(calcActualMotion(entity))
            while (motionLog.size > 5) motionLog.removeFirstOrNull()
            prevMotion = motion
            motion = calcAverageMotion()
        }
    }

    /**
     * Calculate the actual motion of given entity
     *
     * @param entity The entity for motion calculation
     * @return Actual motion vector
     */
    private fun calcActualMotion(entity: Entity): Vec3d {
        return Vec3d(
            entity.x - entity.prevX,
            entity.y - entity.prevY,
            entity.z - entity.prevZ
        )
    }

    /**
     * Calculate the average motion
     *
     * @return Average motion vector
     */
    private fun calcAverageMotion(): Vec3d {
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0

        for (motion in motionLog) {
            sumX += motion.x
            sumY += motion.y
            sumZ += motion.z
        }

        return Vec3d(sumX, sumY, sumZ).multiply(1.0 / motionLog.size)
    }

    /**
     * Calculate the predicted position of the target entity based on [calcAverageMotion]
     *
     * @param [ticksAhead] Amount of prediction ahead
     * @param [interpolation] Whether to return interpolated position or not, default value is false (no interpolation)
     * @return Predicted position of the target entity
     */
    fun calcPosAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3d {
        val relativePos = calcRelativePosAhead(ticksAhead, interpolation)
        val partialTicks = if (interpolation) Wrapper.minecraft.tickDelta else 1.0f
        return EntityUtils.getInterpolatedPos(entity, partialTicks).add(relativePos)
    }

    /**
     * Calculate the predicted moved vector of the target entity based on [calcAverageMotion]
     *
     * @param [ticksAhead] Amount of prediction ahead
     * @param [interpolation] Whether to return interpolated position or not, default value is false (no interpolation)
     * @return Predicted moved vector of the target entity
     */
    fun calcRelativePosAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3d {
        val world = Wrapper.world ?: return Vec3d.ZERO
        val partialTicks = if (interpolation) Wrapper.minecraft.tickDelta else 1.0f

        val averageMotion = prevMotion.add(motion.subtract(prevMotion).multiply(partialTicks.toDouble()))
        var movedVec = Vec3d(0.0, 0.0, 0.0)

        for (ticks in 0..ticksAhead) {
            movedVec = if (canMove(
                    world,
                    entity.boundingBox,
                    movedVec.add(averageMotion)
                )
            ) { // Attempt to move with full motion
                movedVec.add(averageMotion)
            } else if (canMove(
                    world,
                    entity.boundingBox,
                    movedVec.add(averageMotion.x, 0.0, averageMotion.z)
                )
            ) { // Attempt to move horizontally
                movedVec.add(averageMotion.x, 0.0, averageMotion.z)
            } else if (canMove(world, entity.boundingBox, movedVec.add(0.0, averageMotion.y, 0.0))) {
                movedVec.add(0.0, averageMotion.y, 0.0)
            } else {
                break
            }
        }

        return movedVec
    }

    private fun canMove(world: World, bbox: Box, offset: Vec3d): Boolean {
        return world.noCollision(bbox.offset(offset).center.toBlockPos())
    }
}