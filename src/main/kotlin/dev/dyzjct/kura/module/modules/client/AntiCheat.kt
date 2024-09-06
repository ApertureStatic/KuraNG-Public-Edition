package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.input.MovementInputEvent
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.util.math.MathHelper
import kotlin.math.abs


object AntiCheat : Module(
    name = "AntiCheat",
    langName = "反作弊",
    category = Category.CLIENT
) {
    val ac by msetting("AntiCheat", AntiCheats.Vanilla)
    private val moveFix by msetting("MoveFix", MoveFix.NONE)

    init {
        safeEventListener<MovementInputEvent> { event ->
            if (ac == AntiCheats.GrimAC) {
                when (moveFix) {
                    MoveFix.GrimAC -> {
                        RotationManager.rotateYaw?.let { yaw ->
                            val forward = event.forward
                            val strafe = event.sideways

                            val angle = MathHelper.wrapDegrees(Math.toDegrees(direction(player.yaw, forward, strafe)))

                            if (forward == 0f && strafe == 0f) {
                                return@safeEventListener
                            }

                            var closestForward = 0f
                            var closestStrafe = 0f
                            var closestDifference = Float.MAX_VALUE

                            for (predictedForward in -1..1) {
                                for (predictedStrafe in -1..1) {
                                    if (predictedStrafe == 0 && predictedForward == 0) continue

                                    val predictedAngle = MathHelper.wrapDegrees(
                                        Math.toDegrees(
                                            direction(yaw, predictedForward.toFloat(), predictedStrafe.toFloat())
                                        )
                                    )
                                    val difference = abs(angle - predictedAngle)

                                    if (difference < closestDifference) {
                                        closestDifference = difference.toFloat()
                                        closestForward = predictedForward.toFloat()
                                        closestStrafe = predictedStrafe.toFloat()
                                    }
                                }
                            }

                            event.forward = closestForward
                            event.sideways = closestStrafe
                        }
                    }
                }
            }
        }
    }

    private fun direction(yaw: Float, moveForward: Float, moveStrafing: Float): Double {
        var rotationYaw = yaw
        if (moveForward < 0f) rotationYaw += 180f

        var forward = 1f

        if (moveForward < 0f) forward = -0.5f
        else if (moveForward > 0f) forward = 0.5f

        if (moveStrafing > 0f) rotationYaw -= 90f * forward
        if (moveStrafing < 0f) rotationYaw += 90f * forward

        return Math.toRadians(rotationYaw.toDouble())
    }


    enum class MoveFix {
        NONE, GrimAC
    }

    @Suppress("UNUSED")
    enum class AntiCheats {
        Vanilla, NCP, GrimAC,Legit
    }
}