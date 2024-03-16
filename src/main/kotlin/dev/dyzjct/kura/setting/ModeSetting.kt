package dev.dyzjct.kura.setting

import dev.dyzjct.kura.module.AbstractModule

class ModeSetting<T : Enum<*>>(modeName: String, contain: AbstractModule, clazz: T) :
    Setting<T>(modeName, contain, clazz),
    SettingVisibility<ModeSetting<T>> {

    override var value: T
        get() = super.value
        set(value) {
            super.value = value
            indexValue = getIndexMode(value)
        }

    var indexValue: Int = getIndexMode(clazz)
        private set

    val modes: Array<T> get() = value::class.java.enumConstants as Array<T>

    val modesAsStrings: Array<String> get() = modes.map { it.toString() }.toTypedArray()

    fun setValueByString(str: String) {
        value = java.lang.Enum.valueOf(value::class.java, str) as T
    }

    fun setValueByIndex(index: Int) {
        val id = 0.coerceAtLeast((modes.size - 1).coerceAtMost(index))
        indexValue = id
        value = modes[id]
    }

    fun forwardLoop() {
        indexValue = if (indexValue < modes.size - 1) ++indexValue else 0
        value = modes[indexValue]
    }

    fun getIndexMode(clazz: T): Int {
        return modes.indexOf(clazz)
    }

    fun currentEnumName(): String {
        return getProperName(value)
    }

    fun getModes(): Array<String> {
        return getNames(value)
    }

    private fun getProperName(clazz: Enum<*>): String {
        return clazz.name
    }

    private fun getNames(clazz: Enum<*>): Array<String> {
        return clazz::class.java.enumConstants.map { it.name }.toTypedArray()
    }

    val valueAsString: String get() = value.toString()
}