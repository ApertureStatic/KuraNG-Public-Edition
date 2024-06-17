package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.IEventPosting
import dev.dyzjct.kura.event.eventbus.NamedProfilerEventBus

sealed class RunGameLoopEvent : Event {
    object Start : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("start")
    object Tick : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("tick")
    object Render : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("render")
    object End : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("end")
}