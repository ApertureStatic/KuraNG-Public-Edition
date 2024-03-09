package base.events.chat

import base.system.event.*

class CharTypedEvent(var char: Char) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    companion object : EventBus()
}