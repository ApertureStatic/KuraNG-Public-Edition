package dev.dyzjct.kura.event.events.client

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

sealed class VerificationEvent : Event {
    companion object {
        var posted = 0
    }

    class DrawTessellator : VerificationEvent(), IEventPosting by Companion {
        companion object : EventBus()

        override fun post() {
            if (posted < 3) posted += 1
        }
    }

    class DrawBuffer : VerificationEvent(), IEventPosting by Companion {
        companion object : EventBus()

        override fun post() {
            if (posted < 3) posted += 1
        }
    }
}