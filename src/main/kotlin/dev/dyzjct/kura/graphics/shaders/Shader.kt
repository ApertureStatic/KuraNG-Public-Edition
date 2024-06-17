package dev.dyzjct.kura.graphics.shaders

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.system.util.color.ColorRGB
import dev.dyzjct.kura.system.util.io.readText
import base.utils.interfaces.Helper
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import dev.dyzjct.kura.KuraIdentifier
import dev.dyzjct.kura.graphics.GLObject

open class Shader(vertShaderId: KuraIdentifier, fragShaderId: KuraIdentifier) : GLObject, Helper {
    private val matrixBuffer = BufferUtils.createFloatBuffer(4 * 4)
    private val uniformLocations = HashMap<String, Int>()
    final override val id: Int

    init {
        RenderSystem.assertOnRenderThread()
        val vertexShaderID = createShader(vertShaderId, GL_VERTEX_SHADER)
        val fragShaderID = createShader(fragShaderId, GL_FRAGMENT_SHADER)
        val id = glCreateProgram()

        glAttachShader(id, vertexShaderID)
        glAttachShader(id, fragShaderID)

        glLinkProgram(id)
        val linked = glGetProgrami(id, GL_LINK_STATUS)
        if (linked == 0) {
            Kura.logger.error(glGetProgramInfoLog(id, 1024))
            glDeleteProgram(id)
            throw IllegalStateException("Shader failed to link")
        }
        this.id = id

        glDetachShader(id, vertexShaderID)
        glDetachShader(id, fragShaderID)
        glDeleteShader(vertexShaderID)
        glDeleteShader(fragShaderID)
    }

    private fun createShader(id: KuraIdentifier, shaderType: Int): Int {
        RenderSystem.assertOnRenderThread()
        val srcString = mc.resourceManager.getResource(id).get().inputStream.readText()
        val shaderId = glCreateShader(shaderType)

        glShaderSource(shaderId, srcString)
        glCompileShader(shaderId)

        val compiled = glGetShaderi(shaderId, GL_COMPILE_STATUS)
        if (compiled == 0) {
            Kura.logger.error(glGetShaderInfoLog(shaderId, 1024))
            glDeleteShader(shaderId)
            throw IllegalStateException("Failed to compile shader: $id")
        }

        return shaderId
    }

    override fun bind() {
        RenderSystem.assertOnRenderThread()
        glUseProgram(id)
    }

    override fun unbind() {
        RenderSystem.assertOnRenderThread()
        glUseProgram(0)
    }

    override fun destroy() {
        RenderSystem.assertOnRenderThread()
        glDeleteProgram(id)
    }

    private fun getLocation(name: String): Int {
        if (uniformLocations.containsKey(name)) return uniformLocations[name]!!

        val location = GlStateManager._glGetUniformLocation(id, name)
        uniformLocations[name] = location
        return location
    }

    fun set(name: String, v: Boolean) {
        GlStateManager._glUniform1i(getLocation(name), if (v) GL_TRUE else GL_FALSE)
    }

    fun set(name: String, v: Int) {
        GlStateManager._glUniform1i(getLocation(name), v)
    }

    fun set(name: String, v: Double) {
        glUniform1f(getLocation(name), v.toFloat())
    }

    fun set(name: String, v1: Double, v2: Double) {
        glUniform2f(getLocation(name), v1.toFloat(), v2.toFloat())
    }

    fun set(name: String, color: ColorRGB) {
        glUniform4i(getLocation(name), color.r, color.g, color.b, color.a)
    }

    fun set(name: String, mat: Matrix4f) {
        mat.set(matrixBuffer)
        GlStateManager._glUniformMatrix4(getLocation(name), false, matrixBuffer)
    }
}