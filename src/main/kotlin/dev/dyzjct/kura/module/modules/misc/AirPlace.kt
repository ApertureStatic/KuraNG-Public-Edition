package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.utils.extension.fastPos
import dev.dyzjct.kura.utils.extension.sendSequencedPacket
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.item.BlockItem
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos

object AirPlace : Module(
    name = "AirPlace",
    langName = "空放",
    description = "Place Block on the AirBlock.",
    category = Category.MISC
) {
    private var placeDelay = TimerUtils()

    init {
        onMotion {
            var ray: BlockPos? = null
            if (mc.crosshairTarget is BlockHitResult) {
                ray = (mc.crosshairTarget as BlockHitResult).blockPos
            }
            ray?.let { r ->
                if (placeDelay.tickAndReset(50L)) {
                    if (mc.options.useKey.isPressed) {
                        if (player.mainHandStack.item !is BlockItem) return@onMotion
                        sendSequencedPacket(world) {
                            fastPos(r, sequence = it)
                        }
                    }
                }
            }
        }
    }
}