package dev.dyzjct.kura.module.modules.combat

import base.system.event.SafeClientEvent
import base.utils.combat.getTarget
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper

object AimBot : Module(
    name = "AimBot",
    langName = "自动瞄准",
    category = Category.COMBAT,
    type = Type.Both
) {
    private val range by dsetting("Range", 5.0, 1.0, 8.0)

    private var lastTarget: Entity? = null

    init {
        onRender3D {
            lastTarget = getTarget(range)
            lastTarget?.let { target ->
                if (!target.isAlive) {
                    lastTarget = null
                    return@onRender3D
                }
                player.setYaw(getYawToEntityNew(target))
            }
        }
        onLoop {
            lastTarget = getTarget(range)
            lastTarget?.let { target ->
                if (!target.isAlive) {
                    lastTarget = null
                    return@onLoop
                }
                player.setYaw(getYawToEntityNew(target))
            }
        }
    }

    private fun SafeClientEvent.getYawToEntityNew(entity: Entity): Float {
        return getYawBetween(player.yaw, player.x, player.z, entity.x, entity.z)
    }

    private fun getYawBetween(yaw: Float, srcX: Double, srcZ: Double, destX: Double, destZ: Double): Float {
        val xDist = destX - srcX
        val zDist = destZ - srcZ
        val yaw1 = (StrictMath.atan2(zDist, xDist) * 180.0 / 3.141592653589793).toFloat() - 90.0f
        return yaw + MathHelper.wrapDegrees(yaw1 - yaw)
    }

}