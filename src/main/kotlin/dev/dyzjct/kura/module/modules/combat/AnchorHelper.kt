package dev.dyzjct.kura.module.modules.combat

import base.utils.block.BlockUtil.getNeighbor
import base.utils.combat.getTarget
import base.utils.extension.fastPos
import base.utils.extension.sendSequencedPacket
import base.utils.math.distanceSqTo
import base.utils.math.sq
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.item.Items
import net.minecraft.util.math.Direction

object AnchorHelper : Module(
    name = "AnchorHelper",
    langName = "重生锚辅助",
    category = Category.COMBAT,
    type = Type.StrongOnly
) {
    private val delay by isetting("Delay", 50, 0, 500)

    private val timer = TimerUtils()

    init {
        onMotion {
            getTarget(CombatSystem.targetRange)?.let { target ->
                if (AnchorAura.placeInfo == null || AnchorAura.isDisabled) return@onMotion
                if (getNeighbor(AnchorAura.placeInfo!!.blockPos) != null) return@onMotion
                fun findHelperPos(upValue: Int): Direction? {
                    var lastFace: Direction? = null
                    var rangeSq = -1.0
                    for (direction in Direction.entries) {
                        if (direction == Direction.UP || direction == Direction.DOWN) continue
                        val calcPos = target.blockPos.up(upValue).offset(direction)
                        if (getNeighbor(calcPos) == null) continue
                        if (player.distanceSqTo(calcPos) > CombatSystem.placeRange.sq || player.distanceSqTo(calcPos) < rangeSq) continue
                        lastFace = direction
                        rangeSq = player.distanceSqTo(calcPos)
                    }
                    return lastFace
                }
                for (up in 2..3) {
                    findHelperPos(up)?.let { dir ->
                        if (timer.tickAndReset(delay)) {
                            spoofHotbarWithSetting(Items.OBSIDIAN) {
                                sendSequencedPacket(world) {
                                    fastPos(target.blockPos.up(up).offset(dir))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}