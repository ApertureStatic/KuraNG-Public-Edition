package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.events.PacketEvents
import melon.system.event.safeEventListener
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

object Velocity : Module(name = "Velocity", langName = "防击退", category = Category.MOVEMENT) {
    var noPush = bsetting("NoPush", true)
    private var horizontal = fsetting("Horizontal", 0f, 0f, 100f)
    private var vertical = fsetting("Vertical", 0f, 0f, 100f)

    init {
        safeEventListener<PacketEvents.Receive> { event ->
            when (event.packet) {
                is EntityVelocityUpdateS2CPacket -> {
                    val velocity = event.packet
                    if (velocity.id == player.id) {
                        if (horizontal.value == 0f && vertical.value == 0f) event.cancelled = true
                        velocity.velocityX *= (horizontal.value / 100f).toInt()
                        velocity.velocityY *= (vertical.value / 100f).toInt()
                        velocity.velocityZ *= (horizontal.value / 100f).toInt()
                    }
                }

                is ExplosionS2CPacket -> {
                    val velocity = event.packet
                    velocity.playerVelocityX *= horizontal.value / 100f
                    velocity.playerVelocityY *= vertical.value / 100f
                    velocity.playerVelocityZ *= horizontal.value / 100f
                }
            }
        }
    }

    override fun getHudInfo(): String {
        return " H" + horizontal.value + "%" + " |" + "V" + vertical.value + "%"
    }
}