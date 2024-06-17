package dev.dyzjct.kura.utils.math

import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeConcurrentListener
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket
import java.util.*

object LagCompensator : AlwaysListening {
    private val tickRates = FloatArray(20)
    private var timeLastTimeUpdate = -1L
    private var nextIndex = 0

    fun call() {
        Arrays.fill(tickRates, 0.0f)
        safeConcurrentListener<PacketEvents.Receive>(true) {
            when (it.packet) {
                is WorldTimeUpdateS2CPacket -> {
                    onTimeUpdate()
                }
            }
        }
    }

    val tickRate: Float
        get() {
            var numTicks = 0.0f
            var sumTickRates = 0.0f
            for (tickRate in tickRates) {
                if (tickRate > 0.0f) {
                    sumTickRates += tickRate
                    numTicks += 1.0f
                }
            }
            return (sumTickRates / numTicks).coerceIn(0f, 20f)
        }

    private fun onTimeUpdate() {
        if (timeLastTimeUpdate != -1L) {
            val timeElapsed = (System.currentTimeMillis() - timeLastTimeUpdate).toFloat() / 1000.0f
            tickRates[nextIndex % tickRates.size] = (20.0f / timeElapsed).coerceIn(0f, 20f)
            nextIndex += 1
        }
        timeLastTimeUpdate = System.currentTimeMillis()
    }

    //    fun globalInfoPingValue(targetID: EntityPlayer? = null): Int {
//        Wrapper.player?.let { player ->
//            return player.connection?.getPlayerInfo(targetID?.uniqueID ?: player.uniqueID)?.responseTime ?: -1
//        } ?: return -1
//    }
    fun SafeClientEvent.globalInfoPingValue(): Int {
        connection.playerList?.let { playerList ->
            for (players in playerList) {
                players?.let { entry ->
                    entry.profile?.let { profile ->
                        if (profile.id == player.uuid) {
                            return entry.latency
                        }
                    }

                }
            }
        }
        return -1
    }
}