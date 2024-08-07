package dev.dyzjct.kura.manager

import dev.dyzjct.kura.event.events.RunGameLoopEvent
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.safeBackGroundTaskListener
import base.utils.combat.CrystalUtils
import base.utils.math.toVec3dCenter
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.combat.BedAura
import dev.dyzjct.kura.module.modules.misc.AutoCraftBed
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

object SphereCalculatorManager : AlwaysListening {
    var sphereList = CopyOnWriteArrayList<BlockPos>()

    fun onInit() {
        safeBackGroundTaskListener<RunGameLoopEvent.Render>(true) {
            val firstRange = max(
                if (AutoCraftBed.isEnabled) AutoCraftBed.placeRange.value else 0.0,
                if (BedAura.isEnabled) BedAura.range.value.toDouble() else 0.0
            )

            val maxRange = max(firstRange, CombatSystem.placeRange)
            sphereList = CopyOnWriteArrayList(
                CrystalUtils.getSphereVec(
                    DisablerManager.timerFlagData?.playerPos?.toVec3dCenter() ?: CrystalManager.position,
                    maxRange,
                    maxRange,
                    hollow = false,
                    sphere = true,
                    yOffset = 0
                )
            )
        }
    }
}