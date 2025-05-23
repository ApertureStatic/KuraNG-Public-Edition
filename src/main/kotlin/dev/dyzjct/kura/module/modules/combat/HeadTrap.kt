package dev.dyzjct.kura.module.modules.combat

import base.utils.combat.getTarget
import base.utils.entity.EntityUtils.boxCheck
import base.utils.inventory.slot.firstBlock
import base.utils.inventory.slot.hotbarSlots
import base.utils.math.distanceSqToCenter
import base.utils.math.toBox
import base.utils.player.getTargetSpeed
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.swapSpoof
import dev.dyzjct.kura.manager.RotationManager.packetRotate
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.block.BlockUtil.checkNearBlocksExtended
import dev.dyzjct.kura.utils.extension.fastPos
import dev.dyzjct.kura.utils.extension.sendSequencedPacket
import dev.dyzjct.kura.utils.extension.sq
import net.minecraft.block.Blocks

object HeadTrap : Module(name = "HeadTrap", "盖头", description = "1!5!", category = Category.COMBAT) {

    private var placeDelay = isetting("PlaceDelay", 10, 0, 1000)
    private var airPlace = bsetting("AirPlace", false)
    private var rotate = bsetting("Rotate", false)
    private var bypass = bsetting("SpoofBypass", true)
    private var topBlock = msetting("TopBlock", TopBlock.Obi)
    private var maxSpeed = dsetting("MaxSpeed", 2.0, 0.0, 20.0)
    private var placeTimer = TimerUtils()

    init {
        onMotion {
            var supportSlot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
            when (topBlock.value) {
                TopBlock.Obi -> {
                    supportSlot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
                }

                TopBlock.Anchor -> {
                    supportSlot = player.hotbarSlots.firstBlock(Blocks.RESPAWN_ANCHOR)
                }

                TopBlock.Web -> {
                    supportSlot = player.hotbarSlots.firstBlock(Blocks.COBWEB)
                }
            }

            supportSlot?.let { slot ->
                getTarget(CombatSystem.targetRange)?.let { target ->
                    if (getTargetSpeed(target) > maxSpeed.value) {
                        return@onMotion
                    }
                    if (airPlace.value) {
                        if (rotate.value) packetRotate(target.blockPos.up(2))
                        if (placeTimer.tickAndReset(placeDelay.value)) {
                            if (bypass.value) swapSpoof(slot) {
                                sendSequencedPacket(world) { seq ->
                                    fastPos(pos = target.blockPos.up(2), sequence = seq)
                                }
                            } else {
                                spoofHotbar(slot) {
                                    sendSequencedPacket(world) { seq ->
                                        fastPos(pos = target.blockPos.up(2), sequence = seq)
                                    }
                                }
                            }
                        }
                    } else if (boxCheck(target.blockPos.up(2).toBox(), true)) {
                        checkNearBlocksExtended(target.blockPos.up(2))?.let { block ->
                            if (player.distanceSqToCenter(block.position.offset(block.facing)) <= CombatSystem.placeRange.sq) {
                                if (!world.isAir(block.position.offset(block.facing))) return@onMotion
                                if (rotate.value) packetRotate(
                                    block.position.offset(block.facing)
                                )
                                if (placeTimer.tickAndReset(placeDelay.value)) {
                                    if (bypass.value) {
                                        swapSpoof(slot) {
                                            sendSequencedPacket(world) {
                                                fastPos(block.position.offset(block.facing))
                                            }
                                        }
                                    } else {
                                        spoofHotbar(slot) {
                                            sendSequencedPacket(world) {
                                                fastPos(block.position.offset(block.facing))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    enum class TopBlock {
        Obi, Anchor, Web
    }
}