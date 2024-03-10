package base.utils.combat

import dev.dyzjct.kura.utils.animations.fastFloor
import base.system.event.SafeClientEvent
import base.system.render.graphic.mask.DirectionMask
import base.utils.world.FastRayTraceAction
import base.utils.world.fastRaytrace
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Difficulty
import net.minecraft.world.World
import base.utils.math.distanceTo
import kotlin.math.floor
import kotlin.math.min

class CalcContext(
    private val event: SafeClientEvent,
    val entity: LivingEntity,
    val predictPos: Vec3d
) {
    val currentPos = entity.pos
    val currentBox = getBoundingBox(entity, currentPos)
    val predictBox = getBoundingBox(entity, predictPos)

    private val predicting = currentPos.squaredDistanceTo(predictPos) > 0.01
    private val difficulty = event.world.difficulty
    private val reduction = DamageReduction(entity)

    private val exposureSample = ExposureSample.getExposureSample(entity.width, entity.height)
    private val samplePoints = exposureSample.offset(currentBox.minX, currentBox.minY, currentBox.minZ)
    private val samplePointsPredict = exposureSample.offset(predictBox.minX, predictBox.minY, predictBox.minZ)

    fun checkColliding(pos: Vec3d): Boolean {
        val box = Box(
            pos.x - 0.499, pos.y, pos.z - 0.499,
            pos.x + 0.499, pos.y + 2.0, pos.z + 0.499
        )

        return !box.intersects(currentBox)
                && (!predicting && !box.intersects(predictBox))
    }

    fun calcDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        predict: Boolean,
        mutableBlockPos: BlockPos.Mutable,
    ) = calcDamage(crystalX, crystalY, crystalZ, predict, 6.0f, mutableBlockPos)

    fun calcDamage(
        pos: Vec3d,
        predict: Boolean,
        mutableBlockPos: BlockPos.Mutable,
    ) = calcDamage(pos, predict, 6.0f, mutableBlockPos)

    fun calcDamage(
        pos: Vec3d,
        predict: Boolean,
        size: Float,
        mutableBlockPos: BlockPos.Mutable
    ) = calcDamage(pos.x, pos.y, pos.z, predict, size, mutableBlockPos)

    fun calcDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        predict: Boolean,
        size: Float,
        mutableBlockPos: BlockPos.Mutable
    ) = calcDamage(crystalX, crystalY, crystalZ, predict, size, mutableBlockPos) { _, blockState ->
        if (blockState.block != Blocks.AIR && CrystalUtils.isResistant(blockState)) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
    }

    fun calcDamage(
        pos: Vec3d,
        predict: Boolean,
        mutableBlockPos: BlockPos.Mutable,
        function: World.(BlockPos, BlockState) -> FastRayTraceAction
    ) = calcDamage(pos, predict, 6.0f, mutableBlockPos, function)

    fun calcDamage(
        pos: Vec3d,
        predict: Boolean,
        size: Float,
        mutableBlockPos: BlockPos.Mutable,
        function: World.(BlockPos, BlockState) -> FastRayTraceAction
    ) = calcDamage(pos.x, pos.y, pos.z, predict, size, mutableBlockPos, function)

    fun calcDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        predict: Boolean,
        size: Float,
        mutableBlockPos: BlockPos.Mutable,
        function: World.(BlockPos, BlockState) -> FastRayTraceAction
    ): Float {
        if (difficulty == Difficulty.PEACEFUL) return 0.0f

        event {
            val entityPos = if (predict) predictPos else currentPos
            var damage =
                if (crystalY - entityPos.y > exposureSample.maxY
                    && CrystalUtils.isResistant(
                        world.getBlockState(
                            mutableBlockPos.set(
                                crystalX.fastFloor(),
                                crystalY.fastFloor() - 1,
                                crystalZ.fastFloor()
                            )
                        )
                    )
                ) {
                    1.0f
                } else {
                    calcRawDamage(crystalX, crystalY, crystalZ, size, predict, mutableBlockPos, function)
                }

            damage = calcDifficultyDamage(damage)
            return reduction.calcDamage(damage, true)
        }
    }

    private fun calcDifficultyDamage(damage: Float) =
        if (entity is PlayerEntity) {
            when (difficulty) {
                Difficulty.EASY -> {
                    min(damage * 0.5f + 1.0f, damage)
                }

                Difficulty.HARD -> {
                    damage * 1.5f
                }

                else -> {
                    damage
                }
            }
        } else {
            damage
        }

    private fun SafeClientEvent.calcRawDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        size: Float,
        predict: Boolean,
        mutableBlockPos: BlockPos.Mutable,
        function: World.(BlockPos, BlockState) -> FastRayTraceAction
    ): Float {
        val entityPos = if (predict) predictPos else currentPos
        val doubleSize = size * 2.0f
        val scaledDist = entityPos.distanceTo(crystalX, crystalY, crystalZ).toFloat() / doubleSize
        if (scaledDist > 1.0f) return 0.0f

        val factor =
            (1.0f - scaledDist) * getExposureAmount(crystalX, crystalY, crystalZ, predict, mutableBlockPos, function)
        return floor((factor * factor + factor) * doubleSize * 3.5f + 1.0f)
    }

    private fun SafeClientEvent.getExposureAmount(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        predict: Boolean,
        mutableBlockPos: BlockPos.Mutable,
        function: World.(BlockPos, BlockState) -> FastRayTraceAction
    ): Float {
        val box = if (predict) predictBox else currentBox
        if (box.isInside(crystalX, crystalY, crystalZ)) return 1.0f

        val array = if (predict) samplePointsPredict else samplePoints
        return countSamplePoints(array, crystalX, crystalY, crystalZ, mutableBlockPos, function)
    }

    private fun SafeClientEvent.countSamplePoints(
        samplePoints: Array<Vec3d>,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        blockPos: BlockPos.Mutable,
        function: World.(BlockPos, BlockState) -> FastRayTraceAction
    ): Float {
        var count = 0

        for (i in samplePoints.indices) {
            val samplePoint = samplePoints[i]
            if (!world.fastRaytrace(samplePoint, crystalX, crystalY, crystalZ, 20, blockPos, function)) {
                count++
            }
        }

        return count.toFloat() / samplePoints.size
    }

    private fun SafeClientEvent.countSamplePointsOptimized(
        samplePoints: Array<Vec3d>,
        box: Box,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.Mutable,
        function: World.(BlockPos, BlockState) -> FastRayTraceAction
    ): Float {
        var count = 0
        var total = 0

        val sideMask = getSideMask(box, crystalX, crystalY, crystalZ)
        for (i in samplePoints.indices) {
            val pointMask = exposureSample.getMask(i)
            if (sideMask and pointMask == 0x00) {
                continue
            }

            total++
            val samplePoint = samplePoints[i]
            if (!world.fastRaytrace(samplePoint, crystalX, crystalY, crystalZ, 20, mutableBlockPos, function)) {
                count++
            }
        }

        return count.toFloat() / total.toFloat()
    }

    private fun getSideMask(
        box: Box,
        posX: Double,
        posY: Double,
        posZ: Double
    ): Int {
        var mask = 0x00

        if (posX < box.minX) {
            mask = DirectionMask.WEST
        } else if (posX > box.maxX) {
            mask = DirectionMask.EAST
        }

        if (posY < box.minY) {
            mask = mask or DirectionMask.DOWN
        } else if (posY > box.maxY) {
            mask = mask or DirectionMask.UP
        }

        if (posZ < box.minZ) {
            mask = mask or DirectionMask.NORTH
        } else if (posZ > box.maxZ) {
            mask = mask or DirectionMask.SOUTH
        }

        return mask
    }

    private fun Box.isInside(
        x: Double,
        y: Double,
        z: Double
    ): Boolean {
        return x >= this.minX && x <= this.maxX
                && y >= this.minY && y <= this.maxY
                && z >= this.minZ && z <= this.maxZ
    }

    private fun getBoundingBox(entity: LivingEntity, pos: Vec3d): Box {
        val halfWidth = min(entity.width.toDouble(), 2.0) / 2.0
        val height = min(entity.height.toDouble(), 3.0)

        return Box(
            pos.x - halfWidth, pos.y, pos.z - halfWidth,
            pos.x + halfWidth, pos.y + height, pos.z + halfWidth
        )
    }
}