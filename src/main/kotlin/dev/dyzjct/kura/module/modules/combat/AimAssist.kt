package dev.dyzjct.kura.module.modules.combat

import base.system.event.SafeClientEvent
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
    private val speed by dsetting("Speed", 1.0, 0.0, 5.0)
//    private val speed by isetting("Speed", 400, 0, 1000)

    private var lastUpdateTime = 0L

    init {
        onLoop {
            getTarget(range)?.let { target ->
                if (!target.isAlive) {
                    return@onLoop
                }
                val currentYaw = getYawToEntityNew(target)
                fun lerpYaw(yaw1: Float, yaw2: Float, t: Float): Float {
                    var result = yaw2 - yaw1
                    if (result > 180.0f) {
                        result -= 360.0f
                    } else if (result < -180.0f) {
                        result += 360.0f
                    }

                    return yaw1 + result * t
                }

                if (player.yaw != currentYaw) {
                    val t = speed / abs(player.yaw - currentYaw)
                    if (t > 1.0f) {
                        player.setYaw(currentYaw)
                    } else {
                        player.setYaw(lerpYaw(player.yaw, currentYaw, t.toFloat()))
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.getYawToEntityNew(entity: Entity): Float {
        return getRotationToEntity(entity).x
    }
}