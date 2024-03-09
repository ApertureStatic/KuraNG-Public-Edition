package melon.events

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo

sealed class ConnectionEvent : Event {
    class Join(val serverInfo: ServerInfo, val serverAddress: ServerAddress) : ConnectionEvent(),
        IEventPosting by Companion {
        companion object : EventBus()
    }

    internal object Disconnect : ConnectionEvent(), IEventPosting by EventBus()
}