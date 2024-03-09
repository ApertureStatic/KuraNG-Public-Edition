package base.events

import base.system.event.Event
import base.system.event.IEventPosting
import base.system.event.NamedProfilerEventBus

sealed class TickEvent : Event {
    object Pre : TickEvent(), IEventPosting by NamedProfilerEventBus("melonTickPre")
    object Post : TickEvent(), IEventPosting by NamedProfilerEventBus("melonTickPost")
}