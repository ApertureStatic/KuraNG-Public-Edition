package dev.dyzjct.kura.manager

import dev.dyzjct.kura.module.modules.combat.AnchorAura
import dev.dyzjct.kura.module.modules.combat.NewBedAura
import dev.dyzjct.kura.module.modules.crystal.KuraAura
import dev.dyzjct.kura.module.modules.misc.AutoCraftBed
import base.events.RunGameLoopEvent
import base.system.event.AlwaysListening
import base.system.event.safeBackGroundTaskListener
import base.utils.combat.CrystalUtils
import net.minecraft.util.math.BlockPos
import base.utils.math.toVec3dCenter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

object SphereCalculatorManager : AlwaysListening {
    var sphereList = CopyOnWriteArrayList<BlockPos>()

    fun onInit() {
        safeBackGroundTaskListener<RunGameLoopEvent.Render>(true) {
            val firstRange = max(
                if (AutoCraftBed.isEnabled) AutoCraftBed.placeRange.value else 0.0,
                if (NewBedAura.isEnabled) NewBedAura.range.value.toDouble() else 0.0
            )
            val secondRange = max(
                if (AnchorAura.isEnabled) AnchorAura.placeRange.value else 0.0,
                if (KuraAura.isEnabled) KuraAura.placeRange.value else 0.0
            )
            val maxRange = max(firstRange, secondRange)
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