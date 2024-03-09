package melon.events.input

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting

class MouseScrollEvent(var amount: Double) : Event, IEventPosting by Companion {
    companion object : EventBus()
}