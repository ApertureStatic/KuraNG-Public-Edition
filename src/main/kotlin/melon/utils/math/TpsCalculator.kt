package melon.utils.math

import melon.events.ConnectionEvent
import melon.events.PacketEvents
import melon.system.event.AlwaysListening
import melon.system.event.listener
import melon.system.util.collections.CircularArray
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket

object TpsCalculator : AlwaysListening {
    // Circular Buffer lasting ~60 seconds for tick storage
    private val tickRates = CircularArray(120, 20.0f)

    private var timeLastTimeUpdate = -1L

    val tickRate: Float
        get() = tickRates.average().toFloat()

    val adjustTicks: Float
        get() = tickRates.average().toFloat() - 20.0f

    val multiplier: Float
        get() = 20.0f / tickRate

    init {
        listener<PacketEvents.Receive> {
            if (it.packet !is WorldTimeUpdateS2CPacket) return@listener

            if (timeLastTimeUpdate != -1L) {
                val timeElapsed = (System.nanoTime() - timeLastTimeUpdate) / 1E9
                tickRates.add((20.0 / timeElapsed).coerceIn(0.0, 20.0).toFloat())
            }

            timeLastTimeUpdate = System.nanoTime()
        }

        listener<ConnectionEvent.Join> {
            reset()
        }
    }

    private fun reset() {
        tickRates.clear()
        timeLastTimeUpdate = -1L
    }
}