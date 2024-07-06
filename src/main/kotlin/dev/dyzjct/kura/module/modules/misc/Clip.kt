package dev.dyzjct.kura.module.modules.misc

import base.utils.concurrent.threads.runSafe
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import kotlin.math.floor

object Clip :
    Module("Clip", langName = "卡墙", description = "Clip in song's server.", category = Category.MISC) {
    private val delay by isetting("Delay", 100, 0, 500)
    private val clipIn by bsetting("ClipIn", true)
    private val timer = TimerUtils()
    private var cancelPacket = true

    override fun onEnable() {
        runSafe {
            cancelPacket = false
            if (clipIn) {
                val f = player.horizontalFacing
                player.setPosition(
                    player.x + f.offsetX * 0.5,
                    player.y,
                    player.z + f.offsetZ * 0.5
                )
                player.networkHandler.sendPacket(
                    PositionAndOnGround(
                        player.x,
                        player.y,
                        player.z,
                        true
                    )
                )
            } else {
                player.networkHandler.sendPacket(
                    PositionAndOnGround(
                        player.x,
                        player.y,
                        player.z,
                        true
                    )
                )
                player.setPosition(
                    roundToClosest(
                        player.x,
                        floor(player.x) + 0.23,
                        floor(player.x) + 0.77
                    ),
                    player.y,
                    roundToClosest(player.z, floor(player.z) + 0.23, floor(player.z) + 0.77)
                )
                player.networkHandler.sendPacket(
                    PositionAndOnGround(
                        roundToClosest(
                            player.x,
                            floor(player.x) + 0.23,
                            floor(player.x) + 0.77
                        ),
                        player.y,
                        roundToClosest(player.z, floor(player.z) + 0.23, floor(player.z) + 0.77),
                        true
                    )
                )
            }
            cancelPacket = true
        }
    }

    init {
        onMotion {
            if (!insideBurrow()) {
                disable()
            }
        }

        onPacketSend {
            if (cancelPacket && it.packet is PlayerMoveC2SPacket) {
                if (!insideBurrow()) {
                    disable()
                    return@onPacketSend
                }
                if (it.packet.changesLook()) {
                    val packetYaw = it.packet.getYaw(0f)
                    val packetPitch = it.packet.getPitch(0f)
                    if (timer.passedMs(delay.toLong())) {
                        cancelPacket = false
                        player.networkHandler.sendPacket(
                            PlayerMoveC2SPacket.Full(
                                player.x,
                                player.y + 1337,
                                player.z,
                                packetYaw,
                                packetPitch,
                                false
                            )
                        )
                        cancelPacket = true
                        timer.reset()
                    }
                }
                it.cancel()
            }
        }
    }

    private fun roundToClosest(num: Double, low: Double, high: Double): Double {
        val d1 = num - low
        val d2 = high - num

        return if (d2 > d1) {
            low
        } else {
            high
        }
    }

    private fun SafeClientEvent.insideBurrow(): Boolean {
        val playerBlockPos: BlockPos = player.blockPos
        for (xOffset in -1..1) {
            for (yOffset in -1..1) {
                for (zOffset in -1..1) {
                    val offsetPos = playerBlockPos.add(xOffset, yOffset, zOffset)
                    if (world.getBlockState(offsetPos).block === Blocks.BEDROCK) {
                        if (player.boundingBox.intersects(Box(offsetPos))) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}