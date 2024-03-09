package dev.dyzjct.kura.manager

import dev.dyzjct.kura.module.modules.render.PortalESP
import dev.dyzjct.kura.module.modules.render.Xray
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.extension.sq
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import melon.events.TickEvent
import melon.system.event.AlwaysListening
import melon.system.event.SafeClientEvent
import melon.system.event.safeBackGroundTaskListener
import melon.utils.concurrent.threads.defaultScope
import melon.utils.extension.sendSequencedPacket
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.EndPortalBlock
import net.minecraft.block.NetherPortalBlock
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import team.exception.melon.util.math.distanceSqToCenter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

object BlockFinderManager : AlwaysListening {
    val portalBlockList = CopyOnWriteArrayList<BlockPos>()
    val oreBlockList = CopyOnWriteArrayList<BlockPos>()
    private val clickTimer = TimerUtils()
    private val oreBlocks = mutableListOf<Block>(
        Blocks.COAL_ORE,
        Blocks.DEEPSLATE_COAL_ORE,
        Blocks.IRON_ORE,
        Blocks.DEEPSLATE_IRON_ORE,
        Blocks.GOLD_ORE,
        Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.LAPIS_ORE,
        Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.REDSTONE_ORE,
        Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.DIAMOND_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE,
        Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_GOLD_ORE,
        Blocks.NETHER_QUARTZ_ORE,
        Blocks.ANCIENT_DEBRIS
    )

    fun onInit() {
        safeBackGroundTaskListener<TickEvent.Pre>(true) {
            if (PortalESP.isDisabled && Xray.isDisabled) return@safeBackGroundTaskListener
            val range = max(
                if (PortalESP.isEnabled) PortalESP.distance else 0, if (Xray.isEnabled) Xray.distance else 0
            )
            defaultScope.launch { findBlocks(range, PortalESP.isEnabled, Xray.isEnabled) }
            portalBlockList.removeIf { it == null || player.distanceSqToCenter(it) > range.sq || world.isAir(it) }
            oreBlockList.removeIf { it == null || player.distanceSqToCenter(it) > range.sq || world.isAir(it) }
        }
    }

    private suspend fun SafeClientEvent.findBlocks(distance: Int, portalMode: Boolean, xrayMode: Boolean) =
        coroutineScope {
            launch {
                for (x in player.x.toInt() - distance..player.x.toInt() + distance) {
                    for (z in player.z.toInt() - distance..player.z.toInt() + distance) {
                        for (y in player.y.toInt() - distance..player.y.toInt() + distance) {
                            if (y !in -64..320) continue
                            val pos = BlockPos(x, y, z)
                            if (player.distanceSqToCenter(pos) > distance.sq) continue
                            if (world.isAir(pos)) continue
                            if (xrayMode && Xray.wmBypass.value) {
                                if (clickTimer.tickAndReset(Xray.clickDelay)) {
                                    if (Xray.rotate) RotationManager.addRotations(pos)
                                    sendSequencedPacket(world) { sequence ->
                                        PlayerActionC2SPacket(
                                            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                                            pos,
                                            Direction.UP,
                                            sequence
                                        )
                                    }
                                    sendSequencedPacket(world) { sequence ->
                                        PlayerActionC2SPacket(
                                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                                            pos,
                                            Direction.UP,
                                            sequence
                                        )
                                    }
                                    sendSequencedPacket(world) { sequence ->
                                        PlayerActionC2SPacket(
                                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                                            pos,
                                            Direction.UP,
                                            sequence
                                        )
                                    }
                                    player.swingHand(Hand.MAIN_HAND)
                                }
                            }
                            val blockState = world.getBlockState(pos) ?: continue
                            val block = blockState.block ?: continue
                            if ((portalMode && portalBlockList.contains(pos)) || (xrayMode && oreBlockList.contains(pos))) continue
                            when (block) {
                                is NetherPortalBlock -> if (portalMode) portalBlockList.add(pos)
                                is EndPortalBlock -> if (portalMode) portalBlockList.add(pos)
                                in oreBlocks -> if (xrayMode) oreBlockList.add(pos)
                            }
                        }
                    }
                }
            }
        }
}