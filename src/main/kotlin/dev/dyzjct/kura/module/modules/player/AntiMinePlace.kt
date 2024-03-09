package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.render.BreakESP.calcBreakTime
import dev.dyzjct.kura.utils.extension.synchronized
import melon.events.block.BlockBreakEvent
import melon.system.event.safeEventListener
import net.minecraft.util.math.BlockPos
import java.util.concurrent.ConcurrentHashMap

object AntiMinePlace : Module(
    name = "AntiMinePlace",
    langName = "不在挖掘位置放置",
    description = "Other Module's PlaceBlock not place in player mineBlock. ",
    category = Category.PLAYER
) {
    val mineMap = ConcurrentHashMap<BlockPos, MinePos>().synchronized()

    init {
        safeEventListener<BlockBreakEvent> {
            if (it.breakerID != player.id) mineMap[it.blockPos] =
                MinePos(System.currentTimeMillis(), calcBreakTime(it.breakerID, it.blockPos))
        }
    }

    class MinePos(var start: Long, var mine: Float)
}