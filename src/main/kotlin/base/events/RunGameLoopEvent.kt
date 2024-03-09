package base.events

import base.system.event.Event
import base.system.event.IEventPosting
import base.system.event.NamedProfilerEventBus

sealed class RunGameLoopEvent : Event {
    object Start : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("start")
    object Tick : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("tick")
    object Render : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("render")
    object End : RunGameLoopEvent(), IEventPosting by NamedProfilerEventBus("end")
}