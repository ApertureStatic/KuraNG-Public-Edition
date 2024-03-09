package melon.events

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import net.minecraft.entity.Entity

class EntityAddToWorldEvent(val entity: Entity) : Event, IEventPosting by Companion {
    companion object : EventBus()
}