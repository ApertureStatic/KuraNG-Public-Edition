package melon.events

import melon.system.event.*
import net.minecraft.network.packet.Packet

sealed class PacketEvents(val packet: Packet<*>) : Event {
    class Receive(packet: Packet<*>) : PacketEvents(packet), ICancellable by Cancellable(), IEventPosting by Companion {
        companion object : EventBus()
    }

    class PostReceive(packet: Packet<*>) : PacketEvents(packet), IEventPosting by Companion {
        companion object : EventBus()
    }

    class Send(packet: Packet<*>) : PacketEvents(packet), ICancellable by Cancellable(), IEventPosting by Companion {
        companion object : EventBus()
    }

    class PostSend(packet: Packet<*>) : PacketEvents(packet), IEventPosting by Companion {
        companion object : EventBus()
    }
}