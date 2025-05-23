package dev.dyzjct.kura.module.modules.player

import base.utils.entity.EntityUtils.eyePosition
import base.utils.graphics.ESPRenderer
import base.utils.inventory.slot.allSlots
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import base.utils.math.distanceSqTo
import base.utils.math.distanceSqToCenter
import base.utils.math.scale
import base.utils.world.getClickSide
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.block.BlockEvent
import dev.dyzjct.kura.manager.HotbarManager.resetHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.swapSpoof
import dev.dyzjct.kura.manager.RotationManager.packetRotate
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.combat.ManualCev
import dev.dyzjct.kura.module.modules.player.PacketMine.PacketType.Start
import dev.dyzjct.kura.module.modules.player.PacketMine.PacketType.Stop
import dev.dyzjct.kura.system.util.color.ColorRGB
import dev.dyzjct.kura.system.util.color.ColorUtils.toRGB
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.block.BlockUtil.calcBreakTime
import dev.dyzjct.kura.utils.block.BlockUtil.canBreak
import dev.dyzjct.kura.utils.extension.minePacket
import dev.dyzjct.kura.utils.extension.sendSequencedPacket
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import dev.dyzjct.kura.utils.inventory.InventoryUtil.findBestItem
import net.minecraft.block.CobwebBlock
import net.minecraft.block.FireBlock
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.SwordItem
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

object PacketMine : Module(
    name = "PacketMine", langName = "发包挖掘", description = "Better Mine.", category = Category.PLAYER
) {
    private var mode = msetting("Mode", PacketMode.Instant)
    private var mode0 = mode.value as PacketMode
    private var safeSpamFactor by isetting("SafeSpamFactor", 350, 1, 1000).enumIs(mode, PacketMode.Spam)
    private var spamDelay by isetting("SpamDelay", 0, 0, 200).enumIs(mode, PacketMode.Spam)
    private var switchMode = msetting("SwitchMode", SwitchMode.Spoof)
    private var switchMode0 = switchMode.value as SwitchMode
    var inventoryTool by bsetting("InvTool", false).enumIs(switchMode, SwitchMode.Bypass)
    var doubleBreak by bsetting("DoubleBreak", false)
    private var startTime by isetting("StartTime", 0, 0, 1000).isTrue { doubleBreak }
    private var backTime by isetting("BackTime", 0, 0, 500).isTrue { doubleBreak }
    private var haste by bsetting("Haste", false)
    private val amplifier by isetting("Amplifier", 2, 1, 2).isTrue { haste }
    private var setGround by bsetting("SetGround", true)
    private var cancelAbort by bsetting("CancelAbortPacket", false)
    private var rotate = bsetting("Rotate", true)
    private var swing by bsetting("Swing", false)
    private var mainColor by csetting("MainColor", ColorRGB(255, 32, 32))
    private var doubleColor by csetting("DoubleColor", ColorRGB(200, 32, 32)).isTrue { doubleBreak }
    private val renderer = ESPRenderer().apply { aFilled = 35; aOutline = 233 }
    private var packetTimer = TimerUtils()
    private var spamTimer = TimerUtils()
    private var retryTimer = TimerUtils()
    private var inventoryBypass = false
    private var packetSpamming = false
    private var fastSyncCheck = false
    private var forceRetry = false
    var blockData: BlockData? = null
    var doubleData: BlockData? = null
    var onDoubleBreak = false

    override fun onDisable() {
        blockData = null
        doubleData = null
        fastSyncCheck = false
        packetSpamming = false
        timerReset()
        renderer.clear()
    }

    override fun onEnable() {
        blockData = null
        doubleData = null
        fastSyncCheck = false
        packetSpamming = false
        timerReset()
        renderer.clear()
    }

    override fun getHudInfo(): String {
        return mode0.name
    }

    init {
        onPacketSend {
            if (it.packet is PlayerActionC2SPacket && it.packet.action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK && cancelAbort) {
                it.cancelled = true
            }
        }
        safeEventListener<BlockEvent> { event ->
            if (eatingCheck()) return@safeEventListener
            BlockData(
                event.pos, event.facing, findBestItem(event.pos, inventoryBypass)?.let {
                    if (inventoryBypass) player.allSlots.firstItem(it)
                        ?.let { item -> HotbarSlot(item) } else player.hotbarSlots.firstItem(it)
                }, System.currentTimeMillis(), calcBreakTime(event.pos, inventoryBypass)
            ).apply {
                if (!canBreak(blockPos, false)) {
                    blockData = null
                    timerReset()
                    return@safeEventListener
                }
                if (doubleBreak) {
                    val blockData = blockData
                    val doubleData = doubleData
                    if (blockData != null && !world.isAir(blockData.blockPos) && doubleData == null && blockData.blockPos != this.blockPos) {
                        sendMinePacket(Stop, blockData, force = true)
                        PacketMine.doubleData = blockData
                        timerReset()
                    }
                }
                if (world.getBlockState(blockPos).block is CobwebBlock && findBestItem(
                        blockPos, inventoryBypass
                    ) !is SwordItem
                ) {
                    blockData = null
                    timerReset()
                    return@safeEventListener
                }
                if (blockData?.blockPos == event.pos) {
                    retryTimer.reset()
                    return@safeEventListener
                }
                blockData = this
                doubleData?.let {
                    doubleData!!.startTime = this.startTime
                }
                sendMinePacket(Start, this)
                timerReset()
                packetSpamming = true
            }
        }

        onLoop {
            addhaste(haste)
            blockData?.let { blockData ->
                if (!world.isAir(blockData.blockPos)) {
                    packetSpamming = true
                    packetTimer.reset()
                    forceRetry = retryTimer.passed(blockData.breakTime * 1.5)
                } else {
                    forceRetry = false
                    retryTimer.reset()
                }
                if (packetTimer.passed(safeSpamFactor) && world.isAir(blockData.blockPos)) {
                    packetSpamming = false
                }
                fastSyncCheck = if (!mode0.ignoreCheck) {
                    world.isAir(blockData.blockPos)
                } else {
                    packetTimer.passed(blockData.breakTime)
                }
            }
        }

        onMotion {
            mode0 = (mode.value as PacketMode)
            switchMode0 = (switchMode.value as SwitchMode)
            inventoryBypass = inventoryTool && switchMode0.bypass
            if (ManualCev.isEnabled) {
                if (ManualCev.stage == ManualCev.CevStage.Block || ManualCev.stage == ManualCev.CevStage.Place) return@onMotion
            }
            blockData?.let { data ->
                if ((System.currentTimeMillis() - data.startTime) < data.breakTime) return@let
                if (data.blockPos.distanceSqTo(player.blockPos) >= CombatSystem.interactRange.sq) {
                    blockData = null
                    doubleData = null
                    onDoubleBreak = false
                    connection.sendPacket(CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId))
                    return@let
                }
                if (player.distanceSqToCenter(data.blockPos) <= CombatSystem.interactRange.sq) {
                    if (((mode0.ignoreCheck && packetSpamming) || !fastSyncCheck) && !eatingCheck()) {
                        sendMinePacket(Stop, data)
                    }
                    if ((mode0.retry || forceRetry) && !eatingCheck()) hookPos(data.blockPos, true)
                    if (mode0.strict) {
                        blockData = BlockData(
                            data.blockPos, data.facing, findBestItem(data.blockPos, inventoryBypass)?.let {
                                if (inventoryBypass) player.allSlots.firstItem(it)
                                    ?.let { item -> HotbarSlot(item) } else player.hotbarSlots.firstItem(it)
                            }, System.currentTimeMillis(), calcBreakTime(data.blockPos, inventoryBypass)
                        )
                    }
                }
            }
            doubleData?.let { blockData ->
                if (world.getBlockState(blockData.blockPos).block is FireBlock) doubleData = null
                blockData.mineTool?.let {
                    if (player.distanceSqToCenter(blockData.blockPos) <= 5.15.sq) {
                        sendMinePacket(Stop, blockData, true)
                    } else doubleData = null
                }
            }
        }

        onRender3D {
            blockData?.let { blockData ->
                renderer.add(
                    Box(blockData.blockPos).scale(
                        Easing.OUT_CUBIC.inc(
                            Easing.toDelta(
                                blockData.startTime, blockData.breakTime
                            )
                        ).toDouble()
                    ), if (world.isAir(blockData.blockPos)) ColorRGB(32, 255, 32) else mainColor.toRGB()
                )
                renderer.render(it.matrices, true)
            }
            doubleData?.let { data ->
                renderer.add(
                    Box(data.blockPos).scale(
                        Easing.OUT_CUBIC.inc(
                            Easing.toDelta(
                                data.startTime, data.breakTime
                            )
                        ).toDouble()
                    ), if (world.isAir(data.blockPos)) ColorRGB(32, 255, 32) else doubleColor.toRGB()
                )
                renderer.render(it.matrices, true)
            }
        }
    }

    fun SafeClientEvent.hookPos(blockPos: BlockPos, reset: Boolean = false) {
        if (eatingCheck()) return
        if (reset) blockData = null
        world.getBlockState(blockPos).onBlockBreakStart(world, blockPos, player)
        val vector = player.eyePosition.subtract(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
        val side =
            getClickSide(blockPos, true) ?: Direction.getFacing(
                vector.x.toFloat(),
                vector.y.toFloat(),
                vector.z.toFloat()
            )
        BlockEvent(blockPos, side).post()
        timerReset()
    }

    private fun timerReset() {
        packetTimer.reset()
        retryTimer.reset()
        spamTimer.reset()
    }

    private fun SafeClientEvent.sendMinePacket(
        action: PacketType, blockData: BlockData, db: Boolean = false, force: Boolean = false
    ) {
        val toolSlot = blockData.mineTool ?: return
        if (db) {
            if (doubleBreak) {
                if (onDoubleBreak) {
                    doubleData?.let { packetRotate(it.blockPos) }
                }
                if ((System.currentTimeMillis() - blockData.startTime) >= (blockData.breakTime + startTime + backTime) && onDoubleBreak) {
                    onDoubleBreak = false
                    connection.sendPacket(CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId))
                    resetHotbar()
                    doubleData = null
                    return
                } else if ((System.currentTimeMillis() - blockData.startTime) >= (blockData.breakTime + startTime) && !onDoubleBreak && !eatingCheck()) {
                    onDoubleBreak = true
                    connection.sendPacket(CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId))
                    connection.sendPacket(UpdateSelectedSlotC2SPacket(toolSlot.hotbarSlot))
                    if (setGround) player.onGround = true
                }
            }
        } else {
            if (world.getBlockState(blockData.blockPos).block is FireBlock || eatingCheck()) return
            if (((action == Stop && !spamTimer.passed(if (mode0.ignoreCheck) spamDelay else 0)) || (world.isAir(
                    blockData.blockPos
                ) && action == Stop)) && !force
            ) return
            if (rotate.value) packetRotate(blockData.blockPos)
            if (swing) connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
            if (switchMode0.spoof) {
                if (!switchMode0.bypass) {
                    spoofHotbar(toolSlot) {
                        sendSequencedPacket(world) {
                            minePacket(action, blockData, it)
                        }
                    }
                } else {
                    swapSpoof(toolSlot) {
                        sendSequencedPacket(world) {
                            minePacket(action, blockData, it)
                        }
                    }
                }
            } else {
                if (switchMode0 != SwitchMode.Off) {
                    if (player.inventory.selectedSlot != blockData.mineTool.hotbarSlot) {
                        player.inventory.selectedSlot = blockData.mineTool.hotbarSlot
                    }
                    sendSequencedPacket(world) {
                        minePacket(action, blockData, it)
                    }
                }
            }
            spamTimer.reset()
        }
    }

    private fun SafeClientEvent.addhaste(haste: Boolean) {
        if (player.hasStatusEffect(StatusEffects.HASTE) || !haste) return
        player.addStatusEffect(
            StatusEffectInstance(
                StatusEffects.HASTE,
                1,
                amplifier - 1,
                false,
                false,
                true
            )
        )
    }

    @Suppress("UNUSED")
    private enum class SwitchMode(val spoof: Boolean, val bypass: Boolean = false) {
        Spoof(true), Bypass(true, true), Swap(false), Off(false)
    }

    @Suppress("UNUSED")
    enum class PacketMode(val strict: Boolean, val retry: Boolean = false, val ignoreCheck: Boolean = false) {
        Instant(false), Spam(false, ignoreCheck = true), Packet(true), Legit(true, true)
    }

    @Suppress("UNUSED")
    enum class PacketType(val action: PlayerActionC2SPacket.Action) {
        Start(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK), Abort(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK), Stop(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
        )
    }

    fun SafeClientEvent.eatingCheck(): Boolean {
        return player.isUsingItem && CombatSystem.eating
    }

    class BlockData(
        val blockPos: BlockPos,
        val facing: Direction,
        val mineTool: HotbarSlot?,
        var startTime: Long,
        val breakTime: Float
    )
}
