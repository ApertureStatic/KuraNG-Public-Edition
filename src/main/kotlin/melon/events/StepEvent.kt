package melon.events

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting

object StepEvent : Event, IEventPosting by EventBus()