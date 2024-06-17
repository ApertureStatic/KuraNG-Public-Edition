package dev.dyzjct.kura.event.events.render

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.IEventPosting
import dev.dyzjct.kura.event.eventbus.NamedProfilerEventBus
import net.minecraft.client.util.math.MatrixStack

class Render3DEvent(
    var matrices: MatrixStack,
    var tickDelta: Float,
) : Event, IEventPosting by Companion {
    companion object : NamedProfilerEventBus("kuraRender3D")
}