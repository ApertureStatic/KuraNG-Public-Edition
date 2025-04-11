package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.input.MovementInputEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.movement.HoleSnap
import dev.dyzjct.kura.module.modules.player.FreeCam
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d


object AntiCheat : Module(
    name = "AntiCheat",
    langName = "反作弊",
    category = Category.CLIENT
) {
    val ac by msetting("AntiCheat", AntiCheats.Vanilla)
    val look by bsetting("Look", true)
    val look_time by dsetting("LookTime", 0.5, 0.0, 1.0, 0.01)
    val fov by fsetting("Fov", 10f, 0f, 180f)
    val no_spam by bsetting("SpamCheck", true)
    val steps by fsetting("Steps", 0.6f, 0f, 1f)
    val forceSync by bsetting("ServerSide", false)
    val moveFix by msetting("MoveFix", MoveFix.NONE)
    val updateMode by msetting("UpdateMode", UpdateMode.UpdateMouse)

    var fixRotation = 0f
    var fixPitch = 0f
    private var prevYaw = 0f
    private var prevPitch = 0f

    init {
        safeEventListener<MovementInputEvent> { event ->
            if (ac == AntiCheats.GrimAC) {
                when (moveFix) {
                    MoveFix.GrimAC -> {
                        if (HoleSnap.isEnabled) return@safeEventListener
                        if (player.isRiding || FreeCam.isEnabled) return@safeEventListener

                        val mF = player.input.movementForward
                        val mS = player.input.movementSideways
                        val delta = (player.getYaw() - fixRotation) * MathHelper.RADIANS_PER_DEGREE
                        val cos = MathHelper.cos(delta)
                        val sin = MathHelper.sin(delta)
                        player.input.movementSideways = Math.round(mS * cos - mF * sin).toFloat()
                        player.input.movementForward = Math.round(mF * cos + mS * sin).toFloat()
                    }
                }
            }
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

    enum class MoveFix {
        NONE, GrimAC
    }

    @Suppress("UNUSED")
    enum class AntiCheats {
        Vanilla, NCP, GrimAC, Legit
    }

    enum class UpdateMode {
        MovementPacket,
        UpdateMouse,
        All
    }
}