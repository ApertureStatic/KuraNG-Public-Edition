package dev.dyzjct.kura.event.events.input

import dev.dyzjct.kura.event.eventbus.*

class KeyboardInputEvent : Event, IEventPosting by Companion, ICancellable by Cancellable() {
    companion object : EventBus()
}