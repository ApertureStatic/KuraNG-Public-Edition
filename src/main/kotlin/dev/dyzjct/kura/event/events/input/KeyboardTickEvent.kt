package dev.dyzjct.kura.event.events.input

import dev.dyzjct.kura.event.eventbus.*

class KeyboardTickEvent(
    var movementForward: Float,
    var movementSideways: Float
) : Event, ICancellable by Cancellable(), IEventPosting by Companion{
    companion object : EventBus()
}