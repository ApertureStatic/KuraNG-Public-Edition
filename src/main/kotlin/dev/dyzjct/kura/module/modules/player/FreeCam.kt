package dev.dyzjct.kura.module.modules.player

import base.utils.concurrent.threads.runSafe
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.input.KeyboardInputEvent
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import kotlin.math.cos
import kotlin.math.sin


object FreeCam : Module(
    name = "FreeCam",
    langName = "灵魂出窍",
    description = "Free Camera.",
    category = Category.PLAYER
) {
    private val speed by dsetting("HSpeed", 1.0, 0.0, 3.0)
    private val hspeed by dsetting("VSpeed", 0.42, 0.0, 3.0)
    val rotate by bsetting("Rotate", true)
    private var fakeYaw = 0.0f
    private var fakePitch = 0.0f
    private var prevFakeYaw = 0.0f
    private var prevFakePitch = 0.0f
    private var fakeX = 0.0
    private var fakeY = 0.0
    private var fakeZ = 0.0
    private var prevFakeX = 0.0
    private var prevFakeY = 0.0
    private var prevFakeZ = 0.0


    override fun onEnable() {
        runSafe {
            mc.chunkCullingEnabled = false

            fakePitch = player.pitch
            fakeYaw = player.yaw

            prevFakePitch = fakePitch
            prevFakeYaw = fakeYaw

            fakeX = player.x
            fakeY = player.y + player.getEyeHeight(mc.player!!.pose)
            fakeZ = player.z

            prevFakeX = fakeX
            prevFakeY = fakeY
            prevFakeZ = fakeZ
        }
    }


    override fun onDisable() {
        mc.chunkCullingEnabled = true
    }

    init {
        onMotion {
            if (rotate && mc.crosshairTarget != null && mc.crosshairTarget!!.pos != null) {
                RotationManager.rotationTo(mc.crosshairTarget!!.pos, false)
            }
        }

        safeEventListener<KeyboardInputEvent> {
            val motion: DoubleArray = directionSpeed(speed)

            prevFakeX = fakeX
            prevFakeY = fakeY
            prevFakeZ = fakeZ

            fakeX += motion[0]
            fakeZ += motion[1]

            if (mc.options.jumpKey.isPressed) fakeY += hspeed

            if (mc.options.sneakKey.isPressed) fakeY -= hspeed

            player.input.movementForward = 0.0f
            player.input.movementSideways = 0.0f
            player.input.jumping = false
            player.input.sneaking = false
        }
        onRender3D {
            prevFakeYaw = fakeYaw
            prevFakePitch = fakePitch

            fakeYaw = player.yaw
            fakePitch = player.pitch
        }
    }

    fun getFakeYaw(): Float {
        return interpolate(prevFakeYaw.toDouble(), fakeYaw.toDouble(), mc.tickDelta).toFloat()
    }

    fun getFakePitch(): Float {
        return interpolate(prevFakePitch.toDouble(), fakePitch.toDouble(), mc.tickDelta).toFloat()
    }

    fun getFakeX(): Double {
        return interpolate(prevFakeX, fakeX, mc.tickDelta)
    }

    fun getFakeY(): Double {
        return interpolate(prevFakeY, fakeY, mc.tickDelta)
    }

    fun getFakeZ(): Double {
        return interpolate(prevFakeZ, fakeZ, mc.tickDelta)
    }

    private fun directionSpeed(speed: Double): DoubleArray {
        var forward = mc.player!!.input.movementForward
        var side = mc.player!!.input.movementSideways
        var yaw = mc.player!!.prevYaw + (mc.player!!.getYaw() - mc.player!!.prevYaw) * mc.tickDelta
        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += (if ((forward > 0.0f)) -45 else 45).toFloat()
            } else if (side < 0.0f) {
                yaw += (if ((forward > 0.0f)) 45 else -45).toFloat()
            }
            side = 0.0f
            if (forward > 0.0f) {
                forward = 1.0f
            } else if (forward < 0.0f) {
                forward = -1.0f
            }
        }
        val sin = sin(Math.toRadians((yaw + 90.0f).toDouble()))
        val cos = cos(Math.toRadians((yaw + 90.0f).toDouble()))
        val posX = forward * speed * cos + side * speed * sin
        val posZ = forward * speed * sin - side * speed * cos
        return doubleArrayOf(posX, posZ)
    }
}