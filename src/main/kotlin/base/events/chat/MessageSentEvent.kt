package base.events.chat

import base.system.event.*

class MessageSentEvent(var message: String) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    companion object : EventBus()
}