package melon.events

import dev.dyzjct.kura.module.modules.crystal.CrystalDamage
import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting

class CrystalSpawnEvent(
    val entityID: Int,
    val crystalDamage: CrystalDamage
) : Event, IEventPosting by Companion {
    companion object : EventBus()
}