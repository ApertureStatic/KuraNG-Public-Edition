package dev.dyzjct.kura.module.modules.combat

import base.utils.block.getBlock
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.block.Blocks
import net.minecraft.item.EndCrystalItem
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult

object CrystalBasePlacer : Module(
    name = "CrystalBasePlacer",
    langName = "水晶基座放置",
    description = "Auto Place CrystalBase.",
    category = Category.COMBAT,
    type = Type.SafeOnly
) {
    private val backDelay by isetting("BackDelay", 100, 50, 1000)
    private val backTimer = TimerUtils()
    private var backed = true
    private var oldSlot = 0

    init {
        onMotion {
            if (!backed) {
                if (backTimer.passed(backDelay)) {
                    player.inventory.selectedSlot = oldSlot
                    player.useBook(player.mainHandStack, Hand.MAIN_HAND)
                    backed = true
                }
            } else {
                (mc.crosshairTarget as? BlockHitResult)?.blockPos?.let { blockPos ->
                    if (world.getBlock(blockPos) == Blocks.OBSIDIAN || world.getBlock(blockPos) == Blocks.BEDROCK || world.getBlock(
                            blockPos
                        ) == Blocks.AIR
                    ) return@onMotion
                    if (player.mainHandStack.item !is EndCrystalItem) return@onMotion
                    if (mc.options.useKey.isPressed) {
                        player.hotbarSlots.firstItem(Items.OBSIDIAN)?.let {
                            oldSlot = player.inventory.selectedSlot
                            player.inventory.selectedSlot = it.hotbarSlot
                            player.useBook(player.mainHandStack, Hand.MAIN_HAND)
                            backed = false
                            backTimer.reset()
                        }
                    }
                }
            }
        }
    }
}