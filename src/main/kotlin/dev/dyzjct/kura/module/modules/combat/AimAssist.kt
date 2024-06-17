package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.combat.getTarget
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.math.RotationUtils.getRotationToEntity
import net.minecraft.entity.Entity
import kotlin.math.abs

object AimAssist : Module(
    name = "AimAssist",
    langName = "自动瞄准",
    category = Category.COMBAT,
    type = Type.Both
) {
    private val range by dsetting("Range", 5.0, 1.0, 8.0)
    private val speed by dsetting("Speed", 1.0, 0.0, 20.0)

    init {
        onLoop {
            getTarget(range)?.let { target ->
                if (!target.isAlive) {
                    return@onLoop
                }
                val currentYaw = getYawToEntityNew(target)
                val playerYaw = player.getYaw()
                val t = speed.toFloat() / abs(playerYaw - currentYaw)
                if (t > 1.0f) {
                    player.setYaw(currentYaw)
                } else {
                    player.setYaw(lerpYaw(playerYaw, currentYaw, t))
                }
            }
        }
    }

    private fun lerpYaw(yaw1: Float, yaw2: Float, t: Float): Float {
        var result = yaw2 - yaw1
        if (result > 180.0f) {
            result -= 360.0f
        } else if (result < -180.0f) {
            result += 360.0f
        }
        return yaw1 + result * t
    }

    private fun SafeClientEvent.getYawToEntityNew(entity: Entity): Float {
        return getRotationToEntity(entity).x
    }
}
