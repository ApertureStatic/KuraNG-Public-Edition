package dev.dyzjct.kura.utils

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.render.VertexFormats

class BlurRenderer {
    private val client = MinecraftClient.getInstance()
    private val framebuffer: SimpleFramebuffer = SimpleFramebuffer(
        client.framebuffer.textureWidth,
        client.framebuffer.textureHeight,
        true,
        MinecraftClient.IS_SYSTEM_MAC
    )
    private var blurShader: ShaderProgram? = null

    val resourceFactory = MinecraftClient.getInstance().resourceManager
    val shaderName = "blur" // 会加载 blur.vsh 和 blur.fsh
    val vertexFormat = VertexFormats.POSITION_TEXTURE_COLOR

    init {
        loadShader()
    }

    private fun loadShader() {
        try {
            blurShader = ShaderProgram(resourceFactory, shaderName, vertexFormat)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun renderWithBlur(drawBackground: () -> Unit) {
        framebuffer.beginWrite(false)
        drawBackground()
        framebuffer.endWrite()

        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.setShader { blurShader }
        RenderSystem.setShaderTexture(0, framebuffer.colorAttachment)
        framebuffer.draw(framebuffer.textureWidth, framebuffer.textureHeight)
        RenderSystem.disableBlend()
    }

    fun close() {
        blurShader?.close()
        framebuffer.delete()
    }
}
