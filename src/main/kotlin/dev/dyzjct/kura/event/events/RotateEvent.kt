package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

class RotateEvent(
    var yawVal: Float,
    var pitchVal: Float,
    var modified: Boolean = false
) : Event, IEventPosting by Companion {
    companion object : EventBus()
}