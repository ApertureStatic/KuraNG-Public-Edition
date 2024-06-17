package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
import net.minecraft.entity.Entity

class EntityAddToWorldEvent(val entity: Entity) : Event, IEventPosting by Companion {
    companion object : EventBus()
}