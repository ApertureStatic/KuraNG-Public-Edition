package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.player.PlayerMoveEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.rotation.RotationUtils.normalizeAngle
import kotlin.math.abs


object AntiCheat : Module(
    name = "AntiCheat",
    langName = "反作弊",
    category = Category.CLIENT
) {
    private val ac by msetting("AntiCheat", AntiCheats.Vanilla)
    private val moveFix by msetting("MoveFix", MoveFix.NONE)

    private var movementYaw: Float? = null
    private var rotationYaw: Float? = null

    init {
        safeEventListener<PlayerMoveEvent> {
            if (ac == AntiCheats.GrimAC) {
                when (moveFix) {
                    MoveFix.NONE -> movementYaw =
                        null

                    MoveFix.SILENT -> {
                        movementYaw =
                            getRotationYaw()

                        val forward: Float = player.input.movementForward
                        val strafe: Float = player.input.movementSideways

                        val angle: Double = wrapAngleTo180_double(
                            Math.toDegrees(
                                direction(
                                    player.renderYaw,
                                    forward.toDouble(),
                                    strafe.toDouble()
                                )
                            )
                        )

                        if (forward == 0f && strafe == 0f) {
                            return@safeEventListener
                        }

                        var closestForward = 0f
                        var closestStrafe = 0f
                        var closestDifference = Float.MAX_VALUE

                        var predictedForward = -1f
                        while (predictedForward <= 1f) {
                            var predictedStrafe = -1f
                            while (predictedStrafe <= 1f) {
                                if (predictedStrafe == 0f && predictedForward == 0f) {
                                    predictedStrafe += 1f
                                    continue
                                }
                                movementYaw?.let {
                                    val predictedAngle: Double = wrapAngleTo180_double(
                                        Math.toDegrees(
                                            direction(
                                                movementYaw!!,
                                                predictedForward.toDouble(),
                                                predictedStrafe.toDouble()
                                            )
                                        )
                                    )
                                    val difference = abs(angle - predictedAngle)

                                    if (difference < closestDifference) {
                                        closestDifference = difference.toFloat()
                                        closestForward = predictedForward
                                        closestStrafe = predictedStrafe
                                    }
                                }
                                predictedStrafe += 1f
                            }
                            predictedForward += 1f
                        }
                        player.input.movementForward = closestForward
                        player.input.movementSideways = closestStrafe
                    }

                    MoveFix.STRICT -> movementYaw =
                        getRotationYaw()
                }
            }
        }
    }

    private fun SafeClientEvent.getRotationYaw(): Float {
        rotationYaw?.let {
            return normalizeAngle(
                it
            )
        }
        return normalizeAngle(player.renderYaw)
    }

    fun direction(rotationYaw: Float, moveForward: Double, moveStrafing: Double): Double {
        var rotationYawCalced = rotationYaw
        if (moveForward < 0f) rotationYawCalced += 180f

        var forward = 1f

        if (moveForward < 0f) forward = -0.5f
        else if (moveForward > 0f) forward = 0.5f

        if (moveStrafing > 0f) rotationYawCalced -= 90f * forward
        if (moveStrafing < 0f) rotationYawCalced += 90f * forward

        return Math.toRadians(rotationYawCalced.toDouble())
    }


    private fun wrapAngleTo180_double(value: Double): Double {
        var calcValue = value
        calcValue %= 360.0
        if (calcValue >= 180.0) {
            calcValue -= 360.0
        }
        if (calcValue < -180.0) {
            calcValue += 360.0
        }
        return calcValue
    }


    enum class MoveFix {
        NONE, SILENT, STRICT
    }

    enum class AntiCheats {
        Vanilla, NCP, GrimAC
    }
}