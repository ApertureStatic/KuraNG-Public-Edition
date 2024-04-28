package dev.dyzjct.kura.setting

import dev.dyzjct.kura.utils.animations.MathUtils

abstract class NumberSetting<T : Number>(
    name: String,
    contain: dev.dyzjct.kura.module.AbstractModule?,
    defaultValue: T,
) : Setting<T>(name, contain, defaultValue) {
    abstract var max: T
    abstract var min: T

    var percent: Float
        get() = ((value.toFloat() - min.toFloat()) / (max.toFloat() - min.toFloat())).coerceIn(0f, 1f)
        set(percent) {
            val value = ((max.toFloat() - min.toFloat()) * percent.coerceIn(0f, 1f)) + min.toFloat()
            setValueFromString(MathUtils.round(value, 2))
        }

    abstract fun setValueFromString(value: Float)
}