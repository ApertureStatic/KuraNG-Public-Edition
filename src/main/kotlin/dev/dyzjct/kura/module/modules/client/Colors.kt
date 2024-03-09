package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.system.render.graphic.Render2DEngine
import java.awt.Color

object Colors :
    Module(name = "Colors", langName = "主题颜色", category = Category.CLIENT, visible = false, alwaysEnable = true) {
    private val colorMode = msetting("ColorMode", ColorMode.Static)
    private var colorSpeed = isetting("ColorSpeed", 18, 2, 54)
    private var rainbowSaturation = this.fsetting("Saturation", 0.65f, 0.0f, 1.0f)
    private var rainbowBrightness = this.fsetting("Brightness", 1.0f, 0.0f, 1.0f)
    var color = csetting("Color", Color(210, 100, 165))
    private var color2 = csetting("Color2", Color(186, 100, 210)).enumIs(colorMode, ColorMode.Analogous)
    var fadeColor = csetting("FadeColor", Color(100, 181, 210))
    var fontColor = csetting("FontColor", Color(255, 255, 255))
    var hudColor = csetting("HudColor", Color(255, 255, 255))
    var enableGuiColor = bsetting("EnableGuiColor", false)
    var enableColor = csetting("EnableColor", Color(51, 223, 255)).isTrue(enableGuiColor)
    var overwritePanel = bsetting("SetPanelColor", false)
    var panelColor = csetting("PanelColor", Color(210, 100, 165)).isTrue(overwritePanel)

    enum class ColorMode {
        Static,
        Sky,
        LightRainbow,
        Rainbow,
        Fade,
        DoubleColor,
        Analogous
    }

    fun getColor(count: Int): Color {
        return when (colorMode.value) {
            ColorMode.Sky -> {
                Render2DEngine.skyRainbow(colorSpeed.value, count)
            }

            ColorMode.LightRainbow -> {
                Render2DEngine.rainbow(
                    colorSpeed.value,
                    count,
                    rainbowSaturation.value,
                    rainbowBrightness.value,
                    1f
                )
            }

            ColorMode.Rainbow -> {
                Render2DEngine.rainbow(
                    colorSpeed.value,
                    count,
                    rainbowSaturation.value,
                    rainbowBrightness.value,
                    1f
                )
            }

            ColorMode.Fade -> {
                Render2DEngine.fade(colorSpeed.value, count, color.value, 1f)
            }

            ColorMode.DoubleColor -> {
                Render2DEngine.interpolateColorsBackAndForth(
                    colorSpeed.value, count,
                    color.value, Color(-0x1), true
                )
            }

            ColorMode.Analogous -> {
                val analogous = Render2DEngine.getAnalogousColor(color2.value)
                Render2DEngine.interpolateColorsBackAndForth(
                    colorSpeed.value,
                    count,
                    color.value,
                    analogous,
                    true
                )
            }

            else -> {
                color.value
            }
        }
    }
}
