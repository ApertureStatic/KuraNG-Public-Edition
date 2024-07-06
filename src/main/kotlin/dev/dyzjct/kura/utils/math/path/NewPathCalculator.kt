package dev.dyzjct.kura.utils.math.path

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import net.minecraft.util.math.BlockPos

object NewPathCalculator {
    fun SafeClientEvent.findPath(startPos: BlockPos, endPos: BlockPos, range: Int) {
        for (x in startPos.x - range..startPos.x + range) {
            for (y in startPos.y - range..startPos.y + range) {
                for (z in startPos.z - range..startPos.z + range) {
                }
            }
        }
    }
}