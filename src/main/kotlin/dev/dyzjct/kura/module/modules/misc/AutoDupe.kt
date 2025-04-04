package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.utils.block.BlockUtil.checkNearBlocksExtended
import dev.dyzjct.kura.utils.block.BlockUtil.getNeighbor
import dev.dyzjct.kura.utils.block.isFullBox
import base.utils.chat.ChatUtil
import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils.boxCheck
import base.utils.extension.fastPos
import base.utils.math.toBox
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem.swing
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction


object AutoDupe : Module(
    name = "AutoDupe",
    langName = "自动复制潜影盒",
    category = Category.MISC
) {
    private val delay by isetting("Delay", 50, 0, 150)
    private val rotate by bsetting("Rotate", false)
    private val airPlace by bsetting("AirPlace", false)
    private val helper by bsetting("Helper", false).isFalse { airPlace }
    private val debug by bsetting("Debug", false)

    private val placeTimer = TimerUtils()

    init {
        onMotion {
            check()
            if (PacketMine.isDisabled || PacketMine.blockData == null) return@onMotion
            PacketMine.blockData?.blockPos?.let { dupePos ->
                if (isDupePos(dupePos)) {
                    if (world.isAir(dupePos)) {
                        if (placeTimer.tickAndReset(delay)) {
                            for (i in 0..9) {
                                val stack = player.inventory.getStack(i)
                                if (stack.item in SHULKER_BOXES) {
                                    player.inventory.selectedSlot = i
                                    if (rotate) RotationManager.rotationTo(dupePos)
                                    connection.sendPacket(fastPos(dupePos, render = true))
                                    swing()
                                    break
                                }
                            }
                        }
                    }
                } else {
                    if (spoofHotbarWithSetting(Items.OBSIDIAN, isCheck = true) {}) {
                        for (facing in Direction.entries) {
                            if (!boxCheck(dupePos.offset(facing).toBox()) || !world.isAir(dupePos.offset(facing))) {
                                if (debug) ChatUtil.sendMessage("Checked.")
                                continue
                            }
                            fun doPlace(pos: BlockPos) {
                                if (debug) ChatUtil.sendMessage("PlacePos:$pos.")
                                if (rotate) RotationManager.rotationTo(pos)
                                spoofHotbarWithSetting(Items.OBSIDIAN) {
                                    connection.sendPacket(fastPos(pos, render = true))
                                }
                                swing()
                                if (debug) ChatUtil.sendMessage("Placing Helper.")
                            }
                            if (placeTimer.tickAndReset(delay)) {
                                if (getNeighbor(dupePos.offset(facing)) == null && !airPlace) {
                                    if (helper) checkNearBlocksExtended(dupePos.offset(facing))?.let { helper ->
                                        doPlace(helper.position.offset(helper.facing))
                                    }
                                } else {
                                    doPlace(dupePos.offset(facing))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.isDupePos(pos: BlockPos): Boolean {
        if (world.isAir(pos.down()) || !world.getBlockState(pos.down()).isFullBox) return false
        var stage = 0
        for (facing in Direction.entries) {
            if (!world.isAir(pos.offset(facing)) && world.getBlockState(pos.offset(facing)).isFullBox) stage++
        }
        return stage >= 5
    }

    override fun onEnable() {
        runSafe {
            check()
        }
    }

    private fun SafeClientEvent.check() {
        if (PacketMine.isDisabled) {
            ChatUtil.sendMessage("[${ChatUtil.RED}WARNING${ChatUtil.WHITE}] Please Toggle ON PacketMine!")
            toggle()
        }
        if (PacketMine.blockData == null) {
            ChatUtil.sendMessage("[${ChatUtil.RED}WARNING${ChatUtil.WHITE}] Please click on the square next to you!")
            toggle()
        } else {
            var isNotGoodPos = true
            for (facing in Direction.entries) {
                if (player.blockPos.offset(facing) == PacketMine.blockData!!.blockPos ||
                    player.blockPos.offset(facing).up() == PacketMine.blockData!!.blockPos
                ) isNotGoodPos = false
            }
            if (isNotGoodPos) toggle()
        }
    }

    private val SHULKER_BOXES: List<Item> = listOf(
        Items.SHULKER_BOX,
        Items.WHITE_SHULKER_BOX,
        Items.ORANGE_SHULKER_BOX,
        Items.MAGENTA_SHULKER_BOX,
        Items.LIGHT_BLUE_SHULKER_BOX,
        Items.YELLOW_SHULKER_BOX,
        Items.LIME_SHULKER_BOX,
        Items.PINK_SHULKER_BOX,
        Items.GRAY_SHULKER_BOX,
        Items.LIGHT_GRAY_SHULKER_BOX,
        Items.CYAN_SHULKER_BOX,
        Items.PURPLE_SHULKER_BOX,
        Items.BLUE_SHULKER_BOX,
        Items.BROWN_SHULKER_BOX,
        Items.GREEN_SHULKER_BOX,
        Items.RED_SHULKER_BOX,
        Items.BLACK_SHULKER_BOX,
    )
}