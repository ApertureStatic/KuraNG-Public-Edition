package dev.dyzjct.kura.event.events.chat

import dev.dyzjct.kura.event.eventbus.*

class CharTypedEvent(var char: Char) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    companion object : EventBus()
}