package dev.dyzjct.kura.utils.inventory

import base.system.event.SafeClientEvent
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType

object InstantFuture : StepFuture {
    override fun timeout(timeout: Long): Boolean {
        return true
    }

    override fun confirm() {

    }
}

class Click(
    private val windowID: Int,
    private val slot: Slot,
    private val mouseButton: Int,
    private val type: SlotActionType
) : Step {
    override fun run(event: SafeClientEvent): ClickFuture {
        val id = event.clickSlot(windowID, slot, mouseButton, type)
        return ClickFuture(id)
    }
}

class ClickFuture(
    val id: Int,
) : StepFuture {
    private val time = System.currentTimeMillis()
    private var confirmed = false

    override fun timeout(timeout: Long): Boolean {
        return confirmed || System.currentTimeMillis() - time > timeout
    }

    override fun confirm() {
        confirmed = true
    }
}

interface Step {
    fun run(event: SafeClientEvent): StepFuture
}

interface StepFuture {
    fun timeout(timeout: Long): Boolean
    fun confirm()
}