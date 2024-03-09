package melon.events

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import net.minecraft.entity.LivingEntity

sealed class CombatEvent : Event {
    abstract val entity: LivingEntity?

    class UpdateTarget(val prevEntity: LivingEntity?, override val entity: LivingEntity?) : CombatEvent(),
        IEventPosting by Companion {
        companion object : EventBus()
    }
}