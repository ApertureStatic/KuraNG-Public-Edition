package base.events

import dev.dyzjct.kura.module.modules.crystal.CrystalDamage
import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting

class CrystalSpawnEvent(
    val entityID: Int,
    val crystalDamage: CrystalDamage
) : Event, IEventPosting by Companion {
    companion object : EventBus()
}