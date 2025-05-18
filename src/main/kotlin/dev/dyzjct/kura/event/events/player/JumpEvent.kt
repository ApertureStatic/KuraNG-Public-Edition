package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.*

class JumpEvent : Event, ICancellable by Cancellable(), IEventPosting by EventBus()