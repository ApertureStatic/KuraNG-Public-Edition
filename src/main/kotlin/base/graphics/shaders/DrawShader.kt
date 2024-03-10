package base.graphics.shaders

import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL41.glProgramUniformMatrix4fv
import base.KuraIdentifier
import base.graphics.MatrixUtils
import java.nio.FloatBuffer

open class DrawShader(vertShaderPath: KuraIdentifier, fragShaderPath: KuraIdentifier) : Shader(vertShaderPath, fragShaderPath) {
    private val projectionUniform = glGetUniformLocation(id, "projection")
    private val modelViewUniform = glGetUniformLocation(id, "modelView")

    fun updateMatrix() {
        RenderSystem.assertOnRenderThread()
        //set("projection", RenderSystem.getProjectionMatrix())
        //set("modelView", RenderSystem.getModelViewStack().peek().positionMatrix)
        updateModelViewMatrix()
        updateProjectionMatrix()
    }

    fun updateProjectionMatrix() {
        MatrixUtils.loadProjectionMatrix().uploadMatrix(id, projectionUniform)
    }

    fun updateProjectionMatrix(matrix: Matrix4f) {
        MatrixUtils.loadMatrix(matrix).uploadMatrix(id, projectionUniform)
    }

    fun uploadProjectionMatrix(buffer: FloatBuffer) {
        glProgramUniformMatrix4fv(id, projectionUniform, false, buffer)
    }

    fun updateModelViewMatrix() {
        MatrixUtils.loadModelViewMatrix().uploadMatrix(id, modelViewUniform)
    }

    fun updateModelViewMatrix(matrix: Matrix4f) {
        MatrixUtils.loadMatrix(matrix).uploadMatrix(id, modelViewUniform)
    }

    fun uploadModelViewMatrix(buffer: FloatBuffer) {
        glProgramUniformMatrix4fv(id, modelViewUniform, false, buffer)
    }
}