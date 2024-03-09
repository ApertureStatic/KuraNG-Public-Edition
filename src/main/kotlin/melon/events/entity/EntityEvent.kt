package melon.events.entity

import melon.system.event.*
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity

sealed class EntityEvent(var entity: Entity) : Event {
    class Death(entity: LivingEntity) : EntityEvent(entity), IEventPosting by Companion {
        companion object : EventBus()
    }
}