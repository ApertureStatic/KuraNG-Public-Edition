package base.events.render

import base.system.event.Event
import base.system.event.IEventPosting
import base.system.event.NamedProfilerEventBus
import net.minecraft.client.gui.DrawContext

class Render2DEvent(var drawContext: DrawContext, var screenWidth: Int, var screenHeight: Int, var tickDelta: Float) : Event,
    IEventPosting by Companion {
    companion object : IEventPosting by NamedProfilerEventBus("Render2D")
}