package dev.dyzjct.kura.manager

import base.utils.entity.EntityUtils.eyePosition
import base.utils.math.vector.Vec2f
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.MovementPacketsEvent
import dev.dyzjct.kura.event.events.input.KeyboardTickEvent
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.event.events.render.Render3DEvent
import dev.dyzjct.kura.module.modules.client.MovementFix
import dev.dyzjct.kura.module.modules.client.Rotations
import dev.dyzjct.kura.module.modules.render.PopChams
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.applySensitivityPatch
import dev.dyzjct.kura.utils.rotation.RotationUtils.resetRotation
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.*

object RotationManager : AlwaysListening {
    var yaw_value = 0f
        private set
    var pitch_value = 0f
        private set
    var lastUpdate = 0L
        private set

    private var lastYaw = 0f
    private var lastPitch = 0f

    var active = false
        private set

    private var smoothed = false
    private var rotationSpeed = 0.0
    private var randomAngle = 0f

    private var offset = Vec2f(0f, 0f)
    private var raycast: ((Vec2f) -> Boolean)? = null

    private val lock = Any()
    private val tickRequests = CopyOnWriteArrayList<RotationRequest>()

    /**
     * 🌟【自定义独立判断开关】
     * 只有这里返回 true 时，才会允许执行 MovementFix 输入重组。
     */
    fun SafeClientEvent.shouldFixInputIfNecessary(): Boolean {
        val isSystemReady = active && (System.currentTimeMillis() - lastUpdate <= 500L) && lastUpdate != 0L
        if (!isSystemReady) return false

        return MovementFix.isEnabled
    }

    fun onInit() {
        // 1. START 阶段：100% 保持你原本的所有平滑和逻辑计算次序
        safeEventListener<PlayerMotionEvent> { event ->
            if (event.stageType == dev.dyzjct.kura.event.eventbus.StageType.START) {
                if (isExpired) {
                    reset()
                    lastYaw = player.yaw
                    lastPitch = player.pitch
                    return@safeEventListener
                }

                val requestsSnapshot = ArrayList<RotationRequest>()
                synchronized(lock) {
                    requestsSnapshot.addAll(tickRequests.filterNotNull())
                    tickRequests.clear()
                }

                if (requestsSnapshot.isNotEmpty()) {
                    val selected = requestsSnapshot.maxByOrNull { it.priorityValue }
                    if (selected != null) {
                        rotationSpeed = selected.reqSpeed
                        raycast = selected.reqRaycast
                        active = true
                        smoothed = false

                        smooth(selected.targetRotation.x, selected.targetRotation.y)

                        selected.callback?.let {
                            runCatching {
                                it(RotationApplyRecord(Vec2f(yaw_value, pitch_value), active))
                            }
                        }
                    }
                } else if (active) {
                    prepareRotateBack()
                    smooth(player.yaw, player.pitch)
                }

                if (active) {
                    lastYaw = yaw_value
                    lastPitch = pitch_value
                    event.yaw = yaw_value
                    event.pitch = pitch_value

                    if (abs(yaw_value - player.yaw) < 1f && abs(pitch_value - player.pitch) < 1f) {
                        active = false
                        correctDisabledRotations()
                    }
                } else {
                    lastYaw = player.yaw
                    lastPitch = player.pitch
                }
            }
        }

        // 2. 发包阶段更新：保持不变
        safeEventListener<MovementPacketsEvent> { event ->
            if (active && !isExpired) {
                event.yaw = yaw_value
                event.pitch = pitch_value
            }
        }

        // 3. 🛠️【移动修复执行点】：传入计算好的假角度进行输入对齐
        safeEventListener<KeyboardTickEvent> { event ->
            if (shouldFixInputIfNecessary()) {
                MovementFix.fixMovement(event, yaw_value)
            }
        }

        // 4. 渲染层保持不变
        safeEventListener<Render3DEvent> { event ->
            if (isExpired || lastUpdate == 0L || !active) return@safeEventListener
            if (mc.options.perspective.isFirstPerson || Rotations.override_model) return@safeEventListener

            val renderEnt = object : PlayerEntity(
                mc.world, BlockPos.ORIGIN, player.bodyYaw, GameProfile(UUID.randomUUID(), "WATASHI")
            ) {
                override fun isSpectator() = false
                override fun isCreative() = false
            }.apply {
                copyPositionAndRotation(player)
                handSwingProgress = player.handSwingProgress
                handSwingTicks = player.handSwingTicks
                isSneaking = player.isSneaking
                limbAnimator.speed = player.limbAnimator.speed
                pitch = pitch_value
                bodyYaw = yaw_value
                headYaw = yaw_value
            }

            RenderSystem.depthMask(false)
            RenderSystem.disableDepthTest()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(770, 771, 0, 1)
            PopChams.renderEntity(event.matrices, renderEnt, Rotations.color, 1.0f, Rotations.scale)
            RenderSystem.disableBlend()
            RenderSystem.depthMask(true)
        }
    }

    private val isExpired: Boolean
        get() = System.currentTimeMillis() - lastUpdate > 500L

    private fun reset() {
        yaw_value = 0f
        pitch_value = 0f
        lastUpdate = 0L
        active = false
        synchronized(lock) {
            tickRequests.clear()
        }
    }

    private fun SafeClientEvent.correctDisabledRotations() {
        val currentRot = Vec2f(player.yaw, player.pitch)
        val lastRot = Vec2f(lastYaw, lastPitch)
        val fixedRot = resetRotation(applySensitivityPatch(currentRot, lastRot))

        if (fixedRot != null && !fixedRot.x.isNaN() && !fixedRot.y.isNaN()) {
            player.yaw = fixedRot.x
            player.pitch = fixedRot.y
        }
    }

    // ==================== 100% 保持你所有的对外调用接口不变 ====================

    fun SafeClientEvent.applyRotation(rotations: Vec2f, speed: Double, priority: Priority = Priority.Lowest, callback: ((RotationApplyRecord) -> Unit)? = null) =
        applyRotation(rotations.x, rotations.y, speed, null, priority.priority, callback)

    fun SafeClientEvent.applyRotation(blockPos: BlockPos, speed: Double, priority: Priority = Priority.Lowest, callback: ((RotationApplyRecord) -> Unit)? = null) {
        val rot = getRotation(blockPos.toCenterPos())
        applyRotation(rot.yaw, rot.pitch, speed, null, priority.priority, callback)
    }

    fun SafeClientEvent.applyRotation(vec3d: Vec3d, speed: Double, priority: Priority = Priority.Lowest, callback: ((RotationApplyRecord) -> Unit)? = null) {
        val rot = getRotation(vec3d)
        applyRotation(rot.yaw, rot.pitch, speed, null, priority.priority, callback)
    }

    fun SafeClientEvent.applyRotation(
        yaw: Float,
        pitch: Float,
        speed: Double,
        raycast: ((Vec2f) -> Boolean)? = null,
        priorityValue: Int = Priority.Lowest.priority,
        callback: ((RotationApplyRecord) -> Unit)? = null
    ) {
        if (yaw.isNaN() || pitch.isNaN() || yaw.isInfinite() || pitch.isInfinite()) return

        lastUpdate = System.currentTimeMillis()
        val safePriority = priorityValue.coerceAtLeast(Priority.Lowest.priority)

        synchronized(lock) {
            tickRequests.add(
                RotationRequest(Vec2f(yaw, pitch), speed * 18, raycast, safePriority, callback)
            )
        }
    }

    private fun SafeClientEvent.prepareRotateBack() {
        rotationSpeed = 10.0 * 18
        raycast = null
        smoothed = false
    }

    private fun SafeClientEvent.smooth(targetYaw: Float, targetPitch: Float) {
        if (smoothed) return

        var calcYaw = targetYaw
        var calcPitch = targetPitch
        val currentYaw = if (active) yaw_value else player.yaw
        val currentPitch = if (active) pitch_value else player.pitch

        raycast?.let { filter ->
            if (abs(calcYaw - currentYaw) > 5 || abs(calcPitch - currentPitch) > 5) {
                val speed = (Math.random() * Math.random() * Math.random()) * 20
                val direction = if ((player.age / 10) % 2 == 0) -1 else 1
                randomAngle += ((20 + (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360)) * direction).toFloat()

                val nextOffsetX = (offset.x + -sin(Math.toRadians(randomAngle.toDouble())) * speed).toFloat()
                val nextOffsetY = (offset.y + cos(Math.toRadians(randomAngle.toDouble())) * speed).toFloat()
                offset = Vec2f(nextOffsetX, nextOffsetY)

                calcYaw += offset.x
                calcPitch += offset.y

                if (!filter(Vec2f(calcYaw, calcPitch))) {
                    randomAngle = (Math.toDegrees(atan2((targetYaw - calcYaw).toDouble(), (calcPitch - targetPitch).toDouble())) - 180).toFloat()
                    calcYaw -= offset.x
                    calcPitch -= offset.y

                    val retryOffsetX = (offset.x + -sin(Math.toRadians(randomAngle.toDouble())) * speed).toFloat()
                    val retryOffsetY = (offset.y + cos(Math.toRadians(randomAngle.toDouble())) * speed).toFloat()
                    offset = Vec2f(retryOffsetX, retryOffsetY)

                    calcYaw += offset.x
                    calcPitch += offset.y
                }

                if (!filter(Vec2f(calcYaw, calcPitch))) {
                    offset = Vec2f(0f, 0f)
                    calcYaw = (targetYaw + Math.random() * 2).toFloat()
                    calcPitch = (targetPitch + Math.random() * 2).toFloat()
                }
            }
        }

        val deltaYaw = MathHelper.wrapDegrees(calcYaw - currentYaw)
        val maxStep = (rotationSpeed + Math.random()).toFloat()

        yaw_value = currentYaw + MathHelper.clamp(deltaYaw, -maxStep, maxStep)
        pitch_value = (currentPitch + MathHelper.clamp(calcPitch - currentPitch, -maxStep, maxStep)).coerceIn(-90f, 90f)

        smoothed = true
    }

    private fun SafeClientEvent.getRotation(vec: Vec3d): Rotation {
        val diffX = vec.x - player.eyePosition.x
        val diffY = vec.y - player.eyePosition.y
        val diffZ = vec.z - player.eyePosition.z
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90.0f
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()
        return Rotation(MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch))
    }

    enum class Priority(val priority: Int) {
        Lowest(0), Low(1), Medium(2), High(3), Highest(4)
    }

    private data class RotationRequest(
        val targetRotation: Vec2f,
        val reqSpeed: Double,
        val reqRaycast: ((Vec2f) -> Boolean)?,
        val priorityValue: Int,
        val callback: ((RotationApplyRecord) -> Unit)?
    )

    data class RotationApplyRecord(val currentRotation: Vec2f, val isActive: Boolean)
}