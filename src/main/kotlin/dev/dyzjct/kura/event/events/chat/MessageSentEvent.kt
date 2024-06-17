package dev.dyzjct.kura.event.events.chat

import dev.dyzjct.kura.event.eventbus.*

class MessageSentEvent(var message: String) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    companion object : EventBus()
}