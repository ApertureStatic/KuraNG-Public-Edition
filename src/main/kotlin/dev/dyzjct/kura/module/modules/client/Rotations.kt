package dev.dyzjct.kura.module.modules.client

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.input.MovementInputEvent
import dev.dyzjct.kura.event.events.player.JumpEvent
import dev.dyzjct.kura.event.events.player.UpdateMovementEvent
import dev.dyzjct.kura.event.events.player.UpdateVelocityEvent
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.manager.RotationManager.lastUpdate
import dev.dyzjct.kura.mixin.accessor.EntityAccessor
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.player.FreeCam
import net.minecraft.util.math.MathHelper
import java.awt.Color
import kotlin.math.abs

object Rotations :
    Module(name = "Rotations", category = Category.CLIENT, description = "Rotation's Settings.", alwaysEnable = true) {
    val smooth_rotation by bsetting("SmoothRotation", false)
    val duration_ticks by fsetting("SmoothTicks", 1.0F, 0.0F, 2.0F)
    val grim_rotation by bsetting("GrimRotation", false)
    val fov by isetting("FOV", 10, 0, 360)
    private val moveFix by msetting("MoveFix", MoveFix.NONE)
    private val debug by bsetting("Debug", false)
    val override_model by bsetting("OverrideModel", false)
    val scale by fsetting("Scale", 1f, 0.1f, 2f)
    val color by csetting("Color", Color(255, 200, 255, 200))

    var prevFixYaw = 0f
    var prevYaw = 0f
    var prevPitch = 0f

    init {
        safeEventListener<MovementInputEvent> { event ->
            when (moveFix) {
                MoveFix.GrimAC -> {
                    if (System.currentTimeMillis() - lastUpdate <= 500L && lastUpdate != 0L) {
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
                                        direction(
                                            RotationManager.yaw_value,
                                            predictedForward.toFloat(),
                                            predictedStrafe.toFloat()
                                        )
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

                        if (debug) ChatUtil.sendNoSpamMessage("${ChatUtil.GREEN}MOVEMENT FIX IS RUNNING!")
                        event.forward = closestForward
                        event.sideways = closestStrafe
                    }
                }
            }
        }
        safeEventListener<JumpEvent.Post> {
            if (System.currentTimeMillis() - lastUpdate > 500L || lastUpdate == 0L) return@safeEventListener
            if (player.isRiding) return@safeEventListener
            if (FreeCam.isEnabled) return@safeEventListener
            RotationManager.yaw_value
            prevFixYaw = player.yaw
            player.setYaw(RotationManager.yaw_value)
        }

        safeEventListener<UpdateVelocityEvent> { event ->
            if (System.currentTimeMillis() - lastUpdate > 500L || lastUpdate == 0L) return@safeEventListener
            if (FreeCam.isEnabled) FreeCam.disable()
            event.velocity = (
                    EntityAccessor.invokeMovementInputToVelocity(
                        event.movementInput,
                        event.speed,
                        RotationManager.yaw_value
                    )
                    )
            event.cancel()
        }

        safeEventListener<UpdateMovementEvent.Pre> {
            prevYaw = player.getYaw()
            prevPitch = player.getPitch()

            player.setYaw(RotationManager.yaw_value)
            player.setPitch(RotationManager.pitch_value)
        }

        safeEventListener<UpdateMovementEvent.Post> {
            player.setYaw(prevYaw)
            player.setPitch(prevPitch)
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
}