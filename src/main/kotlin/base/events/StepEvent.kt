package base.events

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting

object StepEvent : Event, IEventPosting by EventBus()