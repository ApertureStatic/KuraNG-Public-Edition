package melon.events.player

import melon.system.event.*

object JumpEvent : Event, ICancellable by Cancellable(), IEventPosting by EventBus()