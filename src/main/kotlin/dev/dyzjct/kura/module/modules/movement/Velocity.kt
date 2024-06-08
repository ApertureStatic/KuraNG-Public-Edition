package dev.dyzjct.kura.module.modules.movement

import base.events.PacketEvents
import base.events.TickEvent
import base.system.event.safeEventListener
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.math.Direction

object Velocity : Module(name = "Velocity", langName = "防击退", category = Category.MOVEMENT, type = Type.Both) {
    private val mode by msetting("Mode", Mode.Vanilla)
    var noPush = bsetting("NoPush", true)
    private var horizontal = fsetting("Horizontal", 0f, 0f, 100f)
    private var vertical = fsetting("Vertical", 0f, 0f, 100f)

    private var velocityInput = false

    init {
        safeEventListener<PacketEvents.Receive> { event ->
            when (event.packet) {
                is EntityVelocityUpdateS2CPacket -> {
                    val velocity = event.packet
                    when (mode) {
                        Mode.Vanilla -> {
                            if (velocity.id == player.id) {
                                if (horizontal.value == 0f && vertical.value == 0f) event.cancelled = true
                                velocity.velocityX *= (horizontal.value / 100f).toInt()
                                velocity.velocityY *= (vertical.value / 100f).toInt()
                                velocity.velocityZ *= (horizontal.value / 100f).toInt()
                            }
                        }

                        Mode.GrimAC -> {
                            event.cancelled
                            velocityInput = true
                        }
                    }
                }

                is ExplosionS2CPacket -> {
                    val velocity = event.packet
                    if (mode != Mode.Vanilla) event.cancelled = true
                    velocity.playerVelocityX *= horizontal.value / 100f
                    velocity.playerVelocityY *= vertical.value / 100f
                    velocity.playerVelocityZ *= horizontal.value / 100f
                }
            }
        }

        safeEventListener<TickEvent.Pre> {
            if (mode == Mode.GrimAC) {
                connection.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                        player.blockPos,
                        Direction.DOWN
                    )
                )
                velocityInput = false
            }
        }
    }

    override fun getHudInfo(): String {
        return " H" + horizontal.value + "%" + " |" + "V" + vertical.value + "%"
    }

    enum class Mode {
        Vanilla, GrimAC
    }
}