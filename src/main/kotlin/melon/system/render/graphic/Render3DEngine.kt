package melon.system.render.graphic

import com.mojang.blaze3d.systems.RenderSystem
import melon.events.render.Render3DEvent
import melon.system.event.AlwaysListening
import melon.system.event.listener
import melon.system.render.newfont.FontRenderers
import melon.system.util.interfaces.MinecraftWrapper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.glUniform3f
import org.lwjgl.opengl.GL20.glUniform4f
import team.exception.melon.MelonIdentifier
import team.exception.melon.graphics.MatrixUtils
import team.exception.melon.graphics.shaders.DrawShader
import team.exception.melon.graphics.use
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object Render3DEngine : MinecraftWrapper {
    var lastProjMat = Matrix4f()
    var lastModMat = Matrix4f()
    var lastWorldSpaceMatrix = Matrix4f()
    fun drawFilledBox(stack: MatrixStack, box: Box, c: Color) {
        val minX = (box.minX - mc.entityRenderDispatcher.camera.pos.getX()).toFloat()
        val minY = (box.minY - mc.entityRenderDispatcher.camera.pos.getY()).toFloat()
        val minZ = (box.minZ - mc.entityRenderDispatcher.camera.pos.getZ()).toFloat()
        val maxX = (box.maxX - mc.entityRenderDispatcher.camera.pos.getX()).toFloat()
        val maxY = (box.maxY - mc.entityRenderDispatcher.camera.pos.getY()).toFloat()
        val maxZ = (box.maxZ - mc.entityRenderDispatcher.camera.pos.getZ()).toFloat()
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, minZ).color(c.rgb).next()
        RenderSystem.disableDepthTest()
        //tessellator.draw()
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        RenderSystem.disableBlend()
        //   cleanup();
    }

    fun drawTextIn3D(text: String, pos: Vec3d, offX: Double, offY: Double, textOffset: Double, color: Color) {
        RenderSystem.disableDepthTest()
        val matrices = MatrixStack()
        val camera: Camera = mc.gameRenderer.camera
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.yaw + 180.0f))
        matrices.translate(pos.getX() - camera.pos.x, pos.getY() - camera.pos.y, pos.getZ() - camera.pos.z)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.yaw))
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        matrices.translate(offX, offY - 0.1, -0.01)
        matrices.scale(-0.025f, -0.025f, 0f)
        val immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().buffer)
        FontRenderers.default.drawCenteredString(matrices, text, textOffset, 0.0, color.rgb)
        immediate.draw()
        RenderSystem.disableBlend()
        RenderSystem.enableDepthTest()
    }

    fun worldSpaceToScreenSpace(pos: Vec3d): Vec3d {
        val camera: Camera = mc.entityRenderDispatcher.camera
        val displayHeight: Int = mc.window.height
        val viewport = IntArray(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        val target = Vector3f()
        val deltaX = pos.x - camera.pos.x
        val deltaY = pos.y - camera.pos.y
        val deltaZ = pos.z - camera.pos.z
        val transformedCoordinates = Vector4f(deltaX.toFloat(), deltaY.toFloat(), deltaZ.toFloat(), 1f).mul(
            lastWorldSpaceMatrix
        )
        val matrixProj = Matrix4f(lastProjMat)
        val matrixModel = Matrix4f(lastModMat)
        matrixProj.mul(matrixModel).project(
            transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target
        )
        return Vec3d(
            target.x / mc.window.scaleFactor, (displayHeight - target.y) / mc.window.scaleFactor, target.z.toDouble()
        )
    }

    fun drawFilledFadeBox(stack: MatrixStack, box: Box, c: Color, c1: Color) {
        val minX = (box.minX - mc.entityRenderDispatcher.camera.pos.getX()).toFloat()
        val minY = (box.minY - mc.entityRenderDispatcher.camera.pos.getY()).toFloat()
        val minZ = (box.minZ - mc.entityRenderDispatcher.camera.pos.getZ()).toFloat()
        val maxX = (box.maxX - mc.entityRenderDispatcher.camera.pos.getX()).toFloat()
        val maxY = (box.maxY - mc.entityRenderDispatcher.camera.pos.getY()).toFloat()
        val maxZ = (box.maxZ - mc.entityRenderDispatcher.camera.pos.getZ()).toFloat()
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, minZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, minZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, minZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, maxZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, maxZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, maxZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, minZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, minY, maxZ).color(c.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, maxZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, minZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, minZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, minX, maxY, maxZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, maxZ).color(c1.rgb).next()
        bufferBuilder.vertex(stack.peek().positionMatrix, maxX, maxY, minZ).color(c1.rgb).next()
        RenderSystem.disableDepthTest()
        tessellator.draw()
        RenderSystem.disableBlend()
        //   cleanup();
    }

    fun drawBoxOutline(box0: Box, color: Color, lineWidth: Float) {
        var box = box0
        setup()
        val matrices = matrixFrom(box.minX, box.minY, box.minZ)
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        RenderSystem.disableCull()
        RenderSystem.setShader { GameRenderer.getRenderTypeLinesProgram() }
        RenderSystem.lineWidth(lineWidth)
        buffer.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES)
        box = box.offset(Vec3d(box.minX, box.minY, box.minZ).negate())
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()
        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color)
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color)
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color)
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color)
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color)
        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color)
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color)
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color)
        vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color)
        vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color)
        vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color)
        vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color)
        tessellator.draw()
        RenderSystem.enableCull()
        cleanup()
    }

    fun drawBottomOutline(box0: Box, color: Color, lineWidth: Float) {
        var box = box0
        setup()
        val matrices = matrixFrom(box.minX, box.minY, box.minZ)
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        RenderSystem.disableCull()
        RenderSystem.setShader { GameRenderer.getRenderTypeLinesProgram() }
        RenderSystem.lineWidth(lineWidth)
        buffer.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES)
        box = box.offset(Vec3d(box.minX, box.minY, box.minZ).negate())
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val z2 = box.maxZ.toFloat()
        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color)
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color)
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color)
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color)
        tessellator.draw()
        RenderSystem.enableCull()
        cleanup()
    }

    fun vertexLine(
        matrices: MatrixStack,
        vertexConsumer: VertexConsumer,
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        lineColor: Color
    ) {
        val model = matrices.peek().positionMatrix
        val normal = matrices.peek().normalMatrix
        val normalVec = getNormal(x1, y1, z1, x2, y2, z2)
        vertexConsumer.vertex(model, x1, y1, z1).color(lineColor.red, lineColor.green, lineColor.blue, lineColor.alpha)
            .normal(normal, normalVec.x(), normalVec.y(), normalVec.z()).next()
        vertexConsumer.vertex(model, x2, y2, z2).color(lineColor.red, lineColor.green, lineColor.blue, lineColor.alpha)
            .normal(normal, normalVec.x(), normalVec.y(), normalVec.z()).next()
    }

    fun getNormal(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Vector3f {
        val xNormal = x2 - x1
        val yNormal = y2 - y1
        val zNormal = z2 - z1
        val normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal)
        return Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt)
    }

    fun matrixFrom(x: Double, y: Double, z: Double): MatrixStack {
        val matrices = MatrixStack()
        val camera = MinecraftClient.getInstance().gameRenderer.camera
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.yaw + 180.0f))
        matrices.translate(x - camera.pos.x, y - camera.pos.y, z - camera.pos.z)
        return matrices
    }

    fun setup() {
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
    }

    fun cleanup() {
        RenderSystem.disableBlend()
    }

    fun drawSphere(matrix: MatrixStack, entity: Entity, radius: Float, height: Float, lineWidth: Float, color: Color) {
        setup()
        val x = entity.lastRenderX + (entity.x - entity.lastRenderX) * mc.tickDelta - mc.gameRenderer.camera.pos.x
        val y = entity.lastRenderY + (entity.y - entity.lastRenderY) * mc.tickDelta - mc.gameRenderer.camera.pos.y
        val z = entity.lastRenderZ + (entity.z - entity.lastRenderZ) * mc.tickDelta - mc.gameRenderer.camera.pos.z
        val pix2 = Math.PI * 2.0
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        RenderSystem.lineWidth(lineWidth)
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)
        for (i in 0..180) {
            bufferBuilder.vertex(
                matrix.peek().positionMatrix,
                (x + radius * cos(i * pix2 / 45)).toFloat(),
                (y + height).toFloat(),
                (z + radius * sin(i * pix2 / 45)).toFloat()
            ).color(color.red, color.green, color.blue, color.alpha).next()
        }
        tessellator.draw()
        cleanup()
    }
}
