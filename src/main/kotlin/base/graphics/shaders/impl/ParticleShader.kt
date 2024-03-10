package base.graphics.shaders.impl

import base.events.TickEvent
import base.system.event.AlwaysListening
import base.system.event.listener
import base.KuraIdentifier
import base.graphics.shaders.GLSLSandbox

object ParticleShader : GLSLSandbox(KuraIdentifier("shaders/particle.fsh")), AlwaysListening {
    private val initTime = System.currentTimeMillis()
    private var prevMouseX = 0.0f
    private var prevMouseY = 0.0f
    private var mouseX = 0.0f
    private var mouseY = 0.0f

    init {
        listener<TickEvent.Post>(true) {
            prevMouseX = mouseX
            prevMouseY = mouseY

            mouseX = mc.mouse.x.toFloat() - 1.0f
            mouseY = mc.window.height - mc.mouse.y.toFloat() - 1.0f
        }
    }

    fun render() {
        val deltaTicks = mc.tickDelta
        val width = mc.window.width.toFloat()
        val height = mc.window.height.toFloat()
        val mouseX = prevMouseX + (mouseX - prevMouseX) * deltaTicks
        val mouseY = prevMouseY + (mouseY - prevMouseY) * deltaTicks

        render(width, height, mouseX, mouseY, initTime)
    }
}
