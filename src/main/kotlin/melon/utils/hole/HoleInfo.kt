package melon.utils.hole

import melon.utils.world.collidesWithAnyBlock
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class HoleInfo(
    val origin: BlockPos,
    val center: Vec3d,
    val boundingBox: Box,
    val holePos: Array<BlockPos>,
    val surroundPos: Array<BlockPos>,
    val type: HoleType,
    val isTrapped: Boolean,
    val isFullyTrapped: Boolean
) {
    val isHole = type != HoleType.NONE
    val isSafe = type == HoleType.BEDROCK
    val isTwo = type == HoleType.TWO
    val isFour = type == HoleType.FOUR

    fun canEnter(world: World, pos: BlockPos): Boolean {
        val headPosY = pos.y + 2
        if (origin.y >= headPosY) return false
        val box = boundingBox.expand(0.0, headPosY - origin.y - 1.0, 0.0)
        return !world.collidesWithAnyBlock(box)
    }

    override fun equals(other: Any?) =
        this === other
                || other is HoleInfo
                && origin == other.origin

    override fun hashCode() =
        origin.hashCode()

    companion object {
        fun empty(pos: BlockPos) =
            HoleInfo(
                pos,
                Vec3d.ZERO,
                emptyBox,
                emptyBlockPosArray,
                emptyBlockPosArray,
                HoleType.NONE,
                isTrapped = false,
                isFullyTrapped = false,
            )

        private val emptyBox = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        private val emptyBlockPosArray = emptyArray<BlockPos>()
    }
}