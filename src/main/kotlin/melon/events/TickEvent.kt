package melon.events

import melon.system.event.Event
import melon.system.event.IEventPosting
import melon.system.event.NamedProfilerEventBus

sealed class TickEvent : Event {
    object Pre : TickEvent(), IEventPosting by NamedProfilerEventBus("melonTickPre")
    object Post : TickEvent(), IEventPosting by NamedProfilerEventBus("melonTickPost")
}