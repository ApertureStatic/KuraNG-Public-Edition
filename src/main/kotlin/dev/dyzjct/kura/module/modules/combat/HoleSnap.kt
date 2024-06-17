package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.event.events.player.PlayerMoveEvent
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils
import base.utils.hole.SurroundUtils
import base.utils.hole.SurroundUtils.betterPosition
import base.utils.hole.SurroundUtils.checkHole
import base.utils.math.BlockPosUtil
import base.utils.math.VectorUtils
import base.utils.math.distanceToCenter
import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.movement.Speed
import dev.dyzjct.kura.module.modules.movement.Step
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.toRadian
import dev.dyzjct.kura.utils.math.RandomUtil
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import kotlin.math.*

object HoleSnap :
    Module(name = "HoleSnap", langName = "拉坑", category = Category.COMBAT, description = "Move to the hole.") {
    private var range = isetting("Range", 5, 1, 50)
    private var timerVal = fsetting("TimerVal", 3.4f, 1f, 4f)
    private var timeoutTicks = isetting("TimeOutTicks", 60, 0, 1000)
    private var toggleStep = bsetting("EnableStep", true)
    private var disableStrafe = bsetting("DisableSpeed", false)
    private var antiAim = bsetting("AntiAim", true)
    private var packetListReset = TimerUtils()
    private var holePos: Vec3d? = null
    private var timerBypassing = false
    private var normalLookPos = 0
    private var rotationMode = 1
    private var enabledTicks = 0
    private var stuckTicks = 0
    private var lastPitch = 0f
    private var lastYaw = 0f
    private var normalPos = 0
    private var ranTicks = 0
    private val Entity.motionSpeed get() = hypot(velocity.x, velocity.z)
    private val PlayerEntity.isFlying: Boolean
        get() = this.isFallFlying || this.abilities.flying

    override fun onEnable() {
        runSafe {
            lastYaw = player.yaw
            lastPitch = player.pitch
        }
    }

    override fun onDisable() {
        holePos = null
        stuckTicks = 0
        ranTicks = 0
        enabledTicks = 0
        rotationMode = 1
        timerBypassing = false
        packetListReset.reset()
        Kura.TICK_TIMER = 1.0f
        if (toggleStep.value && Step.isEnabled) {
            ModuleManager.getModuleByClass(Step::class.java).disable()
        }
    }

    init {
        onPacketReceive { event ->
            if (event.packet is PlayerPositionLookS2CPacket) {
                disable()
            }
        }

        onPacketSend { event ->
            if (event.packet is PlayerInteractBlockC2SPacket && rotationMode == 1) {
                normalPos++
                if (normalPos > 20) {
                    rotationMode = if (normalLookPos > 20) {
                        3
                    } else {
                        2
                    }
                }
            } else if (event.packet is PlayerInteractBlockC2SPacket && rotationMode == 2) {
                normalLookPos++
                if (normalLookPos > 20) {
                    rotationMode = if (normalPos > 20) {
                        3
                    } else {
                        1
                    }
                }
            }
        }

        safeEventListener<PlayerMotionEvent> {
            if (packetListReset.passed(1000)) {
                normalPos = 0
                normalLookPos = 0
                lastYaw = player.yaw
                lastPitch = player.pitch
                packetListReset.reset()
            }
            if (timerBypassing && antiAim.value) {
                when (rotationMode) {
                    1 -> {
                        //Pos
                        if (EntityUtils.isMoving()) {
                            it.setRotation(lastYaw, lastPitch)
                        }
                    }

                    2 -> {
                        //PosLook
                        it.setRotation(
                            lastYaw + RandomUtil.nextFloat(1f, 3f), lastPitch + RandomUtil.nextFloat(1f, 5f)
                        )
                    }

                    3 -> {
                        //Mixed
                        it.setRotation(lastYaw, lastPitch)
                        it.setRotation(
                            lastYaw + RandomUtil.nextFloat(1f, 3f), lastPitch + RandomUtil.nextFloat(1f, 5f)
                        )
                    }
                }
            }
        }

        safeEventListener<PlayerMoveEvent> { event ->
            if (++enabledTicks > timeoutTicks.value) {
                disable()
                return@safeEventListener
            }

            if (!player.isAlive || player.isFlying) return@safeEventListener

            val currentSpeed = player.motionSpeed

            if (shouldDisable(currentSpeed)) {
                timerBypassing = false
                disable()
                return@safeEventListener
            }
            getHole()?.let {
                Kura.TICK_TIMER = timerVal.value
                if (Speed.isEnabled && disableStrafe.value) {
                    Speed.disable()
                }
                if (Step.isDisabled) {
                    Step.enable()
                }
                if (!player.isCentered(it)) {
                    timerBypassing = true
                    val playerPos = player.pos
                    val targetPos = Vec3d(it.x, player.y, it.z)

                    val yawRad = getRotationTo(playerPos, targetPos).x.toRadian()
                    val dist = hypot(targetPos.x - playerPos.x, targetPos.z - playerPos.z)
                    val baseSpeed = player.applySpeedPotionEffects(0.2873)
                    val speed = if (player.onGround) baseSpeed else max(currentSpeed + 0.02, baseSpeed)
                    val cappedSpeed = min(speed, dist)

                    event.vec.x = -sin(yawRad) * cappedSpeed
                    event.vec.z = cos(yawRad) * cappedSpeed

                    if (player.horizontalCollision) stuckTicks++
                    else stuckTicks = 0
                }
            }
        }
    }

    private fun LivingEntity.applySpeedPotionEffects(speed: Double): Double {
        return this.getStatusEffect(StatusEffects.SPEED)?.let {
            speed * this.speedEffectMultiplier
        } ?: speed
    }

    private val LivingEntity.speedEffectMultiplier: Double
        get() = this.getStatusEffect(StatusEffects.SPEED)?.let {
            1.0 + (it.amplifier + 1.0) * 0.2
        } ?: 1.0

    private fun getRotationTo(posFrom: Vec3d, posTo: Vec3d): Vec2f {
        return getRotationFromVec(posTo.subtract(posFrom))
    }

    private fun getRotationFromVec(vec: Vec3d): Vec2f {
        val xz = hypot(vec.x, vec.z)
        val yaw = normalizeAngle(Math.toDegrees(atan2(vec.z, vec.x)) - 90.0)
        val pitch = normalizeAngle(Math.toDegrees(-atan2(vec.y, xz)))
        return Vec2f(yaw.toFloat(), pitch.toFloat())
    }

    private fun normalizeAngle(angleIn: Double): Double {
        var angle = angleIn
        angle %= 360.0
        if (angle >= 180.0) {
            angle -= 360.0
        }
        if (angle < -180.0) {
            angle += 360.0
        }
        return angle
    }

    private fun SafeClientEvent.shouldDisable(currentSpeed: Double) =
        holePos?.let { player.y < it.y } ?: false || stuckTicks > 5 && currentSpeed < 0.1 || currentSpeed < 0.01 && getHole()?.let {
            player.isCentered(it)
        } == true || (checkHole(player) != SurroundUtils.HoleType.NONE)

    private fun PlayerEntity.isCentered(center: Vec3d): Boolean {
        return this.isCentered(center.x + 0.5, center.z + 0.5)
    }

    private fun PlayerEntity.isCentered(x: Double, z: Double): Boolean {
        return abs(this.x - x) < 0.2 && abs(this.z - z) < 0.2
    }

    private fun SafeClientEvent.getHole() = if (player.age % 10 == 0 && player.betterPosition != holePos) findHole()
    else holePos ?: findHole()

    private fun SafeClientEvent.findHole(): Vec3d? {
        var closestHole = Pair(69.69, Vec3d.ZERO)
        val playerPos = player.betterPosition
        val ceilRange = range.value
        BlockPosUtil
        val posList = VectorUtils.getBlockPosInSphere(playerPos.toCenterPos(), ceilRange.toFloat())

        for (posXZ in posList) {
            val dist = player.distanceToCenter(posXZ)
            if (dist > range.value || dist > closestHole.first) continue

            for (posY in 0..5) {
                val pos = posXZ.add(0, -posY, 0)
                if (!world.isAir(pos.up())) break
                if (checkHole(pos) == SurroundUtils.HoleType.NONE) continue
                closestHole = dist to pos.toCenterPos()
            }
        }

        return if (closestHole.second != Vec3d.ZERO) closestHole.second.also { holePos = it }
        else null
    }
}