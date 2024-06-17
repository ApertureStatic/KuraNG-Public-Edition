package dev.dyzjct.kura.event.events.render

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Hand

class ItemRenderEvent (val matrices: MatrixStack, val hand: Hand) : Event, IEventPosting by Companion {
    companion object : EventBus()
}