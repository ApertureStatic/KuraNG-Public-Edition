package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarBypass
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.sq
import base.events.player.PlayerMotionEvent
import base.system.event.safeEventListener
import base.utils.block.BlockUtil.canBreak
import base.utils.chat.ChatUtil
import base.utils.extension.fastPos
import base.utils.inventory.slot.firstBlock
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import net.minecraft.block.Blocks
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand
import team.exception.melon.util.math.distanceSqToCenter

object ManualCev : Module(name = "ManualCev", langName = "手动炸头", category = Category.COMBAT, description = "Place and attack crystal to MinePos.") {
    private val mode by msetting("SwitchMode", Mode.Spoof)
    private val delay by isetting("Delay", 50, 0, 500)
    private val range by isetting("range",5,0,10)
    private val rotation by bsetting("Rotation", false)
    private val debug by bsetting("Debug", false)

    private val timer = TimerUtils()

    var stage = CevStage.Mine

    override fun onEnable() {
        stage = CevStage.Block
    }

    init {
        safeEventListener<PlayerMotionEvent> {
            if (PacketMine.isDisabled) {
                PacketMine.enable()
            }

            val obsSlot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
            val crySlot = player.hotbarSlots.firstItem(Items.END_CRYSTAL)
            PacketMine.blockData?.let { blockData ->
                if (player.distanceSqToCenter(blockData.blockPos) > range.sq) return@safeEventListener
                if (!world.isAir(blockData.blockPos.up())) return@safeEventListener
                if (!canBreak(blockData.blockPos,true))
                if (debug) ChatUtil.sendNoSpamMessage("DEBUG >> ${stage.name}")
                if (rotation) {
                    RotationManager.addRotations(blockData.blockPos)
                }
                when (stage) {
                    CevStage.Block -> {
                        obsSlot?.let { obs ->
                            if (world.isAir(blockData.blockPos)) {
                                if (timer.tickAndReset(delay)) {
                                    when (mode) {
                                        Mode.Spoof -> {
                                            spoofHotbar(obs) {
                                                connection.sendPacket(fastPos(blockData.blockPos, true))
                                            }
                                        }

                                        Mode.SpoofBypass -> {
                                            spoofHotbarBypass(obs) {
                                                connection.sendPacket(fastPos(blockData.blockPos, true))
                                            }
                                        }
                                    }
                                }
                            } else {
                                stage = CevStage.Place
                            }
                        }
                    }

                    CevStage.Place -> {
                        crySlot?.let { cry ->
                            if (!world.isAir(blockData.blockPos)) {
                                if (timer.tickAndReset(delay)) {
                                    when (mode) {
                                        Mode.Spoof -> {
                                            spoofHotbar(cry) {
                                                connection.sendPacket(fastPos(blockData.blockPos.up()))
                                            }
                                        }

                                        Mode.SpoofBypass -> {
                                            spoofHotbarBypass(cry) {
                                                connection.sendPacket(fastPos(blockData.blockPos.up()))
                                            }
                                        }
                                    }
                                    stage = CevStage.Mine
                                }
                            } else {
                                stage = CevStage.Mine
                            }
                        }
                    }

                    CevStage.Mine -> {
                        if (world.isAir(blockData.blockPos)) {
                            stage = CevStage.Attack
                        }
                    }

                    CevStage.Attack -> {
                        if (timer.tickAndReset(delay)) {
                            for (ent in world.entities) {
                                if (ent !is EndCrystalEntity) continue
                                connection.sendPacket(PlayerInteractEntityC2SPacket.attack(ent, player.isSneaking))
                                connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                            }
                            stage = CevStage.Block
                        }
                    }
                }
            }
        }
    }

    enum class CevStage {
        Block, Place, Attack, Mine
    }

    enum class Mode {
        Spoof, SpoofBypass
    }
}