package base.events.input

import base.system.event.*

class BindEvent(val key: Int, val scancode: Int, val i: Int) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    val pressed = i != 0
    companion object : EventBus()
}