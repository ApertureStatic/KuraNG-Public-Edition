package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.IEventPosting
import dev.dyzjct.kura.event.eventbus.NamedProfilerEventBus

sealed class UpdateWalkingPlayerEvent : Event {
    object Pre : UpdateWalkingPlayerEvent(), IEventPosting by NamedProfilerEventBus("kuraUpdateWalkingPre")
    object Post : UpdateWalkingPlayerEvent(), IEventPosting by NamedProfilerEventBus("kuraUpdateWalkingPost")
}