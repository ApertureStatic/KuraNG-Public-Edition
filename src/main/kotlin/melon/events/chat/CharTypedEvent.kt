package melon.events.chat

import melon.system.event.*

class CharTypedEvent(var char: Char) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    companion object : EventBus()
}