package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.IEventPosting
import dev.dyzjct.kura.event.eventbus.NamedProfilerEventBus

class PlayerUpdateEvent : Event, IEventPosting by NamedProfilerEventBus("kuraUpdate")
