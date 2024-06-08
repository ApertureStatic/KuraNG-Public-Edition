package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.combat.HolePush.doHolePush
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.module.modules.player.PacketMine.hookPos
import base.events.TickEvent
import base.events.player.PlayerMotionEvent
import base.system.event.safeConcurrentListener
import base.system.event.safeEventListener
import base.utils.TickTimer
import base.utils.block.BlockUtil.canBreak
import base.utils.combat.getTarget
import base.utils.world.getMiningSide
import net.minecraft.block.Blocks
import net.minecraft.block.RedstoneBlock
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CopyOnWriteArrayList

object CityMiner : Module(
    name = "CityMiner",
    "自动挖脚",
    category = Category.COMBAT,
    description = "Auto Mine target's feet for pvp"
) {
    private val range = isetting("Range", 6, 1, 6)
    private var eatingPause by bsetting("EatingPause", false)
    private var raytrace by bsetting("RayTrace", false)
    private var positionTaskMap = CopyOnWriteArrayList<BlockPos>()
    private var singlePos: BlockPos? = null
    private val retryTimer = TickTimer()
    private var inTask = false

    init {
        safeConcurrentListener<TickEvent.Pre> {
            positionTaskMap.forEach { pos ->
                if (world.isAir(pos) || pos == null) {
                    positionTaskMap.remove(pos)
                    retryTimer.reset()
                } else if (!world.isAir(pos) && retryTimer.tickAndReset(650) && PacketMine.blockData?.blockPos != pos && PacketMine.doubleData?.blockPos != pos) {
                    hookPos(pos)
                }
            }
            inTask = if (PacketMine.doubleBreak) {
                positionTaskMap.size >= 2 && !positionTaskMap.any { world.isAir(it) }
            } else {
                singlePos != null && (!world.isAir(singlePos) || PacketMine.blockData?.blockPos == singlePos)
            }
        }

        safeEventListener<PlayerMotionEvent> {
            if (eatingPause && player.isUsingItem) return@safeEventListener
            getTarget(range.value.toDouble())?.let { target ->
                val targetPos = target.blockPos ?: return@safeEventListener
                if (HolePush.isEnabled) {
                    PacketMine.blockData?.let { data ->
                        doHolePush(targetPos.up(1), true, null, null)?.let { stonePos ->
                            if (stonePos == data.blockPos) return@safeEventListener
                        }
                        if (world.getBlockState(data.blockPos).block is RedstoneBlock) return@safeEventListener
                    }
                }
                for (offset in SurroundOffset.entries) {
                    if (inTask) break
                    val offsetPos = targetPos.add(offset.offset)
                    if (!canBreak(offsetPos, false)) continue
                    if (world.isAir(offsetPos)) continue
                    if (positionTaskMap.contains(offsetPos)) continue
                    if (raytrace && getMiningSide(offsetPos) == null) continue
                    if (world.getBlockState(offsetPos).block == Blocks.COBWEB) continue
                    positionTaskMap.add(offsetPos)
                    hookPos(offsetPos)
                    singlePos = offsetPos
                    retryTimer.reset()
                    break
                }
            } ?: run {
                resetTask()
                return@safeEventListener
            }
        }
    }

    override fun onEnable() {
        resetTask()
    }

    private fun resetTask() {
        positionTaskMap.clear()
        retryTimer.reset()
        singlePos = null
        inTask = false
    }

    private enum class SurroundOffset(val offset: BlockPos) {
        CENTER(BlockPos.ORIGIN), NORTH(BlockPos(0, 0, -1)), EAST(BlockPos(1, 0, 0)), SOUTH(BlockPos(0, 0, 1)), WEST(
            BlockPos(-1, 0, 0)
        )
    }
}