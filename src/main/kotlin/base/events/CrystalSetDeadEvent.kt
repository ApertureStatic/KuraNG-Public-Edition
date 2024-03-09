package base.events

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting
import net.minecraft.entity.decoration.EndCrystalEntity

class CrystalSetDeadEvent(
    val x: Double,
    val y: Double,
    val z: Double,
    val crystals: List<EndCrystalEntity>
) : Event, IEventPosting by Companion {
    companion object : EventBus()
}