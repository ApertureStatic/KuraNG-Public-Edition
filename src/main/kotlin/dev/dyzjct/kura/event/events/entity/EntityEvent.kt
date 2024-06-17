package dev.dyzjct.kura.event.events.entity

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity

sealed class EntityEvent(var entity: Entity) : Event {
    class Death(entity: LivingEntity) : EntityEvent(entity), IEventPosting by Companion {
        companion object : EventBus()
    }
}