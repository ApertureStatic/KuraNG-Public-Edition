package dev.dyzjct.kura.module.modules.misc

import base.utils.entity.EntityUtils.autoCenter
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarNoCheck
import dev.dyzjct.kura.manager.RotationManager.packetRotate
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.combat.PearlFucker
import dev.dyzjct.kura.utils.extension.sendSequencedPacket
import dev.dyzjct.kura.utils.rotation.RotationUtils.getPlayerDirection
import dev.dyzjct.kura.utils.rotation.RotationUtils.getRotationToVec2f
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.Direction

object PearlClip : Module(
    name = "PearlClip", langName = "珍珠卡墙", description = "PearlClip Ez", category = Category.MISC
) {
    private val better by bsetting("LookBetter", true)
    private val bedRock by bsetting("BedRock", false)
    private val one by bsetting("OneHeight", true)
    private val safe = msetting("SafeMode", SafeMode.Center)
    private val cpitch by fsetting("Pitch", 89f, 0f, 180f).enumIs(safe, SafeMode.Custom)
    private val multiple by isetting("Multiple", 50, 1, 100).enumIs(safe, SafeMode.Smart)
    private val boundary by bsetting("Boundary", false).enumIs(safe, SafeMode.Smart)
    private val maxVL by fsetting("MaxVL", 6.0f, 0.1f, 8.0f).enumIs(safe, SafeMode.Smart).isTrue { boundary }


    init {
        onLoop {
            if (!spoofHotbarNoCheck(
                    Items.ENDER_PEARL,
                    true
                ) {} || !world.isAir(player.blockPos)
            ) {
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
                    val vl = (0.5f - when (direction) {
                        Direction.EAST -> (centerPos.x - playerPos.x).toFloat()
                        Direction.WEST -> (centerPos.x - playerPos.x).toFloat()
                        Direction.SOUTH -> (centerPos.z - playerPos.z).toFloat()
                        else -> (centerPos.z - playerPos.z).toFloat()
                    }) * multiple
                    return if (boundary) {
                        if (vl > maxVL) maxVL else if (vl < -maxVL) -maxVL else vl
                    } else vl
                }

                var angle = getRotationToVec2f(clipPos.toCenterPos()).x
                var pitch = 89f
                val fix = 3f

                when (safe.value) {
                    SafeMode.Center -> autoCenter()
                    SafeMode.Smart -> pitch -= smartValue(clipDirection)
                    SafeMode.Custom -> pitch = cpitch
                }

                angle = if ((angle + fix) > 180.0f) angle - fix else angle + fix

                packetRotate(angle, pitch)
                if (player.mainHandStack.item == Items.ENDER_PEARL) {
                    packetRotate(angle, pitch)
                    sendSequencedPacket(world) {
                        PlayerInteractItemC2SPacket(
                            Hand.MAIN_HAND, it
                        )
                    }
                } else {
                    spoofHotbarNoCheck(Items.ENDER_PEARL) {
                        packetRotate(angle, pitch)
                        PearlFucker.ignoreTimer.reset()
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

    enum class SafeMode {
        Center, Smart, Custom
    }
}