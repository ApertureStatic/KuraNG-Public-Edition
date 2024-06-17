package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
import net.minecraft.entity.decoration.EndCrystalEntity

class CrystalSetDeadEvent(
    val x: Double,
    val y: Double,
    val z: Double,
    val crystals: List<EndCrystalEntity>
) : Event, IEventPosting by Companion {
    companion object : EventBus()
}