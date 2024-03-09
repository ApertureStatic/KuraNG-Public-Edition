package melon.events.client

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting

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