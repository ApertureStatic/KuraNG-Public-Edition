package dev.dyzjct.kura.module.modules.player.disabler

import base.events.PacketEvents
import base.events.TickEvent
import base.system.event.ListenerOwner
import base.system.event.safeEventListener
import base.utils.entity.EntityUtils.isInBurrow
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object BurrowBypass: ListenerOwner() {

    fun enable() { subscribe() }
    fun disable() { unsubscribe() }

    private var lastDataLook: PlayerMoveC2SPacket.LookAndOnGround? = null

    init {
        safeEventListener<PacketEvents.Send> {
            if (!Disabler.burrowBypass) return@safeEventListener
            if (!isInBurrow()) return@safeEventListener
            if (it.packet is PlayerMoveC2SPacket) {
                if (Disabler.tickPacket) when (it.packet) {
                    is PlayerMoveC2SPacket.LookAndOnGround -> {
                        lastDataLook?.let { data ->
                            if (it.packet == data) {
                                lastDataLook = null
                                return@safeEventListener
                            }
                        }
                    }
                }

                it.cancel()
            }
        }

        safeEventListener<PacketEvents.Receive> {
            if (!Disabler.burrowBypass) return@safeEventListener
            if (!Disabler.noServerMove) return@safeEventListener
            if (!isInBurrow()) return@safeEventListener
            if (it.packet !is PlayerPositionLookS2CPacket) return@safeEventListener
            it.packet.yaw = player.yaw
            it.packet.pitch = player.pitch
//            it.packet.x = player.x
//            it.packet.y = player.y
//            it.packet.z = player.z
        }

        safeEventListener<PacketEvents.Receive> {
            if (!Disabler.burrowBypass) return@safeEventListener
            if (!Disabler.posSync) return@safeEventListener
            if (!isInBurrow()) return@safeEventListener
            if (it.packet !is PlayerPositionLookS2CPacket) return@safeEventListener
            connection.sendPacket(PlayerMoveC2SPacket.Full(
                player.x, -1337.0, player.z,
                player.yaw, player.pitch,
                player.isOnGround
            ))
        }

        safeEventListener<TickEvent.Post> {
            if (!Disabler.burrowBypass) return@safeEventListener
            if (!Disabler.tickPacket) return@safeEventListener
            if (!isInBurrow()) return@safeEventListener
            if (player.yaw == player.lastYaw && player.pitch == player.lastPitch) return@safeEventListener

            lastDataLook = PlayerMoveC2SPacket.LookAndOnGround(player.yaw, player.pitch, player.onGround)
            lastDataLook?.let {
                connection.sendPacket(lastDataLook)
            }
        }
    }

    private data class LastPacket(
        val x: Double, val y: Double, val z: Double,
        val yaw: Float, val pitch: Float, val onGround: Boolean
    )

}