package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.module.modules.crystal.CrystalDamage
import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

class CrystalSpawnEvent(
    val entityID: Int,
    val crystalDamage: CrystalDamage
) : Event, IEventPosting by Companion {
    companion object : EventBus()
}