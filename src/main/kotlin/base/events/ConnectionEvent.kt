package base.events

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo

sealed class ConnectionEvent : Event {
    class Join(val serverInfo: ServerInfo, val serverAddress: ServerAddress) : ConnectionEvent(),
        IEventPosting by Companion {
        companion object : EventBus()
    }

    internal object Disconnect : ConnectionEvent(), IEventPosting by EventBus()
}