package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils
import base.utils.extension.sendSequencedPacket
import base.utils.extension.synchronized
import base.utils.math.distanceSqTo
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarNoAnyCheck
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.inventory.InventoryUtil
import dev.dyzjct.kura.utils.inventory.InventoryUtil.findEmptySlots
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import java.util.concurrent.ConcurrentLinkedQueue

object AutoEXP : Module(
    name = "AutoEXP", langName = "自动修甲", category = Category.COMBAT, description = "Automatically mends armour"
) {
    private var packetDelay = isetting("PacketDelay", 10, 0, 1000)
    private var delay = isetting("Delay", 50, 0, 500)
    private var mendingTakeOff = bsetting("AutoMend", true)
    private var closestEnemy = isetting("EnemyRange", 6, 1, 20).isTrue(mendingTakeOff)
    private var mendPercentage = isetting("Mend%", 100, 1, 100).isTrue(mendingTakeOff)
    private var updateController = bsetting("Update", false)
    private var shiftClick = bsetting("ShiftClick", true)
    private var packetClick = bsetting("PacketClick", true)
    private var taskList = ConcurrentLinkedQueue<InventoryUtil.Task>()
    private var doneSlots = ArrayList<Int>().synchronized()
    private var timerUtils = TimerUtils()
    private var packetTimer = TimerUtils()
    private var elytraTimerUtils = TimerUtils()

    override fun onDisable() {
        runSafe {
            timerUtils.reset()
            taskList.clear()
            doneSlots.clear()
            elytraTimerUtils.reset()
        }
    }

    override fun onEnable() {
        runSafe {
            timerUtils.reset()
            packetTimer.reset()
            elytraTimerUtils.reset()
        }
    }

    override fun onLogout() {
        taskList.clear()
        doneSlots.clear()
    }

    init {
        onPacketSend {
            when (it.packet) {
                is PlayerMoveC2SPacket.Full -> {
                    it.packet.pitch = 90f
                }

                is PlayerMoveC2SPacket.LookAndOnGround -> {
                    it.packet.pitch = 90f
                }
            }
        }

        onMotion { event ->
            if (!spoofHotbarNoAnyCheck(Items.EXPERIENCE_BOTTLE, true) {}) return@onMotion
            event.setRotation(player.yaw, 90f)
            if (packetTimer.tickAndReset(packetDelay.value)) {
                spoofHotbarNoAnyCheck(Items.EXPERIENCE_BOTTLE) {
                    sendSequencedPacket(world) {
                        PlayerInteractItemC2SPacket(Hand.MAIN_HAND, it)
                    }
                }
            }
            if (taskList.isEmpty()) {
                if (mendingTakeOff.value && (isSafe || EntityUtils.isSafe(
                        player,
                        1,
                        floor = false,
                        face = true
                    ))
                ) {
                    val helm = player.inventory.getStack(5)
                    if (!helm.isEmpty && getDamageInPercent(helm) >= mendPercentage.value) {
                        takeOffSlot(5)
                    }
                    val chest2 = player.inventory.getStack(6)
                    if (!chest2.isEmpty && getDamageInPercent(chest2) >= mendPercentage.value) {
                        takeOffSlot(6)
                    }
                    val legging2 = player.inventory.getStack(7)
                    if (!legging2.isEmpty && getDamageInPercent(legging2) >= mendPercentage.value) {
                        takeOffSlot(7)
                    }
                    val feet2 = player.inventory.getStack(8)
                    if (!feet2.isEmpty && getDamageInPercent(feet2) >= mendPercentage.value) {
                        takeOffSlot(8)
                    }
                }
            }
            if (timerUtils.tickAndReset(delay.value.toLong())) {
                if (taskList.isNotEmpty()) {
                    val task = taskList.poll()
                    task.runTask()
                }
            }
        }
    }

    private fun getDamageInPercent(stack: ItemStack): Int {
        return (1 - (stack.maxDamage.toFloat() - stack.damage.toFloat()) / stack.maxDamage.toFloat()).toInt() * 100
    }

    private fun SafeClientEvent.takeOffSlot(slot: Int) {
        if (taskList.isEmpty()) {
            var target = -1
            for (i in findEmptySlots()) {
                if (doneSlots.contains(target)) continue
                target = i
                doneSlots.add(i)
            }
            if (target != -1) {
                if (target in 1..4 || !shiftClick.value) {
                    taskList.add(InventoryUtil.Task(slot, packetClick.value))
                    taskList.add(InventoryUtil.Task(target, packetClick.value))
                } else {
                    taskList.add(InventoryUtil.Task(slot, true, packetClick.value))
                }
                if (updateController.value) {
                    taskList.add(InventoryUtil.Task())
                }
            }
        }
    }

    private val SafeClientEvent.isSafe: Boolean
        get() {
            val closest = EntityUtils.getClosestEnemy(closestEnemy.value.toDouble()) ?: return true
            return player.distanceSqTo(closest) >= closestEnemy.value.sq
        }
}