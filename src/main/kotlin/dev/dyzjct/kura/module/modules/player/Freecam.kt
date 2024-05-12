package dev.dyzjct.kura.module.modules.player

import com.mojang.authlib.GameProfile
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.events.ConnectionEvent
import base.events.PacketEvents
import base.events.player.PlayerMoveEvent
import base.system.event.safeEventListener
import base.utils.concurrent.threads.runSafe
import base.utils.player.RotationUtils.directionSpeed
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object Freecam : Module(name = "Freecam", langName = "灵魂出窍", category = Category.PLAYER, type = Type.Both) {
    private var cancelPackets = bsetting("CancelPackets", true)
    private var rotate = bsetting("Rotate", false)
    private var speed = dsetting("Speed", 1.0, 0.1, 10.0)
    private var firstStart = false
    private var posX = 0.0
    private var posY = 0.0
    private var posZ = 0.0
    private var clonedPlayer: OtherClientPlayerEntity? = null
    private var isRidingEntity = false
    private var ridingEntity: Entity? = null

    init {
        safeEventListener<PacketEvents.Send> { event ->
            if (cancelPackets.value) {
                if (event.packet is PlayerMoveC2SPacket || event.packet is PlayerInputC2SPacket) {
                    event.cancelled = true
                }
            }
        }
    }

    override fun onEnable() {
        runSafe {
            firstStart = true
            isRidingEntity = player.vehicle != null
            player.vehicle?.let {
                ridingEntity = it
                player.dismountVehicle()
            } ?: {
                posX = player.x
                posY = player.y
                posZ = player.z
            }
            clonedPlayer = OtherClientPlayerEntity(world, GameProfile(mc.session.uuidOrNull, mc.session.username)).also {
                it.boundingBox = Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                it.copyFrom(player)
                it.headYaw = player.headYaw
            }
            world.addEntity(-101, clonedPlayer)
            player.abilities.flying = true
            player.abilities.flySpeed = (speed.value / 100f).toFloat()
            player.noClip = true
        }
    }

    override fun onDisable() {
        runSafe {
            clonedPlayer?.let {
                player.setPosition(posX, posY, posZ)
                player.setYaw(it.yaw % 360.0f)
                player.setPitch(it.pitch % 360.0f)
                it.kill()
                it.setRemoved(Entity.RemovalReason.KILLED)
                it.onRemoved()
            }
            clonedPlayer = null
            posZ = 0.0
            posY = posZ
            posX = posY
            player.abilities.flying = false
            player.abilities.flySpeed = 0.05f
            player.noClip = false
            player.velocity.z = 0.0
            player.velocity.y = player.velocity.z
            player.velocity.x = player.velocity.y
            if (isRidingEntity) {
                ridingEntity?.let {
                    player.startRiding(it, true)
                }
            }
        }
    }

    init {
        onMotion {
            player.noClip = true
            player.setVelocity(0.0, 0.0, 0.0)
            val dir = directionSpeed(speed.value)
            if (player.input.movementSideways != 0f || player.input.movementForward != 0f) {
                player.velocity.x = dir[0]
                player.velocity.z = dir[1]
            } else {
                player.velocity.x = 0.0
                player.velocity.z = 0.0
            }
            player.isSprinting = false
            if (rotate.value) {
                clonedPlayer?.let { cp ->
                    cp.prevPitch = player.prevPitch
                    player.prevPitch = cp.prevPitch

                    cp.prevYaw = player.prevYaw
                    player.prevYaw = cp.prevYaw

                    cp.pitch = player.pitch
                    player.pitch = cp.pitch

                    cp.yaw = player.yaw
                    player.yaw = cp.yaw

                    cp.headYaw = player.headYaw
                    player.headYaw = cp.headYaw

                    it.setRotation(cp.yaw, cp.pitch)
                }
            }
            if (mc.options.jumpKey.isPressed) {
                player.velocity.y += speed.value
            }
            if (mc.options.sneakKey.isPressed) {
                player.velocity.y -= speed.value
            }
        }

        safeEventListener<PlayerMoveEvent> { event ->
            if (firstStart) {
                event.vec = Vec3d.ZERO
                firstStart = false
            }
            player.noClip = true
        }

        safeEventListener<ConnectionEvent.Join> {
            safeDisable()
        }
    }
}