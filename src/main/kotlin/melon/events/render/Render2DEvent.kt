package melon.events.render

import melon.system.event.Event
import melon.system.event.IEventPosting
import melon.system.event.NamedProfilerEventBus
import net.minecraft.client.gui.DrawContext

class Render2DEvent(var drawContext: DrawContext, var screenWidth: Int, var screenHeight: Int, var tickDelta: Float) : Event,
    IEventPosting by Companion {
    companion object : IEventPosting by NamedProfilerEventBus("Render2D")
}