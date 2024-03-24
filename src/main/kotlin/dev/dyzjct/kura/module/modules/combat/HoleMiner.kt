package dev.dyzjct.kura.module.modules.combat

import base.system.event.SafeClientEvent
import base.utils.block.BlockUtil.canBreak
import base.utils.combat.getTarget
import base.utils.world.getMiningSide
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.combat.HolePush.doHolePush
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.module.modules.player.PacketMine.hookPos
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.block.CobwebBlock
import net.minecraft.block.RedstoneBlock
import net.minecraft.util.math.BlockPos

object HoleMiner : Module(
    name = "HoleMiner",
    langName = "自动挖角",
    description = "auto mine target feet.",
    category = Category.COMBAT
) {
    private var range by isetting("Range", 6, 1, 6)
    private var eatingPause by bsetting("EatingPause", true)
    private var raytrace by bsetting("RayTrace", true)
    private var ground by bsetting("OnlyGround", true)
    private var instant by bsetting("Instant", false)
    private var delay by isetting("Delay", 50, 0, 300)

    private var timer = TimerUtils()

    init {
        onMotion {
            if (ground && !player.onGround) return@onMotion
            getTarget(range.toDouble())?.let { target ->
                val targetPos = target.blockPos
                if (HolePush.isEnabled) {
                    PacketMine.blockData?.let { data ->
                        doHolePush(targetPos.up(1), true, null, null)?.let { stonePos ->
                            if (stonePos == data.blockPos) return@onMotion
                        }
                        if (world.getBlockState(data.blockPos).block is RedstoneBlock) return@onMotion
                    }
                }
                if (eatingPause && player.isUsingItem) return@onMotion
                for (offset in CityOffset.entries) {
                    PacketMine.blockData?.let { data ->
                        PacketMine.doubleData?.let { dbData ->
                            for (offsetCheck in CityOffset.entries) {
                                if (data.blockPos == targetPos.add(offsetCheck.offset) && dbData.blockPos == targetPos.add(
                                        offsetCheck.offset
                                    )
                                ) {
                                    return@onMotion
                                }
                            }
                        }
                    }
                    val pos = targetPos.add(offset.offset)
                    if (!canBreak(pos, true)) continue
                    if (world.getBlockState(pos).block is CobwebBlock) continue
                    if (world.isAir(pos)) {
                        if (offset != CityOffset.CENTER && instant) {
                            break
                        }
                        continue
                    }
                    if (raytrace && getMiningSide(pos) == null) continue
                    if (!pass()) return@onMotion
                    if (PacketMine.blockData == null) {
                        if (timer.tickAndReset(delay)) hookPos(pos)
                    }
                    PacketMine.blockData?.let { data ->
                        if (data.blockPos != pos) {
                            if (timer.tickAndReset(delay)) hookPos(pos)
                        }
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.pass(): Boolean {
        return (PacketMine.blockData == null || PacketMine.blockData?.let { world.isAir(it.blockPos) } ?: false) || if (PacketMine.doubleBreak) PacketMine.doubleData == null else false
    }

    private enum class CityOffset(val offset: BlockPos) {
        CENTER(BlockPos.ORIGIN),
        NORTH(BlockPos(0, 0, -1)),
        EAST(BlockPos(1, 0, 0)),
        SOUTH(BlockPos(0, 0, 1)),
        WEST(BlockPos(-1, 0, 0))
    }
}