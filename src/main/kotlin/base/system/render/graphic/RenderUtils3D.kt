package base.system.render.graphic

import dev.dyzjct.kura.KuraIdentifier
import base.events.RunGameLoopEvent
import base.events.render.Render3DEvent
import base.graphics.shaders.DrawShader
import base.graphics.shaders.DynamicVAO
import base.graphics.use
import base.system.event.AlwaysListening
import base.system.event.safeEventListener
import base.system.render.graphic.mask.DirectionMask
import base.system.util.color.ColorRGB
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL11C.glDrawArrays
import org.lwjgl.opengl.GL32

object RenderUtils3D : AlwaysListening {
    var vertexSize = 0
    var translationX = 0.0
        private set
    var translationY = 0.0
        private set
    var translationZ = 0.0
        private set

    fun setTranslation(x: Double, y: Double, z: Double) {
        translationX = x
        translationY = y
        translationZ = z
    }

    fun resetTranslation() {
        translationX = 0.0
        translationY = 0.0
        translationZ = 0.0
    }

    fun drawBox(box: Box, color: ColorRGB, sides: Int) {
        if (sides and DirectionMask.DOWN != 0) {
            putVertex(box.minX, box.minY, box.maxZ, color)
            putVertex(box.minX, box.minY, box.minZ, color)
            putVertex(box.maxX, box.minY, box.minZ, color)
            putVertex(box.maxX, box.minY, box.maxZ, color)
        }

        if (sides and DirectionMask.UP != 0) {
            putVertex(box.minX, box.maxY, box.minZ, color)
            putVertex(box.minX, box.maxY, box.maxZ, color)
            putVertex(box.maxX, box.maxY, box.maxZ, color)
            putVertex(box.maxX, box.maxY, box.minZ, color)
        }

        if (sides and DirectionMask.NORTH != 0) {
            putVertex(box.minX, box.minY, box.minZ, color)
            putVertex(box.minX, box.maxY, box.minZ, color)
            putVertex(box.maxX, box.maxY, box.minZ, color)
            putVertex(box.maxX, box.minY, box.minZ, color)
        }

        if (sides and DirectionMask.SOUTH != 0) {
            putVertex(box.maxX, box.minY, box.maxZ, color)
            putVertex(box.maxX, box.maxY, box.maxZ, color)
            putVertex(box.minX, box.maxY, box.maxZ, color)
            putVertex(box.minX, box.minY, box.maxZ, color)
        }

        if (sides and DirectionMask.WEST != 0) {
            putVertex(box.minX, box.minY, box.maxZ, color)
            putVertex(box.minX, box.maxY, box.maxZ, color)
            putVertex(box.minX, box.maxY, box.minZ, color)
            putVertex(box.minX, box.minY, box.minZ, color)
        }

        if (sides and DirectionMask.EAST != 0) {
            putVertex(box.maxX, box.minY, box.minZ, color)
            putVertex(box.maxX, box.maxY, box.minZ, color)
            putVertex(box.maxX, box.maxY, box.maxZ, color)
            putVertex(box.maxX, box.minY, box.maxZ, color)
        }
    }

    fun drawLineTo(position: Vec3d, color: ColorRGB) {
        putVertex(camPos.x, camPos.y, camPos.z, color)
        putVertex(position.x, position.y, position.z, color)
    }

    fun drawOutline(box: Box, color: ColorRGB) {
        putVertex(box.minX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.maxY, box.maxZ, color)
        putVertex(box.maxX, box.maxY, box.maxZ, color)
        putVertex(box.minX, box.maxY, box.maxZ, color)
        putVertex(box.minX, box.maxY, box.maxZ, color)
        putVertex(box.minX, box.maxY, box.minZ, color)

        putVertex(box.minX, box.minY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.maxZ, color)
        putVertex(box.maxX, box.minY, box.maxZ, color)
        putVertex(box.minX, box.minY, box.maxZ, color)
        putVertex(box.minX, box.minY, box.maxZ, color)
        putVertex(box.minX, box.minY, box.minZ, color)

        putVertex(box.minX, box.minY, box.minZ, color)
        putVertex(box.minX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.minZ, color)
        putVertex(box.maxX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.maxZ, color)
        putVertex(box.maxX, box.maxY, box.maxZ, color)
        putVertex(box.minX, box.minY, box.maxZ, color)
        putVertex(box.minX, box.maxY, box.maxZ, color)
    }

    fun putVertex(posX: Double, posY: Double, posZ: Double, color: ColorRGB) {
        DynamicVAO.buffer.apply {
            putFloat((posX + translationX).toFloat())
            putFloat((posY + translationY).toFloat())
            putFloat((posZ + translationZ).toFloat())
            putInt(color.rgba)
        }
        vertexSize++
    }

    fun draw(mode: Int) {
        if (vertexSize == 0) return

        DynamicVAO.POS3_COLOR.upload(vertexSize)

//        NiggerShader.use {
        DynamicVAO.POS3_COLOR.useVao {
            glDrawArrays(mode, 0, vertexSize)
            //glDrawElements(mode, DynamicVAO.buffer)
        }
//        }

        vertexSize = 0
    }

    object NiggerShader : DrawShader(
        KuraIdentifier("shaders/pos3color.vsh"), KuraIdentifier("shaders/pos3color.fsh")
    ) {

        fun drawBox(stack: MatrixStack, box: Box, color: ColorRGB, sides: Int) = use {
            updateMatrix()
            Tessellator.getInstance().buffer.apply {
                begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
                if (sides and DirectionMask.DOWN != 0) {
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                }

                if (sides and DirectionMask.UP != 0) {
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                }

                if (sides and DirectionMask.NORTH != 0) {
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                }

                if (sides and DirectionMask.SOUTH != 0) {
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                }

                if (sides and DirectionMask.WEST != 0) {
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                }

                if (sides and DirectionMask.EAST != 0) {
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                    vertex(
                        stack.peek().positionMatrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()
                    ).color(color.rgba).next()
                }
                BufferRenderer.draw(end())
            }
        }

    }

    fun prepareGL(matrix: MatrixStack) {
        matrix.push()
        RenderSystem.lineWidth(1f)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL32.GL_DEPTH_CLAMP)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        //GlStateManager.shadeModel(GL_SMOOTH)
        GlStateManager._enableBlend()
        GlStateManager._depthMask(false)
    }

    fun releaseGL(matrix: MatrixStack) {
        GlStateManager._enableDepthTest()
        GlStateManager._disableBlend()
        GlStateManager._depthMask(true)
        glDisable(GL32.GL_DEPTH_CLAMP)
        glDisable(GL_LINE_SMOOTH)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.lineWidth(1f)
        matrix.pop()
    }

    @JvmStatic
    var partialTicks = 0.0f
        private set
    var camPos: Vec3d = Vec3d.ZERO
        private set

    init {
        safeEventListener<RunGameLoopEvent.Tick> {
            partialTicks = mc.tickDelta
        }

        safeEventListener<Render3DEvent> {
            setTranslation(
                -mc.entityRenderDispatcher.camera.pos.x,
                -mc.entityRenderDispatcher.camera.pos.y,
                -mc.entityRenderDispatcher.camera.pos.z
            )
        }

        safeEventListener<Render3DEvent>(Int.MAX_VALUE, true) {
            val entity = mc.cameraEntity ?: player
            val ticks = partialTicks
            val x = entity.lastRenderX + (entity.x - entity.lastRenderX) * ticks
            val y = entity.lastRenderY + (entity.y - entity.lastRenderY) * ticks
            val z = entity.lastRenderZ + (entity.z - entity.lastRenderZ) * ticks
            val camOffset = mc.cameraEntity?.pos ?: return@safeEventListener

            camPos = Vec3d(x + camOffset.x, y + camOffset.y, z + camOffset.z)
        }
    }
}