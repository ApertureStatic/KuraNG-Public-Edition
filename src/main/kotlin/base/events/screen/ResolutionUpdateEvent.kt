package base.events.screen

import base.system.event.*

class ResolutionUpdateEvent(val width: Int, val height: Int) : Event, IEventPosting by Companion {
    companion object : EventBus()
}
