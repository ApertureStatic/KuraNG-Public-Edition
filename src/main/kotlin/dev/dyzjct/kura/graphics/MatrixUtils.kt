package dev.dyzjct.kura.graphics

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.util.GlAllocationUtils
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL41
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object MatrixUtils {
    // 修改第 15 行
// 分配 16 * 4 = 64 字节，或者直接写 64
    val matrixBuffer: FloatBuffer = GlAllocationUtils.allocateByteBuffer(16 * java.lang.Float.BYTES).asFloatBuffer()

    fun loadProjectionMatrix(): MatrixUtils {
        RenderSystem.assertOnRenderThread()
        (matrixBuffer as Buffer).clear()
        glGetFloatv(GL_PROJECTION_MATRIX, matrixBuffer)
        return this
    }

    fun loadModelViewMatrix(): MatrixUtils {
        RenderSystem.assertOnRenderThread()
        (matrixBuffer as Buffer).clear()
        glGetFloatv(GL_MODELVIEW_MATRIX, matrixBuffer)
        return this
    }

    fun loadMatrix(matrix: Matrix4f): MatrixUtils {
        RenderSystem.assertOnRenderThread()
        matrix.get(matrixBuffer)
        return this
    }

    fun getMatrix(): Matrix4f {
        RenderSystem.assertOnRenderThread()
        return Matrix4f(matrixBuffer)
    }

    fun getMatrix(matrix: Matrix4f): Matrix4f {
        RenderSystem.assertOnRenderThread()
        (matrixBuffer as Buffer).rewind() // 确保从缓冲区开头开始读取
        matrix.set(matrixBuffer)
        return matrix
    }

    fun uploadMatrix(location: Int) {
        RenderSystem.assertOnRenderThread()
        RenderSystem.glUniformMatrix4(location, false, matrixBuffer)
    }

    fun uploadMatrix(id: Int, location: Int) {
        RenderSystem.assertOnRenderThread()
        GL41.glProgramUniformMatrix4fv(id, location, false, matrixBuffer)
    }
}