package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
import net.minecraft.entity.LivingEntity

sealed class CombatEvent : Event {
    abstract val entity: LivingEntity?

    class UpdateTarget(val prevEntity: LivingEntity?, override val entity: LivingEntity?) : dev.dyzjct.kura.event.events.CombatEvent(),
        IEventPosting by dev.dyzjct.kura.event.events.CombatEvent.UpdateTarget.Companion {
        companion object : EventBus()
    }
}