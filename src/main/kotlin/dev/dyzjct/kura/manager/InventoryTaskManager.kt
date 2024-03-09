package dev.dyzjct.kura.manager

import dev.dyzjct.kura.utils.inventory.ClickFuture
import dev.dyzjct.kura.utils.inventory.InventoryTask
import dev.dyzjct.kura.utils.inventory.StepFuture
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import base.events.ConnectionEvent
import base.events.PacketEvents
import base.events.RunGameLoopEvent
import base.system.event.AlwaysListening
import base.system.event.SafeClientEvent
import base.system.event.listener
import base.system.event.safeEventListener
import base.utils.TickTimer
import base.utils.inventory.InvUtils.removeHoldingItem
import base.utils.math.TpsCalculator
import net.minecraft.network.packet.s2c.play.ScreenHandlerPropertyUpdateS2CPacket
import net.minecraft.screen.ScreenHandlerContext
import java.util.*

object InventoryTaskManager : AlwaysListening {
    private val confirmMap = Int2ObjectOpenHashMap<ClickFuture>()
    private val taskQueue = PriorityQueue<InventoryTask>()
    private val timer = TickTimer()
    private var lastTask: InventoryTask? = null

    fun onInit() {
        listener<PacketEvents.Receive> {
            if (it.packet !is ScreenHandlerPropertyUpdateS2CPacket) return@listener
            synchronized(InventoryTaskManager) {
                confirmMap.remove(it.packet.syncId)?.confirm()
            }
        }

        safeEventListener<RunGameLoopEvent.Render> {
            if (lastTask == null && taskQueue.isEmpty()) return@safeEventListener
            if (!timer.tick(0L)) return@safeEventListener

            lastTaskOrNext()?.let {
                runTask(it)
            }
        }

        listener<ConnectionEvent.Disconnect> {
            reset()
        }
    }

    fun addTask(task: InventoryTask) {
        synchronized(InventoryTaskManager) {
            taskQueue.add(task)
        }
    }

    fun runNow(event: SafeClientEvent, task: InventoryTask) {
        event {
            if (!player.inventory.isEmpty) {
                removeHoldingItem()
            }

            while (!task.finished) {
                task.runTask(event)?.let {
                    handleFuture(it)
                }
            }

            timer.reset((task.postDelay * TpsCalculator.multiplier).toLong())
        }
    }

    private fun SafeClientEvent.lastTaskOrNext(): InventoryTask? {
        return lastTask ?: run {
            val newTask = synchronized(InventoryTaskManager) {
                taskQueue.poll()?.also { lastTask = it }
            } ?: return null

            if (!player.inventory.isEmpty) {
                removeHoldingItem()
                return null
            }

            newTask
        }
    }

    private fun SafeClientEvent.runTask(task: InventoryTask) {
        if (mc.currentScreen is ScreenHandlerContext && !task.runInGui && !player.inventory.isEmpty) {
            timer.reset(500L)
            return
        }

        if (task.delay == 0L) {
            runNow(this, task)
        } else {
            task.runTask(this)?.let {
                handleFuture(it)
                timer.reset((task.delay * TpsCalculator.multiplier).toLong())
            }
        }

        if (task.finished) {
            timer.reset((task.postDelay * TpsCalculator.multiplier).toLong())
            lastTask = null
            return
        }
    }

    private fun handleFuture(future: StepFuture) {
        if (future is ClickFuture) {
            synchronized(InventoryTaskManager) {
                confirmMap[future.id] = future
            }
        }
    }

    private fun reset() {
        synchronized(InventoryTaskManager) {
            confirmMap.clear()
            lastTask?.cancel()
            lastTask = null
            taskQueue.clear()
        }
    }

}