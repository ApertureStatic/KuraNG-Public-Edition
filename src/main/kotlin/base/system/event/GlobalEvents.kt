package base.system.event

import dev.dyzjct.kura.Kura.Companion.verifiedState
import base.events.ConnectionEvent
import base.events.RunGameLoopEvent
import base.events.WorldEvent
import base.events.client.VerificationEvent
import base.system.util.interfaces.MinecraftWrapper
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld

abstract class AbstractClientEvent {
    val mc = MinecraftWrapper.mc
    abstract val world: ClientWorld?
    abstract val player: ClientPlayerEntity?
    abstract val playerController: ClientPlayerInteractionManager?
    abstract val connection: ClientPlayNetworkHandler?
}

open class ClientEvent : AbstractClientEvent() {
    final override val world = mc.world
    final override val player = mc.player
    final override val playerController = mc.interactionManager
    final override val connection = mc.networkHandler

    inline operator fun <T> invoke(block: ClientEvent.() -> T) = run(block)
}

open class SafeClientEvent internal constructor(
    override val world: ClientWorld,
    override val player: ClientPlayerEntity,
    override val playerController: ClientPlayerInteractionManager,
    override val connection: ClientPlayNetworkHandler
) : AbstractClientEvent() {
    inline operator fun <T> invoke(block: SafeClientEvent.() -> T) = run(block)

    companion object : ListenerOwner(), MinecraftWrapper {
        var instance: SafeClientEvent? = null; private set

        init {
            listener<ConnectionEvent.Disconnect>(Int.MAX_VALUE, true) {
                reset()
            }

            listener<WorldEvent.Unload>(Int.MAX_VALUE, true) {
                reset()
            }

            listener<RunGameLoopEvent.Tick>(Int.MAX_VALUE, true) {
                update()
            }
        }

        fun update() {
            if (verifiedState <= 0 || VerificationEvent.posted < 2) {
                reset()
                return
            }
            val world = mc.world ?: return
            val player = mc.player ?: return
            val playerController = mc.interactionManager ?: return
            val connection = mc.networkHandler ?: return

            instance = SafeClientEvent(world, player, playerController, connection)
        }

        fun reset() {
            instance = null
        }
    }
}