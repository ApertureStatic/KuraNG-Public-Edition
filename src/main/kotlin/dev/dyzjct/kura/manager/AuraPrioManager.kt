package dev.dyzjct.kura.manager

import base.events.TickEvent
import base.system.event.AlwaysListening
import base.system.event.safeEventListener
import dev.dyzjct.kura.module.modules.client.CombatSystem.Auras
import dev.dyzjct.kura.module.modules.client.CombatSystem.BestAura
import dev.dyzjct.kura.module.modules.client.CombatSystem.bestAura
import dev.dyzjct.kura.module.modules.client.CombatSystem.smartAura
import dev.dyzjct.kura.module.modules.combat.AnchorAura
import dev.dyzjct.kura.module.modules.combat.KillAura
import dev.dyzjct.kura.module.modules.crystal.AutoCrystal
import java.util.concurrent.CopyOnWriteArrayList

object AuraPrioManager : AlwaysListening {
    fun onInit() {
        safeEventListener<TickEvent.Pre> {
            if (smartAura) {
                val auraList = CopyOnWriteArrayList<Auras>()
                if (KillAura.isEnabled) auraList.add(Auras(BestAura.Sword, KillAura.kadamage))
                if (AutoCrystal.isEnabled) auraList.add(Auras(BestAura.Crystal, AutoCrystal.cadamage))
                if (AnchorAura.isEnabled) auraList.add(Auras(BestAura.Anchor, AnchorAura.anchorDamage))
                bestAura = auraList.maxByOrNull { it.damage }!!.aura
            }
        }
    }
}