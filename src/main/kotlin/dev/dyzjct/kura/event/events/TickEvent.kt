package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.IEventPosting
import dev.dyzjct.kura.event.eventbus.NamedProfilerEventBus

sealed class TickEvent : Event {
    object Pre : TickEvent(), IEventPosting by NamedProfilerEventBus("kuraTickPre")
    object Post : TickEvent(), IEventPosting by NamedProfilerEventBus("kuraTickPost")
}