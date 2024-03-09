package dev.dyzjct.kura.manager

import base.events.PacketEvents
import base.events.TickEvent
import base.system.event.AlwaysListening
import base.system.event.safeEventListener
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import kotlin.math.hypot
import kotlin.math.max

object MovementManager : AlwaysListening {
    var currentPlayerSpeed = 0.0
    var boostSpeed = 0.0

    fun onInit() {
        safeEventListener<PacketEvents.Receive>(Int.MAX_VALUE, true) { event ->
            if (event.packet is EntityVelocityUpdateS2CPacket) {
                if (event.packet.id == player.id) {
                    boostSpeed = hypot(
                        event.packet.velocityX / 8000.0,
                        event.packet.velocityZ / 8000.0
                    )
                }
            }
        }

        safeEventListener<PacketEvents.PostReceive>(Int.MAX_VALUE, true) { event ->
            if (event.packet is EntityVelocityUpdateS2CPacket) {
                if (event.packet.id == player.id) {
                    boostSpeed = max(
                        boostSpeed, hypot(
                            event.packet.velocityX / 8000.0,
                            event.packet.velocityZ / 8000.0
                        )
                    )
                }
            }
        }

        safeEventListener<TickEvent.Post>(true) {
            currentPlayerSpeed = hypot((player.x - player.prevX), (player.z - player.prevZ))
        }
    }

    fun boostReset() {
        boostSpeed = 0.0
    }
}