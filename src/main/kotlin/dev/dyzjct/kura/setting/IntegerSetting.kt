package dev.dyzjct.kura.setting

import dev.dyzjct.kura.module.AbstractModule

class IntegerSetting(
    name: String,
    contain: dev.dyzjct.kura.module.AbstractModule,
    defaultValue: Int,
    override var min: Int,
    override var max: Int,
    var modify: Int = 0
) : NumberSetting<Int>(name, contain, defaultValue), SettingVisibility<IntegerSetting> {
    override fun setValueFromString(value: Float) {
        this.value = value.toInt().coerceIn(min, max)
    }
}