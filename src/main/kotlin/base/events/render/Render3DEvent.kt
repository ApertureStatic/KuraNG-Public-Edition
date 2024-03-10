package base.events.render

import base.system.event.Event
import base.system.event.IEventPosting
import base.system.event.NamedProfilerEventBus
import net.minecraft.client.util.math.MatrixStack

class Render3DEvent(
    var matrices: MatrixStack,
    var tickDelta: Float,
) : Event, IEventPosting by Companion {
    companion object : NamedProfilerEventBus("kuraRender3D")
}