package dev.dyzjct.kura.module.modules.movement

import base.utils.entity.EntityUtils.isInBurrow
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

object Velocity : Module(name = "Velocity", langName = "防击退", category = Category.MOVEMENT) {
    private val mode = msetting("Mode", Mode.Vanilla)
    var noPush = bsetting("NoPush", true)
    private var horizontal = fsetting("Horizontal", 0f, 0f, 100f).enumIs(mode, Mode.Vanilla)
    private var vertical = fsetting("Vertical", 0f, 0f, 100f).enumIs(mode, Mode.Vanilla)

    init {
        safeEventListener<PacketEvents.Receive> { event ->
            var h = horizontal.value
            var v = vertical.value
            if (mode.value == Mode.Wall) {
                h = 0f
                v = 0f
            }
            when (event.packet) {
                is EntityVelocityUpdateS2CPacket -> {
                    val velocity = event.packet
                    if (mode.value == Mode.Vanilla || isInBurrow(player)) {
                        if (velocity.id == player.id) {
                            if (h == 0f && v == 0f) event.cancelled = true
                            velocity.velocityX *= (h / 100f).toInt()
                            velocity.velocityY *= (v / 100f).toInt()
                            velocity.velocityZ *= (h / 100f).toInt()
                        }
                    }
                }

                is ExplosionS2CPacket -> {
                    val velocity = event.packet
                    if (mode.value == Mode.Vanilla || isInBurrow(player)) {
                        velocity.playerVelocityX *= h / 100f
                        velocity.playerVelocityY *= v / 100f
                        velocity.playerVelocityZ *= h / 100f
                    }
                }
            }
        }
    }

    override fun getHudInfo(): String {
        return mode.value.name
    }

    enum class Mode {
        Vanilla, Wall
    }
}