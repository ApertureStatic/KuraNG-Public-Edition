package dev.dyzjct.kura.event.events.screen

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

class ResolutionUpdateEvent(val width: Int, val height: Int) : Event, IEventPosting by Companion {
    companion object : EventBus()
}
