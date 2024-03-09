package dev.dyzjct.kura.setting

import dev.dyzjct.kura.module.AbstractModule
import java.awt.Color

class ColorSetting(name: String, contain: dev.dyzjct.kura.module.AbstractModule, defaultValue: Color) : Setting<Color>(
    name, contain, defaultValue
), SettingVisibility<ColorSetting> {

    fun getColorObject(): Color {
        val color = defaultValue.rgb
        val alpha = color shr 24 and 0xff
        val red = color shr 16 and 0xFF
        val green = color shr 8 and 0xFF
        val blue = color and 0xFF
        return Color(red, green, blue, alpha)
    }
}
