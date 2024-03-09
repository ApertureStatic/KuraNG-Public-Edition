package base.events.player

import base.system.event.*

object JumpEvent : Event, ICancellable by Cancellable(), IEventPosting by EventBus()