package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.IEventPosting
import dev.dyzjct.kura.event.eventbus.NamedProfilerEventBus

sealed class UpdateMovementEvent : Event {
    object Pre : UpdateMovementEvent(), IEventPosting by NamedProfilerEventBus("kuraMovementPre")
    object Post : UpdateMovementEvent(), IEventPosting by NamedProfilerEventBus("kuraMovementPost")
}
