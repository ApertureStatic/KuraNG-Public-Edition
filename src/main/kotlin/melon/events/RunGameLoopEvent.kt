package melon.events

import melon.system.event.Event
import melon.system.event.IEventPosting
import melon.system.event.NamedProfilerEventBus

sealed class RunGameLoopEvent : Event {
    object Start : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("start")
    object Tick : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("tick")
    object Render : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("render")
    object End : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("end")
}