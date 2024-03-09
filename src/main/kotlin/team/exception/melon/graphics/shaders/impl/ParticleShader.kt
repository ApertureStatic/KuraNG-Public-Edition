package team.exception.melon.graphics.shaders.impl

import melon.events.TickEvent
import melon.system.event.AlwaysListening
import melon.system.event.listener
import team.exception.melon.MelonIdentifier
import team.exception.melon.graphics.shaders.GLSLSandbox

object ParticleShader : GLSLSandbox(MelonIdentifier("shaders/particle.fsh")), AlwaysListening {
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
