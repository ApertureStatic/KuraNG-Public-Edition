package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo

sealed class ConnectionEvent : Event {
    class Join(val serverInfo: ServerInfo, val serverAddress: ServerAddress) : ConnectionEvent(),
        IEventPosting by Companion {
        companion object : EventBus()
    }

    internal object Disconnect : ConnectionEvent(), IEventPosting by EventBus()
}