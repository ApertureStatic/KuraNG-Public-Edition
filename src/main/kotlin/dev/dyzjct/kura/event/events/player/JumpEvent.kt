package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

sealed class JumpEvent : Event {
    class Post : JumpEvent(), IEventPosting by Companion {
        companion object : EventBus()
    }

    class Pre : JumpEvent(), IEventPosting by Companion {
        companion object : EventBus()
    }
}