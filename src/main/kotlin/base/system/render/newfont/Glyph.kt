package base.system.render.newfont

import dev.dyzjct.kura.Kura
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import org.lwjgl.BufferUtils
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.ceil

class Glyph(val char: Char, val font: Font, fallbackFont: Font) {
    val imageTex =
        Texture("${Kura.DIRECTORY}/glyphs/${font.name.lowercase(Locale.getDefault()).hashCode()}-${char.code}")

    private val finalFont = if (font.canDisplay(char.code)) font else fallbackFont
    val dimensions: Rectangle2D =
        finalFont.getStringBounds(char.toString(), FontRenderContext(AffineTransform(), true, true))

    init {
        generateTexture()
    }

    private fun generateTexture() {
        val bufferedImage =
            BufferedImage(ceil(dimensions.width).toInt(), ceil(dimensions.height).toInt(), BufferedImage.TYPE_INT_ARGB)
        bufferedImage.createGraphics().also {
            it.font = finalFont
            // Set the image background to transparent
            it.color = Color(255, 255, 255, 0)
            it.fillRect(0, 0, bufferedImage.width, bufferedImage.height)
            // Char Render
            it.color = Color.white
            it.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            it.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            it.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            it.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            it.drawString(char.toString(), 0, it.fontMetrics.ascent)
            it.dispose()
        }
        registerBufferedImageTexture(imageTex, bufferedImage)
    }

    companion object {
        private var mc = MinecraftClient.getInstance()
        fun registerBufferedImageTexture(identifier: Identifier, image: BufferedImage) {
            try {
                val bytes = ByteArrayOutputStream().use {
                    ImageIO.write(image, "png", it)
                    it.toByteArray()
                }
                registerTexture(identifier, bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun registerTexture(identifier: Identifier, bytes: ByteArray) {
            try {
                val data = BufferUtils.createByteBuffer(bytes.size).put(bytes).also { it.flip() }
                val tex = NativeImageBackedTexture(NativeImage.read(data))
                mc.execute { mc.textureManager.registerTexture(identifier, tex) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
