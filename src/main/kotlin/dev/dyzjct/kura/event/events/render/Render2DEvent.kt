package dev.dyzjct.kura.event.events.render

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.IEventPosting
import dev.dyzjct.kura.event.eventbus.NamedProfilerEventBus
import net.minecraft.client.gui.DrawContext

class Render2DEvent(var drawContext: DrawContext, var screenWidth: Int, var screenHeight: Int, var tickDelta: Float) : Event,
    IEventPosting by Companion {
    companion object : IEventPosting by NamedProfilerEventBus("Render2D")
}