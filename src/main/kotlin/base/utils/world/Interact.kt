package base.utils.world

import dev.dyzjct.kura.manager.HotbarManager.serverSideItem
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import base.system.event.SafeClientEvent
import base.system.util.collections.EnumSet
import base.utils.block.blockBlacklist
import base.utils.block.getBlock
import base.utils.block.isFullBox
import base.utils.entity.EntityUtils.eyePosition
import net.minecraft.block.Blocks
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import team.exception.melon.util.math.toVec3dCenter
import team.exception.melon.util.math.vector.Vec3f
import java.util.*

fun SafeClientEvent.getNeighborSequence(
    pos: BlockPos,
    attempts: Int = 3,
    range: Float = 4.25f,
    visibleSideCheck: Boolean = false,
    entityCheck: Boolean = true,
    sides: Array<Direction> = Direction.values()
) =
    getNeighborSequence(
        player.eyePosition,
        pos,
        attempts,
        range,
        visibleSideCheck,
        entityCheck,
        sides,
        ArrayList(),
        pos,
        0
    )


private fun SafeClientEvent.getNeighborSequence(
    eyePos: Vec3d,
    pos: BlockPos,
    attempts: Int,
    range: Float,
    visibleSideCheck: Boolean,
    entityCheck: Boolean,
    sides: Array<Direction>,
    sequence: ArrayList<PlaceInfo>,
    origin: BlockPos,
    lastDist: Int
): List<PlaceInfo>? {
    for (side in sides) {
        checkNeighbor(eyePos, pos, side, range, visibleSideCheck, entityCheck, true, origin, lastDist)?.let {
            sequence.add(it)
            sequence.reverse()
            return sequence
        }
    }

    if (attempts > 1) {
        for (side in sides) {
            val newPos = pos.offset(side)

            val placeInfo =
                checkNeighbor(eyePos, pos, side, range, visibleSideCheck, entityCheck, false, origin, lastDist)
                    ?: continue
            val newSequence = ArrayList(sequence)
            newSequence.add(placeInfo)

            return getNeighborSequence(
                eyePos,
                newPos,
                attempts - 1,
                range,
                visibleSideCheck,
                entityCheck,
                sides,
                newSequence,
                origin,
                lastDist + 1
            )
                ?: continue
        }
    }

    return null
}

fun SafeClientEvent.getNeighbor(
    pos: BlockPos,
    attempts: Int = 3,
    range: Float = 4.25f,
    visibleSideCheck: Boolean = false,
    entityCheck: Boolean = true,
    sides: Array<Direction> = Direction.values()
) =
    getNeighbor(player.eyePosition, pos, attempts, range, visibleSideCheck, entityCheck, sides, pos, 0)

private fun SafeClientEvent.getNeighbor(
    eyePos: Vec3d,
    pos: BlockPos,
    attempts: Int,
    range: Float,
    visibleSideCheck: Boolean,
    entityCheck: Boolean,
    sides: Array<Direction>,
    origin: BlockPos,
    lastDist: Int
): PlaceInfo? {
    for (side in sides) {
        val result = checkNeighbor(eyePos, pos, side, range, visibleSideCheck, entityCheck, true, origin, lastDist)
        if (result != null) return result
    }

    if (attempts > 1) {
        for (side in sides) {
            val newPos = pos.offset(side)
            if (!world.isPlaceable(newPos)) continue

            return getNeighbor(
                eyePos,
                newPos,
                attempts - 1,
                range,
                visibleSideCheck,
                entityCheck,
                sides,
                origin,
                lastDist + 1
            )
                ?: continue
        }
    }

    return null
}

private fun SafeClientEvent.checkNeighbor(
    eyePos: Vec3d,
    pos: BlockPos,
    side: Direction,
    range: Float,
    visibleSideCheck: Boolean,
    entityCheck: Boolean,
    checkReplaceable: Boolean,
    origin: BlockPos,
    lastDist: Int
): PlaceInfo? {
    val offsetPos = pos.offset(side)
    val oppositeSide = side.opposite

    val distToOrigin = (offsetPos.x - origin.x).sq + (offsetPos.y - origin.y).sq + (offsetPos.z - origin.z).sq
    if (distToOrigin <= lastDist.sq) return null

    val hitVec = getHitVec(offsetPos, oppositeSide)
    val dist = eyePos.distanceTo(hitVec)

    if (dist > range) return null
    if (visibleSideCheck && !getVisibleSides(offsetPos, true).contains(oppositeSide)) return null
    if (checkReplaceable && world.getBlockState(offsetPos).isReplaceable) return null
    if (!world.getBlockState(pos).isReplaceable) return null
    if (entityCheck && !world.noCollision(pos)) return null

    val hitVecOffset = getHitVecOffset(oppositeSide)
    return PlaceInfo(offsetPos, oppositeSide, dist, hitVecOffset, hitVec, pos)
}

fun SafeClientEvent.getMiningSide(pos: BlockPos): Direction? {
    val eyePos = player.eyePosition

    return getVisibleSides(pos)
        .filter { !world.getBlockState(pos.offset(it)).isFullBox }
        .minByOrNull { eyePos.squaredDistanceTo(getHitVec(pos, it)) }
}

fun SafeClientEvent.getClosestVisibleSide(pos: BlockPos): Direction? {
    val eyePos = player.eyePosition

    return getVisibleSides(pos)
        .minByOrNull { eyePos.squaredDistanceTo(getHitVec(pos, it)) }
}

/**
 * Get the "visible" sides related to player's eye position
 */
fun SafeClientEvent.getVisibleSides(pos: BlockPos, assumeAirAsFullBox: Boolean = false): Set<Direction> {
    val visibleSides = EnumSet<Direction>()

    val eyePos = player.eyePosition
    val blockCenter = pos.toVec3dCenter()
    val blockState = world.getBlockState(pos)
    val isFullBox = assumeAirAsFullBox && blockState.block == Blocks.AIR || blockState.isFullBox

    return visibleSides
        .checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, !isFullBox)
        .checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true)
        .checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, !isFullBox)
}

fun SafeClientEvent.getEmptyVisibleSides(pos: BlockPos): Direction? {
    var directionFound: Direction? = null
    for (direction in Direction.values()) {
        val offsetPos = pos.offset(direction)
        if (!world.isAir(offsetPos)) continue
        directionFound = direction
    }

    return directionFound
}

private fun EnumSet<Direction>.checkAxis(
    diff: Double,
    negativeSide: Direction,
    positiveSide: Direction,
    bothIfInRange: Boolean
) =
    this.apply {
        when {
            diff < -0.5 -> {
                add(negativeSide)
            }

            diff > 0.5 -> {
                add(positiveSide)
            }

            else -> {
                if (bothIfInRange) {
                    add(negativeSide)
                    add(positiveSide)
                }
            }
        }
    }

fun getHitVec(pos: BlockPos, facing: Direction): Vec3d {
    val vec = facing.vector
    return Vec3d(vec.x * 0.5 + 0.5 + pos.x, vec.y * 0.5 + 0.5 + pos.y, vec.z * 0.5 + 0.5 + pos.z)
}

fun getHitVecOffset(facing: Direction): Vec3f {
    val vec = facing.vector
    return Vec3f(vec.x * 0.5f + 0.5f, vec.y * 0.5f + 0.5f, vec.z * 0.5f + 0.5f)
}

/**
 * Placing block without desync
 */
fun SafeClientEvent.placeBlock(
    placeInfo: PlaceInfo,
    hand: Hand = Hand.MAIN_HAND
) {
    if (!world.isPlaceable(placeInfo.placedPos)) return

    val sneak = !player.isSneaking && blockBlacklist.contains(world.getBlock(placeInfo.pos))
    if (sneak) connection.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))

    connection.sendPacket(placeInfo.toPlacePacket(hand))
    player.swingHand(hand)

    if (sneak) connection.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))

    val itemStack = player.serverSideItem
    val block = (itemStack.item as? BlockItem?)?.block ?: return
    val blockState = block.getPlacementState(
        ItemPlacementContext(
            world,
            player,
            hand,
            itemStack,
            BlockHitResult(placeInfo.hitVec, placeInfo.side, placeInfo.pos, false)
        )
    )
    val soundType = blockState?.block?.getSoundGroup(blockState)
    world.playSound(
        player,
        placeInfo.pos,
        soundType?.placeSound,
        SoundCategory.BLOCKS,
        ((soundType?.getVolume() ?: 1f) + 1.0f) / 2.0f,
        (soundType?.getPitch() ?: 1f) * 0.8f
    )
}

/**
 * Placing block without desync
 */
fun SafeClientEvent.placeBlock(
    placeInfo: PlaceInfo,
    slot: HotbarSlot
) {
    if (!world.isPlaceable(placeInfo.placedPos)) return

    val sneak = !player.isSneaking && blockBlacklist.contains(world.getBlock(placeInfo.pos))
    if (sneak) connection.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
    val packet = placeInfo.toPlacePacket(Hand.MAIN_HAND)

    spoofHotbar(slot) {
        connection.sendPacket(packet)
    }
    player.swingHand(Hand.MAIN_HAND)

    if (sneak) connection.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))

    val itemStack = player.serverSideItem
    val block = (itemStack.item as? BlockItem?)?.block ?: return
    val blockState = block.getPlacementState(
        ItemPlacementContext(
            world,
            player,
            Hand.MAIN_HAND,
            itemStack,
            BlockHitResult(placeInfo.hitVec, placeInfo.side, placeInfo.pos, false)
        )
    )
    val soundType = blockState?.block?.getSoundGroup(blockState)
    world.playSound(
        player,
        placeInfo.pos,
        soundType?.placeSound,
        SoundCategory.BLOCKS,
        ((soundType?.getVolume() ?: 1f) + 1.0f) / 2.0f,
        (soundType?.getPitch() ?: 1f) * 0.8f
    )
}

fun PlaceInfo.toPlacePacket(hand: Hand, sequence: Int = 0) =
    PlayerInteractBlockC2SPacket(
        hand,
        BlockHitResult(hitVec, side, pos, false),
        sequence
    )