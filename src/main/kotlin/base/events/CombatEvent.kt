package base.events

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting
import net.minecraft.entity.LivingEntity

sealed class CombatEvent : Event {
    abstract val entity: LivingEntity?

    class UpdateTarget(val prevEntity: LivingEntity?, override val entity: LivingEntity?) : CombatEvent(),
        IEventPosting by Companion {
        companion object : EventBus()
    }
}