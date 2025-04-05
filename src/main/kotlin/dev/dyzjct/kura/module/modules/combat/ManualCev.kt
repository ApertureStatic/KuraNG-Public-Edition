package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.utils.block.BlockUtil.canBreak
import base.utils.chat.ChatUtil
import dev.dyzjct.kura.utils.extension.fastPos
import base.utils.math.distanceSqToCenter
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem.swing
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.sq
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand

object ManualCev : Module(
    name = "ManualCev",
    langName = "手动炸头",
    description = "Place and attack crystal to MinePos.",
    category = Category.COMBAT
) {
    private val delay by isetting("Delay", 50, 0, 500)
    private val range by isetting("range", 5, 0, 10)
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

            PacketMine.blockData?.let { blockData ->
                if (player.distanceSqToCenter(blockData.blockPos) > range.sq) return@safeEventListener
                if (!world.isAir(blockData.blockPos.up())) return@safeEventListener
                if (!canBreak(blockData.blockPos, true))
                    if (debug) ChatUtil.sendNoSpamMessage("DEBUG >> ${stage.name}")
                if (rotation) {
                    RotationManager.rotationTo(blockData.blockPos)
                }
                when (stage) {
                    CevStage.Block -> {
                        if (world.isAir(blockData.blockPos)) {
                            if (timer.tickAndReset(delay)) {
                                spoofHotbarWithSetting(Items.OBSIDIAN) {
                                    connection.sendPacket(fastPos(blockData.blockPos))
                                }
                            }
                        } else {
                            stage = CevStage.Place
                        }
                    }

                    CevStage.Place -> {
                        if (!world.isAir(blockData.blockPos)) {
                            if (timer.tickAndReset(delay)) {
                                if (player.offHandStack.item == Items.END_CRYSTAL) {
                                    connection.sendPacket(fastPos(blockData.blockPos.up(), hand = Hand.OFF_HAND))
                                } else {
                                    spoofHotbarWithSetting(Items.END_CRYSTAL) {
                                        connection.sendPacket(fastPos(blockData.blockPos.up()))
                                    }
                                }
                                stage = CevStage.Mine
                            }
                        } else {
                            stage = CevStage.Mine
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
                                swing()
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
}