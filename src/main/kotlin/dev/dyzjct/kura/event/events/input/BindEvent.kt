package dev.dyzjct.kura.event.events.input

import dev.dyzjct.kura.event.eventbus.*

class BindEvent(val key: Int, val scancode: Int, val i: Int) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    val pressed = i != 0
    companion object : EventBus()
}