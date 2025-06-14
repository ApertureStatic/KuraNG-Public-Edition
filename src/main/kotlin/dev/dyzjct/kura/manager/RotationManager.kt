package dev.dyzjct.kura.manager

import base.utils.entity.EntityUtils.eyePosition
import base.utils.math.vector.Vec2f
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.RunGameLoopEvent
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.event.events.render.Render3DEvent
import dev.dyzjct.kura.module.modules.client.Rotations
import dev.dyzjct.kura.module.modules.render.PopChams
import dev.dyzjct.kura.utils.extension.sendSequencedPacket
import dev.dyzjct.kura.utils.math.EasedSmoother
import dev.dyzjct.kura.utils.rotation.Rotation
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.atan2
import kotlin.math.sqrt

object RotationManager : AlwaysListening {
    var yaw_value: Float = 0f
    var pitch_value: Float = 0f
    var smooth_yaw = EasedSmoother(0f, Rotations.duration_ticks)
    var smooth_pitch = EasedSmoother(0f, Rotations.duration_ticks)

    var lastUpdate = 0L

    fun onInit() {
        safeEventListener<RunGameLoopEvent.Render> {
            if (System.currentTimeMillis() - lastUpdate > 500L) {
                yaw_value = 0f
                pitch_value = 0f
                lastUpdate = 0L
            }
        }
        safeEventListener<PlayerMotionEvent> {
            if (System.currentTimeMillis() - lastUpdate > 500L || lastUpdate == 0L) return@safeEventListener
            if (!Rotations.override_model) return@safeEventListener
            it.setRenderRotation(yaw_value, pitch_value)
        }
        safeEventListener<Render3DEvent> {
            if (System.currentTimeMillis() - lastUpdate > 500L || lastUpdate == 0L) return@safeEventListener
            if (mc.options.perspective.isFirstPerson) return@safeEventListener
            if (Rotations.override_model) return@safeEventListener
            val renderEnt: PlayerEntity = object : PlayerEntity(
                mc.world,
                BlockPos.ORIGIN,
                player.bodyYaw,
                GameProfile(
                    UUID.randomUUID(),
                    "WATASHI"
                )
            ) {
                override fun isSpectator(): Boolean {
                    return false
                }

                override fun isCreative(): Boolean {
                    return false
                }
            }
            renderEnt.copyPositionAndRotation(player)
            renderEnt.handSwingProgress = player.handSwingProgress
            renderEnt.handSwingTicks = player.handSwingTicks
            renderEnt.isSneaking = player.isSneaking
            renderEnt.limbAnimator.speed = player.limbAnimator.speed
            renderEnt.pitch = pitch_value
            renderEnt.bodyYaw = yaw_value
            renderEnt.headYaw = yaw_value

            RenderSystem.depthMask(false)
            RenderSystem.disableDepthTest()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(770, 771, 0, 1)
            PopChams.renderEntity(it.matrices, renderEnt, Rotations.color, 1.0f, Rotations.scale)
            RenderSystem.disableBlend()
            RenderSystem.depthMask(true)
        }
    }

    fun SafeClientEvent.packetRotate(rotations: Vec2f) {
        packetRotate(rotations.x, rotations.y)
    }

    fun SafeClientEvent.packetRotate(vec3d: Vec3d) {
        val rotations = getRotation(vec3d)
        packetRotate(rotations.yaw, rotations.pitch)
    }

    fun SafeClientEvent.packetRotate(blockPos: BlockPos) {
        val rotations = getRotation(blockPos.toCenterPos())
        packetRotate(rotations.yaw, rotations.pitch)
    }

    fun SafeClientEvent.packetRotate(yaw: Float, pitch: Float) {
        if (Rotations.grim_rotation) {
            if (MathHelper.angleBetween(
                    yaw,
                    Rotations.prevFixYaw
                ) < Rotations.fov && Math.abs(
                    yaw - Rotations.prevPitch
                ) < Rotations.fov
            ) {
                return
            }
        }
        yaw_value = yaw
        pitch_value = pitch
        if (Rotations.smooth_rotation) {
            if (lastUpdate == 0L) {
                smooth_yaw.setCurrentYaw(yaw)
                smooth_pitch.setCurrentYaw(pitch)
            }
            smooth_yaw.setTarget(yaw)
            smooth_pitch.setTarget(pitch)
            yaw_value = smooth_yaw.update()
            pitch_value = smooth_pitch.update()
        }
        yaw_value = MathHelper.wrapDegrees(yaw_value)
        if (mc.player != null) {
            sendSequencedPacket(world) { id ->
                (PlayerMoveC2SPacket.Full(
                    player.x,
                    player.y,
                    player.z,
                    yaw_value,
                    pitch_value,
                    player.isOnGround
                ))
            }
        }
        lastUpdate = System.currentTimeMillis()
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
}