package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarBypass
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import dev.dyzjct.kura.utils.math.RotationUtils.getRotationTo
import melon.system.event.SafeClientEvent
import melon.utils.entity.EntityUtils.autoCenter
import melon.utils.extension.sendSequencedPacket
import melon.utils.inventory.slot.allSlots
import melon.utils.inventory.slot.firstItem
import melon.utils.inventory.slot.hotbarSlots
import melon.utils.player.RotationUtils.getPlayerDirection
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.Direction

object PearlClip : Module(
    name = "PearlClip", langName = "珍珠卡墙", category = Category.MISC, description = "PearlClip Ez"
) {
    private val eatingPause by bsetting("EatingPause", false)
    private val better by bsetting("LookBetter", true)
    private val bedRock by bsetting("BedRock", false)
    private val one by bsetting("OneHeight", true)
    private val bypass by bsetting("SpoofBypass", false)
    private val safe = msetting("SafeMode", SafeMode.Center)
    private val boundary by bsetting("Boundary", false).enumIs(safe, SafeMode.Smart)


    init {
        onLoop {
            val slot = if (bypass) player.allSlots.firstItem(Items.ENDER_PEARL)
                ?.let { item -> HotbarSlot(item) } else player.hotbarSlots.firstItem(Items.ENDER_PEARL)
            if ((eatingPause && player.isUsingItem) || slot == null || !world.isAir(player.blockPos)) {
                toggle()
                return@onLoop
            }
            val look = player.blockPos.offset(getPlayerDirection())
            for (face in Direction.entries) {
                if (face == Direction.DOWN || face == Direction.UP) continue
                var clipPos = player.blockPos.offset(face)
                var clipDirection = face
                if (world.isAir(clipPos)) continue
                if (world.getBlockState(clipPos).block == Blocks.BEDROCK && !bedRock) continue
                if (one && !world.isAir(clipPos.up())) continue
                if (!world.isAir(look) && (world.getBlockState(look).block != Blocks.BEDROCK || bedRock) && (!one || world.isAir(
                        look.up()
                    )) && better
                ) {
                    clipPos = look
                    clipDirection = getPlayerDirection()
                }

                fun smartValue(direction: Direction): Float {
                    val centerPos = player.blockPos.toCenterPos()
                    val playerPos = player.pos
                    val vl = (when (direction) {
                        Direction.EAST -> (centerPos.x - playerPos.x).toFloat()
                        Direction.WEST -> (centerPos.x - playerPos.x).toFloat()
                        Direction.SOUTH -> (centerPos.z - playerPos.z).toFloat()
                        else -> (centerPos.z - playerPos.z).toFloat()
                    }) * 50
                    return if (boundary) {
                        if (vl > 6f) 6f else if (vl < -6f) -6f else vl
                    } else vl
                }

                var angle = getRotationTo(clipPos.toCenterPos()).x
                var pitch = 75f
                val fix = 3f

                when (safe.value) {
                    SafeMode.Center -> autoCenter()
                    SafeMode.Smart -> pitch -= smartValue(clipDirection)
                }

                angle = if ((angle + fix) > 180.0f) angle - fix else angle + fix
                RotationManager.addRotations(angle, pitch, true)
                RotationManager.stopRotation()
                sendPlayerRotation(angle, pitch, player.onGround)
                RotationManager.startRotation()
                if (player.mainHandStack.item == Items.ENDER_PEARL) {
                    sendSequencedPacket(world) {
                        PlayerInteractItemC2SPacket(
                            Hand.MAIN_HAND, it
                        )
                    }
                } else if (bypass) spoofHotbarBypass(slot) {
                    sendSequencedPacket(
                        world
                    ) {
                        PlayerInteractItemC2SPacket(
                            Hand.MAIN_HAND, it
                        )
                    }
                } else {
                    spoofHotbar(slot) {
                        sendSequencedPacket(
                            world
                        ) {
                            PlayerInteractItemC2SPacket(
                                Hand.MAIN_HAND, it
                            )
                        }
                    }
                }
                break
            }
            disable()
        }
    }

    private fun SafeClientEvent.sendPlayerRotation(yaw: Float, pitch: Float, onGround: Boolean) {
        connection.sendPacket(PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround))
    }

    enum class SafeMode {
        Center, Smart
    }
}