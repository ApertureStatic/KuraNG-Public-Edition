package dev.dyzjct.kura.setting

import dev.dyzjct.kura.module.AbstractModule
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Setting<T: Any>(val name: String, var contain: dev.dyzjct.kura.module.AbstractModule?, val defaultValue: T) : ReadWriteProperty<Any, T>,
    ISettingVisibility {

    val visibility = ArrayList<() -> Boolean>()
    val consumers = ArrayList<(prev: T, input: T) -> T>()
    val valueListeners = ArrayList<(prev: T, input: T) -> Unit>()

    open var value: T = defaultValue
        set(value) {
            if (value != field) {
                val prev = field
                var new = value
                if (consumers.isNotEmpty()) {
                    for (index in consumers.size - 1 downTo 0) {
                        new = consumers[index](prev, new)
                    }
                }
                field = new
                valueListeners.forEach { it(prev, field) }
            }
        }

    fun <S: Setting<out Any>> addConsumer(block: (prev: T, input: T) -> T): S {
        consumers.add(block)
        return this as S
    }

    fun <S: Setting<out Any>> onChange(block: (prev: T, input: T) -> Unit): S {
        valueListeners.add(block)
        return this as S
    }

    fun <S: Setting<out Any>> onChange(block: (input: T) -> Unit): S {
        valueListeners.add{ _, it -> block(it) }
        return this as S
    }

    override fun <S: Setting<out Any>> addVisibility(visibleBlock: () -> Boolean): S {
        visibility.add(visibleBlock)
        return this as S
    }

    fun resetValue() {
        this.value = defaultValue
    }

    fun isVisible(): Boolean {
        return visibility.firstOrNull { !it() }?.let { false } ?: true
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return this.value
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }
}
