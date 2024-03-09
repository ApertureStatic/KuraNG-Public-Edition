package melon.utils.world

import dev.dyzjct.kura.utils.animations.fastFloor
import dev.dyzjct.kura.utils.extension.sq
import melon.system.event.SafeClientEvent
import melon.utils.Wrapper
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.world.World

fun World.getGroundPos(entity: Entity): BlockPos {
    return getGroundPos(entity.boundingBox)
}

fun World.getGroundPos(boundingBox: Box): BlockPos {
    val center = boundingBox.center

    val cx = center.x.fastFloor()
    val cz = center.z.fastFloor()

    var rx = cx
    var ry = Int.MIN_VALUE
    var rz = cz

    val pos = BlockPos.Mutable()

    for (x in boundingBox.minX.fastFloor()..boundingBox.maxX.fastFloor()) {
        for (z in boundingBox.minZ.fastFloor()..boundingBox.maxZ.fastFloor()) {
            for (y in (boundingBox.minY - 0.5).fastFloor() downTo -1) {
                if (y < ry) break

                pos.set(x, y, z)
                val box = this.getBlockState(pos).getCollisionShape(this, pos)
                if ((!box.isEmpty && box.boundingBox != null) && (ry == Int.MIN_VALUE || y > ry || (x - cx).sq <= (rx - cx).sq && (z - cz).sq <= (rz - cz).sq)) {
                    rx = x
                    ry = y
                    rz = z
                }
            }
        }
    }

    return BlockPos(rx, if (ry == Int.MIN_VALUE) -999 else ry, rz)
}

inline fun World.canBreakBlock(pos: BlockPos): Boolean {
    val blockState = this.getBlockState(pos)
    return blockState.block == Blocks.AIR || blockState.getHardness(this, pos) != -1.0f
}

fun World.isLiquid(pos: BlockPos): Boolean {
    return this.getBlockState(pos).isLiquid
}

fun SafeClientEvent.hasNeighbor(pos: BlockPos): Boolean {
    return Direction.values().any {
        !world.getBlockState(pos.offset(it)).isReplaceable
    }
}

/**
 * Checks if given [pos] is able to place block in it
 *
 * @return true playing is not colliding with [pos] and there is block below it
 */
fun World.isPlaceable(pos: BlockPos, entity: Entity) =
    this.getBlockState(pos).isReplaceable
            && this.isSpaceEmpty(entity, Box(pos))

fun World.isPlaceable(pos: BlockPos, ignoreSelfCollide: Boolean = false) =
    this.getBlockState(pos).isReplaceable
            && this.isSpaceEmpty(if (ignoreSelfCollide) Wrapper.player else null, Box(pos))

fun World.noCollision(pos: BlockPos) = this.isSpaceEmpty(Wrapper.player, Box(pos))

fun World.noCollision(box: Box) = this.isSpaceEmpty(Wrapper.player, box)

fun World.noCollision(target: PlayerEntity, pos: BlockPos) = this.isSpaceEmpty(target, Box(pos))