package base.events.client

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting

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