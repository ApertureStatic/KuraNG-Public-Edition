package base.events

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting
import net.minecraft.entity.Entity

class EntityAddToWorldEvent(val entity: Entity) : Event, IEventPosting by Companion {
    companion object : EventBus()
}