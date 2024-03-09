package melon.utils.graphics.color

import java.awt.Color

object RainbowColor {
    fun getRainbow(speed: Float, saturation: Float, brightness: Float): Int {
        val hue = (System.currentTimeMillis() % 11520L).toFloat() / 11520.0f * speed
        return Color.HSBtoRGB(hue, saturation, brightness)
    }

    fun getRainbowColor(speed: Float, saturation: Float, brightness: Float): Color {
        return Color(getRainbow(speed, saturation, brightness))
    }

    fun getRainbowColor(speed: Float, saturation: Float, brightness: Float, add: Long): Color {
        return Color(getRainbow(speed, saturation, brightness, add))
    }

    fun getRainbow(speed: Float, saturation: Float, brightness: Float, add: Long): Int {
        val hue = ((System.currentTimeMillis() + add) % 11520L).toFloat() / 11520.0f * speed
        return Color.HSBtoRGB(hue, saturation, brightness)
    }
}
