package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.input.KeyboardInputEvent
import dev.dyzjct.kura.event.events.player.JumpEvent
import dev.dyzjct.kura.event.events.player.UpdateVelocityEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.movement.HoleSnap
import dev.dyzjct.kura.module.modules.player.FreeCam
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

object MoveFix : Module(
    name = "MoveFix",
    description = "",
    category = Category.CLIENT
) {
    var fixRotation: Float = 0f
    var fixPitch: Float = 0f

    private var prevYaw = 0f
    private var prevPitch = 0f

    init {
        safeEventListener<JumpEvent.Pre> {
            if (player.isRiding) return@safeEventListener
            prevYaw = player.yaw
            prevPitch = player.pitch
            player.yaw = fixRotation
            player.pitch = fixPitch
        }

        safeEventListener<JumpEvent.Post> {
            if (player.isRiding) return@safeEventListener
            player.yaw = prevYaw
            player.pitch = prevPitch
        }

        safeEventListener<UpdateVelocityEvent> {
            if (player.isRiding) return@safeEventListener
            it.cancel()
            it.velocity = (movementInputToVelocity(it.movementInput, it.speed, fixRotation))
        }

        safeEventListener<KeyboardInputEvent> {
            if (HoleSnap.isEnabled) return@safeEventListener
            if (player.isRiding || FreeCam.isEnabled) return@safeEventListener

            val mF: Float = player.input.movementForward
            val mS: Float = player.input.movementSideways
            val delta: Float = (player.yaw - fixRotation) * MathHelper.RADIANS_PER_DEGREE
            val cos = MathHelper.cos(delta)
            val sin = MathHelper.sin(delta)
            player.input.movementSideways = Math.round(mS * cos - mF * sin).toFloat()
            player.input.movementForward = Math.round(mF * cos + mS * sin).toFloat()
        }
    }


    private fun movementInputToVelocity(movementInput: Vec3d, speed: Float, yaw: Float): Vec3d {
        val d = movementInput.lengthSquared()
        if (d < 1.0E-7) {
            return Vec3d.ZERO
        } else {
            val vec3d = (if (d > 1.0) movementInput.normalize() else movementInput).multiply(speed.toDouble())
            val f = MathHelper.sin(yaw * 0.017453292f)
            val g = MathHelper.cos(yaw * 0.017453292f)
            return Vec3d(
                vec3d.x * g.toDouble() - vec3d.z * f.toDouble(),
                vec3d.y,
                vec3d.z * g.toDouble() + vec3d.x * f.toDouble()
            )
        }
    }
}