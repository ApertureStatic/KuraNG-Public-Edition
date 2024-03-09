package dev.dyzjct.kura.utils.delegate

import kotlin.reflect.KProperty

class RecordValue<T>(var value0: T) {
    var prevValue = value0

    fun rollBack() {
        value0 = prevValue
    }

    fun record() {
        prevValue = value0
    }


    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value0
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value0 = value
    }
}