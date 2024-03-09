package melon.events.render

import melon.system.event.Event
import melon.system.event.IEventPosting
import melon.system.event.NamedProfilerEventBus
import net.minecraft.client.util.math.MatrixStack

class Render3DEvent(
    var matrices: MatrixStack,
    var tickDelta: Float,
) : Event, IEventPosting by Companion {
    companion object : NamedProfilerEventBus("melonRender3D")
}