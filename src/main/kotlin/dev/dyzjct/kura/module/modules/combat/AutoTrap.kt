package dev.dyzjct.kura.module.modules.combat

import base.utils.combat.getTarget
import base.utils.entity.EntityUtils.spoofSneak
import base.utils.extension.fastPos
import base.utils.extension.sendSequencedPacket
import base.utils.inventory.slot.firstBlock
import base.utils.inventory.slot.hotbarSlots
import base.utils.math.distanceSqToCenter
import base.utils.world.isPlaceable
import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.sq
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

@Suppress("unused")
object AutoTrap : Module(name = "AutoTrap", "自动陷阱", category = Category.COMBAT) {
    private var placeMode = msetting("Mode", Mode.Normal)
    private var placeRange by isetting("PlaceRange", 4, 0, 6)
    private var placeDelay by isetting("PlaceDelay", 10, 0, 1000)
    private var rotate by bsetting("Rotate", false)
    private var placeTimer = TimerUtils()

    override fun onEnable() {
        placeTimer.reset()
    }

    init {
        onLoop {
            val target = getTarget(CombatSystem.targetRange) ?: return@onLoop
            player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)?.let { hotbarSlot ->
                outerLoop@ for (pos in (placeMode.value as Mode).posArray) {
                    val placePos = target.blockPos.add(pos)
                    if (!world.isPlaceable(placePos)) continue
                    if (player.distanceSqToCenter(placePos) > CombatSystem.placeRange.sq) continue
                    for (e in EntityManager.entity) {
                        if (e.boundingBox.intersects(Box(placePos))) continue@outerLoop
                    }
                    if (placeTimer.tickAndReset(placeDelay)) {
                        player.spoofSneak {
                            if (rotate) RotationManager.rotationTo(placePos)
                            spoofHotbar(hotbarSlot) {
                                sendSequencedPacket(world) {
                                    fastPos(placePos, sequence = it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    enum class Mode(val posArray: Array<BlockPos>) {
        Extra(
            arrayOf(
                BlockPos(0, 0, -1),
                BlockPos(1, 0, 0),
                BlockPos(0, 0, 1),
                BlockPos(-1, 0, 0),
                BlockPos(0, 1, -1),
                BlockPos(1, 1, 0),
                BlockPos(0, 1, 1),
                BlockPos(-1, 1, 0),
                BlockPos(0, 2, -1),
                BlockPos(1, 2, 0),
                BlockPos(0, 2, 1),
                BlockPos(-1, 2, 0),
                BlockPos(0, 3, -1),
                BlockPos(0, 3, 0),
                BlockPos(0, 4, 0)
            )
        ),
        Normal(
            arrayOf(
                BlockPos(0, 0, -1),
                BlockPos(1, 0, 0),
                BlockPos(0, 0, 1),
                BlockPos(-1, 0, 0),
                BlockPos(0, 1, -1),
                BlockPos(1, 1, 0),
                BlockPos(0, 1, 1),
                BlockPos(-1, 1, 0),
                BlockPos(0, 2, -1),
                BlockPos(1, 2, 0),
                BlockPos(0, 2, 1),
                BlockPos(-1, 2, 0),
                BlockPos(0, 3, -1),
                BlockPos(0, 3, 0)
            )
        ),
        Feet(
            arrayOf(
                BlockPos(0, 0, -1),
                BlockPos(0, 1, -1),
                BlockPos(0, 2, -1),
                BlockPos(1, 2, 0),
                BlockPos(0, 2, 1),
                BlockPos(-1, 2, 0),
                BlockPos(0, 3, -1),
                BlockPos(0, 3, 1),
                BlockPos(1, 3, 0),
                BlockPos(-1, 3, 0),
                BlockPos(0, 3, 0),
                BlockPos(0, 4, 0)
            )
        )
    }
}