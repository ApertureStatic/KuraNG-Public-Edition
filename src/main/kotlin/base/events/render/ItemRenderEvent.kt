package base.events.render

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Hand

class ItemRenderEvent (val matrices: MatrixStack, val hand: Hand) : Event, IEventPosting by Companion {
    companion object : EventBus()
}