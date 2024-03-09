package team.exception.melon.graphics.shaders.impl

import melon.events.render.Render3DEvent
import melon.system.event.AlwaysListening
import melon.system.event.listener
import melon.system.render.graphic.Render3DEngine
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Box
import org.lwjgl.opengl.GL20
import team.exception.melon.MelonIdentifier
import team.exception.melon.graphics.MatrixUtils
import team.exception.melon.graphics.shaders.DrawShader
import team.exception.melon.graphics.use
import java.awt.Color

object FadeBoxShader : DrawShader(MelonIdentifier("shaders/pos3color.vsh"), MelonIdentifier("shaders/fadebox.frag")),
    AlwaysListening {
    init {
        listener<Render3DEvent>(Int.MAX_VALUE - 1, true) {
            bind()
            updateProjectionMatrix()
        }
    }

    fun translate(xOffset: Double, yOffset: Double, zOffset: Double) {
        val x = xOffset - mc.entityRenderDispatcher.camera.pos.x
        val y = yOffset - mc.entityRenderDispatcher.camera.pos.y
        val z = zOffset - mc.entityRenderDispatcher.camera.pos.z

        val modelView = MatrixUtils.loadModelViewMatrix().getMatrix().translate(x.toFloat(), y.toFloat(), z.toFloat())

        updateModelViewMatrix(modelView)
    }
}

fun drawFilledFadeBox(stack: MatrixStack, box: Box, c: Color, c1: Color) {
    FadeBoxShader.use {
        translate(
            Render3DEngine.mc.entityRenderDispatcher.camera.pos.x,
            Render3DEngine.mc.entityRenderDispatcher.camera.pos.y,
            Render3DEngine.mc.entityRenderDispatcher.camera.pos.z
        )
        val minX = (box.minX - Render3DEngine.mc.entityRenderDispatcher.camera.pos.getX()).toFloat()
        val minY = (box.minY - Render3DEngine.mc.entityRenderDispatcher.camera.pos.getY()).toFloat()
        val minZ = (box.minZ - Render3DEngine.mc.entityRenderDispatcher.camera.pos.getZ()).toFloat()
        val maxX = (box.maxX - Render3DEngine.mc.entityRenderDispatcher.camera.pos.getX()).toFloat()
        val maxY = (box.maxY - Render3DEngine.mc.entityRenderDispatcher.camera.pos.getY()).toFloat()
        val maxZ = (box.maxZ - Render3DEngine.mc.entityRenderDispatcher.camera.pos.getZ()).toFloat()
        GL20.glUniform3f(GL20.glGetUniformLocation(id, "minBox"), minX, minY, minZ)
        GL20.glUniform3f(GL20.glGetUniformLocation(id, "maxBox"), maxX, maxY, maxZ)
        GL20.glUniform4f(
            GL20.glGetUniformLocation(id, "color1"), c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f
        )
        GL20.glUniform4f(
            GL20.glGetUniformLocation(id, "color2"), c1.red / 255f, c1.green / 255f, c1.blue / 255f, c1.alpha / 255f
        )
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