package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

class PlayerUpdateEvent : Event, IEventPosting by Companion{
    companion object : EventBus()
}
