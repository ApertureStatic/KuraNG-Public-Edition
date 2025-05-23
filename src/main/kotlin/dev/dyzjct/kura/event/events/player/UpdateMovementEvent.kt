package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

sealed class UpdateMovementEvent : Event {
    class Pre : UpdateMovementEvent(), IEventPosting by Companion {
        companion object : EventBus()
    }

    class Post : UpdateMovementEvent(), IEventPosting by Companion {
        companion object : EventBus()
    }
}
