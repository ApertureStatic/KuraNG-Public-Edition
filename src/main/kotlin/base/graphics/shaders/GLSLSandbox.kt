package base.graphics.shaders

import dev.dyzjct.kura.KuraIdentifier
import base.graphics.use
import base.system.render.graphic.Render2DEngine
import dev.dyzjct.kura.module.modules.client.UiSetting
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import org.lwjgl.opengl.GL20.*

open class GLSLSandbox(fragShaderId: KuraIdentifier) :
    Shader(KuraIdentifier("shaders/defaultvertex.vsh"), fragShaderId) {
    private val timeUniform = glGetUniformLocation(id, "time")
    private val mouseUniform = glGetUniformLocation(id, "mouse")
    private val resolutionUniform = glGetUniformLocation(id, "resolution")
    private val red = glGetUniformLocation(id, "red")
    private val green = glGetUniformLocation(id, "green")
    private val blue = glGetUniformLocation(id, "blue")

    fun render(width: Float, height: Float, mouseX: Float, mouseY: Float, initTime: Long) {
        use {
            glUniform2f(resolutionUniform, width, height)
            glUniform2f(mouseUniform, mouseX / width, (height - 1.0f - mouseY) / height)
            glUniform1f(timeUniform, ((System.currentTimeMillis() - initTime) / 1000.0).toFloat())
            if (UiSetting.getThemeSetting().pRainbow) {
                val rainbowColor = Render2DEngine.astolfo(true, 0)
                glUniform1f(red, rainbowColor.red / 255f)
                glUniform1f(green, rainbowColor.green / 255f)
                glUniform1f(blue, rainbowColor.blue / 255f)
            } else {
                glUniform1f(red, UiSetting.getThemeSetting().pColor.red / 255f)
                glUniform1f(green, UiSetting.getThemeSetting().pColor.green / 255f)
                glUniform1f(blue, UiSetting.getThemeSetting().pColor.blue / 255f)
            }

            Tessellator.getInstance().buffer.apply {
                begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
                vertex(-1.0, -1.0, 0.0).next()
                vertex(1.0, -1.0, 0.0).next()
                vertex(1.0, 1.0, 0.0).next()
                vertex(-1.0, 1.0, 0.0).next()
                BufferRenderer.draw(end())
            }
        }
    }
}