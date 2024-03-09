package melon.events.render

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Hand

class ItemRenderEvent (val matrices: MatrixStack, val hand: Hand) : Event, IEventPosting by Companion {
    companion object : EventBus()
}