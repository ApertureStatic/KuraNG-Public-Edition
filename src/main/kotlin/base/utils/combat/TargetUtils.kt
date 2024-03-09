package base.utils.combat

import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.canMove
import dev.dyzjct.kura.utils.animations.sq
import base.system.event.SafeClientEvent
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import team.exception.melon.util.math.distanceSqToCenter

fun SafeClientEvent.getTarget(range: Double): PlayerEntity? {
    for (ent in world.entities.filter {
        player.distanceSqToCenter(it.blockPos) <= range.sq && it.isAlive && it is PlayerEntity && it != player && !FriendManager.isFriend(
            it
        )
    }.sortedBy { player.distanceSqToCenter(it.blockPos) }) {
        if (ent !is PlayerEntity) continue
        return ent
    }
    return null
}

fun SafeClientEvent.getEntityTarget(
    range: Double,
    mob: Boolean = true,
    ani: Boolean = true
): Entity? {
    for (ent in world.entities.filter {
        player.distanceSqToCenter(it.blockPos) <= range.sq && it.isAlive && (it !is PlayerEntity || it != player && !FriendManager.isFriend(
            it
        ))
    }.sortedBy { player.distanceSqToCenter(it.blockPos) }) {
        if (ent is PlayerEntity || (mob && ent is MobEntity) || (ani && ent is AnimalEntity)) return ent
    }
    return null
}

fun SafeClientEvent.getInfiniteTarget(
    min: Double,
    max: Double,
    mob: Boolean = true,
    ani: Boolean = true
): Entity? {
    for (ent in world.entities.filter {
        player.distanceSqToCenter(it.blockPos) <= max.sq && player.distanceSqToCenter(it.blockPos) > min.sq && it.isAlive && (it !is PlayerEntity || it != player && !FriendManager.isFriend(
            it
        ))
    }.sortedBy { player.distanceSqToCenter(it.blockPos) }) {
        if (ent is PlayerEntity || (mob && ent is MobEntity) || (ani && ent is AnimalEntity)) return ent
    }
    return null
}

fun SafeClientEvent.getPredictedTarget(entity: PlayerEntity, ticks: Int): TargetInfo {
    val motionX = (entity.x - entity.lastRenderX).coerceIn(-0.6, 0.6)
    val motionY = (entity.y - entity.lastRenderY).coerceIn(-0.5, 0.5)
    val motionZ = (entity.z - entity.lastRenderZ).coerceIn(-0.6, 0.6)

    val entityBox = entity.boundingBox
    var targetBox = entityBox

    for (tick in 0..ticks) {
        targetBox =
            canMove(targetBox, motionX, motionY, motionZ) ?: canMove(targetBox, motionX, 0.0, motionZ) ?: canMove(
                targetBox, 0.0, motionY, 0.0
            ) ?: break
    }

    val offsetX = targetBox.minX - entityBox.minX
    val offsetY = targetBox.minY - entityBox.minY
    val offsetZ = targetBox.minZ - entityBox.minZ
    val motion = Vec3d(offsetX, offsetY, offsetZ)

    return TargetInfo(
        entity, entity.pos.add(motion), targetBox, entity.blockPos
    )
}

data class TargetInfo(
    val entity: PlayerEntity, val pos: Vec3d, val box: Box, val blockPos: BlockPos
)