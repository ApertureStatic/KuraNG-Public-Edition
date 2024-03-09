package dev.dyzjct.kura.setting

import dev.dyzjct.kura.module.AbstractModule

class DoubleSetting(
    name: String,
    contain: dev.dyzjct.kura.module.AbstractModule,
    defaultValue: Double,
    override var min: Double,
    override var max: Double,
    var modify: Double = 0.0
) : NumberSetting<Double>(name, contain, defaultValue), SettingVisibility<DoubleSetting> {
    override fun setValueFromString(value: Float) {
        this.value = value.toDouble().coerceIn(min, max)
    }
}