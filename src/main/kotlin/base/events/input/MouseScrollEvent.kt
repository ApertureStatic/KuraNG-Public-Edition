package base.events.input

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting

class MouseScrollEvent(var amount: Double) : Event, IEventPosting by Companion {
    companion object : EventBus()
}