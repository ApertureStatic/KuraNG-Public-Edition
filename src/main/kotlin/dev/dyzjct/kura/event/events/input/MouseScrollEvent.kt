package dev.dyzjct.kura.event.events.input

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

class MouseScrollEvent(var amount: Double) : Event, IEventPosting by Companion {
    companion object : EventBus()
}