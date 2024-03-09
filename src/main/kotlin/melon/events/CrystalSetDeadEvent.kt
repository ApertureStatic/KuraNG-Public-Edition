package melon.events

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import net.minecraft.entity.decoration.EndCrystalEntity

class CrystalSetDeadEvent(
    val x: Double,
    val y: Double,
    val z: Double,
    val crystals: List<EndCrystalEntity>
) : Event, IEventPosting by Companion {
    companion object : EventBus()
}