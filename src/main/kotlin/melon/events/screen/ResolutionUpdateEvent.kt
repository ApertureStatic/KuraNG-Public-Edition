package melon.events.screen

import melon.system.event.*

class ResolutionUpdateEvent(val width: Int, val height: Int) : Event, IEventPosting by Companion {
    companion object : EventBus()
}
