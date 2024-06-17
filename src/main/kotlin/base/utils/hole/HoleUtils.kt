package base.utils.hole

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.combat.CrystalUtils
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import base.utils.math.toVec3d

@Suppress("NOTHING_TO_INLINE")
object HoleUtils {
    private val holeOffset1 = arrayOf(
        BlockPos(0, 0, 0),
    )

    private val holeOffsetCheck1 = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 1, 0)
    )

    private val surroundOffset1 = arrayOf(
        BlockPos(0, -1, 0), // Down
        BlockPos(0, 0, -1), // North
        BlockPos(1, 0, 0),  // East
        BlockPos(0, 0, 1),  // South
        BlockPos(-1, 0, 0)  // West
    )

    private val holeOffset2X = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(1, 0, 0),
    )

    private val holeOffsetCheck2X = arrayOf(
        *holeOffset2X,
        BlockPos(0, 1, 0),
        BlockPos(1, 1, 0),
    )

    private val holeOffset2Z = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 0, 1),
    )

    private val holeOffsetCheck2Z = arrayOf(
        *holeOffset2Z,
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
    )

    private val surroundOffset2X = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(1, -1, 0),
        BlockPos(-1, 0, 0),
        BlockPos(0, 0, -1),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, -1),
        BlockPos(1, 0, 1),
        BlockPos(2, 0, 0)
    )

    private val surroundOffset2Z = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(0, -1, 1),
        BlockPos(0, 0, -1),
        BlockPos(-1, 0, 0),
        BlockPos(1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(1, 0, 1),
        BlockPos(0, 0, 2)
    )

    private val holeOffset4 = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, 0),
        BlockPos(1, 0, 1)
    )

    private val holeOffsetCheck4 = arrayOf(
        *holeOffset4,
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(1, 1, 0),
        BlockPos(1, 1, 1)
    )

    private val surroundOffset4 = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(0, -1, 1),
        BlockPos(1, -1, 0),
        BlockPos(1, -1, 1),
        BlockPos(-1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(0, 0, -1),
        BlockPos(1, 0, -1),
        BlockPos(0, 0, 2),
        BlockPos(1, 0, 2),
        BlockPos(2, 0, 0),
        BlockPos(2, 0, 1)
    )

    private val mutableBlockPos = ThreadLocal.withInitial {
        BlockPos.Mutable()
    }

    fun SafeClientEvent.checkHoleM(pos: BlockPos): HoleInfo {
        if (pos.getY() !in 1..255 || !world.isAir(pos)) return HoleInfo.empty(pos.toImmutable())
        //ChatUtil.sendMessage("11111")
        val mutablePos = mutableBlockPos.get().set(pos)

        return checkHole1(pos, mutablePos)
            ?: checkHole2(pos, mutablePos)
            ?: checkHole4(pos, mutablePos)
            ?: HoleInfo.empty(pos.toImmutable())
    }

    private inline fun SafeClientEvent.checkHole1(pos: BlockPos, mutablePos: BlockPos.Mutable): HoleInfo? {
        if (!checkAir(holeOffsetCheck1, pos, mutablePos)) return null

//        return checkType(
//            pos,
//            mutablePos,
//            HoleType.BEDROCK,
//            HoleType.OBBY,
//            surroundOffset1,
//            holeOffset1,
//            Box(pos),
//            0.5,
//            0.5
//        )

        val type = checkSurroundPos(pos, mutablePos, surroundOffset1, HoleType.BEDROCK, HoleType.OBBY)
        return if (type == HoleType.NONE) {
            null
        } else {
            val holePosArray = holeOffset1.offset(pos)

            var trapped = false
            var fullyTrapped = true

            for (holePos in holePosArray) {
                if (world.isAir(mutablePos.set(holePos.getX(), holePos.getY() + 2, holePos.getZ()))) {
                    fullyTrapped = false
                } else {
                    trapped = true
                }
            }

            HoleInfo(
                pos.toImmutable(),
                pos.toVec3d(0.5,
                    0.0, 0.5
                ),
                Box(pos),
                holePosArray,
                surroundOffset1.offset(pos),
                type,
                trapped,
                fullyTrapped
            )
        }
    }

    private inline fun SafeClientEvent.checkHole2(pos: BlockPos, mutablePos: BlockPos.Mutable): HoleInfo? {
        var x = true

        if (!world.isAir(mutablePos.set(pos.getX() + 1, pos.getY(), pos.getZ()))) {
            if (!world.isAir(mutablePos.set(pos.getX(), pos.getY(), pos.getZ() + 1))) return null
            else x = false
        }

        val checkArray = if (x) holeOffsetCheck2X else holeOffsetCheck2Z
        if (!checkAir(checkArray, pos, mutablePos)) return null

        val surroundOffset = if (x) surroundOffset2X else surroundOffset2Z
        val holeOffset = if (x) holeOffset2X else holeOffset2Z
        val centerX = if (x) 1.0 else 0.5
        val centerZ = if (x) 0.5 else 1.0

//        val boundingBox = if (x) {
//            Box(
//                pos.getX().toDouble(), pos.getY().toDouble(), pos.getZ().toDouble(),
//                pos.getX() + 2.0, pos.getY() + 1.0, pos.getZ() + 1.0
//            )
//        } else {
//            Box(
//                pos.getX().toDouble(), pos.getY().toDouble(), pos.getZ().toDouble(),
//                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 2.0
//            )
//        }
//
//        return checkType(
//            pos,
//            mutablePos,
//            HoleType.TWO,
//            HoleType.TWO,
//            surroundOffset,
//            holeOffset,
//            boundingBox,
//            centerX,
//            centerZ
//        )

        val type = checkSurroundPos(pos, mutablePos, surroundOffset, HoleType.TWO, HoleType.TWO)
        return if (type == HoleType.NONE) {
            null
        } else {
            val holePosArray = holeOffset.offset(pos)

            var trapped = false
            var fullyTrapped = true

            for (holePos in holePosArray) {
                if (world.isAir(mutablePos.set(holePos.getX(), holePos.getY() + 2, holePos.getZ()))) {
                    fullyTrapped = false
                } else {
                    trapped = true
                }
            }

            HoleInfo(
                pos.toImmutable(),
                pos.toVec3d(centerX,
                    0.0, centerZ
                ),
                if (x) {
                    Box(
                        pos.getX().toDouble(), pos.getY().toDouble(), pos.getZ().toDouble(),
                        pos.getX() + 2.0, pos.getY() + 1.0, pos.getZ() + 1.0
                    )
                } else {
                    Box(
                        pos.getX().toDouble(), pos.getY().toDouble(), pos.getZ().toDouble(),
                        pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 2.0
                    )
                },
                holePosArray,
                surroundOffset.offset(pos),
                type,
                trapped,
                fullyTrapped
            )
        }
    }

    private inline fun SafeClientEvent.checkHole4(pos: BlockPos, mutablePos: BlockPos.Mutable): HoleInfo? {
        if (!checkAir(holeOffsetCheck4, pos, mutablePos)) return null

//        val boundingBox = Box(
//            pos.getX().toDouble(), pos.getY().toDouble(), pos.getZ().toDouble(),
//            pos.getX() + 2.0, pos.getY() + 1.0, pos.getZ() + 2.0
//        )
//
//        return checkType(
//            pos,
//            mutablePos,
//            HoleType.FOUR,
//            HoleType.FOUR,
//            surroundOffset4,
//            holeOffset4,
//            boundingBox,
//            1.0,
//            1.0
//        )

        val type = checkSurroundPos(pos, mutablePos, surroundOffset4, HoleType.FOUR, HoleType.FOUR)
        return if (type == HoleType.NONE) {
            null
        } else {
            val holePosArray = holeOffset4.offset(pos)

            var trapped = false
            var fullyTrapped = true

            for (holePos in holePosArray) {
                if (world.isAir(mutablePos.set(holePos.getX(), holePos.getY() + 2, holePos.getZ()))) {
                    fullyTrapped = false
                } else {
                    trapped = true
                }
            }

            HoleInfo(
                pos.toImmutable(),
                pos.toVec3d(1.0,
                    0.0, 1.0
                ),
                Box(
                    pos.getX().toDouble(), pos.getY().toDouble(), pos.getZ().toDouble(),
                    pos.getX() + 2.0, pos.getY() + 1.0, pos.getZ() + 2.0
                ),
                holePosArray,
                surroundOffset4.offset(pos),
                type,
                trapped,
                fullyTrapped
            )
        }
    }

    private inline fun SafeClientEvent.checkAir(array: Array<BlockPos>, pos: BlockPos, mutablePos: BlockPos.Mutable) =
        array.all {
            world.isAir(mutablePos.set(pos.getX() + it.getX(), pos.getY() + it.getY(), pos.getZ() + it.getZ()))
        }

    private inline fun Array<BlockPos>.offset(pos: BlockPos) =
        Array(this.size) {
            pos.add(this[it])
        }

    private inline fun SafeClientEvent.checkType(
        pos: BlockPos,
        mutablePos: BlockPos.Mutable,
        expectType: HoleType,
        obbyType: HoleType,
        surroundOffset: Array<BlockPos>,
        holeOffset: Array<BlockPos>,
        boundingBox: Box,
        centerX: Double,
        centerZ: Double
    ): HoleInfo? {
        val type = checkSurroundPos(pos, mutablePos, surroundOffset, expectType, obbyType)

        return if (type == HoleType.NONE) {
            null
        } else {
            val holePosArray = holeOffset.offset(pos)

            var trapped = false
            var fullyTrapped = true

            for (holePos in holePosArray) {
                if (world.isAir(mutablePos.set(holePos.getX(), holePos.getY() + 2, holePos.getZ()))) {
                    fullyTrapped = false
                } else {
                    trapped = true
                }
            }

            HoleInfo(
                pos.toImmutable(),
                pos.toVec3d(centerX, 0.0, centerZ),
                boundingBox,
                holePosArray,
                surroundOffset.offset(pos),
                type,
                trapped,
                fullyTrapped
            )
        }
    }

    private inline fun SafeClientEvent.checkSurroundPos(pos: BlockPos, mutablePos: BlockPos.Mutable, surroundOffset: Array<BlockPos>, expectType: HoleType, obbyType: HoleType): HoleType {
        var type = expectType

        for (offset in surroundOffset) {
            val blockState = world.getBlockState(mutablePos.set(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ()))
            when {
                blockState.block == Blocks.BEDROCK -> continue
                blockState.block != Blocks.AIR && CrystalUtils.isResistant(blockState) -> type = obbyType
                else -> return HoleType.NONE
            }
        }

        return type
    }
}