package team.exception.melon.graphics.shaders.impl

import com.mojang.blaze3d.platform.GlConst
import com.mojang.blaze3d.platform.GlStateManager
import base.events.screen.ResolutionUpdateEvent
import base.system.event.AlwaysListening
import base.system.event.listener
import base.system.util.interfaces.MinecraftWrapper
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import team.exception.melon.MelonIdentifier
import team.exception.melon.graphics.MatrixUtils
import team.exception.melon.graphics.shaders.DrawShader
import team.exception.melon.graphics.use

object WindowBlurShader : AlwaysListening, MinecraftWrapper {
    private val pass1 = Pass(MelonIdentifier("shaders/window/windowblurh.vsh"))
    private val pass2 = Pass(MelonIdentifier("shaders/window/windowblurv.vsh"))

    init {
        updateResolution(mc.window.width, mc.window.height)

        listener<ResolutionUpdateEvent>(true) { updateResolution(it.width, it.height) }
    }

    private fun updateResolution(width: Int, height: Int) {
        if (width < 0 || height < 0) return
        pass1.bind()
        pass1.updateResolution(width.toFloat(), height.toFloat())
        pass2.bind()
        pass2.updateResolution(width.toFloat(), height.toFloat())
        mc.framebuffer.resize(width, height, true)
    }

    fun render(width: Double, height: Double) {
        render(0.0, 0.0, width, height)
    }

    fun render(x1: Double, y1: Double, x2: Double, y2: Double) {
        val buffer = Tessellator.getInstance().buffer

        GlStateManager._activeTexture(GlConst.GL_TEXTURE0)
        GlStateManager._disableDepthTest()
        GlStateManager._depthMask(false)

        mc.framebuffer.beginRead()
        mc.framebuffer.beginWrite(false)
        GlStateManager._enableBlend()

        pass1.bind()
        pass1.updateMatrix()

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
        buffer.vertex(x1, x1, 1.0).next()
        buffer.vertex(x1, y2, -1.0).next()
        buffer.vertex(x2, y2, -1.0).next()
        buffer.vertex(x2, y1, 1.0).next()
        BufferRenderer.draw(buffer.end())

        mc.framebuffer.beginRead()
        mc.framebuffer.beginWrite(false)

        pass2.bind()
        pass2.updateMatrix()

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
        buffer.vertex(x1, y1, -1.0).next()
        buffer.vertex(x1, y2, -1.0).next()
        buffer.vertex(x2, y2, 1.0).next()
        buffer.vertex(x2, y1, 1.0).next()
        BufferRenderer.draw(buffer.end())

        mc.framebuffer.endRead()
        mc.framebuffer.beginWrite(false)
        GlStateManager._enableBlend()
    }

    private open class Pass(vertShaderPath: MelonIdentifier) :
        DrawShader(vertShaderPath, MelonIdentifier("shaders/window/windowblur.fsh")) {
        val reverseProjectionUniform = glGetUniformLocation(id, "reverseProjection")
        val resolutionUniform = glGetUniformLocation(id, "resolution")

        init {
            use {
                updateResolution(mc.window.width.toFloat(), mc.window.height.toFloat())
                glUniform1i(glGetUniformLocation(id, "background"), 1)
            }
        }

        fun updateResolution(width: Float, height: Float) {
            glUniform2f(resolutionUniform, width, height)

            val matrix = Matrix4f().ortho(0.0f, width, 0.0f, height, 1000.0f, 3000.0f).invert()

            MatrixUtils.loadMatrix(matrix).uploadMatrix(reverseProjectionUniform)
        }
    }
}
